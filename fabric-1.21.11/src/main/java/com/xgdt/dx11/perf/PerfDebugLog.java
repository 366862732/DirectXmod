package com.xgdt.dx11.perf;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-second counter logger for the perf layer.
 *
 * <p>This is intentionally driven from the render path rather than a tick
 * event: the counters describe submitted work, and logging from the first
 * render pass of a frame avoids another mixin.  Output is gated by
 * {@code perf.debugLog} or {@code -Dgl4dx11.perf.debug=true}.</p>
 */
public final class PerfDebugLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static volatile long lastLogNanos;
    private static volatile PerfCounters.Snapshot lastSnapshot = new PerfCounters.Snapshot(0, 0, 0, 0, 0, 0, 0, 0);

    private PerfDebugLog() {
    }

    public static void maybeLog() {
        if (!PerfState.active()) {
            return;
        }
        PerfConfig config = PerfState.config();
        if (!config.debugLog && !Boolean.getBoolean(PerfState.DEBUG_PROPERTY)) {
            return;
        }

        long now = System.nanoTime();
        long elapsed = now - lastLogNanos;
        if (elapsed < 1_000_000_000L) {
            return;
        }

        PerfCounters.Snapshot current = PerfCounters.snapshot();
        PerfCounters.Snapshot delta = current.delta(lastSnapshot);
        lastSnapshot = current;
        lastLogNanos = now;

        LOGGER.info(
            String.format(
                Locale.ROOT,
                "[dx11-perf] %.2fs window | draws=%d indexed=%d binds=%d uniforms=%d passes=%d uploads=%d (%.2f MiB)",
                elapsed / 1_000_000_000.0,
                delta.draws(),
                delta.indexedDraws(),
                delta.bufferBinds(),
                delta.uniformBinds(),
                delta.renderPasses(),
                delta.uploads(),
                delta.uploadMiB()));
    }
}
