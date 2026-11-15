package com.xgdt.dx11.perf.culling;

import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Captures the fog ranges vanilla uploaded for the current frame and exposes a
 * conservative section culler.
 *
 * <p>{@code FogRenderer.updateBuffer} receives the exact values written to the
 * {@code Fog} uniform (environmental and render-distance start/end).  Capturing
 * them there avoids re-deriving biome, rain, water and boss-fog rules.  The
 * debug fog toggle is tracked through {@code FogRenderer.toggleFog}: when fog
 * is disabled the uniform contains no fog and no section may be culled.</p>
 *
 * <p>A safety margin is added to both ends so a small camera or fog-range change
 * between capture and submission can never cull a section that still has
 * visible, fog-blended vertices.</p>
 */
public final class FogOcclusion {
    /** Extra distance required before a section is considered fully fogged. */
    private static final double MARGIN = 8.0;

    private static volatile boolean fogEnabled = true;
    private static volatile float environmentalEnd = Float.MAX_VALUE;
    private static volatile float renderDistanceEnd = Float.MAX_VALUE;
    private static long culledSections;

    private FogOcclusion() {
    }

    /**
     * Records the ranges passed to the fog UBO.  Called once per frame from
     * {@code FogRenderer.setupFog}; the constructor also calls the same method
     * with maximum values, which simply disables culling until the first real
     * frame.
     */
    public static void capture(
        float environmentalStart,
        float environmentalEnd,
        float renderDistanceStart,
        float renderDistanceEnd) {
        if (!Float.isFinite(environmentalEnd) || !Float.isFinite(renderDistanceEnd)) {
            return;
        }
        // The start values are not needed for the fully-fogged test, but keep
        // the guard explicit: a malformed range must not enable culling.
        if (!Float.isFinite(environmentalStart) || !Float.isFinite(renderDistanceStart)) {
            return;
        }
        FogOcclusion.environmentalEnd = environmentalEnd;
        FogOcclusion.renderDistanceEnd = renderDistanceEnd;
    }

    /** Tracks vanilla's debug fog toggle (F3+F). */
    public static void setFogEnabled(boolean enabled) {
        fogEnabled = enabled;
    }

    /**
     * @return a culler for this frame, or {@code null} when fog culling is
     *         disabled, fog is off, or the captured ranges are unusable
     */
    public static @Nullable FogCuller culler(
        double cameraX, double cameraY, double cameraZ) {
        if (!PerfState.regionActive()) {
            return null;
        }
        PerfConfig config = PerfState.config();
        if (!config.fogOcclusion || !fogEnabled) {
            return null;
        }
        float environment = environmentalEnd;
        float render = renderDistanceEnd;
        if (!Float.isFinite(environment) || !Float.isFinite(render)) {
            return null;
        }
        if (environment <= 0.0f && render <= 0.0f) {
            // Degenerate ranges would classify every section as fogged while
            // the sky pass still has its own fog rules; stay conservative.
            return null;
        }
        return new FogCuller(
            (double) environment + MARGIN,
            (double) render + MARGIN,
            cameraX,
            cameraY,
            cameraZ);
    }

    /** Records one section-layer draw that fog culling skipped. */
    public static void onCulledSection() {
        culledSections++;
    }

    public static void resetStats() {
        culledSections = 0;
    }

    public static String describe() {
        if (!PerfState.regionActive() || !PerfState.config().fogOcclusion) {
            return "fogCull=off";
        }
        return "fogCull=on"
            + " fog=" + (fogEnabled ? "on" : "off")
            + " envEnd=" + format(environmentalEnd)
            + " renderEnd=" + format(renderDistanceEnd)
            + " culled=" + culledSections;
    }

    private static String format(float value) {
        if (!Float.isFinite(value) || value > 1.0e6f) {
            return "inf";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
