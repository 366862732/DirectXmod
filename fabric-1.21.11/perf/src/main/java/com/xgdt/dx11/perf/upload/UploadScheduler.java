package com.xgdt.dx11.perf.upload;

import com.mojang.blaze3d.vertex.MeshData;
import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.metrics.Dx11Metrics;
import com.xgdt.dx11.perf.PerfState;
import com.xgdt.dx11.perf.culling.VisibleSections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bounded, distance-prioritized draining of the section renderer upload queue.
 *
 * <p>Vanilla's {@code SectionRenderDispatcher.mainThreadUploadExecutor} merely
 * appends a {@link Runnable} to its private queue; every frame
 * {@code uploadAllPendingUploads()} runs every queued upload.  A chunk burst
 * therefore uploads all completed sections in one frame.  This class wraps each
 * section-upload runnable with its origin and estimated byte size, then runs a
 * limited prefix per frame from nearest to farthest.</p>
 *
 * <p>Deferred runnables stay in the vanilla queue.  That is safe and bounded:
 * the compile task that produced the mesh keeps its
 * {@code SectionBufferBuilderPack} until the upload future completes, so the
 * buffer pool naturally limits how many uploads can be outstanding.  No copy of
 * the mesh bytes is needed, which keeps the deferral allocation-free.</p>
 *
 * <p>Render-thread only.  The thread-local byte estimate bridges the worker
 * that calls {@code RenderSection.upload} and the wrapper installed around the
 * runnable in the same call.</p>
 */
