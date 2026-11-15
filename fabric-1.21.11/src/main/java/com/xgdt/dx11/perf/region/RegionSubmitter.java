package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfCounters;
import com.xgdt.dx11.perf.PerfState;
import com.xgdt.dx11.perf.culling.FogCuller;
import com.xgdt.dx11.perf.culling.FogOcclusion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jspecify.annotations.Nullable;

/**
 * Issues draws for region-backed sections.
 *
 * <p>Phase 2A keeps one draw per section and therefore keeps vanilla's
 * per-section {@code ChunkSection} uniform; only the vertex storage changes.
 * Phase 2B (when the slice was uploaded region-relative) groups co-located
 * slices by region, coalesces contiguous arena ranges and issues one indexed
 * draw per run with a single region uniform.  Phase 5 draws each translucent
 * region once with a per-region, farthest-first sorted index buffer and orders
 * the regions themselves back-to-front.  Non-region draws in the same list are
 * delegated back to vanilla so mixed lists (for example after an arena-budget
 * fallback) stay correct.</p>
 */
public final class RegionSubmitter {
    private static final RegionIndexBuffer REGION_INDICES = new RegionIndexBuffer();

    private RegionSubmitter() {
    }

    /**
     * @return {@code true} when the list contained region slices and was fully
     *         submitted, {@code false} when the caller should use vanilla
     */
    public static <T> boolean submit(
        RenderPass pass,
        Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer defaultIndexBuffer,
        VertexFormat.@Nullable IndexType defaultIndexType,
        Collection<String> uniformNames,
        T uniformOwner) {
        if (draws.isEmpty()) {
            return false;
        }
        if (RegionVertexSlice.liveSlices() == 0) {
            return false;
        }

        boolean containsRegionHandle = false;
        boolean hasLiveRegionSlice = false;
        boolean allRegion = true;
        boolean allRebased = true;
        boolean singleLayer = true;
        ChunkSectionLayer layer = null;

        for (RenderPass.Draw<T> draw : draws) {
            if (!(draw.vertexBuffer() instanceof RegionVertexSlice slice)) {
                allRegion = false;
                continue;
            }
            containsRegionHandle = true;
            if (slice.isClosed()) {
                continue;
            }
            hasLiveRegionSlice = true;
            if (!slice.regionRelative()) {
                allRebased = false;
            }
            if (layer == null) {
                layer = slice.layer();
            } else if (layer != slice.layer()) {
                singleLayer = false;
            }
        }

        // A list without any region handle is entirely vanilla's.  A list that
        // only contains closed region handles must be swallowed: vanilla would
        // try to bind the registry handle as a real GPU buffer.
        if (!containsRegionHandle) {
            return false;
        }
        if (!hasLiveRegionSlice) {
            return true;
        }

        String uniformName = uniformNames.isEmpty()
            ? "ChunkSection"
            : uniformNames.iterator().next();
        FogCuller fog = FogOcclusion.culler(
            RegionFrame.cameraX(), RegionFrame.cameraY(), RegionFrame.cameraZ());

        if (allRegion
            && allRebased
            && singleLayer
            && (layer == ChunkSectionLayer.SOLID || layer == ChunkSectionLayer.CUTOUT)) {
            if (submitRuns(pass, draws, uniformName, layer, fog)) {
                return true;
            }
        } else if (layer == ChunkSectionLayer.TRANSLUCENT
            && allRegion
            && allRebased
            && singleLayer
            && translucentRegionEnabled()) {
            if (submitTranslucent(pass, draws, uniformName, layer)) {
                return true;
            }
        }

        submitPerSection(
            pass,
            draws,
            defaultIndexBuffer,
            defaultIndexType,
            uniformNames,
            uniformOwner,
            uniformName,
            layer,
            fog);
        return true;
    }

