package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Lifecycle tests for the region map/slice refcount that do not need a GPU
 * device.  Region arenas themselves are created lazily by {@code RegionBuffer},
 * so these regions never touch the render system.
 */
class RegionLifecycleTest {
    @AfterEach
    void cleanUp() {
        RegionManager.closeAll();
    }

    @Test
    void regionIsSharedAndRemovedWhenItsLastSliceCloses() {
        RegionPos pos = new RegionPos(1, 0, -2);
        Region region = RegionManager.getOrCreate(pos, RegionSize.REGION_8);
        assertSame(region, RegionManager.getOrCreate(pos, RegionSize.REGION_8));
        assertEquals(1, RegionManager.liveRegions());

        region.retainSlice();
        region.retainSlice();
        assertEquals(2, region.liveSlices());
        assertFalse(region.isEmpty());

        region.releaseSlice();
        assertEquals(1, region.liveSlices());
        assertEquals(1, RegionManager.liveRegions());

        region.releaseSlice();
        assertEquals(0, region.liveSlices());
        assertTrue(region.isEmpty());
        assertEquals(0, RegionManager.liveRegions());
    }

    @Test
    void anExtraReleaseDoesNotUnderflowTheRefcount() {
        Region region = new Region(new RegionPos(0, 0, 0), RegionSize.REGION_4);
        region.releaseSlice();
        assertEquals(0, region.liveSlices());
        assertTrue(region.isEmpty());
    }

    @Test
    void discardOnlyRemovesEmptyRegions() {
        Region region = RegionManager.getOrCreate(new RegionPos(2, 2, 2), RegionSize.REGION_4);
        region.retainSlice();
        RegionManager.discardIfEmpty(region);
        assertEquals(1, RegionManager.liveRegions());
        assertFalse(region.isDetached());

        region.releaseSlice();
        assertEquals(0, RegionManager.liveRegions());

        Region empty = RegionManager.getOrCreate(new RegionPos(3, 3, 3), RegionSize.REGION_4);
        RegionManager.discardIfEmpty(empty);
        assertEquals(0, RegionManager.liveRegions());
        assertTrue(empty.isDetached());
    }

    @Test
    void clearDetachesLiveRegionsWithoutLosingTheirSlices() {
        Region region = RegionManager.getOrCreate(new RegionPos(-1, 0, 1), RegionSize.REGION_8);
        region.retainSlice();

        RegionManager.clear();
        assertEquals(0, RegionManager.liveRegions());
        assertTrue(region.isDetached());
        assertEquals(1, region.liveSlices());

        region.releaseSlice();
        assertEquals(0, region.liveSlices());
    }
}
