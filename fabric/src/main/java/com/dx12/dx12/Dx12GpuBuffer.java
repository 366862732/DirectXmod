package com.dx12.dx12;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
        // P26 fix：NewDirectByteBuffer 默认 BIG_ENDIAN，而 D3D12 期望小端字节序。
        // MC 的 DynamicUniformStorage.writeUniform 通过 Std140Builder.putFloat 写入
        // 该 buffer，若不加显式小端，UBO 内容会以 BIG_ENDIAN 写入（日志 rbBuf[ubo]
        // 读到 hex=[3F 80 00 00] 即大端 1.0f），HLSL 按小端读取后矩阵全垃圾，
        // 顶点被变换到裁剪区外 → colorTex 全黑。这里统一强制 LITTLE_ENDIAN。
        data.order(ByteOrder.LITTLE_ENDIAN);
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