    private static <T> boolean submitRuns(
        RenderPass pass,
        Collection<RenderPass.Draw<T>> draws,
        String uniformName,
        ChunkSectionLayer layer,
        @Nullable FogCuller fog) {
        Map<GroupKey, List<RegionVertexSlice>> groups = new LinkedHashMap<>();
        for (RenderPass.Draw<T> draw : draws) {
            if (draw.vertexBuffer() instanceof RegionVertexSlice slice && !slice.isClosed()) {
                if (fog != null && cullOpaque(fog, slice)) {
                    continue;
                }
                groups.computeIfAbsent(
                        new GroupKey(slice.regionBuffer(), draw.slot()),
                        key -> new ArrayList<>())
                    .add(slice);
            }
        }

        List<GroupRuns> plans = new ArrayList<>(groups.size());
        int maxRunIndexCount = 0;
        for (Map.Entry<GroupKey, List<RegionVertexSlice>> entry : groups.entrySet()) {
            RegionBuffer buffer = entry.getKey().buffer();
            if (buffer.isClosed() || buffer.gpuBuffer().isClosed()) {
                continue;
            }

            List<RegionVertexSlice> slices = entry.getValue();
            slices.sort(Comparator.comparingLong(RegionVertexSlice::offset));
            List<RegionRuns.Span> spans = new ArrayList<>(slices.size());
            for (RegionVertexSlice slice : slices) {
                spans.add(new RegionRuns.Span(
                    slice.offset(),
                    slice.byteSize(),
                    slice.vertexStride(),
                    slice.indexCount(),
                    slice.compact()));
            }
            List<RegionRuns.Run> runs = RegionRuns.coalesce(entry.getKey().slot(), spans);
            if (runs.isEmpty()) {
                continue;
            }
            for (RegionRuns.Run run : runs) {
                maxRunIndexCount = Math.max(maxRunIndexCount, run.indexCount());
            }
            plans.add(new GroupRuns(entry.getKey(), runs));
        }

        if (plans.isEmpty() || maxRunIndexCount <= 0) {
            return true;
        }

        GpuBuffer indexBuffer = REGION_INDICES.get(maxRunIndexCount);
        if (indexBuffer == null) {
            return false;
        }
        VertexFormat.IndexType indexType = REGION_INDICES.type();

        RegionBuffer boundBuffer = null;
        int boundSlot = -1;
        RenderPipeline boundPipeline = null;

        // Every region draw in this layer uses the same sequential QUADS
        // index buffer, so bind it once for the whole list.
        pass.setIndexBuffer(indexBuffer, indexType);

        for (GroupRuns plan : plans) {
            RegionBuffer buffer = plan.key().buffer();
            Region region = buffer.region();
            GpuBufferSlice uniform = RegionFrame.uniform(region.pos());
            if (uniform == null) {
                RegionFrame.warnMissingUniform(region);
                continue;
            }

            if (boundBuffer != buffer || boundSlot != plan.key().slot()) {
                pass.setVertexBuffer(plan.key().slot(), buffer.gpuBuffer());
                boundBuffer = buffer;
                boundSlot = plan.key().slot();
            }
            pass.setUniform(uniformName, uniform);
            for (RegionRuns.Run run : plan.runs()) {
                if (run.compact() && !PerfTerrainPipelines.isUsable(layer)) {
                    // The compact layout cannot be drawn by the vanilla
                    // pipeline; skip rather than binding an invalid pipeline.
                    continue;
                }
                RenderPipeline wanted = run.compact()
                    ? PerfTerrainPipelines.compact(layer)
                    : layer.pipeline();
                if (wanted != boundPipeline) {
                    pass.setPipeline(wanted);
                    boundPipeline = wanted;
                }
                PerfCounters.onRegionRun();
                pass.drawIndexed(run.startVertex(), 0, run.indexCount(), 1);
            }
        }
        return true;
    }

