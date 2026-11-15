package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-frame bridge between chunk-render preparation and region submission.
 *
 * <p>Vanilla builds one {@code ChunkSection} uniform per visible section during
 * {@code LevelRenderer.prepareChunkRenders}.  Rebased region vertices need one
 * uniform per <em>region</em> instead: the region origin plus the maximum
 * visibility of the region's visible sections.  The preparation mixin captures
 * the frame matrix; the {@code ChunkSectionInfo} constructor mixin reports each
 * visible section's origin, visibility and atlas size; and this class writes
 * the region uniforms once the section list has been walked.  The submitter
 * then only looks slices up by region.</p>
 */
public final class RegionFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final long WARN_INTERVAL_NANOS = 10_000_000_000L;
    private static final Map<RegionPos, Float> VISIBLE = new HashMap<>();
    private static final Map<RegionPos, GpuBufferSlice> UNIFORMS = new HashMap<>();

    private static @Nullable Matrix4f matrix;
    private static @Nullable RegionSize regionSize;
    private static int atlasWidth;
    private static int atlasHeight;
    private static boolean recording;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static long lastMissingWarnNanos;

    private RegionFrame() {
    }

    /**
     * Starts a preparation pass.  Records only when region draws are armed or
     * when rebased slices from a previous mode are still alive, so the default
     * (non-region) path pays nothing.
     */
    public static void begin(
        Matrix4fc modelView, double viewX, double viewY, double viewZ) {
        reset();
        cameraX = viewX;
        cameraY = viewY;
        cameraZ = viewZ;
        RegionTranslucentSorter.beginFrame();
        boolean needed;
        try {
            PerfConfig config = PerfState.config();
            needed = (PerfState.regionActive() && config.regionDraws)
                || RegionVertexSlice.liveRebasedSlices() > 0;
        } catch (Throwable t) {
            needed = RegionVertexSlice.liveRebasedSlices() > 0;
        }
        if (!needed) {
            return;
        }
        matrix = new Matrix4f(modelView);
        regionSize = PerfState.config().regionSize();
        recording = true;
    }

    /** Camera X captured at the start of the frame's chunk preparation. */
    public static double cameraX() {
        return cameraX;
    }

    public static double cameraY() {
        return cameraY;
    }

    public static double cameraZ() {
        return cameraZ;
    }

    /**
     * Called for every visible section's {@code ChunkSectionInfo}.  The
     * coordinates are section-origin block coordinates.
     */
    public static void recordSection(
        int blockX, int blockY, int blockZ, float visibility, int width, int height) {
        if (!recording) {
            return;
        }
        if (width > 0 && height > 0) {
            atlasWidth = width;
            atlasHeight = height;
        }
        RegionSize size = regionSize;
        if (size == null) {
            return;
        }
        RegionPos pos = RegionPos.fromSection(
            Math.floorDiv(blockX, 16),
            Math.floorDiv(blockY, 16),
            Math.floorDiv(blockZ, 16),
            size);
        Float current = VISIBLE.get(pos);
        if (current == null || visibility > current) {
            VISIBLE.put(pos, visibility);
        }
    }

    /**
     * Finishes the preparation pass and writes one region uniform per visible
     * region.  Called at the return of {@code prepareChunkRenders}, before any
     * render pass is opened.
     */
    public static void end() {
        if (!recording) {
            reset();
            return;
        }
        recording = false;
        if (RegionVertexSlice.liveRebasedSlices() == 0
            || matrix == null
            || regionSize == null
            || atlasWidth <= 0
            || atlasHeight <= 0
            || VISIBLE.isEmpty()) {
            return;
        }

        RegionSize size = regionSize;
        RegionPos[] positions = new RegionPos[VISIBLE.size()];
        DynamicUniforms.ChunkSectionInfo[] infos =
            new DynamicUniforms.ChunkSectionInfo[positions.length];
        int index = 0;
        for (Map.Entry<RegionPos, Float> entry : VISIBLE.entrySet()) {
            RegionPos pos = entry.getKey();
            positions[index] = pos;
            infos[index] = new DynamicUniforms.ChunkSectionInfo(
                matrix,
                pos.originBlockX(size),
                pos.originBlockY(size),
                pos.originBlockZ(size),
                entry.getValue(),
                atlasWidth,
                atlasHeight);
            index++;
        }

        GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms().writeChunkSections(infos);
        int count = Math.min(positions.length, slices.length);
        for (int i = 0; i < count; i++) {
            UNIFORMS.put(positions[i], slices[i]);
        }
        VISIBLE.clear();
    }

    /** The region uniform written for this frame, or {@code null}. */
    public static @Nullable GpuBufferSlice uniform(RegionPos pos) {
        return UNIFORMS.get(pos);
    }

    /** Throttled diagnostic for a rebased slice whose region uniform is gone. */
    public static void warnMissingUniform(Region region) {
        long now = System.nanoTime();
        if (now - lastMissingWarnNanos < WARN_INTERVAL_NANOS) {
            return;
        }
        lastMissingWarnNanos = now;
        LOGGER.warn(
            "[dx11-perf] No ChunkSection uniform for region {}; skipping its region draws",
            region.pos());
    }

    /** Drops all per-frame state, for example on a level change. */
    public static void reset() {
        VISIBLE.clear();
        UNIFORMS.clear();
        matrix = null;
        regionSize = null;
        atlasWidth = 0;
        atlasHeight = 0;
        cameraX = 0.0;
        cameraY = 0.0;
        cameraZ = 0.0;
        recording = false;
    }
}
