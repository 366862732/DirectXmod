package com.xgdt.dx11.config;

import com.xgdt.dx11.dx11.Dx11Native;
import java.util.Locale;

/**
 * Early, dependency-free configuration for the D3D11 backport.
 *
 * <p>The backend is opt-in while it is being developed. Once the full
 * {@code GpuDevice} implementation is complete the default will become
 * {@code auto}: probe D3D11 feature level 10_1+ and use it when available,
 * otherwise leave the vanilla OpenGL path untouched.</p>
 */
public final class Dx11Config {
    /** auto | dx11 | gl */
    public static final String BACKEND_PROPERTY = "gl4dx12.backend";
    /** Safety gate for the still-incomplete backend. */
    public static final String ENABLE_PROPERTY = "gl4dx12.dx11.enabled";

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
     * True only when the native probe succeeded and the user explicitly enabled
     * the experimental backend. This keeps 1.21.11 bootable with OpenGL while
     * the D3D11 implementation is being completed.
     */
    public static boolean shouldUseDx11() {
        if (!isDx11Requested()) {
            return false;
        }
        ensureProbe();
        return probeOk && Boolean.getBoolean(ENABLE_PROPERTY);
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
}
