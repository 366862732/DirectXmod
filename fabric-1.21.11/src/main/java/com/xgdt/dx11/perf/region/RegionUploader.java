package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xgdt.dx11.mixin.CompiledSectionMeshAccessor;
import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies compiled section meshes into region arenas.
 *
 * <p>The vanilla render-thread upload body is replaced at HEAD; this class
 * reimplements it for {@link ChunkSectionLayer#SOLID} and
 * {@link ChunkSectionLayer#CUTOUT}, plus translucent terrain when
 * {@code translucentRegion} is enabled.  Translucent slices also keep a sorted
 * per-section index buffer so a mixed/fallback list still renders in vanilla
 * order.  Tripwire keeps vanilla buffers.  Every {@link MeshData} is closed
 * exactly once, whether the region path or the vanilla fallback ran.</p>
 *
 * <p>When region draws are enabled the vertex positions are rebased from the
 * section origin to the region origin, so several sections can share one
 * region uniform and be drawn as contiguous runs.  Translucent slices store
 * region-local quad centroids for the per-region sort.</p>
 */
public final class RegionUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final long WARN_INTERVAL_NANOS = 10_000_000_000L;
    private static final Supplier<String> TRANSLUCENT_INDEX_NAME =
        () -> "dx11 translucent section indices";
    private static final ThreadLocal<ByteBuffer> REBASE_SCRATCH =
        ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder()));
    private static long lastWarnNanos;

    private RegionUploader() {
    }

    /**
     * @return {@code true} when this method took over the whole upload body,
     *         {@code false} to let vanilla run unchanged
     */
    public static boolean tryUpload(
        BlockPos origin,
        long sectionNode,
        Map<ChunkSectionLayer, MeshData> meshes,
        CompiledSectionMesh mesh) {
        if (meshes.isEmpty()) {
            return false;
        }

        PerfConfig config = PerfState.config();
        if (!PerfState.regionActive() || !config.regionUpload) {
            // Vanilla will take over.  It may reuse an existing buffer in
            // place through writeToBuffer, so region-backed layers must be
            // detached before the vanilla body runs.
            detachRegionBuffers(mesh, meshes.keySet());
            return false;
        }

        RegionSize size = config.regionSize();
        RegionPos regionPos = RegionPos.fromSection(
            origin.getX() >> 4,
            origin.getY() >> 4,
            origin.getZ() >> 4,
            size);
        Region region = RegionManager.getOrCreate(regionPos, size);

        for (Map.Entry<ChunkSectionLayer, MeshData> entry : meshes.entrySet()) {
            ChunkSectionLayer layer = entry.getKey();
            MeshData data = entry.getValue();
            if (data == null) {
                continue;
            }

            boolean uploadedToRegion = false;
            boolean vanillaAttempted = false;
            try {
                if (isOpaque(layer)) {
                    uploadedToRegion = uploadOpaque(region, layer, data, mesh, origin);
                } else if (isTranslucentRegion(layer, config)) {
                    uploadedToRegion = uploadTranslucent(region, layer, data, mesh, origin);
                }
                if (!uploadedToRegion) {
                    vanillaAttempted = true;
                    detachRegionBuffers(mesh, layer);
                    mesh.uploadMeshLayer(layer, data, sectionNode);
                }
            } catch (RuntimeException e) {
                warn("Region upload failed for " + layer + ": " + e);
                if (!uploadedToRegion && !vanillaAttempted) {
                    try {
                        detachRegionBuffers(mesh, layer);
                        mesh.uploadMeshLayer(layer, data, sectionNode);
                    } catch (RuntimeException fallbackFailure) {
                        warn("Vanilla upload fallback also failed: " + fallbackFailure);
                    }
                }
            } finally {
                data.close();
            }
        }
        // If this was a fresh region and every layer fell back (budget or
        // unsupported mesh shape), do not park an empty region in the map.
        RegionManager.discardIfEmpty(region);
        return true;
    }

    private static boolean uploadOpaque(
        Region region,
        ChunkSectionLayer layer,
        MeshData data,
        CompiledSectionMesh mesh,
        BlockPos sectionOrigin) {
        MeshData.DrawState draw = data.drawState();
        VertexFormat format = draw.format();
        // Region submission uses the shared sequential QUADS index buffer, so
        // only take over meshes that match that shape.  Anything else keeps
        // vanilla's per-section buffers and draw path.
        if (draw.mode() != VertexFormat.Mode.QUADS
            || draw.indexCount() <= 0
            || data.indexBuffer() != null) {
            return false;
        }
        int stride = format.getVertexSize();
        ByteBuffer vertices = data.vertexBuffer();
        int byteSize = vertices.remaining();

        if (stride <= 0 || byteSize <= 0 || byteSize % stride != 0) {
            return false;
        }

        boolean regionRelative = PerfState.config().regionDraws;
        int vertexCount = byteSize / stride;
        int positionOffset = -1;
        boolean compact = false;
        int targetStride = stride;
        int targetByteSize = byteSize;
        if (regionRelative) {
            if (vertexCount % 4 != 0 || draw.indexCount() != vertexCount / 4 * 6) {
                // Coalescing treats neighbouring slices as one vertex stream,
                // so a mesh that is not a whole number of quads cannot join a
                // run.
                return false;
            }
            positionOffset = positionOffset(format);
            if (positionOffset < 0) {
                // A custom/unknown terrain format: keep per-section buffers
                // rather than drawing it against the wrong uniform.
                return false;
            }
            compact = shouldCompact(layer, format);
            if (compact) {
                targetStride = CompactTerrainFormat.STRIDE;
                targetByteSize = vertexCount * targetStride;
            }
        }

        RegionBuffer regionBuffer = region.getBuffer(layer);
        if (regionBuffer == null) {
            return false;
        }

        long alignment = Math.max(4, targetStride);
        RegionAllocator.Allocation allocation =
            regionBuffer.allocate(targetByteSize, alignment);
        if (allocation == null) {
            return false;
        }
        if (allocation.offset() % targetStride != 0) {
            // DrawIndexedInstanced addresses vertices in whole strides; a
            // sub-stride offset would draw the wrong rows.  Only tiny,
            // non-terrain strides can hit this with the 4-byte minimum
            // alignment, and those keep vanilla buffers.
            regionBuffer.free(allocation.offset(), allocation.size());
            return false;
        }

        RegionVertexSlice slice = null;
        try {
            if (regionRelative) {
                ByteBuffer scratch = scratch(targetByteSize);
                int deltaX = sectionOrigin.getX() - region.originBlockX();
                int deltaY = sectionOrigin.getY() - region.originBlockY();
                int deltaZ = sectionOrigin.getZ() - region.originBlockZ();
                if (compact) {
                    CompactTerrainFormat.convert(
                        vertices,
                        scratch,
                        vertexCount,
                        format,
                        deltaX,
                        deltaY,
                        deltaZ);
                } else {
                    RegionVertexRebase.rebase(
                        vertices,
                        scratch,
                        byteSize,
                        stride,
                        positionOffset,
                        deltaX,
                        deltaY,
                        deltaZ);
                }
                slice = new RegionVertexSlice(
                    regionBuffer,
                    allocation.offset(),
                    targetByteSize,
                    targetStride,
                    draw.indexCount(),
                    vertexCount,
                    layer,
                    sectionOrigin.getX(),
                    sectionOrigin.getY(),
                    sectionOrigin.getZ(),
                    true,
                    compact,
                    null);
                regionBuffer.write(allocation, scratch);
            } else {
                slice = new RegionVertexSlice(
                    regionBuffer,
                    allocation.offset(),
                    byteSize,
                    stride,
                    draw.indexCount(),
                    vertexCount,
                    layer,
                    sectionOrigin.getX(),
                    sectionOrigin.getY(),
                    sectionOrigin.getZ(),
                    false,
                    false,
                    null);
                regionBuffer.write(allocation, vertices);
            }

            Map<ChunkSectionLayer, SectionBuffers> buffers =
                ((CompiledSectionMeshAccessor) (Object) mesh).dx11$getBuffers();
            SectionBuffers previous = buffers.put(
                layer,
                new SectionBuffers(slice, null, draw.indexCount(), draw.indexType()));
            if (previous != null) {
                closeBuffers(previous);
            }
            return true;
        } catch (RuntimeException e) {
            if (slice != null) {
                slice.close();
            } else {
                regionBuffer.free(allocation.offset(), allocation.size());
            }
            throw e;
        }
    }

    /**
     * Copies a translucent mesh into the region arena and records region-local
     * quad centroids.  The mesh's vanilla sorted index buffer is uploaded too:
     * region submission normally ignores it, but a mixed/fallback list can
     * still draw the section correctly with vanilla ordering.
     */
    private static boolean uploadTranslucent(
        Region region,
        ChunkSectionLayer layer,
        MeshData data,
        CompiledSectionMesh mesh,
        BlockPos sectionOrigin) {
        MeshData.DrawState draw = data.drawState();
        VertexFormat format = draw.format();
        if (draw.mode() != VertexFormat.Mode.QUADS
            || draw.indexCount() <= 0
            || data.indexBuffer() == null) {
            return false;
        }
        MeshData.SortState sortState = mesh.getTransparencyState();
        if (sortState == null) {
            return false;
        }
        CompactVectorArray centroids = sortState.centroids();

        int stride = format.getVertexSize();
        ByteBuffer vertices = data.vertexBuffer();
        int byteSize = vertices.remaining();
        if (stride <= 0 || byteSize <= 0 || byteSize % stride != 0) {
            return false;
        }
        int vertexCount = byteSize / stride;
        if (vertexCount % 4 != 0) {
            return false;
        }
        int quadCount = vertexCount / 4;
        if (draw.indexCount() != quadCount * 6 || centroids.size() != quadCount) {
            return false;
        }
        int positionOffset = positionOffset(format);
        if (positionOffset < 0) {
            return false;
        }
        boolean compact = shouldCompact(layer, format);
        int targetStride = compact ? CompactTerrainFormat.STRIDE : stride;
        int targetByteSize = vertexCount * targetStride;

        RegionBuffer regionBuffer = region.getBuffer(layer);
        if (regionBuffer == null) {
            return false;
        }
        long alignment = Math.max(4, targetStride);
        RegionAllocator.Allocation allocation =
            regionBuffer.allocate(targetByteSize, alignment);
        if (allocation == null) {
            return false;
        }
        if (allocation.offset() % targetStride != 0) {
            regionBuffer.free(allocation.offset(), allocation.size());
            return false;
        }

        int deltaX = sectionOrigin.getX() - region.originBlockX();
        int deltaY = sectionOrigin.getY() - region.originBlockY();
        int deltaZ = sectionOrigin.getZ() - region.originBlockZ();
        float[] regionCentroids = new float[quadCount * 3];
        for (int quad = 0; quad < quadCount; quad++) {
            int offset = quad * 3;
            regionCentroids[offset] = centroids.getX(quad) + deltaX;
            regionCentroids[offset + 1] = centroids.getY(quad) + deltaY;
            regionCentroids[offset + 2] = centroids.getZ(quad) + deltaZ;
        }

        RegionVertexSlice slice = null;
        GpuBuffer indexBuffer = null;
        try {
            ByteBuffer scratch = scratch(targetByteSize);
            if (compact) {
                CompactTerrainFormat.convert(
                    vertices,
                    scratch,
                    vertexCount,
                    format,
                    deltaX,
                    deltaY,
                    deltaZ);
            } else {
                RegionVertexRebase.rebase(
                    vertices,
                    scratch,
                    byteSize,
                    stride,
                    positionOffset,
                    deltaX,
                    deltaY,
                    deltaZ);
            }
            slice = new RegionVertexSlice(
                regionBuffer,
                allocation.offset(),
                targetByteSize,
                targetStride,
                draw.indexCount(),
                vertexCount,
                layer,
                sectionOrigin.getX(),
                sectionOrigin.getY(),
                sectionOrigin.getZ(),
                true,
                compact,
                regionCentroids);
            regionBuffer.write(allocation, scratch);

            indexBuffer = RenderSystem.getDevice().createBuffer(
                TRANSLUCENT_INDEX_NAME,
                GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                data.indexBuffer());

            Map<ChunkSectionLayer, SectionBuffers> buffers =
                ((CompiledSectionMeshAccessor) (Object) mesh).dx11$getBuffers();
            SectionBuffers previous = buffers.put(
                layer,
                new SectionBuffers(slice, indexBuffer, draw.indexCount(), draw.indexType()));
            if (previous != null) {
                closeBuffers(previous);
            }
            return true;
        } catch (RuntimeException e) {
            if (indexBuffer != null) {
                try {
                    indexBuffer.close();
                } catch (RuntimeException ignored) {
                    // Already released.
                }
            }
            if (slice != null) {
                slice.close();
            } else {
                regionBuffer.free(allocation.offset(), allocation.size());
            }
            throw e;
        }
    }

    /**
     * True when this upload should use the 24-byte compact layout.  Compact
     * mode needs region-relative draws and a terrain shader that only reads
     * the compact-provided inputs; an explicit force flag overrides the shader
     * check for custom packs.
     */
    private static boolean shouldCompact(ChunkSectionLayer layer, VertexFormat format) {
        PerfConfig config = PerfState.config();
        if (!config.compactVertices || !config.regionDraws) {
            return false;
        }
        if (!CompactTerrainFormat.supports(format)) {
            return false;
        }
        if (!config.compactVerticesForce
            && CompactTerrainFormat.shaderObserved()
            && !CompactTerrainFormat.shaderCompatible()) {
            // A vanilla terrain pipeline already proved this pack needs
            // non-compact inputs; do not spend a compile attempt per layer.
            CompactTerrainFormat.warnShaderIncompatibleOnce();
            return false;
        }
        // When the interface has not been observed yet, compiling the compact
        // pipeline validates the real shader variant before any vertex is
        // packed.  This also lets compact mode come back after a shader reload
        // without waiting for a vanilla terrain draw.
        return PerfTerrainPipelines.isUsable(layer);
    }

    /**
     * Finds the byte offset of a three-float position element.  Returns -1 for
     * layouts the rebase cannot handle; those keep vanilla buffers.
     */
    static int positionOffset(VertexFormat format) {
        VertexFormatElement position = null;
        for (VertexFormatElement element : format.getElements()) {
            if (element.usage() == VertexFormatElement.Usage.POSITION) {
                if (position != null) {
                    return -1;
                }
                position = element;
            }
        }
        if (position == null
            || position.type() != VertexFormatElement.Type.FLOAT
            || position.count() != 3) {
            return -1;
        }
        return format.getOffset(position);
    }

    private static ByteBuffer scratch(int byteSize) {
        ByteBuffer scratch = REBASE_SCRATCH.get();
        if (scratch.capacity() < byteSize) {
            scratch = ByteBuffer.allocateDirect(byteSize).order(ByteOrder.nativeOrder());
            REBASE_SCRATCH.set(scratch);
        }
        return scratch;
    }

    /**
     * Removes a region-backed layer before asking vanilla to upload it.
     * Vanilla's {@code uploadMeshLayer} may reuse an existing buffer in place
     * via {@code writeToBuffer}; a {@link RegionVertexSlice} is not a
     * {@code Dx11Buffer}, so it must be detached first.
     */
    private static void detachRegionBuffers(CompiledSectionMesh mesh, ChunkSectionLayer layer) {
        Map<ChunkSectionLayer, SectionBuffers> buffers =
            ((CompiledSectionMeshAccessor) (Object) mesh).dx11$getBuffers();
        SectionBuffers existing = buffers.get(layer);
        if (existing != null && existing.getVertexBuffer() instanceof RegionVertexSlice) {
            buffers.remove(layer);
            closeBuffers(existing);
        }
    }

    private static void detachRegionBuffers(
        CompiledSectionMesh mesh, Collection<ChunkSectionLayer> layers) {
        for (ChunkSectionLayer layer : layers) {
            detachRegionBuffers(mesh, layer);
        }
    }

    private static void closeBuffers(SectionBuffers buffers) {
        try {
            buffers.getVertexBuffer().close();
        } catch (RuntimeException ignored) {
            // Already closed.
        }
        try {
            if (buffers.getIndexBuffer() != null) {
                buffers.getIndexBuffer().close();
            }
        } catch (RuntimeException ignored) {
            // Already closed.
        }
    }

    private static boolean isOpaque(ChunkSectionLayer layer) {
        return layer == ChunkSectionLayer.SOLID || layer == ChunkSectionLayer.CUTOUT;
    }

    /**
     * Translucent slices are only rebased when region draws are on: the
     * region-relative vertices need the per-region uniform, which also covers
     * the translucent layer.
     */
    private static boolean isTranslucentRegion(ChunkSectionLayer layer, PerfConfig config) {
        return layer == ChunkSectionLayer.TRANSLUCENT
            && config.translucentRegion
            && config.regionDraws;
    }

    private static void warn(String message) {
        long now = System.nanoTime();
        if (now - lastWarnNanos < WARN_INTERVAL_NANOS) {
            return;
        }
        lastWarnNanos = now;
        LOGGER.warn("[dx11-perf] {}", message);
    }
}
