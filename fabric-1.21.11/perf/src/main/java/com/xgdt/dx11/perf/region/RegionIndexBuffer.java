package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated sequential QUADS index buffer for coalesced region runs.
 *
 * <p>Region runs are larger than a single section, so the vanilla shared
 * {@code AutoStorageIndexBuffer} would have to grow while a
 * {@code ChunkSectionsToRender} frame still holds its old buffer; the next
 * layer in the same group would then be handed a closed buffer.  Keeping a
 * private buffer avoids that hazard entirely.  The index pattern is the same
 * one vanilla uses for QUADS: {@code v, v+1, v+2, v+2, v+3, v} per quad.</p>
 *
 * <p>Render-thread only.  The buffer grows geometrically and is never shrunk,
 * so the steady state allocates nothing.</p>
 */
public final class RegionIndexBuffer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final int USAGE = GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST;
    private static final int INITIAL_INDICES = 6 * 1024;
    private static final long MAX_INDICES = 24L * 1024L * 1024L;

    private GpuBuffer buffer;
    private int capacity;
    private VertexFormat.IndexType type = VertexFormat.IndexType.SHORT;
    private boolean closed;

    /** Returns a buffer holding at least {@code required} indices, or null. */
    public GpuBuffer get(int required) {
        if (this.closed || required <= 0) {
            return null;
        }
        if (this.buffer != null && required <= this.capacity) {
            return this.buffer;
        }

        long target = Math.max((long) required, Math.max(INITIAL_INDICES, (long) this.capacity * 2L));
        target = Math.min(target, MAX_INDICES);
        int newCapacity = (int) ((target + 5L) / 6L * 6L);
        if (newCapacity < required) {
            return null;
        }

        VertexFormat.IndexType newType =
            VertexFormat.IndexType.least(newCapacity / 6 * 4);
        ByteBuffer data;
        try {
            data = sequentialQuads(newCapacity, newType);
        } catch (OutOfMemoryError e) {
            LOGGER.warn("[dx11-perf] Could not allocate {} region indices", newCapacity);
            return null;
        }

        GpuBuffer grown;
        try {
            grown = RenderSystem.getDevice().createBuffer(
                () -> "dx11 region index buffer", USAGE, data);
        } catch (RuntimeException e) {
            LOGGER.warn("[dx11-perf] Could not create region index buffer: {}", e.toString());
            return null;
        }

        GpuBuffer old = this.buffer;
        this.buffer = grown;
        this.capacity = newCapacity;
        this.type = newType;
        if (old != null) {
            try {
                old.close();
            } catch (RuntimeException ignored) {
                // The replacement is already live.
            }
        }
        return grown;
    }

    public VertexFormat.IndexType type() {
        return this.type;
    }

    public int capacity() {
        return this.capacity;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
        }
        this.capacity = 0;
    }

    /** Builds the vanilla QUADS index pattern for {@code indexCount} indices. */
    public static ByteBuffer sequentialQuads(
        int indexCount, VertexFormat.IndexType type) {
        if (indexCount < 0 || indexCount % 6 != 0) {
            throw new IllegalArgumentException("Index count must be a non-negative multiple of 6");
        }
        ByteBuffer data = ByteBuffer.allocateDirect(indexCount * type.bytes)
            .order(ByteOrder.nativeOrder());
        for (int index = 0; index < indexCount; index += 6) {
            int vertex = (index / 6) * 4;
            put(data, type, vertex);
            put(data, type, vertex + 1);
            put(data, type, vertex + 2);
            put(data, type, vertex + 2);
            put(data, type, vertex + 3);
            put(data, type, vertex);
        }
        data.flip();
        return data;
    }

    private static void put(ByteBuffer data, VertexFormat.IndexType type, int value) {
        if (type == VertexFormat.IndexType.INT) {
            data.putInt(value);
        } else {
            data.putShort((short) value);
        }
    }
}
