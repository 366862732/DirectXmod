package com.xgdt.dx11.perf.region;

import java.util.Arrays;

/**
 * Pure ordering and index-generation rules for translucent region quads.
 *
 * <p>Vanilla sorts each section's quads by distance to the camera and rebuilds
 * the section index buffer when the camera crosses a section boundary.  A
 * region draw needs the same order across every visible section of the region,
 * with indices rebased to the region vertex arena.  This class is deliberately
 * free of GPU and Minecraft state so the ordering rules can be unit-tested.</p>
 */
public final class TranslucentOrder {
    private TranslucentOrder() {
    }

    /**
     * Fills {@code order[0..quadCount)} with quad indices ordered farthest to
     * nearest by squared distance from the camera to each quad centroid.
     * Ties keep ascending quad index, matching vanilla's stable merge sort.
     *
     * @param centroidsX region-local centroid X per quad
     * @param centroidsY region-local centroid Y per quad
     * @param centroidsZ region-local centroid Z per quad
     * @param order      output scratch, at least {@code quadCount} long
     * @param keys       output scratch, at least {@code quadCount} long
     */
    public static void rank(
        float[] centroidsX,
        float[] centroidsY,
        float[] centroidsZ,
        int quadCount,
        double cameraX,
        double cameraY,
        double cameraZ,
        int[] order,
        long[] keys) {
        if (quadCount < 0
            || order.length < quadCount
            || keys.length < quadCount
            || centroidsX.length < quadCount
            || centroidsY.length < quadCount
            || centroidsZ.length < quadCount) {
            throw new IllegalArgumentException("Translucent ordering buffers are too small");
        }

        for (int quad = 0; quad < quadCount; quad++) {
            double dx = centroidsX[quad] - cameraX;
            double dy = centroidsY[quad] - cameraY;
            double dz = centroidsZ[quad] - cameraZ;
            float distance = (float) (dx * dx + dy * dy + dz * dz);
            // Non-negative float bit patterns compare like the values.  The
            // complemented distance makes the ascending long sort farthest
            // first; the low word keeps ties in insertion order.
            long key = ((long) (~Float.floatToIntBits(distance)) & 0xFFFF_FFFFL) << 32;
            keys[quad] = key | (quad & 0xFFFF_FFFFL);
        }

        Arrays.sort(keys, 0, quadCount);
        for (int rank = 0; rank < quadCount; rank++) {
            order[rank] = (int) keys[rank];
        }
    }

    /**
     * Writes the vanilla QUADS index pattern for the sorted quads.  Each quad
     * base vertex is a region-arena vertex index.
     */
    public static void writeIndices(
        int[] order, int quadCount, int[] quadBaseVertex, int[] indices) {
        if (quadCount < 0
            || order.length < quadCount
            || quadBaseVertex.length < quadCount
            || indices.length < quadCount * 6) {
            throw new IllegalArgumentException("Translucent index buffers are too small");
        }

        int out = 0;
        for (int rank = 0; rank < quadCount; rank++) {
            int base = quadBaseVertex[order[rank]];
            indices[out++] = base;
            indices[out++] = base + 1;
            indices[out++] = base + 2;
            indices[out++] = base + 2;
            indices[out++] = base + 3;
            indices[out++] = base;
        }
    }
}
