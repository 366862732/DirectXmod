package com.xgdt.dx11.perf.overlay;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xgdt.dx11.perf.PerfCounters;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;

/**
 * Rolling measurements for the debug overlay.
 *
 * <p>All state is touched from the render thread only.  The JMX CPU sample is
 * cached for half a second so the overlay never turns into a profiling tool;
 * the GPU timer reuses the existing {@link GpuQuery} implementation and stops
 * itself permanently if the backend cannot provide timestamps.</p>
 */
public final class PerfOverlayStats {
    private static final double EWMA = 0.16D;
    private static final long CPU_SAMPLE_NS = 500_000_000L;
    private static final long COUNTER_WINDOW_NS = 1_000_000_000L;
    private static final int HISTORY_SIZE = 120;
    private static final int[] REFRESH_RATES = {60, 75, 90, 120, 144, 165, 240, 360};

    private static final com.sun.management.OperatingSystemMXBean OS_BEAN =
        ManagementFactory.getOperatingSystemMXBean()
            instanceof com.sun.management.OperatingSystemMXBean os ? os : null;

    private static final long[] FRAME_HISTORY = new long[HISTORY_SIZE];
    private static int historyIndex;
    private static int historyCount;

    private static long lastFrameNanos;
    private static long renderStartNanos;
    private static long lastCpuSampleNanos;

    private static double frameMs = 16.7D;
    private static double cpuMs;
    private static double gpuMs;
    private static double cpuLoad = -1.0D;
    private static double processLoad = -1.0D;

    private static PerfCounters.Snapshot windowSnapshot = PerfCounters.snapshot();
    private static long windowStartNanos;
    private static int windowFrames;
    private static double drawsPerFrame;
    private static double uploadMiBPerSecond;

    private static GpuQuery gpuQuery;
    private static boolean gpuQueryEnded;
    private static int gpuQueryAge;
    private static int gpuSamples;
    private static boolean gpuTimingAvailable = true;

    private PerfOverlayStats() {
    }

    /** Called at the head of {@code GameRenderer.render}. */
    public static void beginCpuFrame() {
        if (!PerfOverlay.isVisible()) {
            renderStartNanos = 0L;
            return;
        }
        renderStartNanos = System.nanoTime();
        beginGpuQuery();
    }

    /** Called at the return of {@code GameRenderer.render}. */
    public static void endCpuFrame() {
        long start = renderStartNanos;
        renderStartNanos = 0L;
        if (start == 0L) {
            return;
        }
        double elapsed = (System.nanoTime() - start) / 1_000_000.0D;
        if (elapsed > 0.0D && elapsed < 1_000.0D) {
            cpuMs = ewma(cpuMs, elapsed);
        }
    }

