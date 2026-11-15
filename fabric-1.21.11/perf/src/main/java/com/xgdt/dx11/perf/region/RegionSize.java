package com.xgdt.dx11.perf.region;

/**
 * Region dimensions in chunk sections.
 *
 * <p>The default is Sodium's 8×4×8 shape: wide enough to coalesce a useful
 * number of adjacent sections, shallow enough to avoid wasting memory on the
 * vertical range most worlds actually use.  {@code REGION_4} is kept as a
 * lower-memory benchmark variant.</p>
 */
public enum RegionSize {
    REGION_4(4, 4, 4),
    REGION_8(8, 4, 8);

    private final int sectionsX;
    private final int sectionsY;
    private final int sectionsZ;

    RegionSize(int sectionsX, int sectionsY, int sectionsZ) {
        this.sectionsX = sectionsX;
        this.sectionsY = sectionsY;
        this.sectionsZ = sectionsZ;
    }

    public int sectionsX() {
        return this.sectionsX;
    }

    public int sectionsY() {
        return this.sectionsY;
    }

    public int sectionsZ() {
        return this.sectionsZ;
    }

    public int sectionCount() {
        return this.sectionsX * this.sectionsY * this.sectionsZ;
    }

    /** Region width in blocks (16 blocks per section). */
    public int blocksX() {
        return this.sectionsX * 16;
    }

    public int blocksY() {
        return this.sectionsY * 16;
    }

    public int blocksZ() {
        return this.sectionsZ * 16;
    }

    public static RegionSize fromName(String name) {
        if (name == null) {
            return REGION_8;
        }
        return switch (name.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "REGION_4", "4", "4X4X4" -> REGION_4;
            case "REGION_8", "8", "8X4X8" -> REGION_8;
            default -> REGION_8;
        };
    }

    public static boolean isKnown(String name) {
        if (name == null) {
            return false;
        }
        return switch (name.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "REGION_4", "4", "4X4X4", "REGION_8", "8", "8X4X8" -> true;
            default -> false;
        };
    }
}
