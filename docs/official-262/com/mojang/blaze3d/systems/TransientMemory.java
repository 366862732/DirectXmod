/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface TransientMemory {
    default public ByteBuffer allocateCpu(long size, long alignment) {
        return this.allocateCpu(size, alignment, size, 1L);
    }

    public ByteBuffer allocateCpu(long var1, long var3, long var5, long var7);

    default public GpuBufferSlice.MappedView allocateStaging(long size, long alignment, @GpuBuffer.Usage int usage) {
        return this.allocateStaging(size, alignment, usage, size, 1L);
    }

    public GpuBufferSlice.MappedView allocateStaging(long var1, long var3, @GpuBuffer.Usage int var5, long var6, long var8);

    default public GpuBufferSlice allocateGpu(long size, long alignment, @GpuBuffer.Usage int usage) {
        return this.allocateGpu(size, alignment, usage, size, 1L);
    }

    public GpuBufferSlice allocateGpu(long var1, long var3, @GpuBuffer.Usage int var5, long var6, long var8);

    default public GpuBufferSlice.MappedView allocateGpuMapped(long size, long alignment, @GpuBuffer.Usage int usage) {
        return this.allocateGpuMapped(size, alignment, usage, size, 1L);
    }

    public GpuBufferSlice.MappedView allocateGpuMapped(long var1, long var3, @GpuBuffer.Usage int var5, long var6, long var8);

    default public GpuBufferSlice uploadStaging(ByteBuffer data, long alignment, @GpuBuffer.Usage int usage) {
        return this.uploadStaging(data, alignment, usage, (long)data.remaining(), 1L);
    }

    default public GpuBufferSlice uploadStaging(ByteBuffer data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        return this.uploadStaging(List.of(data), alignment, usage, minimumAllocation, elementSize);
    }

    default public GpuBufferSlice uploadStaging(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage) {
        long totalSize = 0L;
        for (ByteBuffer buffer : data) {
            totalSize += (long)buffer.remaining();
        }
        return this.uploadStaging(data, alignment, usage, totalSize, 1L);
    }

    public GpuBufferSlice uploadStaging(List<ByteBuffer> var1, long var2, @GpuBuffer.Usage int var4, long var5, long var7);

    default public GpuBufferSlice uploadGpu(ByteBuffer data, long alignment, @GpuBuffer.Usage int usage) {
        return this.uploadGpu(data, alignment, usage, (long)data.remaining(), 1L);
    }

    default public GpuBufferSlice uploadGpu(ByteBuffer data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        return this.uploadGpu(List.of(data), alignment, usage, minimumAllocation, elementSize);
    }

    default public GpuBufferSlice uploadGpu(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage) {
        long totalSize = 0L;
        for (ByteBuffer buffer : data) {
            totalSize += (long)buffer.remaining();
        }
        return this.uploadGpu(data, alignment, usage, totalSize, 1L);
    }

    public GpuBufferSlice uploadGpu(List<ByteBuffer> var1, long var2, @GpuBuffer.Usage int var4, long var5, long var7);

    public List<GpuBufferSlice> multiUploadStaging(List<ByteBuffer> var1, long var2, @GpuBuffer.Usage int var4);

    public List<GpuBufferSlice> multiUploadGpu(List<ByteBuffer> var1, long var2, @GpuBuffer.Usage int var4);
}

