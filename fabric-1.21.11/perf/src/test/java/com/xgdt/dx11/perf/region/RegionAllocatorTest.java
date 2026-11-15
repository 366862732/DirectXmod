package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RegionAllocatorTest {
    @Test
    void allocatesFromTheStartAndAlignsTheSecondAllocation() {
        RegionAllocator allocator = new RegionAllocator(1024);
        RegionAllocator.Allocation first = allocator.allocate(100, 32);
        RegionAllocator.Allocation second = allocator.allocate(100, 32);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(0, first.offset());
        assertEquals(128, second.offset());
        assertEquals(200, allocator.liveBytes());
        assertEquals("", allocator.validate());
    }

    @Test
    void keepsWholeVertexOffsetsForANonPowerOfTwoStride() {
        RegionAllocator allocator = new RegionAllocator(4096);
        // 20-byte stride is the compact vertex layout; alignment must not be
        // treated as a power-of-two bitmask.
        RegionAllocator.Allocation first = allocator.allocate(20 * 3, 20);
        RegionAllocator.Allocation second = allocator.allocate(20 * 5, 20);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(0, first.offset() % 20);
        assertEquals(0, second.offset() % 20);
        assertEquals(60, second.offset());

        assertTrue(allocator.free(first));
        RegionAllocator.Allocation reused = allocator.allocate(20, 16);
        assertNotNull(reused);
        assertEquals(0, reused.offset() % 20);
        assertEquals("", allocator.validate());
    }

    @Test
    void freesAndCoalescesAdjacentRanges() {
        RegionAllocator allocator = new RegionAllocator(1024);
        RegionAllocator.Allocation first = allocator.allocate(100, 1);
        RegionAllocator.Allocation second = allocator.allocate(100, 1);

        assertTrue(allocator.free(first));
        assertTrue(allocator.free(second));
        assertEquals(1, allocator.freeRanges().size());
        assertEquals(0, allocator.freeRanges().get(0).start());
        assertEquals(1024, allocator.freeRanges().get(0).end());
        assertEquals("", allocator.validate());
    }

    @Test
    void reusesAFreedRange() {
        RegionAllocator allocator = new RegionAllocator(256);
        RegionAllocator.Allocation first = allocator.allocate(128, 1);
        assertTrue(allocator.free(first));

        RegionAllocator.Allocation reused = allocator.allocate(64, 1);
        assertNotNull(reused);
        assertEquals(0, reused.offset());
        assertEquals("", allocator.validate());
    }

    @Test
    void splitsFreeRangesAroundAnAllocation() {
        RegionAllocator allocator = new RegionAllocator(256);
        RegionAllocator.Allocation first = allocator.allocate(64, 16);
        RegionAllocator.Allocation second = allocator.allocate(64, 16);
        assertTrue(allocator.free(first));

        // The 0..64 gap is too small for a 128-byte allocation aligned at 64,
        // so the allocator must use the free tail starting at 128.
        RegionAllocator.Allocation third = allocator.allocate(128, 64);
        assertNotNull(third);
        assertEquals(128, third.offset());
        assertEquals("", allocator.validate());
        assertEquals(64, allocator.freeRanges().get(0).size());
    }

    @Test
    void growsCapacityAndKeepsLiveAllocations() {
        RegionAllocator allocator = new RegionAllocator(128);
        RegionAllocator.Allocation first = allocator.allocate(128, 1);
        assertNotNull(first);

        assertTrue(allocator.grow(512));
        assertEquals(512, allocator.capacity());
        RegionAllocator.Allocation second = allocator.allocate(128, 1);
        assertNotNull(second);
        assertEquals(128, second.offset());
        assertEquals("", allocator.validate());
    }

    @Test
    void shrinkingIsRejected() {
        RegionAllocator allocator = new RegionAllocator(128);
        assertThrows(IllegalArgumentException.class, () -> allocator.grow(64));
    }

    @Test
    void returnsNullWhenTheArenaIsFull() {
        RegionAllocator allocator = new RegionAllocator(100);
        assertNotNull(allocator.allocate(100, 1));
        assertNull(allocator.allocate(1, 1));
    }

    @Test
    void rejectsInvalidArguments() {
        RegionAllocator allocator = new RegionAllocator(128);
        assertThrows(IllegalArgumentException.class, () -> allocator.allocate(0, 1));
        assertThrows(IllegalArgumentException.class, () -> allocator.allocate(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new RegionAllocator(-1));
    }

    @Test
    void rejectsDoubleFree() {
        RegionAllocator allocator = new RegionAllocator(128);
        RegionAllocator.Allocation allocation = allocator.allocate(32, 1);
        assertTrue(allocator.free(allocation));
        assertFalse(allocator.free(allocation));
        assertEquals("", allocator.validate());
    }

    @Test
    void resetClearsAllocationsAndFreeSpace() {
        RegionAllocator allocator = new RegionAllocator(256);
        allocator.allocate(64, 1);
        allocator.allocate(64, 1);
        allocator.reset();

        assertEquals(0, allocator.liveBytes());
        assertEquals(0, allocator.liveAllocationCount());
        assertEquals(1, allocator.freeRanges().size());
        assertEquals(256, allocator.largestFreeRange());
        assertEquals("", allocator.validate());
    }

    @Test
    void randomizedOperationsMatchABitmapModel() {
        RegionAllocator allocator = new RegionAllocator(4096);
        boolean[] used = new boolean[4096];
        List<RegionAllocator.Allocation> live = new ArrayList<>();
        Random random = new Random(0x5EED);

        for (int step = 0; step < 4000; step++) {
            boolean allocate = live.isEmpty() || random.nextInt(100) < 60;
            if (allocate) {
                long size = 1 + random.nextInt(96);
                long alignment = 1L << random.nextInt(6); // 1, 2, 4, 8, 16, 32
                RegionAllocator.Allocation allocation = allocator.allocate(size, alignment);
                if (allocation != null) {
                    assertEquals(0, allocation.offset() % alignment);
                    for (long i = 0; i < allocation.size(); i++) {
                        int index = (int) (allocation.offset() + i);
                        assertFalse(used[index], "overlapping allocation at " + index);
                        used[index] = true;
                    }
                    live.add(allocation);
                }
            } else {
                int index = random.nextInt(live.size());
                RegionAllocator.Allocation allocation = live.remove(index);
                assertTrue(allocator.free(allocation));
                for (long i = 0; i < allocation.size(); i++) {
                    used[(int) (allocation.offset() + i)] = false;
                }
            }

            assertEquals("", allocator.validate());
            assertEquals(live.size(), allocator.liveAllocationCount());
        }
    }
}
