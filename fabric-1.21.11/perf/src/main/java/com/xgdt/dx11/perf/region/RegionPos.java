package com.xgdt.dx11.perf.region;

/**
 * Region grid coordinates.  A region contains {@link RegionSize} chunk
 * sections and is keyed by flooring the section coordinates, so negative
 * section coordinates map to the correct negative region.
 */
public record RegionPos(int x, int y, int z) {
    public static RegionPos fromSection(
        int sectionX, int sectionY, int sectionZ, RegionSize size) {
        return new RegionPos(
            Math.floorDiv(sectionX, size.sectionsX()),
            Math.floorDiv(sectionY, size.sectionsY()),
            Math.floorDiv(sectionZ, size.sectionsZ()));
    }

    /** World block X of the region's minimum corner. */
    public int originBlockX(RegionSize size) {
        return this.x * size.blocksX();
    }

    public int originBlockY(RegionSize size) {
        return this.y * size.blocksY();
    }

    public int originBlockZ(RegionSize size) {
        return this.z * size.blocksZ();
    }

    /** Section coordinate of the region's minimum corner. */
    public int sectionX(RegionSize size) {
        return this.x * size.sectionsX();
    }

    public int sectionY(RegionSize size) {
        return this.y * size.sectionsY();
    }

    public int sectionZ(RegionSize size) {
        return this.z * size.sectionsZ();
    }
}
