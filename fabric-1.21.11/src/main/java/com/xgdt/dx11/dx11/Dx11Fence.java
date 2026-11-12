package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.buffers.GpuFence;

/** {@link GpuFence} backed by a native {@code D3D11_QUERY_EVENT}. */
final class Dx11Fence implements GpuFence, Dx11Resource {
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
        // "wait indefinitely".
        return Dx11Native.nativeFenceAwait(this.handle, timeoutNanos);
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
