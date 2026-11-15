package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class RegionVertexRebaseTest {
    @Test
    void addsTheSectionToRegionDeltaToEveryPosition() {
        int stride = 20;
        ByteBuffer source = ByteBuffer.allocate(stride * 2).order(ByteOrder.nativeOrder());
        source.putFloat(0, 1.0f).putFloat(4, 2.0f).putFloat(8, 3.0f).putFloat(12, 10.0f);
        source.putFloat(stride, 4.0f).putFloat(stride + 4, 5.0f).putFloat(stride + 8, 6.0f);
        source.putFloat(stride + 12, 20.0f);
        source.position(0);
        source.limit(stride * 2);

        ByteBuffer target = ByteBuffer.allocate(stride * 2);
        RegionVertexRebase.rebase(source, target, stride * 2, stride, 0, 16.0f, -64.0f, 32.0f);

        assertEquals(17.0f, target.getFloat(0));
        assertEquals(-62.0f, target.getFloat(4));
        assertEquals(35.0f, target.getFloat(8));
        assertEquals(10.0f, target.getFloat(12));
        assertEquals(20.0f, target.getFloat(stride));
        assertEquals(-59.0f, target.getFloat(stride + 4));
        assertEquals(38.0f, target.getFloat(stride + 8));
        assertEquals(20.0f, target.getFloat(stride + 12));
        assertEquals(0, target.position());
        assertEquals(stride * 2, target.limit());
    }

    @Test
    void copiesFromTheSourcePositionAndLeavesTheSourceUntouched() {
        int stride = 16;
        ByteBuffer source = ByteBuffer.allocate(4 + stride * 2).order(ByteOrder.nativeOrder());
        source.putFloat(4, 1.0f).putFloat(8, 2.0f).putFloat(12, 3.0f);
        source.putFloat(4 + stride, 7.0f).putFloat(4 + stride + 4, 8.0f);
        source.putFloat(4 + stride + 8, 9.0f);
        source.position(4);
        source.limit(4 + stride * 2);

        ByteBuffer target = ByteBuffer.allocate(stride * 2);
        RegionVertexRebase.rebase(source, target, stride * 2, stride, 0, 1.0f, 1.0f, 1.0f);

        assertEquals(4, source.position());
        assertEquals(4 + stride * 2, source.limit());
        assertEquals(2.0f, target.getFloat(0));
        assertEquals(3.0f, target.getFloat(4));
        assertEquals(4.0f, target.getFloat(8));
        assertEquals(8.0f, target.getFloat(stride));
        assertEquals(9.0f, target.getFloat(stride + 4));
        assertEquals(10.0f, target.getFloat(stride + 8));
    }
}
