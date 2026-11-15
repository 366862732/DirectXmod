package com.xgdt.dx11.perf.culling;

import java.util.Arrays;

/**
 * Sorted set of section coordinates backed by a primitive {@code long[]}.
 *
 * <p>The renderer needs a per-frame membership test for a few thousand visible
 * sections.  A sorted array keeps the capture allocation-free after warm-up,
 * avoids boxing, and answers membership in {@code O(log n)}.  Coordinates are
 * packed into one long as 22 signed bits for X, 20 for Y and 22 for Z.  That
 * covers the full world border (±1,874,999 sections) without collisions, while
 * still leaving ample headroom for the much smaller vertical build range.</p>
 *
 * <p>No Minecraft dependency, so the lookup and range rules are unit-tested on
 * the Linux host.</p>
 */
public final class SectionVisibilitySet {
    private static final int XZ_BITS = 22;
    private static final int Y_BITS = 20;
    private static final int XZ_MASK = (1 << XZ_BITS) - 1;
    private static final int Y_MASK = (1 << Y_BITS) - 1;
    private static final int Z_SHIFT = 0;
    private static final int Y_SHIFT = XZ_BITS;
    private static final int X_SHIFT = XZ_BITS + Y_BITS;

    private long[] keys = new long[256];
    private int size;
    private boolean captured;

    public void clear() {
        this.size = 0;
        this.captured = false;
    }

    /** Adds a section coordinate.  Duplicate keys are harmless. */
    public void add(int sectionX, int sectionY, int sectionZ) {
        if (this.size == this.keys.length) {
            this.keys = Arrays.copyOf(this.keys, this.keys.length * 2);
        }
        this.keys[this.size++] = key(sectionX, sectionY, sectionZ);
    }

    /** Sorts the captured keys and marks the set usable for lookups. */
    public void freeze() {
        Arrays.sort(this.keys, 0, this.size);
        this.captured = true;
    }

    public boolean captured() {
        return this.captured;
    }

    public int size() {
        return this.size;
    }

    public boolean contains(int sectionX, int sectionY, int sectionZ) {
        return containsKey(key(sectionX, sectionY, sectionZ));
    }

    public boolean containsKey(long key) {
        if (!this.captured || this.size == 0) {
            return false;
        }
        return Arrays.binarySearch(this.keys, 0, this.size, key) >= 0;
    }

    /**
     * True when any section in the inclusive coordinate range is present.
     * Used for entity bounding boxes, which may straddle section boundaries.
     */
    public boolean intersects(
        int minSectionX,
        int minSectionY,
        int minSectionZ,
        int maxSectionX,
        int maxSectionY,
        int maxSectionZ) {
        if (!this.captured || this.size == 0) {
            return false;
        }
        for (int x = minSectionX; x <= maxSectionX; x++) {
            for (int y = minSectionY; y <= maxSectionY; y++) {
                for (int z = minSectionZ; z <= maxSectionZ; z++) {
                    if (contains(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Packs the three section coordinates into one key. */
    public static long key(int sectionX, int sectionY, int sectionZ) {
        return ((long) (sectionX & XZ_MASK) << X_SHIFT)
            | ((long) (sectionY & Y_MASK) << Y_SHIFT)
            | ((long) (sectionZ & XZ_MASK) << Z_SHIFT);
    }
}
