package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
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
 *
 * <p>The visible set is keyed by a packed primitive {@code long} and stamped
 * with the preparation pass number, so the common path allocates nothing per
 * visible section: no {@link RegionPos} key and no boxed visibility value.  A
 * key is appended to the reusable frame list only on its first section of the
 * pass; later sections just raise the stored maximum.</p>
 */
public final class RegionFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final long WARN_INTERVAL_NANOS = 10_000_000_000L;

    /** 21 bits per region axis, which covers every coordinate inside the world border. */
    private static final int AXIS_BITS = 21;
    private static final long AXIS_MASK = (1L << AXIS_BITS) - 1L;

    private static final Long2LongOpenHashMap VISIBLE = new Long2LongOpenHashMap();
    private static final Long2ObjectOpenHashMap<GpuBufferSlice> UNIFORMS =
        new Long2ObjectOpenHashMap<>();
    private static final Matrix4f MATRIX = new Matrix4f();

    private static long[] frameRegionKeys = new long[256];
    private static int frameRegionCount;
    private static int frame;

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
        beginFrame();
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
        MATRIX.set(modelView);
        regionSize = PerfState.config().regionSize();
        recording = true;
    }

    /** Resets the per-pass state without clearing the stamped visibility map. */
    private static void beginFrame() {
        frame = frame == Integer.MAX_VALUE ? 1 : frame + 1;
        frameRegionCount = 0;
        UNIFORMS.clear();
        regionSize = null;
        atlasWidth = 0;
        atlasHeight = 0;
        recording = false;
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
        int regionX = Math.floorDiv(blockX >> 4, size.sectionsX());
        int regionY = Math.floorDiv(blockY >> 4, size.sectionsY());
        int regionZ = Math.floorDiv(blockZ >> 4, size.sectionsZ());
        long key = pack(regionX, regionY, regionZ);
        long entry = VISIBLE.get(key);
        if ((int) (entry >>> 32) == frame) {
            if (visibility <= Float.intBitsToFloat((int) entry)) {
                return;
            }
        } else {
            if (frameRegionCount == frameRegionKeys.length) {
                frameRegionKeys = Arrays.copyOf(frameRegionKeys, frameRegionCount << 1);
            }
            frameRegionKeys[frameRegionCount++] = key;
        }
        VISIBLE.put(key, ((long) frame << 32) | (Float.floatToRawIntBits(visibility) & 0xFFFFFFFFL));
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
        int count = frameRegionCount;
        RegionSize size = regionSize;
        if (count == 0
            || RegionVertexSlice.liveRebasedSlices() == 0
            || size == null
            || atlasWidth <= 0
            || atlasHeight <= 0) {
            return;
        }

        DynamicUniforms.ChunkSectionInfo[] infos =
            new DynamicUniforms.ChunkSectionInfo[count];
        for (int i = 0; i < count; i++) {
            long key = frameRegionKeys[i];
            float visibility = Float.intBitsToFloat((int) VISIBLE.get(key));
            infos[i] = new DynamicUniforms.ChunkSectionInfo(
                MATRIX,
                unpackX(key) * size.blocksX(),
                unpackY(key) * size.blocksY(),
                unpackZ(key) * size.blocksZ(),
                visibility,
                atlasWidth,
                atlasHeight);
        }

        GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms().writeChunkSections(infos);
        int written = Math.min(count, slices.length);
        for (int i = 0; i < written; i++) {
            UNIFORMS.put(frameRegionKeys[i], slices[i]);
        }
    }

    /** The region uniform written for this frame, or {@code null}. */
    public static @Nullable GpuBufferSlice uniform(RegionPos pos) {
        return UNIFORMS.get(pack(pos.x(), pos.y(), pos.z()));
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
        frame = 0;
        frameRegionCount = 0;
        VISIBLE.clear();
        UNIFORMS.clear();
        regionSize = null;
        atlasWidth = 0;
        atlasHeight = 0;
        cameraX = 0.0;
        cameraY = 0.0;
        cameraZ = 0.0;
        recording = false;
    }

    /** Packs region coordinates into one primitive key (x, then y, then z). */
    static long pack(int x, int y, int z) {
        return ((long) (x & AXIS_MASK) << (AXIS_BITS * 2))
            | ((long) (y & AXIS_MASK) << AXIS_BITS)
            | (z & AXIS_MASK);
    }

    static int unpackX(long key) {
        return (int) (key << 1 >> 43);
    }

    static int unpackY(long key) {
        return (int) (key << 22 >> 43);
    }

    static int unpackZ(long key) {
        return (int) (key << 43 >> 43);
    }
}
