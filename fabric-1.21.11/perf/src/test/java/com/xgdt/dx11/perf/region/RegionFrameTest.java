package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RegionFrameTest {
    @Test
    void packedKeysRoundTripNegativeAndPositiveCoordinates() {
        int[] values = {0, 1, -1, 7, -7, 1234, -1234, 234567, -234567};
        for (int x : values) {
            for (int y : values) {
                for (int z : values) {
                    long key = RegionFrame.pack(x, y, z);
                    assertEquals(x, RegionFrame.unpackX(key), "x");
                    assertEquals(y, RegionFrame.unpackY(key), "y");
                    assertEquals(z, RegionFrame.unpackZ(key), "z");
                }
            }
        }
    }

    @Test
    void positiveAndNegativeCoordinatesUseDifferentKeys() {
        assertNotEquals(RegionFrame.pack(1, 0, 0), RegionFrame.pack(-1, 0, 0));
        assertNotEquals(RegionFrame.pack(0, 1, 0), RegionFrame.pack(0, -1, 0));
        assertNotEquals(RegionFrame.pack(0, 0, 1), RegionFrame.pack(0, 0, -1));
        assertNotEquals(RegionFrame.pack(3, -4, 5), RegionFrame.pack(5, -4, 3));
    }
}