    /**
     * Draws one sorted index list per visible region, farthest region first.
     * The visible slice set is exact (it comes from vanilla's occlusion-filtered
     * draw list), so the region index buffer never references a hidden section.
     */
    private static <T> boolean submitTranslucent(
        RenderPass pass,
        Collection<RenderPass.Draw<T>> draws,
        String uniformName,
        ChunkSectionLayer layer) {
        Map<RegionBuffer, List<RegionVertexSlice>> groups = new LinkedHashMap<>();
        Map<RegionBuffer, Integer> slots = new LinkedHashMap<>();
        Map<RegionBuffer, Boolean> compact = new LinkedHashMap<>();

        for (RenderPass.Draw<T> draw : draws) {
            if (!(draw.vertexBuffer() instanceof RegionVertexSlice slice)
                || slice.isClosed()) {
                return false;
            }
            RegionBuffer buffer = slice.regionBuffer();
            Integer previousSlot = slots.putIfAbsent(buffer, draw.slot());
            if (previousSlot != null && previousSlot != draw.slot()) {
                // One region index buffer cannot serve two vertex slots.
                return false;
            }
            Boolean previousCompact = compact.putIfAbsent(buffer, slice.compact());
            if (previousCompact != null && previousCompact != slice.compact()) {
                // A single draw cannot mix vertex strides.
                return false;
            }
            groups.computeIfAbsent(buffer, key -> new ArrayList<>()).add(slice);
        }
        if (groups.isEmpty()) {
            return true;
        }

        double cameraX = RegionFrame.cameraX();
        double cameraY = RegionFrame.cameraY();
        double cameraZ = RegionFrame.cameraZ();
        List<TranslucentPlan> plans = new ArrayList<>(groups.size());
        for (Map.Entry<RegionBuffer, List<RegionVertexSlice>> entry : groups.entrySet()) {
            RegionBuffer buffer = entry.getKey();
            if (buffer.isClosed() || buffer.gpuBuffer().isClosed()) {
                return false;
            }
            Region region = buffer.region();
            RegionTranslucentIndex index = region.getOrCreateTranslucentIndex(layer);
            if (index == null) {
                return false;
            }
            List<RegionVertexSlice> slices = entry.getValue();
            int indexCount =
                RegionTranslucentSorter.prepare(index, slices, cameraX, cameraY, cameraZ);
            if (indexCount < 0) {
                return false;
            }
            if (indexCount == 0) {
                continue;
            }
            GpuBufferSlice uniform = RegionFrame.uniform(region.pos());
            if (uniform == null) {
                RegionFrame.warnMissingUniform(region);
                return false;
            }
            plans.add(new TranslucentPlan(
                buffer,
                slots.get(buffer),
                compact.get(buffer),
                index,
                indexCount,
                uniform));
        }
        if (plans.isEmpty()) {
            return true;
        }

        // Translucency must be submitted back-to-front.  The per-region sort
        // handles quads inside a region; this orders the regions themselves.
        plans.sort((left, right) -> Double.compare(
            right.index.centerDistanceSquared(cameraX, cameraY, cameraZ),
            left.index.centerDistanceSquared(cameraX, cameraY, cameraZ)));

        RenderPipeline boundPipeline = null;
        for (TranslucentPlan plan : plans) {
            if (plan.compact && !PerfTerrainPipelines.isUsable(layer)) {
                continue;
            }
            RenderPipeline wanted = plan.compact
                ? PerfTerrainPipelines.compact(layer)
                : layer.pipeline();
            if (wanted != boundPipeline) {
                pass.setPipeline(wanted);
                boundPipeline = wanted;
            }
            pass.setVertexBuffer(plan.slot, plan.buffer.gpuBuffer());
            pass.setUniform(uniformName, plan.uniform);
            pass.setIndexBuffer(plan.index.gpuBuffer(), VertexFormat.IndexType.INT);
            PerfCounters.onRegionRun();
            pass.drawIndexed(0, 0, plan.indexCount, 1);
        }
        return true;
    }

