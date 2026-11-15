package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One growable GPU vertex arena for a region/layer pair.
 *
 * <p>The arena is a byte-addressed {@link GpuBuffer}; sections are packed into
 * it by {@link RegionUploader} and released by {@link RegionVertexSlice}.  It
 * is confined to the render thread, like every other D3D11 upload path.</p>
 */
public final class RegionBuffer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final int USAGE =
        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC;
    // D3D11 buffer resources are limited to 128 MiB and lower feature levels
    // are stricter; a single region/layer never needs more than this.
    private static final long MAX_CAPACITY = 64L * 1024L * 1024L;

    private final Region region;
    private final ChunkSectionLayer layer;
    private final RegionAllocator allocator;
    private GpuBuffer buffer;
    private long capacity;
    private boolean closed;

    RegionBuffer(Region region, ChunkSectionLayer layer, long initialCapacity) {
        this.region = region;
        this.layer = layer;
        this.capacity = initialCapacity;
        this.buffer = create(initialCapacity);
        this.allocator = new RegionAllocator(initialCapacity);
    }

    public Region region() {
        return this.region;
    }

    public ChunkSectionLayer layer() {
        return this.layer;
    }

    public GpuBuffer gpuBuffer() {
        return this.buffer;
    }

    public long capacity() {
        return this.capacity;
    }

    public boolean isClosed() {
        return this.closed;
    }

    /** Allocates a whole-vertex range, growing the arena when necessary. */
    public RegionAllocator.Allocation allocate(long size, long alignment) {
        if (this.closed || size <= 0) {
            return null;
        }

        RegionAllocator.Allocation allocation = this.allocator.allocate(size, alignment);
        if (allocation != null) {
            return allocation;
        }

        long required = this.capacity + size + alignment;
        long newCapacity = this.capacity;
        while (newCapacity < required) {
            newCapacity *= 2;
            if (newCapacity > MAX_CAPACITY) {
                newCapacity = MAX_CAPACITY;
                break;
            }
        }
        if (newCapacity < required || newCapacity <= this.capacity) {
            return null;
        }

        long growth = newCapacity - this.capacity;
        if (!RegionManager.tryReserve(growth)) {
            return null;
        }

        GpuBuffer grown = null;
        try {
            grown = create(newCapacity);
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyToBuffer(this.buffer.slice(), grown.slice(0L, this.capacity));

            GpuBuffer old = this.buffer;
            this.buffer = grown;
            this.capacity = newCapacity;
            this.allocator.grow(newCapacity);
            try {
                old.close();
            } catch (RuntimeException ignored) {
                // The new arena is already live; a failed close of the old
                // buffer only leaks one allocation until device teardown.
            }
            return this.allocator.allocate(size, alignment);
        } catch (RuntimeException e) {
            RegionManager.release(growth);
            if (grown != null) {
                grown.close();
            }
            LOGGER.warn("[dx11-perf] Could not grow region arena: {}", e.toString());
            return null;
        }
    }

    /** Copies vertex bytes into the arena at {@code allocation}. */
    public void write(RegionAllocator.Allocation allocation, ByteBuffer data) {
        if (this.closed) {
            return;
        }
        RenderSystem.getDevice()
            .createCommandEncoder()
            .writeToBuffer(this.buffer.slice(allocation.offset(), allocation.size()), data);
    }

    public void free(long offset, long size) {
        if (!this.closed) {
            this.allocator.free(offset, size);
        }
    }

    public long liveBytes() {
        return this.allocator.liveBytes();
    }

    public int liveAllocations() {
        return this.allocator.liveAllocationCount();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        RegionManager.release(this.capacity);
        this.buffer.close();
    }

    private static GpuBuffer create(long size) {
        return RenderSystem.getDevice().createBuffer(
            () -> "dx11 region vertex arena", USAGE, size);
    }
}
