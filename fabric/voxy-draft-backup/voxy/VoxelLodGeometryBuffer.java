package com.xgdt.dx12.lod.voxy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.xgdt.dx12.dx12.Dx12GpuBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Manages the geometry buffers for Voxy-style voxel LOD.
 * Uses a ring buffer of GPU buffers for multi-frame in-flight rendering.
 * Each buffer stores quads (8 bytes each: 2 x uint32).
 */
@Environment(EnvType.CLIENT)
public final class VoxelLodGeometryBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-voxy-geom");

    /** Ring size (matches frame in-flight count). */
    private static final int RING_SIZE = VoxelLodConstants.GEOMETRY_RING_SIZE;

    /** Geometry buffers (one per frame in flight). */
    private final Dx12GpuBuffer[] geometryBuffers;
    /** Geometry buffer mapped views (for CPU fallback). */
    private final GpuBufferSlice.MappedView[] geometryViews;
    /** Geometry buffer byte buffers. */
    private final ByteBuffer[] geometryBytes;
    /** Current ring index. */
    private int ringIndex = 0;
    /** Current buffer capacity in quads. */
    private int quadCapacity = 0;
    /** Maximum geometry capacity in bytes. */
    private final long maxCapacityBytes;
    /** Used quads in current frame. */
    private int usedQuads = 0;
    /** Total used bytes across all frames. */
    private long totalUsedBytes = 0;

    /** Indirect draw argument buffers (one per ring). */
    private final Dx12GpuBuffer[] argsBuffers;
    /** Indirect args mapped views. */
    private final GpuBufferSlice.MappedView[] argsViews;
    /** Indirect args byte buffers. */
    private final ByteBuffer[] argsBytes;

    /** Counter buffers for atomic quad allocation (one per ring). */
    private final Dx12GpuBuffer[] counterBuffers;

    public VoxelLodGeometryBuffer(long maxCapacityBytes) {
        this.maxCapacityBytes = maxCapacityBytes;
        this.geometryBuffers = new Dx12GpuBuffer[RING_SIZE];
        this.geometryViews = new GpuBufferSlice.MappedView[RING_SIZE];
        this.geometryBytes = new ByteBuffer[RING_SIZE];
        this.argsBuffers = new Dx12GpuBuffer[RING_SIZE];
        this.argsViews = new GpuBufferSlice.MappedView[RING_SIZE];
        this.argsBytes = new ByteBuffer[RING_SIZE];
        this.counterBuffers = new Dx12GpuBuffer[RING_SIZE];
    }

    /** Get the current frame's geometry buffer. */
    public Dx12GpuBuffer getCurrentGeometryBuffer() {
        return this.geometryBuffers[this.ringIndex];
    }

    /** Get the current frame's args buffer. */
    public Dx12GpuBuffer getCurrentArgsBuffer() {
        return this.argsBuffers[this.ringIndex];
    }

    /** Get the current frame's counter buffer. */
    public Dx12GpuBuffer getCurrentCounterBuffer() {
        return this.counterBuffers[this.ringIndex];
    }

    /** Get the current ring index. */
    public int getRingIndex() {
        return this.ringIndex;
    }

    /** Advance to the next ring buffer. */
    public void nextFrame() {
        this.ringIndex = (this.ringIndex + 1) % RING_SIZE;
        this.usedQuads = 0;
    }

    /** Ensure geometry buffers have enough capacity. */
    public void ensureCapacity(int requiredQuads) {
        int requiredBytes = requiredQuads * VoxelLodConstants.QUAD_SIZE_BYTES;
        if (this.quadCapacity >= requiredQuads) {
            return;
        }

        int newQuadCapacity = Math.max(requiredQuads, Math.max(this.quadCapacity * 2, 65536));
        long newBytes = (long) newQuadCapacity * VoxelLodConstants.QUAD_SIZE_BYTES;

        if (newBytes > this.maxCapacityBytes) {
            newQuadCapacity = (int) (this.maxCapacityBytes / VoxelLodConstants.QUAD_SIZE_BYTES);
            newBytes = this.maxCapacityBytes;
        }

        // Recreate all ring buffers
        for (int i = 0; i < RING_SIZE; i++) {
            recreateBuffer(i, newQuadCapacity, newBytes);
        }

        this.quadCapacity = newQuadCapacity;
        LOGGER.info("[voxy] Geometry buffer expanded to {} quads ({} MB per ring, {} rings)",
            newQuadCapacity, newBytes / (1024*1024), RING_SIZE);
    }

    /** Recreate a single ring's buffers. */
    private void recreateBuffer(int ring, int quadCapacity, long bytes) {
        // Geometry buffer (UAV for compute write, SRV for vertex shader read)
        if (this.geometryViews[ring] != null) {
            try { this.geometryViews[ring].close(); } catch (Throwable ignored) {}
            this.geometryViews[ring] = null;
        }
        if (this.geometryBuffers[ring] != null) {
            try { this.geometryBuffers[ring].close(); } catch (Throwable ignored) {}
            this.geometryBuffers[ring] = null;
        }

        this.geometryBuffers[ring] = new Dx12GpuBuffer(
            VoxelLodConstants.BUFFER_USAGE_UAV | GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_STORAGE_BUFFER,
            bytes);
        this.geometryViews[ring] = this.geometryBuffers[ring].map(0L, bytes, false, true);
        this.geometryBytes[ring] = this.geometryViews[ring].data();
        this.geometryBytes[ring].order(ByteOrder.LITTLE_ENDIAN);

        // Indirect args buffer (5 uints for DrawIndexedIndirect: indexCount, instanceCount, startIndex, baseVertex, startInstance)
        if (this.argsViews[ring] != null) {
            try { this.argsViews[ring].close(); } catch (Throwable ignored) {}
            this.argsViews[ring] = null;
        }
        if (this.argsBuffers[ring] != null) {
            try { this.argsBuffers[ring].close(); } catch (Throwable ignored) {}
            this.argsBuffers[ring] = null;
        }

        long argsBytes = 5L * 4L; // 5 uints
        this.argsBuffers[ring] = new Dx12GpuBuffer(
            VoxelLodConstants.BUFFER_USAGE_UAV | GpuBuffer.USAGE_INDIRECT_BUFFER,
            argsBytes);
        this.argsViews[ring] = this.argsBuffers[ring].map(0L, argsBytes, false, true);
        this.argsBytes[ring] = this.argsViews[ring].data();
        this.argsBytes[ring].order(ByteOrder.LITTLE_ENDIAN);

        // Counter buffer (1 uint)
        if (this.counterBuffers[ring] != null) {
            try { this.counterBuffers[ring].close(); } catch (Throwable ignored) {}
            this.counterBuffers[ring] = null;
        }

        this.counterBuffers[ring] = new Dx12GpuBuffer(
            VoxelLodConstants.BUFFER_USAGE_UAV | GpuBuffer.USAGE_STORAGE_BUFFER,
            4L); // 1 uint
    }

    /** Reset current frame's used quad count and counter. */
    public void resetFrame() {
        this.usedQuads = 0;
        if (this.argsBytes[this.ringIndex] != null) {
            this.argsBytes[this.ringIndex].putInt(0, 0); // indexCount = 0
            this.argsBytes[this.ringIndex].putInt(4, 1); // instanceCount = 1
            this.argsBytes[this.ringIndex].putInt(8, 0); // startIndex = 0
            this.argsBytes[this.ringIndex].putInt(12, 0); // baseVertex = 0
            this.argsBytes[this.ringIndex].putInt(16, 0); // startInstance = 0
        }
        if (this.counterBuffers[this.ringIndex] != null) {
            // Counter will be reset by compute shader
        }
    }

    /** Get used quads in current frame. */
    public int getUsedQuads() {
        return this.usedQuads;
    }

    /** Set used quads (called after compute shader finishes). */
    public void setUsedQuads(int quads) {
        this.usedQuads = quads;
        if (this.argsBytes[this.ringIndex] != null) {
            this.argsBytes[this.ringIndex].putInt(0, quads * 6); // 6 indices per quad
        }
    }

    /** Get total geometry capacity in quads. */
    public int getQuadCapacity() {
        return this.quadCapacity;
    }

    /** Get total geometry capacity in bytes. */
    public long getCapacityBytes() {
        return (long) this.quadCapacity * VoxelLodConstants.QUAD_SIZE_BYTES;
    }

    /** Get free space in current frame. */
    public int getFreeQuads() {
        return this.quadCapacity - this.usedQuads;
    }

    /** Write a quad directly (CPU fallback). */
    public int writeQuadCpu(int quadIndex, int positionData, int colorData) {
        if (quadIndex >= this.quadCapacity || this.geometryBytes[this.ringIndex] == null) {
            return -1;
        }
        int offset = quadIndex * VoxelLodConstants.QUAD_SIZE_BYTES;
        this.geometryBytes[this.ringIndex].putInt(offset, positionData);
        this.geometryBytes[this.ringIndex].putInt(offset + 4, colorData);
        return quadIndex;
    }

    /** Close all buffers. */
    public void close() {
        for (int i = 0; i < RING_SIZE; i++) {
            if (this.geometryViews[i] != null) {
                try { this.geometryViews[i].close(); } catch (Throwable ignored) {}
                this.geometryViews[i] = null;
            }
            if (this.geometryBuffers[i] != null) {
                try { this.geometryBuffers[i].close(); } catch (Throwable ignored) {}
                this.geometryBuffers[i] = null;
            }
            if (this.argsViews[i] != null) {
                try { this.argsViews[i].close(); } catch (Throwable ignored) {}
                this.argsViews[i] = null;
            }
            if (this.argsBuffers[i] != null) {
                try { this.argsBuffers[i].close(); } catch (Throwable ignored) {}
                this.argsBuffers[i] = null;
            }
            if (this.counterBuffers[i] != null) {
                try { this.counterBuffers[i].close(); } catch (Throwable ignored) {}
                this.counterBuffers[i] = null;
            }
        }
        this.quadCapacity = 0;
    }
}