public final class UploadScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final long MiB = 1024L * 1024L;
    private static final long WARN_INTERVAL_NANOS = 10_000_000_000L;
    private static final ThreadLocal<Long> PENDING_BYTES = new ThreadLocal<>();
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final AtomicLong WRAPPED = new AtomicLong();

    private static long totalDeferred;
    private static long cappedFrames;
    private static long softLimitHits;
    private static long failedUploads;
    private static long lastQueue;
    private static long lastRan;
    private static long lastBytes;
    private static long lastEstimatedBytes;
    private static long lastVisibleQueued;
    private static long lastOffscreenQueued;
    private static long lastPlainQueued;
    private static long lastMs;
    private static long maxMs;
    private static long totalVisibleQueued;
    private static long totalOffscreenQueued;
    private static long totalPlainQueued;
    private static long totalRankedQueued;
    private static long totalUnrankedQueued;
    private static long lastWarnNanos;

    private UploadScheduler() {
    }

    /**
     * Records the byte size of the mesh map that is about to be scheduled.
     * Called on the compile worker from {@code RenderSection.upload} HEAD.
     */
    public static void notePendingBytes(Map<ChunkSectionLayer, MeshData> meshes) {
        if (!PerfState.regionActive() || !PerfState.config().uploadBudget) {
            PENDING_BYTES.remove();
            return;
        }
        long bytes = 0L;
        if (meshes != null) {
            for (MeshData data : meshes.values()) {
                if (data == null) {
                    continue;
                }
                try {
                    bytes += Math.max(0, data.vertexBuffer().remaining());
                    if (data.indexBuffer() != null) {
                        bytes += Math.max(0, data.indexBuffer().remaining());
                    }
                } catch (RuntimeException ignored) {
                    // A already-closed mesh contributes no bytes.
                }
            }
        }
        PENDING_BYTES.set(bytes);
    }

    /** Clears a stale byte estimate after an early return in the vanilla call. */
    public static void clearPendingBytes() {
        PENDING_BYTES.remove();
    }

    /**
     * Wraps the runnable that vanilla submits to the main-thread upload
     * executor.  The origin is copied because the section's mutable block
     * position may move before the runnable executes.
     */
    public static Runnable wrap(BlockPos origin, Runnable delegate) {
        Long bytes = PENDING_BYTES.get();
        PENDING_BYTES.remove();
        WRAPPED.incrementAndGet();
        int x = origin == null ? 0 : origin.getX();
        int y = origin == null ? 0 : origin.getY();
        int z = origin == null ? 0 : origin.getZ();
        return new BudgetedUpload(
            delegate, x, y, z, bytes == null ? 0L : Math.max(0L, bytes),
            SEQUENCE.getAndIncrement());
    }

    /**
     * Runs a budgeted prefix of the pending upload queue and drains the stale
     * mesh close queue.
     *
     * @return {@code true} when this method handled the queue and the vanilla
     *         drain must be cancelled
     */
    public static boolean drain(
        Queue<Runnable> uploads,
        Queue<? extends AutoCloseable> toClose,
        double cameraX,
        double cameraY,
        double cameraZ) {
        PerfConfig config = PerfState.config();
        if (!PerfState.regionActive() || !config.uploadBudget) {
            return false;
        }

        List<Runnable> pending = new ArrayList<>();
        for (Runnable task; (task = uploads.poll()) != null; ) {
            pending.add(task);
        }

        if (pending.isEmpty()) {
            closeQueued(toClose);
            publishStats(0, 0L, 0L, 0, 0L, 0, 0, 0);
            return true;
        }

        boolean canClassify = VisibleSections.captured();
        boolean rankOffscreen = config.deferredOffscreenUpdates && canClassify;
        long estimatedBytes = 0L;
        int visibleQueued = 0;
        int offscreenQueued = 0;
        int plainQueued = 0;
        List<UploadOrder.Entry> entries = new ArrayList<>(pending.size());
        for (Runnable task : pending) {
            if (task instanceof BudgetedUpload upload) {
                estimatedBytes += upload.estimatedBytes;
                boolean visible = true;
                if (canClassify) {
                    visible = VisibleSections.isVisibleBlock(
                        upload.originX, upload.originY, upload.originZ);
                    if (visible) {
                        visibleQueued++;
                        totalVisibleQueued++;
                    } else {
                        offscreenQueued++;
                        totalOffscreenQueued++;
                    }
                } else {
                    totalUnrankedQueued++;
                }
                if (rankOffscreen) {
                    totalRankedQueued++;
                }
                entries.add(new UploadOrder.Entry(
                    true,
                    visible,
                    upload.originX,
                    upload.originY,
                    upload.originZ,
                    upload.sequence,
                    upload.estimatedBytes));
            } else {
                plainQueued++;
                totalPlainQueued++;
                entries.add(new UploadOrder.Entry(
                    false, true, 0.0, 0.0, 0.0, entries.size(), 0L));
            }
        }

        int maxSections = config.uploadMaxSectionsPerFrame;
        long maxBytes = config.uploadMaxBytesPerFrame;
        long softLimit = Math.max(0L, config.deferredUploadLimitMb) * MiB;
        if (softLimit > 0 && estimatedBytes > softLimit) {
            softLimitHits++;
            // Keep the vanilla buffer pool moving even if the per-frame cap is
            // very small.  The byte cap below still applies.
            maxSections = maxSections > 0 ? Math.max(1, maxSections * 4) : 0;
        }

        int[] ranked = UploadOrder.rank(entries, cameraX, cameraY, cameraZ);
        UploadOrder.Selection selection =
            UploadOrder.select(entries, ranked, maxSections, maxBytes);

        long startNanos = System.nanoTime();
        long bytesUsed = 0L;
        int ran = 0;
        for (int rank = 0; rank < selection.count(); rank++) {
            Runnable task = pending.get(ranked[rank]);
            long before = Dx11Metrics.uploadBytes();
            try {
                task.run();
            } catch (RuntimeException e) {
                failedUploads++;
                warn("Section upload failed: " + e);
            }
            long delta = Dx11Metrics.uploadBytes() - before;
            if (delta > 0L) {
                bytesUsed += delta;
            }
            ran++;
        }

        for (int rank = selection.count(); rank < ranked.length; rank++) {
            uploads.add(pending.get(ranked[rank]));
        }
        closeQueued(toClose);

        totalDeferred += ranked.length - selection.count();
        if (selection.capped()) {
            cappedFrames++;
        }
        publishStats(
            ran,
            bytesUsed,
            estimatedBytes,
            ranked.length,
            System.nanoTime() - startNanos,
            visibleQueued,
            offscreenQueued,
            plainQueued);
        return true;
    }

    /** Human-readable budget state for {@code /dx11perf stats}. */
    public static String describe() {
        PerfConfig config = PerfState.config();
        if (!PerfState.regionActive() || !config.uploadBudget) {
            return "uploadBudget=off";
        }
        return "uploadBudget=on"
            + " queue=" + lastQueue
            + " ran=" + lastRan
            + " bytes=" + lastBytes
            + " est=" + lastEstimatedBytes
            + " visible=" + lastVisibleQueued
            + " offscreen=" + lastOffscreenQueued
            + " plain=" + lastPlainQueued
            + " wrapped=" + WRAPPED.get()
            + " ranked=" + totalRankedQueued
            + " unranked=" + totalUnrankedQueued
            + " visibleTotal=" + totalVisibleQueued
            + " offscreenTotal=" + totalOffscreenQueued
            + " plainTotal=" + totalPlainQueued
            + " deferred=" + totalDeferred
            + " capped=" + cappedFrames
            + " soft=" + softLimitHits
            + " failed=" + failedUploads
            + " ms=" + String.format(java.util.Locale.ROOT, "%.2f", lastMs / 1_000_000.0)
            + " maxMs=" + String.format(java.util.Locale.ROOT, "%.2f", maxMs / 1_000_000.0);
    }

    private static void publishStats(
        int ran,
        long bytes,
        long estimatedBytes,
        int queued,
        long elapsedNanos,
        int visibleQueued,
        int offscreenQueued,
        int plainQueued) {
        // Keep the last non-empty queue visible in /dx11perf stats; an idle
        // frame would otherwise overwrite the counters with zeros.
        if (queued > 0) {
            lastQueue = queued;
            lastRan = ran;
            lastBytes = bytes;
            lastEstimatedBytes = estimatedBytes;
            lastVisibleQueued = visibleQueued;
            lastOffscreenQueued = offscreenQueued;
            lastPlainQueued = plainQueued;
            lastMs = elapsedNanos;
            if (elapsedNanos > maxMs) {
                maxMs = elapsedNanos;
            }
        }
    }

    /** Clears the session counters along with the rest of the perf stats. */
    public static void resetStats() {
        totalDeferred = 0L;
        cappedFrames = 0L;
        softLimitHits = 0L;
        failedUploads = 0L;
        lastQueue = 0L;
        lastRan = 0L;
        lastBytes = 0L;
        lastEstimatedBytes = 0L;
        lastVisibleQueued = 0L;
        lastOffscreenQueued = 0L;
        lastPlainQueued = 0L;
        lastMs = 0L;
        maxMs = 0L;
        totalVisibleQueued = 0L;
        totalOffscreenQueued = 0L;
        totalPlainQueued = 0L;
        totalRankedQueued = 0L;
        totalUnrankedQueued = 0L;
        WRAPPED.set(0L);
    }

    private static void closeQueued(Queue<? extends AutoCloseable> toClose) {
        AutoCloseable mesh;
        while ((mesh = toClose.poll()) != null) {
            try {
                mesh.close();
            } catch (Exception e) {
                warn("Could not close stale section mesh: " + e);
            }
        }
    }

    private static void warn(String message) {
        long now = System.nanoTime();
        if (now - lastWarnNanos < WARN_INTERVAL_NANOS) {
            return;
        }
        lastWarnNanos = now;
        LOGGER.warn("[dx11-perf] {}", message);
    }

    /** One queued section upload with its ranking metadata. */
    private static final class BudgetedUpload implements Runnable {
        private final Runnable delegate;
        private final int originX;
        private final int originY;
        private final int originZ;
        private final long estimatedBytes;
        private final long sequence;

        private BudgetedUpload(
            Runnable delegate,
            int originX,
            int originY,
            int originZ,
            long estimatedBytes,
            long sequence) {
            this.delegate = delegate;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.estimatedBytes = estimatedBytes;
            this.sequence = sequence;
        }

        @Override
        public void run() {
            this.delegate.run();
        }
    }
}
