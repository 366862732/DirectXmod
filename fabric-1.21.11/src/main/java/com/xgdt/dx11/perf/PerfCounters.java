package com.xgdt.dx11.perf;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Cheap render-thread counters shared by the debug HUD and the per-second
 * logger.
 *
 * <p>When the perf layer is inactive the hot methods return after a single
 * volatile read, so the default (disabled) path adds no meaningful cost to the
 * existing D3D11 command stream.  Counters are session totals; the debug
 * logger and {@code /dx11perf reset} take care of windows and resets.</p>
 */
public final class PerfCounters {
    private static final AtomicLong DRAWS = new AtomicLong();
    private static final AtomicLong INDEXED_DRAWS = new AtomicLong();
    private static final AtomicLong BUFFER_BINDS = new AtomicLong();
    private static final AtomicLong UNIFORM_BINDS = new AtomicLong();
    private static final AtomicLong RENDER_PASSES = new AtomicLong();
    private static final AtomicLong UPLOADS = new AtomicLong();
    private static final AtomicLong UPLOAD_BYTES = new AtomicLong();
    private static final AtomicLong REGION_RUNS = new AtomicLong();
    private static volatile boolean enabled;

    private PerfCounters() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void onDraw() {
        if (enabled) {
            DRAWS.incrementAndGet();
        }
    }

    public static void onIndexedDraw() {
        if (enabled) {
            INDEXED_DRAWS.incrementAndGet();
        }
    }

    public static void onBufferBind() {
        if (enabled) {
            BUFFER_BINDS.incrementAndGet();
        }
    }

    public static void onUniformBind() {
        if (enabled) {
            UNIFORM_BINDS.incrementAndGet();
        }
    }

    public static void onRenderPass() {
        if (enabled) {
            RENDER_PASSES.incrementAndGet();
        }
    }

    public static void onRegionRun() {
        if (enabled) {
            REGION_RUNS.incrementAndGet();
        }
    }

    public static void onUpload(long bytes) {
        if (enabled) {
            UPLOADS.incrementAndGet();
            if (bytes > 0) {
                UPLOAD_BYTES.addAndGet(bytes);
            }
        }
    }

    /** Raw uploaded-byte counter, used by the upload budget window. */
    public static long uploadBytes() {
        return UPLOAD_BYTES.get();
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            DRAWS.get(),
            INDEXED_DRAWS.get(),
            BUFFER_BINDS.get(),
            UNIFORM_BINDS.get(),
            RENDER_PASSES.get(),
            UPLOADS.get(),
            UPLOAD_BYTES.get(),
            REGION_RUNS.get());
    }

    public static void reset() {
        DRAWS.set(0);
        INDEXED_DRAWS.set(0);
        BUFFER_BINDS.set(0);
        UNIFORM_BINDS.set(0);
        RENDER_PASSES.set(0);
        UPLOADS.set(0);
        UPLOAD_BYTES.set(0);
        REGION_RUNS.set(0);
    }

    /** Immutable counter snapshot. */
    public record Snapshot(
        long draws,
        long indexedDraws,
        long bufferBinds,
        long uniformBinds,
        long renderPasses,
        long uploads,
        long uploadBytes,
        long regionRuns) {

        public Snapshot delta(Snapshot earlier) {
            return new Snapshot(
                this.draws - earlier.draws,
                this.indexedDraws - earlier.indexedDraws,
                this.bufferBinds - earlier.bufferBinds,
                this.uniformBinds - earlier.uniformBinds,
                this.renderPasses - earlier.renderPasses,
                this.uploads - earlier.uploads,
                this.uploadBytes - earlier.uploadBytes,
                this.regionRuns - earlier.regionRuns);
        }

        public double uploadMiB() {
            return this.uploadBytes / (1024.0 * 1024.0);
        }

        public String format() {
            return "draws=" + this.draws
                + " indexed=" + this.indexedDraws
                + " binds=" + this.bufferBinds
                + " uniforms=" + this.uniformBinds
                + " passes=" + this.renderPasses
                + " uploads=" + this.uploads
                + " uploadBytes=" + this.uploadBytes
                + " runs=" + this.regionRuns;
        }
    }
}
