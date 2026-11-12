package com.xgdt.dx11.config;

import com.xgdt.dx11.dx11.Dx11Native;
import java.util.Locale;

/**
 * Early, dependency-free configuration for the D3D11 backport.
 *
 * <p>The backend is opt-in: without {@link #ENABLE_PROPERTY} no native
 * library is loaded and the vanilla OpenGL path is untouched.</p>
 */
public final class Dx11Config {
    /** auto | dx11 | gl */
    public static final String BACKEND_PROPERTY = "gl4dx12.backend";
    /** Safety gate for the still-incomplete backend. */
    public static final String ENABLE_PROPERTY = "gl4dx12.dx11.enabled";
    /**
     * Enables verbose per-frame logging (draws, bindings, clears, presents).
     * Off by default: those messages produce gigabytes of output during normal
     * play. Pass {@code -Dgl4dx11.debug=true} to turn them back on.
     */
    public static final String DEBUG_PROPERTY = "gl4dx11.debug";

    private static volatile String probeResult = "not run";
    private static volatile boolean probeOk;
    private static volatile boolean probeAttempted;

    private Dx11Config() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static String backend() {
        return System.getProperty(BACKEND_PROPERTY, "auto").toLowerCase(Locale.ROOT);
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
     * True only when the user explicitly enabled the backend and the native
     * probe succeeded. The property is checked before the probe so a normal
     * launch never loads {@code dx11_mc.dll} or creates a D3D11 device.
     */
    public static boolean shouldUseDx11() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            return false;
        }
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
        setProbeResult("not run");
    }
}
