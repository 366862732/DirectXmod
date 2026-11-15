package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RegionPosTest {
    @Test
    void positiveSectionsMapToTheExpectedRegion() {
        RegionPos position = RegionPos.fromSection(17, 5, 31, RegionSize.REGION_8);
        assertEquals(2, position.x());
        assertEquals(1, position.y());
        assertEquals(3, position.z());
    }

    @Test
    void negativeSectionsUseFloorDivision() {
        RegionPos position = RegionPos.fromSection(-1, -5, -17, RegionSize.REGION_8);
        assertEquals(-1, position.x());
        assertEquals(-2, position.y());
        assertEquals(-3, position.z());
    }

    @Test
    void regionOriginsAreInBlocksAndSections() {
        RegionPos position = new RegionPos(2, 1, 3);
        assertEquals(256, position.originBlockX(RegionSize.REGION_8));
        assertEquals(64, position.originBlockY(RegionSize.REGION_8));
        assertEquals(384, position.originBlockZ(RegionSize.REGION_8));
        assertEquals(16, position.sectionX(RegionSize.REGION_8));
        assertEquals(4, position.sectionY(RegionSize.REGION_8));
        assertEquals(24, position.sectionZ(RegionSize.REGION_8));
    }

    @Test
    void regionFourHasTheExpectedShape() {
        assertEquals(64, RegionSize.REGION_4.sectionCount());
        assertEquals(256, RegionSize.REGION_8.sectionCount());
        assertEquals(64, RegionSize.REGION_4.blocksX());
        assertEquals(128, RegionSize.REGION_8.blocksX());
        assertEquals(64, RegionSize.REGION_8.blocksY());
    }
}
