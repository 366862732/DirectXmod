package com.xgdt.dx11.perf.region;

import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Builds the sorted translucent index list for one visible region.
 *
 * <p>The visible section set comes from vanilla's per-layer draw list, so it is
 * already occlusion-filtered.  When that set changes the list is rebuilt
 * immediately; when only the camera moved, the rebuild is throttled by
 * {@code perf.translucentSortBudget} and the previous buffer is reused until a
 * slot is available.  A rebuild sorts every visible quad in the region
 * back-to-front and writes region-arena vertex indices.</p>
 *
 * <p>All state is render-thread-only.  Scratch arrays are shared because at
 * most one region is sorted at a time.</p>
 */
public final class RegionTranslucentSorter {
    /** Re-sort after moving this many blocks; smaller moves reuse the sort. */
    private static final double SORT_DISTANCE_SQ = 1.0;
    private static final int MIN_SCRATCH_QUADS = 1024;

    private static float[] centroidX;
    private static float[] centroidY;
    private static float[] centroidZ;
    private static int[] quadBaseVertex;
    private static int[] order;
    private static long[] keys;
    private static int[] indices;
    private static ByteBuffer indexBytes;

    private static int budgetUsed;
    private static long sorts;
    private static long reuses;
    private static long skipped;
    private static long failures;
    private static long lastQuads;
    private static long maxQuads;
    private static long lastNanos;
    private static long maxNanos;

    private RegionTranslucentSorter() {
    }

    /** Resets the per-frame resort budget.  Called when a frame starts. */
    public static void beginFrame() {
        budgetUsed = 0;
    }

    /**
     * Ensures the region has an index buffer for the visible slices.
     *
     * @return the number of indices to draw, zero when there is nothing to
     *         draw, or {@code -1} when the caller must fall back to vanilla
     *         per-section translucent draws
     */
    public static int prepare(
        RegionTranslucentIndex index,
        List<RegionVertexSlice> visible,
        double cameraX,
        double cameraY,
        double cameraZ) {
        if (visible.isEmpty()) {
            return 0;
        }
        // Deterministic arena order makes the live-slice comparison cheap.
        visible.sort(Comparator.comparingInt(RegionVertexSlice::startVertex));

        ChunkSectionLayer layer = index.layer();
        for (RegionVertexSlice slice : visible) {
            if (slice.isClosed()
                || slice.layer() != layer
                || slice.centroids() == null
                || !slice.regionRelative()) {
                return -1;
            }
        }

        if (index.matches(visible)) {
            double dx = cameraX - index.lastSortX();
            double dy = cameraY - index.lastSortY();
            double dz = cameraZ - index.lastSortZ();
            boolean closeEnough = dx * dx + dy * dy + dz * dz <= SORT_DISTANCE_SQ;
            PerfConfig config = PerfState.config();
            boolean budgetSpent = config.translucentSortBudget > 0
                && budgetUsed >= config.translucentSortBudget;
            if (closeEnough) {
                reuses++;
                return index.indexCount();
            }
            // Starvation guard: a region may reuse a stale sort while the
            // budget is spent, but never for more than two consecutive frames.
            if (budgetSpent && index.skippedFrames() < 2) {
                index.noteSkippedFrame();
                skipped++;
                return index.indexCount();
            }
        }

        return rebuild(index, visible, cameraX, cameraY, cameraZ);
    }

    private static int rebuild(
        RegionTranslucentIndex index,
        List<RegionVertexSlice> visible,
        double cameraX,
        double cameraY,
        double cameraZ) {
        int quadCount = 0;
        for (RegionVertexSlice slice : visible) {
            quadCount += slice.quadCount();
        }
        if (quadCount <= 0) {
            index.markSorted(visible, cameraX, cameraY, cameraZ, 0.0f, 0.0f, 0.0f, 0);
            return 0;
        }

        ensureScratch(quadCount);
        Region region = index.region();
        double localCameraX = cameraX - region.originBlockX();
        double localCameraY = cameraY - region.originBlockY();
        double localCameraZ = cameraZ - region.originBlockZ();

        int quad = 0;
        double centerX = 0.0;
        double centerY = 0.0;
        double centerZ = 0.0;
        for (RegionVertexSlice slice : visible) {
            float[] centroids = slice.centroids();
            int baseVertex = slice.startVertex();
            int sliceQuads = slice.quadCount();
            for (int local = 0; local < sliceQuads; local++) {
                int offset = local * 3;
                float x = centroids[offset];
                float y = centroids[offset + 1];
                float z = centroids[offset + 2];
                centroidX[quad] = x;
                centroidY[quad] = y;
                centroidZ[quad] = z;
                quadBaseVertex[quad] = baseVertex + local * 4;
                centerX += x;
                centerY += y;
                centerZ += z;
                quad++;
            }
        }

        long start = System.nanoTime();
        TranslucentOrder.rank(
            centroidX,
            centroidY,
            centroidZ,
            quadCount,
            localCameraX,
            localCameraY,
            localCameraZ,
            order,
            keys);
        TranslucentOrder.writeIndices(order, quadCount, quadBaseVertex, indices);

        int indexCount = quadCount * 6;
        ByteBuffer data = indexData(indexCount);
        data.asIntBuffer().put(indices, 0, indexCount);
        data.limit(indexCount * Integer.BYTES);

        if (!index.upload(data, indexCount)) {
            failures++;
            return -1;
        }

        index.markSorted(
            visible,
            cameraX,
            cameraY,
            cameraZ,
            (float) (centerX / quadCount),
            (float) (centerY / quadCount),
            (float) (centerZ / quadCount),
            indexCount);

        long elapsed = System.nanoTime() - start;
        budgetUsed++;
        sorts++;
        lastQuads = quadCount;
        if (quadCount > maxQuads) {
            maxQuads = quadCount;
        }
        lastNanos = elapsed;
        if (elapsed > maxNanos) {
            maxNanos = elapsed;
        }
        return indexCount;
    }

    private static void ensureScratch(int quadCount) {
        if (centroidX == null || centroidX.length < quadCount) {
            int capacity = Math.max(quadCount, MIN_SCRATCH_QUADS);
            centroidX = new float[capacity];
            centroidY = new float[capacity];
            centroidZ = new float[capacity];
            quadBaseVertex = new int[capacity];
            order = new int[capacity];
            keys = new long[capacity];
        }
        if (indices == null || indices.length < quadCount * 6) {
            indices = new int[quadCount * 6];
        }
    }

    private static ByteBuffer indexData(int indexCount) {
        int bytes = indexCount * Integer.BYTES;
        if (indexBytes == null || indexBytes.capacity() < bytes) {
            indexBytes = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        }
        indexBytes.clear();
        return indexBytes;
    }

    /** One-line state for {@code /dx11perf stats}. */
    public static String describe() {
        return "translucentSorts=" + sorts
            + " reused=" + reuses
            + " skipped=" + skipped
            + " failed=" + failures
            + " quads=" + lastQuads
            + " maxQuads=" + maxQuads
            + " ms=" + millis(lastNanos)
            + " maxMs=" + millis(maxNanos);
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }
}
