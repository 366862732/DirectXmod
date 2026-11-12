package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * {@link GpuBuffer} backed by a native {@code ID3D11Buffer}.
 *
 * <p>Mapped views expose only the requested slice, mirroring the vanilla GL
 * backend even though D3D11 maps whole buffers.</p>
 */
final class Dx11Buffer extends GpuBuffer implements Dx11Resource {
    private final Dx11Device device;
    private final long handle;
    private boolean closed;
    private @Nullable Dx11MappedView mappedView;

    Dx11Buffer(Dx11Device device, @Nullable String label, int usage, long size) {
        super(usage, size);
        if (size <= 0L || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid buffer size: " + size);
        }
        this.device = device;
        this.handle = Dx11Native.nativeCreateBuffer(device.nativeHandle(), usage, (int) size, 0L, 0);
        if (this.handle == 0L) {
            throw new IllegalStateException("Could not create D3D11 buffer");
        }
        device.register(this);
    }

    Dx11Buffer(Dx11Device device, @Nullable String label, int usage, ByteBuffer data) {
        super(usage, data.remaining());
        if (data.remaining() <= 0) {
            throw new IllegalArgumentException("Invalid buffer size: " + data.remaining());
        }
        this.device = device;
        this.handle = Dx11Native.nativeCreateBuffer(
            device.nativeHandle(), usage, data.remaining(), MemoryUtil.memAddress(data), data.remaining());
        if (this.handle == 0L) {
            throw new IllegalStateException("Could not create D3D11 buffer");
        }
        device.register(this);
    }

    long handle() {
        return this.handle;
    }

    Dx11MappedView map(long offset, long size, boolean read, boolean write) {
        if (this.closed) {
            throw new IllegalStateException("Buffer is closed");
        }
        if (this.mappedView != null && !this.mappedView.isClosed()) {
            throw new IllegalStateException("Buffer is already mapped");
        }
        ByteBuffer data = Dx11Native.nativeMapBuffer(this.handle, offset, size, read, write);
        if (data == null) {
            throw new IllegalStateException("Could not map buffer");
        }
        Dx11MappedView view = new Dx11MappedView(this, data);
        this.mappedView = view;
        return view;
    }

    void onUnmapped(Dx11MappedView view) {
        if (this.mappedView == view) {
            this.mappedView = null;
        }
        Dx11Native.nativeUnmapBuffer(this.handle);
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.mappedView != null && !this.mappedView.isClosed()) {
                this.mappedView.close();
            }
            this.device.unregister(this);
            Dx11Native.nativeDestroyBuffer(this.handle);
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
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
