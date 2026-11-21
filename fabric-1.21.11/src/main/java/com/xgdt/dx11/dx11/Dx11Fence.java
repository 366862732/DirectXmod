package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.buffers.GpuFence;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link GpuFence} backed by a native {@code D3D11_QUERY_EVENT}. */
final class Dx11Fence implements GpuFence, Dx11Resource {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    /**
     * Vanilla waits for {@code MappableRingBuffer} fences with
     * {@link Long#MAX_VALUE}.  A D3D11 event query can stay pending when the
     * immediate context is not submitted before the wait starts (for example
     * while the shutdown panorama renders and the last world frame is still
     * queued).  An unbounded wait then trips the 15-second client shutdown
     * watchdog.  Cap infinite waits so the render thread always makes
     * progress; ring buffers use dynamic buffers, so reusing a slot after a
     * timeout can only tear one frame instead of freezing the process.
     */
    private static final long INFINITE_WAIT_CAP_NANOS = TimeUnit.SECONDS.toNanos(5);

    /** Log a warning when a wait takes long enough to risk the watchdog. */
    private static final long SLOW_WAIT_LOG_NANOS = TimeUnit.SECONDS.toNanos(1);

    /** Avoid flooding the log if every frame waits for the same stuck GPU. */
    private static final int SLOW_WAIT_LOG_LIMIT = 8;

    private static final AtomicInteger slowWaitLogs = new AtomicInteger();

    /**
     * Latched after the first infinite wait times out.  A GPU that cannot
     * retire a fence within the cap is wedged or already shutting down, so
     * blocking on every later fence would only delay shutdown further.
     */
    private static volatile boolean skipInfiniteWaits;

    private final Dx11Device device;
    private final long handle;
    private boolean closed;

    Dx11Fence(Dx11Device device) {
        this.device = device;
        this.handle = Dx11Native.nativeCreateFence(device.nativeHandle());
        if (this.handle == 0L) {
            throw new IllegalStateException("Could not create D3D11 fence");
        }
        device.register(this);
    }

    @Override
    public boolean awaitCompletion(long timeoutNanos) {
        if (this.closed) {
            return true;
        }
        // Matches glClientWaitSync: nanoseconds, with Long.MAX_VALUE meaning
        // "wait indefinitely".  Indefinite waits are capped (see above).
        final boolean infinite =
            timeoutNanos < 0 || timeoutNanos == Long.MAX_VALUE;
        if (infinite && skipInfiniteWaits) {
            return true;
        }
        final long effectiveTimeout =
            infinite ? INFINITE_WAIT_CAP_NANOS : timeoutNanos;
        final long start = System.nanoTime();
        final boolean completed =
            Dx11Native.nativeFenceAwait(this.handle, effectiveTimeout);
        final long elapsed = System.nanoTime() - start;

        if (elapsed >= SLOW_WAIT_LOG_NANOS
            && slowWaitLogs.getAndIncrement() < SLOW_WAIT_LOG_LIMIT) {
            LOGGER.warn(
                "[dx11] Fence wait took {} ms (completed={}, infinite={})",
                TimeUnit.NANOSECONDS.toMillis(elapsed),
                completed,
                infinite);
        }

        if (!completed && infinite) {
            // The waiter is MappableRingBuffer.currentBuffer(), which ignores
            // the result and immediately reuses the slot; returning true keeps
            // vanilla's "go ahead" semantics after the bounded wait.
            skipInfiniteWaits = true;
            LOGGER.warn(
                "[dx11] GPU fence still pending after {} ms; skipping further "
                    + "indefinite fence waits",
                TimeUnit.NANOSECONDS.toMillis(elapsed));
            return true;
        }
        return completed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.device.unregister(this);
            Dx11Native.nativeDestroyFence(this.handle);
        }
    }

    @Override
    public long nativeHandle() {
        return this.handle;
    }

    @Override
    public void destroyNative() {
        close();
    }
}
