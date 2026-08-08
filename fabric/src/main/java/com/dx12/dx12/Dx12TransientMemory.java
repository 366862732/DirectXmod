package com.dx12.dx12;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.TransientMemory;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link TransientMemory}, mirroring the official
 * {@code VulkanTransientMemory} lifecycle.
 *
 * P3 simplification: every allocation creates a fresh D3D12 committed buffer
 * (no block allocator yet). Buffers allocated during one frame are retained for
 * {@link #FRAMES_IN_FLIGHT} frames (matching the native double-buffered submit)
 * and released on {@link #rotate()}, which the encoder calls on every
 * {@code submit()}.
 *
 * <ul>
 *   <li>{@code allocateStaging} -> UPLOAD heap buffer (MAP_WRITE | COPY_SRC)</li>
 *   <li>{@code allocateGpu} -> DEFAULT heap buffer (usage | COPY_DST)</li>
 *   <li>{@code allocateGpuMapped} -> UPLOAD heap buffer (MAP_WRITE | usage)</li>
 *   <li>{@code upload*} -> staging write + {@code CopyBufferRegion} into a GPU buffer</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class Dx12TransientMemory implements TransientMemory {
    static final int FRAMES_IN_FLIGHT = 2;

    private final long ctx;
    private final Deque<List<Dx12GpuBuffer>> frames = new ArrayDeque<>();
    private List<Dx12GpuBuffer> frame = new ArrayList<>();
    private boolean closed;

    Dx12TransientMemory(long ctx) {
        this.ctx = ctx;
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
        Dx12GpuBuffer buffer = new Dx12GpuBuffer(
            usage | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST
                | GpuBuffer.USAGE_COPY_SRC, size);
        this.register(buffer);
        return buffer.map(0, size, false, true);
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
     * Called by the encoder on every {@code submit()}: retire this frame's
     * buffers (keep {@link #FRAMES_IN_FLIGHT} frames alive, matching the native
     * fence wait of value-2) and release the oldest frame.
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
