package com.xgdt.dx11.perf.region;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Byte-range allocator for a growable region vertex/index arena.
 *
 * <p>This class is deliberately independent from the game and the native
 * backend: it is the part of the region renderer that can be unit-tested on a
 * development machine without a D3D11 device.  It is <strong>not</strong>
 * thread-safe; callers must confine it to the render thread, matching the
 * render-thread upload and submission paths.</p>
 *
 * <p>Allocations are first-fit, aligned to the requested alignment, and freed
 * ranges are coalesced with their neighbours.  Sizes are exact: callers that
 * allocate whole vertices should pass a size that is a multiple of the vertex
 * stride.</p>
 */
public final class RegionAllocator {
    private long capacity;
    private long liveBytes;
    private final List<FreeRange> freeRanges = new ArrayList<>();
    private final Map<Long, Allocation> allocations = new LinkedHashMap<>();

    public RegionAllocator(long capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be non-negative");
        }
        this.capacity = capacity;
        if (capacity > 0) {
            this.freeRanges.add(new FreeRange(0, capacity));
        }
    }

    public long capacity() {
        return this.capacity;
    }

    public long liveBytes() {
        return this.liveBytes;
    }

    public int liveAllocationCount() {
        return this.allocations.size();
    }

    /**
     * Allocates {@code size} bytes at an offset divisible by {@code alignment}.
     *
     * @return the allocation, or {@code null} when no free range is large
     *         enough
     */
    public Allocation allocate(long size, long alignment) {
        checkSize(size);
        checkAlignment(alignment);

        for (int index = 0; index < this.freeRanges.size(); index++) {
            FreeRange range = this.freeRanges.get(index);
            long start = alignUp(range.start, alignment);
            if (start > range.end || size > range.end - start) {
                continue;
            }

            long end = start + size;
            if (start == range.start && end == range.end) {
                this.freeRanges.remove(index);
            } else if (start == range.start) {
                range.start = end;
            } else if (end == range.end) {
                range.end = start;
            } else {
                // Split the free range around the allocation.
                long oldEnd = range.end;
                range.end = start;
                this.freeRanges.add(index + 1, new FreeRange(end, oldEnd));
            }

            Allocation allocation = new Allocation(start, size);
            this.allocations.put(start, allocation);
            this.liveBytes += size;
            return allocation;
        }
        return null;
    }

    /**
     * Frees a previously returned allocation.
     *
     * @return {@code true} when the allocation was live; {@code false} for an
     *         unknown or already-freed range
     */
    public boolean free(long offset, long size) {
        Allocation allocation = this.allocations.get(offset);
        if (allocation == null || allocation.size() != size) {
            return false;
        }
        this.allocations.remove(offset);
        this.liveBytes -= size;
        insertFreeRange(offset, offset + size);
        return true;
    }

    public boolean free(Allocation allocation) {
        return allocation != null && free(allocation.offset(), allocation.size());
    }

    /**
     * Grows the arena.  The caller is responsible for copying the existing
     * bytes into the new native buffer; this method only updates the address
     * space model.  Shrinking is rejected because live allocations may occupy
     * the removed tail.
     *
     * @return {@code true} when the capacity grew
     */
    public boolean grow(long newCapacity) {
        if (newCapacity < this.capacity) {
            throw new IllegalArgumentException("cannot shrink a region arena");
        }
        if (newCapacity == this.capacity) {
            return false;
        }

        long oldCapacity = this.capacity;
        this.capacity = newCapacity;
        insertFreeRange(oldCapacity, newCapacity);
        return true;
    }

    /** Frees every live allocation and resets the arena to one empty range. */
    public void reset() {
        this.allocations.clear();
        this.freeRanges.clear();
        if (this.capacity > 0) {
            this.freeRanges.add(new FreeRange(0, this.capacity));
        }
        this.liveBytes = 0;
    }

    public Allocation allocation(long offset) {
        return this.allocations.get(offset);
    }

    /** Immutable snapshot of the free list, in ascending offset order. */
    public List<FreeRange> freeRanges() {
        return List.copyOf(this.freeRanges);
    }

    /** Immutable snapshot of live allocations, in allocation order. */
    public List<Allocation> allocations() {
        return List.copyOf(this.allocations.values());
    }

    public long largestFreeRange() {
        long largest = 0;
        for (FreeRange range : this.freeRanges) {
            largest = Math.max(largest, range.size());
        }
        return largest;
    }

    /**
     * Returns a compact, human-readable invariant check used by tests and the
     * leak diagnostics.  An empty string means the allocator is consistent.
     */
    public String validate() {
        long expectedFree = this.capacity - this.liveBytes;
        long actualFree = 0;
        long previousEnd = -1;
        for (FreeRange range : this.freeRanges) {
            if (range.start < 0 || range.end < range.start || range.end > this.capacity) {
                return "free range out of bounds: " + range;
            }
            if (range.start < previousEnd) {
                return "overlapping free ranges: " + this.freeRanges;
            }
            if (range.start == previousEnd) {
                return "adjacent free ranges were not coalesced: " + this.freeRanges;
            }
            previousEnd = range.end;
            actualFree += range.size();
        }
        if (actualFree != expectedFree) {
            return "free bytes " + actualFree + " != capacity - live " + expectedFree;
        }

        // The map is keyed by offset but iterates in insertion order.  Sort a
        // snapshot so the overlap/coverage checks below are order-independent.
        List<Allocation> ordered = new ArrayList<>(this.allocations.values());
        ordered.sort(java.util.Comparator.comparingLong(Allocation::offset));

        long actualLive = 0;
        long previousAllocationEnd = -1;
        for (Allocation allocation : ordered) {
            if (allocation.size <= 0 || allocation.offset < 0) {
                return "invalid allocation: " + allocation;
            }
            if (allocation.offset < previousAllocationEnd) {
                return "overlapping allocations: " + ordered;
            }
            previousAllocationEnd = allocation.offset + allocation.size;
            if (previousAllocationEnd > this.capacity) {
                return "allocation out of bounds: " + allocation;
            }
            actualLive += allocation.size;
        }
        if (actualLive != this.liveBytes) {
            return "live bytes " + this.liveBytes + " != sum of allocations " + actualLive;
        }
        if (actualLive + actualFree != this.capacity) {
            return "allocations + free space do not cover the arena";
        }
        return "";
    }

    private void insertFreeRange(long start, long end) {
        if (start >= end) {
            return;
        }
        int index = 0;
        while (index < this.freeRanges.size()
            && this.freeRanges.get(index).start < start) {
            index++;
        }

        // Merge with the previous range when they touch.
        if (index > 0 && this.freeRanges.get(index - 1).end == start) {
            this.freeRanges.get(index - 1).end = end;
            if (index < this.freeRanges.size()
                && this.freeRanges.get(index).start == end) {
                this.freeRanges.get(index - 1).end = this.freeRanges.get(index).end;
                this.freeRanges.remove(index);
            }
            return;
        }

        // Merge with the next range when they touch.
        if (index < this.freeRanges.size()
            && this.freeRanges.get(index).start == end) {
            this.freeRanges.get(index).start = start;
            return;
        }

        this.freeRanges.add(index, new FreeRange(start, end));
    }

    private static long alignUp(long value, long alignment) {
        long remainder = value % alignment;
        if (remainder == 0) {
            return value;
        }
        long increment = alignment - remainder;
        if (value > Long.MAX_VALUE - increment) {
            return Long.MAX_VALUE;
        }
        return value + increment;
    }

    private static void checkSize(long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
    }

    private static void checkAlignment(long alignment) {
        if (alignment <= 0) {
            throw new IllegalArgumentException("alignment must be positive");
        }
    }

    /** A live allocation in the arena. */
    public record Allocation(long offset, long size) {
        public long end() {
            return this.offset + this.size;
        }
    }

    /** A free byte range.  Mutable internally; exposed as an immutable view. */
    public static final class FreeRange {
        private long start;
        private long end;

        private FreeRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long start() {
            return this.start;
        }

        public long end() {
            return this.end;
        }

        public long size() {
            return this.end - this.start;
        }

        @Override
        public String toString() {
            return "[" + this.start + ", " + this.end + ")";
        }
    }
}
