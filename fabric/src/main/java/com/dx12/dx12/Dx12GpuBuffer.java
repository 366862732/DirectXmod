package com.dx12.dx12;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link GpuBuffer}.
 *
 * Mirrors the official {@code VulkanGpuBuffer.Direct}: the native side picks the
 * D3D12 heap type from the usage flags (UPLOAD for MAP_WRITE, READBACK for
 * MAP_READ, DEFAULT otherwise) and {@link #map} returns a direct ByteBuffer over
 * the persistently mapped memory.
 */
@Environment(EnvType.CLIENT)
public class Dx12GpuBuffer extends GpuBuffer {
    private final long handle;
    private boolean closed;

    public Dx12GpuBuffer(@Usage int usage, long size) {
        super(usage, size);
        this.handle = Dx12Native.dx12CreateBuffer(usage, size);
        if (this.handle == 0) {
            throw new IllegalStateException("dx12CreateBuffer returned a null handle");
        }
    }

    /** Native handle ({@code Dx12Object*} as long). */
    public long handle() {
        return this.handle;
    }

    @Override
    public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Mapping buffer slice larger than 2GB is not supported");
        }
        ByteBuffer data = Dx12Native.dx12MapBuffer(this.handle, offset, length, read, write);
        if (data == null) {
            throw new IllegalStateException("dx12MapBuffer returned null");
        }
        return new GpuBufferSlice.MappedView(this.slice(offset, length), data,
            () -> Dx12Native.dx12UnmapBuffer(this.handle));
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Dx12Native.dx12DestroyResource(this.handle);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
