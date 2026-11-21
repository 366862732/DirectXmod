package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;

class RegionIndexBufferTest {
    @Test
    void shortIndicesUseTheVanillaQuadPattern() {
        ByteBuffer data = RegionIndexBuffer.sequentialQuads(12, VertexFormat.IndexType.SHORT);

        assertEquals(24, data.remaining());
        short[] expected = {0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4};
        for (short value : expected) {
            assertEquals(value, data.getShort());
        }
    }

    @Test
    void intIndicesUseTheVanillaQuadPattern() {
        ByteBuffer data = RegionIndexBuffer.sequentialQuads(6, VertexFormat.IndexType.INT);

        assertEquals(24, data.remaining());
        int[] expected = {0, 1, 2, 2, 3, 0};
        for (int value : expected) {
            assertEquals(value, data.getInt());
        }
    }

    @Test
    void rejectsIndexCountsThatAreNotWholeQuads() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RegionIndexBuffer.sequentialQuads(4, VertexFormat.IndexType.SHORT));
        assertThrows(
            IllegalArgumentException.class,
            () -> RegionIndexBuffer.sequentialQuads(-6, VertexFormat.IndexType.SHORT));
    }
}
