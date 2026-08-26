package com.dx12.dx12;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.TransientMemory;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link TransientMemory}, mirroring the official
 * {@code VulkanTransientMemory} lifecycle.
 *
 * <p>P3 simplification: every allocation creates a fresh D3D12 committed buffer
 * (no block allocator yet). Buffers allocated during one frame are retained for
 * {@link #FRAMES_IN_FLIGHT} frames (matching the native double-buffered submit)
 * and released on {@link #rotate()}, which the encoder calls on every
 * {@code submit()}.
 *
 * <p>{@code allocateGpuMapped} uses a ring buffer of 3 UPLOAD heap buffers
 *（镜像 {@code MappableRingBuffer} 的 3 路轮换）：每帧 rotate() 切换到下一路，
 * 当前路的 offset 在该帧内单调递增。这保证了每帧 DynamicUniforms 等使用
 * allocateGpuMapped 的代码都能拿到新的独立偏移，而非复用同一缓冲起始处。
 */
@Environment(EnvType.CLIENT)
public class Dx12TransientMemory implements TransientMemory {
    static final int FRAMES_IN_FLIGHT = 2;
    /** allocateGpuMapped ring buffer 路数（镜像 MappableRingBuffer.BUFFER_COUNT=3）。 */
    private static final int UBO_RING_COUNT = 3;
    /** 每路 allocateGpuMapped 缓冲大小（4KB，足够容纳多组 std140 uniform 块）。 */
    private static final long UBO_RING_BLOCK_SIZE = 4096L;

    private final long ctx;
    private final Deque<List<Dx12GpuBuffer>> frames = new ArrayDeque<>();
    private List<Dx12GpuBuffer> frame = new ArrayList<>();
    private boolean closed;

    // allocateGpuMapped ring buffer
    private final Dx12GpuBuffer[] uboRing = new Dx12GpuBuffer[UBO_RING_COUNT];
    /** 当前正在写入的 ring buffer 路索引。 */
    private int uboRingIdx = 0;
    /** 当前路的已用字节数（从 0 开始单调递增，达到 BLOCK_SIZE 时 rotate）。 */
    private long uboRingOffset = 0;

    Dx12TransientMemory(long ctx) {
        this.ctx = ctx;
        // 预分配 3 路 UPLOAD 缓冲（同 MappableRingBuffer 构造时的 3 个 buffer）
        for (int i = 0; i < UBO_RING_COUNT; i++) {
            uboRing[i] = new Dx12GpuBuffer(
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC, UBO_RING_BLOCK_SIZE);
            register(uboRing[i]);
        }
    }

    private void register(Dx12GpuBuffer buffer) {
        this.frame.add(buffer);
    }

    @Override
    public ByteBuffer allocateCpu(long size, long alignment, long minimumAllocation, long elementSize) {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("allocateCpu larger than 2GB is not supported");
        }
        return ByteBuffer.allocateDirect((int) size);
    }

    @Override
    public GpuBufferSlice.MappedView allocateStaging(long size, long alignment,
        @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        Dx12GpuBuffer buffer = new Dx12GpuBuffer(
            usage | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC, size);
        this.register(buffer);
        return buffer.map(0, size, false, true);
    }

    @Override
    public GpuBufferSlice allocateGpu(long size, long alignment,
        @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        Dx12GpuBuffer buffer = new Dx12GpuBuffer(usage | GpuBuffer.USAGE_COPY_DST, size);
        this.register(buffer);
        return buffer.slice();
    }

    @Override
    public GpuBufferSlice.MappedView allocateGpuMapped(long size, long alignment,
        @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        // 镜像 VulkanTransientMemory.allocateGpuMapped：在 ring buffer 当前路中分配，
        // 超过BLOCK_SIZE时自动切换到下一路（rotate）。
        // 与 MappableRingBuffer 语义对齐：每帧 rotate() 后切换到新路径，旧路径供 GPU 读取。
        long remaining = UBO_RING_BLOCK_SIZE - uboRingOffset;
        if (size > remaining) {
            // 当前路剩余空间不足，切换到下一路
            uboRingIdx = (uboRingIdx + 1) % UBO_RING_COUNT;
            uboRingOffset = 0;
            remaining = UBO_RING_BLOCK_SIZE;
        }
        Dx12GpuBuffer buf = uboRing[uboRingIdx];
        long offset = uboRingOffset;
        uboRingOffset += size;
        return buf.map(offset, size, false, true);
    }

    @Override
    public GpuBufferSlice uploadStaging(List<ByteBuffer> data, long alignment,
        @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        long total = 0;
        for (ByteBuffer buffer : data) {
            total += buffer.remaining();
        }
        return this.upload(data, usage, total);
    }

    @Override
    public GpuBufferSlice uploadGpu(List<ByteBuffer> data, long alignment,
        @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        long total = 0;
        for (ByteBuffer buffer : data) {
            total += buffer.remaining();
        }
        return this.upload(data, usage, total);
    }

    @Override
    public List<GpuBufferSlice> multiUploadStaging(List<ByteBuffer> data,
        long alignment, @GpuBuffer.Usage int usage) {
        return this.multiUpload(data, usage);
    }

    @Override
    public List<GpuBufferSlice> multiUploadGpu(List<ByteBuffer> data,
        long alignment, @GpuBuffer.Usage int usage) {
        return this.multiUpload(data, usage);
    }

    /**
     * Upload a set of CPU buffers into a single GPU-side (DEFAULT heap) buffer:
     * write them into one UPLOAD staging buffer, then one CopyBufferRegion.
     */
    private GpuBufferSlice upload(List<ByteBuffer> data, int usage, long total) {
        if (total == 0) {
            throw new IllegalArgumentException("Cannot upload zero bytes");
        }
        // P22: 在写入前检测 NaN/Infinity，定位污染源（BufferBuilder 未初始化尾部浮点）。
        checkForNanInfinity(data, "upload");
        Dx12GpuBuffer staging = new Dx12GpuBuffer(
            GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC, total);
        this.register(staging);
        try (GpuBufferSlice.MappedView view = staging.map(0, total, false, true)) {
            ByteBuffer dst = view.data();
            for (ByteBuffer buffer : data) {
                dst.put(buffer.duplicate());
            }
        }
        Dx12GpuBuffer gpu = new Dx12GpuBuffer(usage | GpuBuffer.USAGE_COPY_DST, total);
        this.register(gpu);
        Dx12Native.dx12CopyBuffer(this.ctx, staging.handle(), 0, gpu.handle(), 0, total);
        return gpu.slice();
    }

    /** Shared-block variant of {@link #upload}: returns one slice per input buffer. */
    private List<GpuBufferSlice> multiUpload(List<ByteBuffer> data, int usage) {
        long total = 0;
        for (ByteBuffer buffer : data) {
            total += buffer.remaining();
        }
        if (total == 0) {
            return List.of();
        }
        checkForNanInfinity(data, "multiUpload");
        Dx12GpuBuffer staging = new Dx12GpuBuffer(
            GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_SRC, total);
        this.register(staging);
        try (GpuBufferSlice.MappedView view = staging.map(0, total, false, true)) {
            ByteBuffer dst = view.data();
            for (ByteBuffer buffer : data) {
                dst.put(buffer.duplicate());
            }
        }
        Dx12GpuBuffer gpu = new Dx12GpuBuffer(usage | GpuBuffer.USAGE_COPY_DST, total);
        this.register(gpu);
        Dx12Native.dx12CopyBuffer(this.ctx, staging.handle(), 0, gpu.handle(), 0, total);

        List<GpuBufferSlice> result = new ArrayList<>(data.size());
        long offset = 0;
        for (ByteBuffer buffer : data) {
            long length = buffer.remaining();
            result.add(gpu.slice(offset, length));
            offset += length;
        }
        return result;
    }

    /**
     * 检查所有 input buffers 的 float 值是否含 NaN 或 Infinity。
     * 在上传到 GPU 之前调用，定位污染源。
     *
     * <p>注意：某些合法数据（如 self-test 的 0xFC,0xFD,0xFE,0xFF 字节模式）按
     * float 读恰为 NaN，因此这里仅提示一次，避免逐帧刷屏，也不应阻断上传。
     */
    private static void checkForNanInfinity(List<ByteBuffer> data, String method) {
        if (nanWarned) return;
        for (int bi = 0; bi < data.size(); bi++) {
            ByteBuffer buf = data.get(bi);
            if (buf.remaining() < 4) continue;
            // 使用 FloatBuffer 视图逐 float 检测（不修改原始 buffer position）
            FloatBuffer fb = buf.duplicate().order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            while (fb.hasRemaining()) {
                float v = fb.get();
                if (Float.isNaN(v) || Float.isInfinite(v)) {
                    System.err.printf("[dx12-java] NaN/Inf detected in %s: buffer[%d] floatIdx=%d value=%s (数据未修改；仅提示一次)%n",
                        method, bi, fb.position() - 1,
                        Float.isNaN(v) ? "NaN" : "Infinity");
                    System.err.flush();
                    nanWarned = true;
                    return; // 只报一次
                }
            }
        }
    }

    /** 进程内仅提示一次，避免合法字节模式（如 0xFC..0xFF）被反复误报刷屏。 */
    private static boolean nanWarned = false;

    /**
     * Called by the encoder on every {@code submit()}: retire this frame's
     * buffers (keep {@link #FRAMES_IN_FLIGHT} frames alive, matching the native
     * fence wait of value-2) and release the oldest frame.
     *
     * <p>同时 rotate allocateGpuMapped ring buffer：切换至下一路，使新帧的 uniform
     * 写入不会覆盖仍在被 GPU 读取的旧帧数据。
     */
    void rotate() {
        if (this.closed) {
            return;
        }
        this.frames.addLast(this.frame);
        this.frame = new ArrayList<>();
        while (this.frames.size() > FRAMES_IN_FLIGHT) {
            for (Dx12GpuBuffer buffer : this.frames.removeFirst()) {
                buffer.close();
            }
        }
        // Rotate the UBO ring buffer：每帧切到下一路，确保不同帧的 uniform 数据不重叠。
        this.uboRingIdx = (this.uboRingIdx + 1) % UBO_RING_COUNT;
        this.uboRingOffset = 0;
    }

    void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        System.err.println("[dx12-java] transientMemory.close: current=" + this.frame.size()
            + " queuedFrames=" + this.frames.size());
        System.err.flush();
        for (Dx12GpuBuffer buffer : this.frame) {
            buffer.close();
        }
        this.frame = new ArrayList<>();
        for (List<Dx12GpuBuffer> old : this.frames) {
            for (Dx12GpuBuffer buffer : old) {
                buffer.close();
            }
        }
        this.frames.clear();
        System.err.println("[dx12-java] transientMemory.close: done");
        System.err.flush();
    }
}