    private static boolean translucentRegionEnabled() {
        try {
            PerfConfig config = PerfState.config();
            return PerfState.regionActive() && config.translucentRegion && config.regionDraws;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Fog-culls only the opaque terrain layers.  Translucent sections keep
     * their draws because culling water/glass could change alpha blending near
     * the fog boundary, and because the per-region translucent sort would
     * otherwise rebuild whenever the camera crosses that boundary.
     */
    private static boolean cullOpaque(FogCuller fog, RegionVertexSlice slice) {
        ChunkSectionLayer layer = slice.layer();
        if (layer != ChunkSectionLayer.SOLID && layer != ChunkSectionLayer.CUTOUT) {
            return false;
        }
        if (!fog.isFullyFogged(
            slice.sectionOriginX(), slice.sectionOriginY(), slice.sectionOriginZ())) {
            return false;
        }
        FogOcclusion.onCulledSection();
        return true;
    }

    private static <T> void submitPerSection(
        RenderPass pass,
        Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer defaultIndexBuffer,
        VertexFormat.@Nullable IndexType defaultIndexType,
        Collection<String> uniformNames,
        T uniformOwner,
        String uniformName,
        @Nullable ChunkSectionLayer listLayer,
        @Nullable FogCuller fog) {
        RegionBuffer boundRegion = null;
        int boundSlot = -1;
        GpuBuffer boundIndex = null;
        VertexFormat.IndexType boundIndexType = null;
        RenderPipeline boundPipeline = null;

        for (RenderPass.Draw<T> draw : draws) {
            if (!(draw.vertexBuffer() instanceof RegionVertexSlice slice)) {
                // Mixed/fallback list: let vanilla bind its own buffers and
                // upload the per-section uniform for this draw.  Restore the
                // layer pipeline in case the previous draw used compact data.
                if (listLayer != null && boundPipeline != listLayer.pipeline()) {
                    pass.setPipeline(listLayer.pipeline());
                    boundPipeline = listLayer.pipeline();
                }
                pass.drawMultipleIndexed(
                    List.of(draw), defaultIndexBuffer, defaultIndexType, uniformNames, uniformOwner);
                boundRegion = null;
                boundSlot = -1;
                boundIndex = null;
                boundIndexType = null;
                continue;
            }
            if (slice.isClosed()) {
                // The section was dropped between draw-list construction and
                // submission; its arena slice is already gone.
                continue;
            }
            if (fog != null && cullOpaque(fog, slice)) {
                continue;
            }

            if (slice.compact() && !PerfTerrainPipelines.isUsable(slice.layer())) {
                continue;
            }
            RenderPipeline wanted = slice.compact()
                ? PerfTerrainPipelines.compact(slice.layer())
                : slice.layer().pipeline();
            if (wanted != boundPipeline) {
                pass.setPipeline(wanted);
                boundPipeline = wanted;
            }

            RegionBuffer regionBuffer = slice.regionBuffer();
            if (regionBuffer == null || regionBuffer.isClosed()
                || regionBuffer.gpuBuffer().isClosed()) {
                continue;
            }

            if (boundRegion != regionBuffer || boundSlot != draw.slot()) {
                pass.setVertexBuffer(draw.slot(), regionBuffer.gpuBuffer());
                boundRegion = regionBuffer;
                boundSlot = draw.slot();
            }

            GpuBuffer drawIndexBuffer = draw.indexBuffer();
            VertexFormat.IndexType drawIndexType = draw.indexType();
            if (drawIndexBuffer == null) {
                GpuBuffer sequentialBuffer = defaultIndexBuffer;
                VertexFormat.IndexType sequentialType =
                    defaultIndexType == null ? VertexFormat.IndexType.SHORT : defaultIndexType;
                if (sequentialBuffer == null) {
                    RenderSystem.AutoStorageIndexBuffer sequential =
                        RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                    sequentialBuffer = sequential.getBuffer(slice.indexCount());
                    sequentialType = sequential.type();
                }
                if (boundIndex != sequentialBuffer || boundIndexType != sequentialType) {
                    pass.setIndexBuffer(sequentialBuffer, sequentialType);
                    boundIndex = sequentialBuffer;
                    boundIndexType = sequentialType;
                }
            } else if (boundIndex != drawIndexBuffer || boundIndexType != drawIndexType) {
                pass.setIndexBuffer(
                    drawIndexBuffer,
                    drawIndexType == null
                        ? (defaultIndexType == null
                            ? VertexFormat.IndexType.SHORT
                            : defaultIndexType)
                        : drawIndexType);
                boundIndex = drawIndexBuffer;
                boundIndexType = drawIndexType;
            }

            if (slice.regionRelative()) {
                // Rebased vertices already include the section offset; the
                // vanilla per-section slice would add it a second time.
                GpuBufferSlice uniform = RegionFrame.uniform(regionBuffer.region().pos());
                if (uniform == null) {
                    RegionFrame.warnMissingUniform(regionBuffer.region());
                    continue;
                }
                pass.setUniform(uniformName, uniform);
            } else if (draw.uniformUploaderConsumer() != null) {
                draw.uniformUploaderConsumer().accept(
                    uniformOwner, (name, value) -> pass.setUniform(name, value));
            }
            pass.drawIndexed(slice.startVertex(), 0, slice.indexCount(), 1);
        }
    }

    private record TranslucentPlan(
        RegionBuffer buffer,
        int slot,
        boolean compact,
        RegionTranslucentIndex index,
        int indexCount,
        GpuBufferSlice uniform) {
    }

    private record GroupKey(RegionBuffer buffer, int slot) {
    }

    private record GroupRuns(GroupKey key, List<RegionRuns.Run> runs) {
    }
}
