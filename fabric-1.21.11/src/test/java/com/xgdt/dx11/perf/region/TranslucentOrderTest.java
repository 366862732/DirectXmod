package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class TranslucentOrderTest {
    @Test
    void ranksQuadsFarthestFirst() {
        float[] x = {0, 0, 0};
        float[] y = {0, 0, 0};
        float[] z = {0, 10, 5};
        int[] order = new int[3];
        long[] keys = new long[3];

        TranslucentOrder.rank(x, y, z, 3, 0, 0, 0, order, keys);

        assertArrayEquals(new int[] {1, 2, 0}, order);
    }

    @Test
    void equalDistanceKeepsQuadOrder() {
        float[] x = {0, 0, 0};
        float[] y = {0, 0, 0};
        float[] z = {5, -5, 0};
        int[] order = new int[3];
        long[] keys = new long[3];

        TranslucentOrder.rank(x, y, z, 3, 0, 0, 0, order, keys);

        assertArrayEquals(new int[] {0, 1, 2}, order);
    }

    @Test
    void usesCameraRelativeCentroids() {
        float[] x = {100, 100};
        float[] y = {0, 0};
        float[] z = {0, 8};
        int[] order = new int[2];
        long[] keys = new long[2];

        TranslucentOrder.rank(x, y, z, 2, 100, 0, 0, order, keys);

        assertArrayEquals(new int[] {1, 0}, order);
    }

    @Test
    void writesVanillaQuadIndexPattern() {
        int[] order = {2, 0};
        int[] baseVertex = {4, 10, 20};
        int[] indices = new int[12];

        TranslucentOrder.writeIndices(order, 2, baseVertex, indices);

        assertArrayEquals(
            new int[] {20, 21, 22, 22, 23, 20, 4, 5, 6, 6, 7, 4},
            indices);
    }

    @Test
    void keepsFullRegionVertexIndexes() {
        int[] order = {0};
        int[] baseVertex = {70_000};
        int[] indices = new int[6];

        TranslucentOrder.writeIndices(order, 1, baseVertex, indices);

        assertArrayEquals(new int[] {70_000, 70_001, 70_002, 70_002, 70_003, 70_000}, indices);
    }
}
