package com.xgdt.dx11.config;

import com.xgdt.dx11.dx11.Dx11Native;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Early, dependency-free configuration for the D3D11 backport.
 *
 * <p>The backend is selected automatically on Windows: the native library is
 * probed at startup and installed only when the probe succeeds.  A missing DLL
 * or a failed probe leaves the vanilla OpenGL path untouched.  Set
 * {@link #BACKEND_PROPERTY} to {@code gl} for an explicit opt-out.</p>
 */
public final class Dx11Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    /** auto | dx11 | gl */
    public static final String BACKEND_PROPERTY = "gl4dx.backend";
    /**
     * Optional feature-level clamp used to validate the FL10.x path on a WARP
     * device that would otherwise report a higher level.  Accepted forms:
     * {@code 10_0}, {@code 10.0}, {@code 10_1}, {@code 11_0}, ... .
     */
    public static final String MAX_FEATURE_LEVEL_PROPERTY = "gl4dx11.maxFeatureLevel";
    /**
     * Enables verbose per-frame logging (draws, bindings, clears, presents).
     * Off by default: those messages produce gigabytes of output during normal
     * play. Pass {@code -Dgl4dx11.debug=true} to turn them back on.
     */
    public static final String DEBUG_PROPERTY = "gl4dx11.debug";

    private static volatile String probeResult = "not run";
    private static volatile boolean probeOk;
    private static volatile boolean probeAttempted;
    private static volatile String appliedFeatureLevel;

    /**
     * Per-frame hot path (the flipFrame mixins ask {@link #shouldUseDx11()}
     * twice per frame).  The raw property is still read so tests and dev
     * overrides keep working, but the transformed result is cached per raw
     * value: the old code allocated a fresh lowercase string on every call.
     * {@code getProperty} returns the same stored instance each call, so the
     * reference-equality fast path inside {@code String.equals} makes the
     * recheck nearly free.
     */
    private static volatile String cachedOsRaw;
    private static volatile boolean cachedOsWindows;
    private static volatile String cachedBackendRaw;
    private static volatile String cachedBackendMode = "auto";

    private Dx11Config() {
    }

    public static boolean isWindows() {
        String raw = System.getProperty("os.name", "");
        String cached = cachedOsRaw;
        if (cached != raw && !raw.equals(cached)) {
            cachedOsWindows = raw.toLowerCase(Locale.ROOT).contains("win");
            cachedOsRaw = raw;
        }
        return cachedOsWindows;
    }

    public static String backend() {
        String raw = System.getProperty(BACKEND_PROPERTY, "auto");
        String cached = cachedBackendRaw;
        if (cached != raw && !raw.equals(cached)) {
            cachedBackendMode = raw.toLowerCase(Locale.ROOT);
            cachedBackendRaw = raw;
        }
        return cachedBackendMode;
    }

    /** True when verbose per-frame diagnostics were explicitly requested. */
    public static boolean debugLogging() {
        return Boolean.getBoolean(DEBUG_PROPERTY);
    }

    public static boolean isDx11Requested() {
        if (!isWindows()) {
            return false;
        }
        String backend = backend();
        return backend.equals("auto") || backend.equals("dx11") || backend.equals("d3d11");
    }

    public static String maxFeatureLevel() {
        return System.getProperty(MAX_FEATURE_LEVEL_PROPERTY, "").trim();
    }

    /**
     * Parses a feature-level clamp such as {@code 10_1} into
     * {@code {major, minor}}.  Returns {@code null} for malformed or
     * unsupported values.
     */
    public static int[] parseFeatureLevel(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.startsWith("D3D_FEATURE_LEVEL_")) {
            value = value.substring("D3D_FEATURE_LEVEL_".length());
        }
        value = value.replace('.', '_');
        String[] parts = value.split("_");
        if (parts.length != 2) {
            return null;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (!isKnownFeatureLevel(major, minor)) {
                return null;
            }
            return new int[] {major, minor};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isKnownFeatureLevel(int major, int minor) {
        return (major == 9 && minor >= 1 && minor <= 3)
            || (major == 10 && minor >= 0 && minor <= 1)
            || (major == 11 && minor >= 0 && minor <= 1)
            || (major == 12 && minor >= 0 && minor <= 2);
    }

    /**
     * Applies {@link #MAX_FEATURE_LEVEL_PROPERTY} to the native device factory.
     * Must run before any D3D11 device is created.  On a non-Windows host or
     * without the property this is a no-op.
     */
    public static synchronized void applyFeatureLevelCap() {
        if (!isWindows()) {
            return;
        }
        String raw = maxFeatureLevel();
        if (raw.isEmpty()) {
            return;
        }
        if (raw.equals(appliedFeatureLevel)) {
            return;
        }

        int[] parsed = parseFeatureLevel(raw);
        if (parsed == null) {
            LOGGER.warn(
                "[dx11] Ignoring invalid -D{}={} (expected e.g. 10_0 or 10_1)",
                MAX_FEATURE_LEVEL_PROPERTY,
                raw);
            return;
        }

        try {
            Dx11Native.nativeSetMaxFeatureLevel(parsed[0], parsed[1]);
            appliedFeatureLevel = raw;
            LOGGER.info(
                "[dx11] Feature-level clamp requested: {}_{}", parsed[0], parsed[1]);
        } catch (Throwable t) {
            LOGGER.warn(
                "[dx11] Could not apply feature-level clamp {}_{}: {}",
                parsed[0],
                parsed[1],
                t.toString());
        }
    }

    /**
     * Runs the native probe at most once.  This must be callable before the
     * GLFW window is created: the {@code Window} mixin needs to know whether to
     * request {@code GLFW_NO_API}.
     */
    public static synchronized void ensureProbe() {
        if (probeAttempted) {
            return;
        }
        probeAttempted = true;
        if (!isWindows()) {
            setProbeResult("SKIP: not Windows");
            return;
        }
        applyFeatureLevelCap();
        try {
            if (!Dx11Native.isLoaded()) {
                setProbeResult("ERROR: dx11_mc.dll was not loaded");
                return;
            }
            setProbeResult(Dx11Native.dx11Probe());
        } catch (Throwable t) {
            setProbeResult("ERROR: " + t);
        }
    }

    /**
     * True when the platform and backend selection allow D3D11 and the native
     * probe succeeded.  The probe runs automatically on Windows; a missing DLL
     * or a failed probe returns false so the vanilla OpenGL path stays in use.
     */
    public static boolean shouldUseDx11() {
        if (!isDx11Requested()) {
            return false;
        }
        ensureProbe();
        return probeOk;
    }

    public static void setProbeResult(String result) {
        probeResult = result == null ? "null" : result;
        probeOk = probeResult.startsWith("OK");
    }

    public static String probeResult() {
        return probeResult;
    }

    public static boolean probeOk() {
        return probeOk;
    }

    /** Test hook: restores the process-wide probe state to its initial values. */
    static void resetProbeState() {
        probeAttempted = false;
        appliedFeatureLevel = null;
        setProbeResult("not run");
    }
}