    /**
     * Called once per frame at the head of {@code RenderSystem.flipFrame}.
     * This is the frame boundary for FPS, counter windows and GPU timing.
     */
    public static void onFrameBoundary() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            long delta = now - lastFrameNanos;
            if (delta > 0L && delta < 2_000_000_000L) {
                double sample = delta / 1_000_000.0D;
                frameMs = ewma(frameMs, sample);
                FRAME_HISTORY[historyIndex] = delta;
                historyIndex = (historyIndex + 1) % HISTORY_SIZE;
                if (historyCount < HISTORY_SIZE) {
                    historyCount++;
                }
            }
        }
        lastFrameNanos = now;

        sampleCpu(now);
        updateCounterWindow(now);
        finishGpuQuery();

        if (!PerfOverlay.isVisible()) {
            resetGpuQuery();
        }
    }

    /**
     * Starts the timestamp at the beginning of the frame's command stream.
     * The query is ended at the next {@code flipFrame} head, after the world
     * and GUI commands have been submitted but before the present, so the
     * sample covers one frame of real GPU work.
     */
    private static void beginGpuQuery() {
        if (!gpuTimingAvailable || gpuQuery != null) {
            return;
        }
        try {
            GpuDevice device = RenderSystem.getDevice();
            if (device == null) {
                return;
            }
            CommandEncoder encoder = device.createCommandEncoder();
            gpuQuery = encoder.timerQueryBegin();
            gpuQueryEnded = false;
            gpuQueryAge = 0;
        } catch (Throwable t) {
            gpuTimingAvailable = false;
            gpuQuery = null;
        }
    }

    private static void finishGpuQuery() {
        if (gpuQuery == null) {
            return;
        }
        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            if (!gpuQueryEnded) {
                encoder.timerQueryEnd(gpuQuery);
                gpuQueryEnded = true;
            }
            OptionalLong value = gpuQuery.getValue();
            if (value.isPresent()) {
                double sample = value.getAsLong() / 1_000_000.0D;
                if (sample > 0.0D && sample < 1_000.0D) {
                    gpuMs = ewma(gpuMs, sample);
                    gpuSamples++;
                }
                closeGpuQuery();
            } else if (++gpuQueryAge > 240) {
                // A dead query must not pin an overlay-only resource forever.
                closeGpuQuery();
                gpuTimingAvailable = false;
            }
        } catch (Throwable t) {
            closeGpuQuery();
            gpuTimingAvailable = false;
        }
    }

    public static void resetGpuQuery() {
        closeGpuQuery();
    }

    private static void closeGpuQuery() {
        if (gpuQuery != null) {
            try {
                gpuQuery.close();
            } catch (Throwable ignored) {
            }
            gpuQuery = null;
        }
        gpuQueryEnded = false;
        gpuQueryAge = 0;
    }

    private static void sampleCpu(long now) {
        if (now - lastCpuSampleNanos < CPU_SAMPLE_NS) {
            return;
        }
        lastCpuSampleNanos = now;
        if (OS_BEAN == null) {
            return;
        }
        double system = OS_BEAN.getCpuLoad();
        if (system >= 0.0D) {
            cpuLoad = system;
        } else {
            double average = OS_BEAN.getSystemLoadAverage();
            if (average >= 0.0D) {
                cpuLoad = average / Math.max(1, OS_BEAN.getAvailableProcessors());
            }
        }
        double process = OS_BEAN.getProcessCpuLoad();
        if (process >= 0.0D) {
            processLoad = process;
        }
    }

    private static void updateCounterWindow(long now) {
        if (windowStartNanos == 0L) {
            windowStartNanos = now;
            windowSnapshot = PerfCounters.snapshot();
            windowFrames = 0;
        }
        windowFrames++;
        long elapsed = now - windowStartNanos;
        if (elapsed < COUNTER_WINDOW_NS) {
            return;
        }
        PerfCounters.Snapshot current = PerfCounters.snapshot();
        PerfCounters.Snapshot delta = current.delta(windowSnapshot);
        drawsPerFrame = windowFrames == 0
            ? 0.0D
            : delta.draws() / (double) windowFrames;
        uploadMiBPerSecond = delta.uploadMiB() / (elapsed / 1_000_000_000.0D);
        windowSnapshot = current;
        windowStartNanos = now;
        windowFrames = 0;
    }

    private static double ewma(double previous, double sample) {
        return previous <= 0.0D ? sample : previous + EWMA * (sample - previous);
    }

    public static int fps() {
        try {
            int vanilla = Minecraft.getInstance().getFps();
            if (vanilla > 0) {
                return vanilla;
            }
        } catch (Throwable ignored) {
        }
        return (int) Math.round(1000.0D / Math.max(0.1D, frameMs));
    }

    public static double frameMs() {
        return frameMs;
    }

    public static double frameAverageMs() {
        if (historyCount == 0) {
            return frameMs;
        }
        long total = 0L;
        for (int i = 0; i < historyCount; i++) {
            total += FRAME_HISTORY[i];
        }
        return total / (historyCount * 1_000_000.0D);
    }

    public static double frameMaxMs() {
        long max = 0L;
        for (int i = 0; i < historyCount; i++) {
            max = Math.max(max, FRAME_HISTORY[i]);
        }
        return max == 0L ? frameMs : max / 1_000_000.0D;
    }

    public static double cpuMs() {
        return cpuMs;
    }

    public static boolean gpuAvailable() {
        return gpuSamples > 0;
    }

    public static boolean gpuTimingAvailable() {
        return gpuTimingAvailable;
    }

    public static double gpuMs() {
        return gpuMs;
    }

    public static double gpuWaitMs() {
        return Math.max(0.0D, frameMs - cpuMs);
    }

    public static double cpuLoad() {
        return cpuLoad;
    }

    public static double processLoad() {
        return processLoad;
    }

    public static double drawsPerFrame() {
        return drawsPerFrame;
    }

    public static double uploadMiBPerSecond() {
        return uploadMiBPerSecond;
    }

    public static double cpuShare() {
        return Math.max(0.0D, cpuMs / Math.max(0.1D, frameMs));
    }

    public static double gpuShare() {
        return gpuAvailable() ? Math.max(0.0D, gpuMs / Math.max(0.1D, frameMs)) : 0.0D;
    }

    public static Bottleneck bottleneck() {
        double frame = Math.max(0.1D, frameMs);
        double cpu = cpuShare();
        if (gpuAvailable()) {
            double gpu = gpuShare();
            if (cpu >= 0.85D && cpu >= gpu) {
                return Bottleneck.CPU;
            }
            if (gpu >= 0.85D && gpu > cpu) {
                return Bottleneck.GPU;
            }
            if (nearRefresh(fps(), frame) && cpu < 0.70D && gpu < 0.70D) {
                return Bottleneck.VSYNC;
            }
            return gpu >= cpu ? Bottleneck.GPU : Bottleneck.CPU;
        }
        if (cpu >= 0.85D) {
            return Bottleneck.CPU;
        }
        if (nearRefresh(fps(), frame) && cpu < 0.70D) {
            return Bottleneck.VSYNC;
        }
        return Bottleneck.GPU;
    }

    private static boolean nearRefresh(int fps, double frame) {
        for (int rate : REFRESH_RATES) {
            double target = 1000.0D / rate;
            if (Math.abs(fps - rate) <= Math.max(1.0D, rate * 0.02D)
                && Math.abs(frame - target) <= Math.max(0.5D, target * 0.05D)) {
                return true;
            }
        }
        return false;
    }

    public static String formatPercent(double fraction) {
        return fraction < 0.0D
            ? "n/a"
            : String.format(Locale.ROOT, "%d%%", Math.round(Math.min(1.0D, fraction) * 100.0D));
    }

    public enum Bottleneck {
        CPU("CPU"),
        GPU("GPU"),
        VSYNC("VSync");

        private final String label;

        Bottleneck(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }
}
