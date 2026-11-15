package com.xgdt.dx11.perf;

import java.util.Locale;

/**
 * Formatting for the optional F3 perf line.
 */
public final class PerfDebugHud {
    private PerfDebugHud() {
    }

    /**
     * @return the line to append to the F3 screen, or {@code null} when the
     *         perf layer or the debug HUD is disabled
     */
    public static String line() {
        if (!PerfState.active() || !PerfState.config().debugHud) {
            return null;
        }
        PerfCounters.Snapshot snapshot = PerfCounters.snapshot();
        return String.format(
            Locale.ROOT,
            "DX11 perf: ON | region %s | draws %d | binds %d | uniforms %d | passes %d | uploads %d / %.2f MiB",
            PerfState.config().regionSize,
            snapshot.draws(),
            snapshot.bufferBinds(),
            snapshot.uniformBinds(),
            snapshot.renderPasses(),
            snapshot.uploads(),
            snapshot.uploadMiB());
    }
}
