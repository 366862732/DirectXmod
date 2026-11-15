package com.xgdt.dx11.perf.culling;

/**
 * Conservative fog test for one 16×16×16 section.
 *
 * <p>The terrain shader computes fog from the camera-relative vertex position:</p>
 * <pre>
 * spherical   = length(pos)
 * cylindrical = max(length(pos.xz), abs(pos.y))
 * fog         = max(linear(spherical, envStart, envEnd),
 *                   linear(cylindrical, renderStart, renderEnd))
 * </pre>
 *
 * <p>A section can only be skipped when <em>every</em> vertex is fully fogged.
 * The shader takes the maximum of the environmental (spherical) and
 * render-distance (cylindrical) terms, so the whole box is fully fogged when
 * its nearest point already exceeds <em>either</em> end.  That covers the
 * render-distance boundary as well as the much nearer environmental boundary
 * used by lava, water, blindness, darkness, weather and boss fog.  Using the
 * nearest point keeps the test conservative: a box that merely straddles a
 * boundary still contains non-fogged vertices and is never culled.
 * Coordinates are camera-relative.</p>
 *
 * <p>This class has no Minecraft dependency so the distance rules can be
 * unit-tested on the Linux host.</p>
 */
public record FogCuller(
    double environmentalEnd,
    double renderDistanceEnd,
    double cameraX,
    double cameraY,
    double cameraZ) {
    /** Width of a chunk section in blocks. */
    public static final int SECTION_SIZE = 16;

    /**
     * @param sectionOriginX section-origin block coordinate
     * @param sectionOriginY section-origin block coordinate
     * @param sectionOriginZ section-origin block coordinate
     * @return true when the whole section is beyond either fog end
     */
    public boolean isFullyFogged(
        int sectionOriginX, int sectionOriginY, int sectionOriginZ) {
        double minX = sectionOriginX - this.cameraX;
        double minY = sectionOriginY - this.cameraY;
        double minZ = sectionOriginZ - this.cameraZ;
        double maxX = minX + SECTION_SIZE;
        double maxY = minY + SECTION_SIZE;
        double maxZ = minZ + SECTION_SIZE;

        double nearestX = nearestToZero(minX, maxX);
        double nearestY = nearestToZero(minY, maxY);
        double nearestZ = nearestToZero(minZ, maxZ);

        double spherical = Math.sqrt(
            nearestX * nearestX + nearestY * nearestY + nearestZ * nearestZ);
        double cylindrical = Math.max(
            Math.sqrt(nearestX * nearestX + nearestZ * nearestZ), nearestY);
        return spherical >= this.environmentalEnd
            || cylindrical >= this.renderDistanceEnd;
    }

    /** Distance from zero to the closest point of {@code [min, max]}. */
    static double nearestToZero(double min, double max) {
        if (min > 0.0) {
            return min;
        }
        if (max < 0.0) {
            return -max;
        }
        return 0.0;
    }
}
