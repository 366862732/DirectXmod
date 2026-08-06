/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntComparator
 *  it.unimi.dsi.fastutil.objects.ReferenceArrayList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.opengl.ARBBufferStorage
 *  org.lwjgl.opengl.GL33C
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.BufferStorage;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlDebugLabel;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlUtil;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.util.TransientBlockAllocator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public abstract class GlTransientMemory
implements TransientMemory,
AutoCloseable {
    private static final long BLOCK_SIZE = 524288L;
    private static final long MAX_CPU_ALIGNMENT = 16L;
    private static final long MAX_GPU_ALIGNMENT = Long.highestOneBit(Long.MAX_VALUE);
    final GlCommandEncoder encoder;
    protected final DirectStateAccess dsa;
    protected final BufferStorage bufferStorage;
    protected final GlDebugLabel debugLabels;
    private final TransientBlockAllocator<Long> cpuBlockAllocator = new TransientBlockAllocator<Long>(524288L, 16L, TransientBlockAllocator.Allocator.create(MemoryUtil::nmemAlloc, MemoryUtil::nmemFree));

    GlTransientMemory(GlDevice device, GlCommandEncoder encoder) {
        this.encoder = encoder;
        this.dsa = device.directStateAccess();
        this.bufferStorage = device.getBufferStorage();
        this.debugLabels = device.debugLabels();
    }

    @Override
    public void close() {
    }

    @Override
    public ByteBuffer allocateCpu(long size, long alignment, long minimumAllocation, long elementSize) {
        assert (size <= Integer.MAX_VALUE);
        TransientBlockAllocator.Allocation<Long> alloc = this.cpuBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
        return MemoryUtil.memByteBuffer((long)(alloc.block() + alloc.offset()), (int)((int)alloc.size()));
    }

    public void rotate() {
        this.cpuBlockAllocator.rotate().run();
    }

    @Environment(value=EnvType.CLIENT)
    private class TransientGpuBuffer
    extends GlBuffer {
        private boolean closed;
        private final long bufferSubmitIndex;
        final /* synthetic */ GlTransientMemory this$0;

        protected TransientGpuBuffer(GlTransientMemory glTransientMemory, @GpuBuffer.Usage int handle, int usage, long size) {
            GlTransientMemory glTransientMemory2 = glTransientMemory;
            Objects.requireNonNull(glTransientMemory2);
            this.this$0 = glTransientMemory2;
            super(usage, size, handle, true);
            this.closed = false;
            this.bufferSubmitIndex = glTransientMemory.encoder.currentSubmitIndex();
        }

        @Override
        public boolean isClosed() {
            if (this.closed) {
                return true;
            }
            this.closed = this.bufferSubmitIndex < this.this$0.encoder.currentSubmitIndex();
            return this.closed;
        }

        @Override
        public void close() {
            this.closed = true;
        }

        @Override
        public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
            throw new IllegalStateException("Cannot map Transient buffer");
        }

        @Override
        public GpuBufferSlice slice(long offset, long length) {
            throw new IllegalStateException("Cannot slice transient buffer");
        }

        @Override
        public GpuBufferSlice slice() {
            throw new IllegalStateException("Cannot slice transient buffer");
        }
    }

    @Environment(value=EnvType.CLIENT)
    static class PersistentMapping
    extends GlTransientMemory {
        private final TransientBlockAllocator<GlAllocation> stagingBlockAllocator;
        private final TransientBlockAllocator<GlAllocation> gpuBlockAllocator;
        private final TransientBlockAllocator<GlAllocation> gpuMappedBlockAllocator;
        private final @Nullable Rotation[] rotations = new Rotation[2];

        PersistentMapping(GlDevice device, GlCommandEncoder encoder) {
            super(device, encoder);
            this.stagingBlockAllocator = new TransientBlockAllocator<GlAllocation>(524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(size -> this.allocateGlBlock(size, true, true), this::freeGlBlock));
            this.gpuBlockAllocator = new TransientBlockAllocator<GlAllocation>(524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(size -> this.allocateGlBlock(size, false, false), this::freeGlBlock));
            this.gpuMappedBlockAllocator = new TransientBlockAllocator<GlAllocation>(524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(size -> this.allocateGlBlock(size, false, true), this::freeGlBlock));
        }

        @Override
        public void close() {
            GL33C.glFinish();
            for (Rotation rotation : this.rotations) {
                if (rotation == null) continue;
                rotation.run();
            }
        }

        @Override
        public void rotate() {
            Rotation previousRotation = this.rotations[this.encoder.currentSubmitSlot()];
            if (previousRotation != null) {
                previousRotation.run();
            }
            this.rotations[this.encoder.currentSubmitSlot()] = new Rotation(this.stagingBlockAllocator.rotate(), this.gpuBlockAllocator.rotate(), this.gpuMappedBlockAllocator.rotate());
            super.rotate();
        }

        private GlAllocation allocateGlBlock(long size, boolean host, boolean mapped) {
            int glBuffer = this.dsa.createBuffer();
            int usageFlags = 0;
            if (host) {
                usageFlags |= 0x200;
            }
            if (mapped) {
                usageFlags |= 2;
                usageFlags |= 0x40;
                usageFlags |= 0x80;
            }
            GlStateManager._glBindBuffer(34962, glBuffer);
            ARBBufferStorage.glBufferStorage((int)34962, (long)size, (int)usageFlags);
            long hostPtr = mapped ? GL33C.nglMapBufferRange((int)34962, (long)0L, (long)size, (int)226) : 0L;
            GlStateManager._glBindBuffer(34962, 0);
            this.debugLabels.applyLabel(new TransientGpuBuffer(this, glBuffer, 0, size), () -> "OpenGL Transient Buffer");
            return new GlAllocation(glBuffer, hostPtr, size);
        }

        private void freeGlBlock(GlAllocation allocation) {
            GlStateManager._glDeleteBuffers(allocation.glBuffer);
        }

        @Override
        public GpuBufferSlice.MappedView allocateStaging(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
            assert (size <= Integer.MAX_VALUE);
            TransientBlockAllocator.Allocation<GlAllocation> alloc = this.stagingBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
            TransientGpuBuffer apiBuffer = new TransientGpuBuffer(this, alloc.block().glBuffer, usage, (int)alloc.block().size);
            ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer((long)(alloc.block().hostPtr + alloc.offset()), (int)((int)alloc.size()));
            return new GpuBufferSlice.MappedView(new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size()), cpuBuffer, () -> {});
        }

        @Override
        public GpuBufferSlice allocateGpu(long size, long alignment, int usage, @GpuBuffer.Usage long minimumAllocation, long elementSize) {
            assert (size <= Integer.MAX_VALUE);
            TransientBlockAllocator.Allocation<GlAllocation> alloc = this.gpuBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
            TransientGpuBuffer apiBuffer = new TransientGpuBuffer(this, alloc.block().glBuffer, usage, (int)alloc.block().size);
            return new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size());
        }

        @Override
        public GpuBufferSlice.MappedView allocateGpuMapped(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
            assert (size <= Integer.MAX_VALUE);
            TransientBlockAllocator.Allocation<GlAllocation> alloc = this.gpuMappedBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
            TransientGpuBuffer apiBuffer = new TransientGpuBuffer(this, alloc.block().glBuffer, usage, (int)alloc.block().size);
            ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer((long)(alloc.block().hostPtr + alloc.offset()), (int)((int)alloc.size()));
            return new GpuBufferSlice.MappedView(new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size()), cpuBuffer, () -> {});
        }

        @Override
        public GpuBufferSlice uploadStaging(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
            return this.upload(data, alignment, usage, minimumAllocation, elementSize, true);
        }

        @Override
        public GpuBufferSlice uploadGpu(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
            return this.upload(data, alignment, usage, minimumAllocation, elementSize, false);
        }

        public GpuBufferSlice upload(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize, boolean staging) {
            long totalSize = 0L;
            for (ByteBuffer buffer : data) {
                totalSize += (long)buffer.remaining();
                totalSize = Mth.roundToward(totalSize, alignment);
            }
            try (GpuBufferSlice.MappedView mapped = staging ? this.allocateStaging(totalSize, alignment, usage, minimumAllocation, elementSize) : this.allocateGpuMapped(totalSize, alignment, usage, minimumAllocation, elementSize);){
                long mappedPtr = MemoryUtil.memAddress((ByteBuffer)mapped.data());
                long offset = 0L;
                for (ByteBuffer buffer : data) {
                    MemoryUtil.memCopy((long)MemoryUtil.memAddress((ByteBuffer)buffer), (long)(mappedPtr + offset), (long)Math.min(mapped.slice().length() - offset, (long)buffer.remaining()));
                    offset += (long)buffer.remaining();
                    if ((offset = Mth.roundToward(offset, alignment)) < mapped.slice().length()) continue;
                    break;
                }
                GpuBufferSlice gpuBufferSlice = mapped.slice();
                return gpuBufferSlice;
            }
        }

        @Override
        public List<GpuBufferSlice> multiUploadStaging(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage) {
            return this.multiUpload(data, alignment, usage, true);
        }

        @Override
        public List<GpuBufferSlice> multiUploadGpu(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage) {
            return this.multiUpload(data, alignment, usage, false);
        }

        public List<GpuBufferSlice> multiUpload(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage, boolean staging) {
            ReferenceArrayList uploadedBuffers = new ReferenceArrayList();
            uploadedBuffers.size(data.size());
            TransientBlockAllocator<GlAllocation> allocatorInUse = staging ? this.stagingBlockAllocator : this.gpuMappedBlockAllocator;
            IntArrayList sortedDataIndices = IntArrayList.toList((IntStream)IntStream.range(0, data.size()));
            sortedDataIndices.sort(IntComparator.comparing(index -> ((ByteBuffer)data.get(index)).remaining()));
            while (!sortedDataIndices.isEmpty()) {
                boolean allocatedAnything = false;
                for (int i = sortedDataIndices.size() - 1; i >= 0; --i) {
                    int bufferIndex = sortedDataIndices.getInt(i);
                    ByteBuffer currentBuffer = data.get(bufferIndex);
                    if (!allocatorInUse.canAllocateInCurrentBlock(currentBuffer.remaining(), alignment)) continue;
                    sortedDataIndices.removeInt(i);
                    try (GpuBufferSlice.MappedView view = staging ? this.allocateStaging(currentBuffer.remaining(), alignment, usage) : this.allocateGpuMapped(currentBuffer.remaining(), alignment, usage);){
                        MemoryUtil.memCopy((ByteBuffer)currentBuffer, (ByteBuffer)view.data());
                        uploadedBuffers.set(bufferIndex, (Object)view.slice());
                    }
                    allocatedAnything = true;
                    break;
                }
                if (allocatedAnything) continue;
                int bufferIndex = sortedDataIndices.popInt();
                ByteBuffer currentBuffer = data.get(bufferIndex);
                GpuBufferSlice.MappedView view = this.allocateGpuMapped(currentBuffer.remaining(), alignment, usage);
                try {
                    MemoryUtil.memCopy((ByteBuffer)currentBuffer, (ByteBuffer)view.data());
                    uploadedBuffers.set(bufferIndex, (Object)view.slice());
                }
                finally {
                    if (view == null) continue;
                    view.close();
                }
            }
            return uploadedBuffers;
        }

        @Environment(value=EnvType.CLIENT)
        private record Rotation(Runnable staging, Runnable gpu, Runnable gpuMapped) implements Runnable
        {
            @Override
            public void run() {
                this.staging.run();
                this.gpu.run();
                this.gpuMapped.run();
            }
        }

        @Environment(value=EnvType.CLIENT)
        private record GlAllocation(int glBuffer, long hostPtr, long size) {
        }
    }

    @Environment(value=EnvType.CLIENT)
    static class Fallback
    extends GlTransientMemory {
        private final TransientBlockAllocator<GlAllocation> blockAllocator = new TransientBlockAllocator<GlAllocation>(524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(this::allocateGlBlock, this::freeGlBlock));

        Fallback(GlDevice device, GlCommandEncoder encoder) {
            super(device, encoder);
        }

        @Override
        public void close() {
            this.rotate();
        }

        @Override
        public void rotate() {
            this.blockAllocator.rotate().run();
        }

        private GlAllocation allocateGlBlock(long size) {
            GlBuffer buffer = this.bufferStorage.createBuffer(this.dsa, 40, size);
            this.debugLabels.applyLabel(buffer, () -> "OpenGL Transient Buffer");
            long hostPtr = MemoryUtil.nmemAlloc((long)size);
            return new GlAllocation(buffer, hostPtr);
        }

        private void freeGlBlock(GlAllocation allocation) {
            allocation.glBuffer.close();
            MemoryUtil.nmemFree((long)allocation.hostBuffer);
        }

        @Override
        public GpuBufferSlice.MappedView allocateStaging(long size, long alignment, int usage, long minimumAllocation, long elementSize) {
            TransientBlockAllocator.Allocation<GlAllocation> allocation = this.blockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
            TransientGpuBuffer transientBuffer = new TransientGpuBuffer(this, allocation.block().glBuffer().handle(), usage, allocation.block().glBuffer().size());
            GpuBufferSlice slice = new GpuBufferSlice(transientBuffer, allocation.offset(), allocation.size());
            ByteBuffer hostBuffer = MemoryUtil.memByteBuffer((long)(allocation.block().hostBuffer + allocation.offset()), (int)((int)allocation.size()));
            return new GpuBufferSlice.MappedView(slice, hostBuffer, () -> this.dsa.bufferSubData(transientBuffer.handle(), slice.offset(), MemoryUtil.memByteBuffer((long)(((GlAllocation)allocation.block()).hostBuffer + allocation.offset()), (int)((int)allocation.size())), usage));
        }

        @Override
        public GpuBufferSlice allocateGpu(long size, long alignment, int usage, long minimumAllocation, long elementSize) {
            TransientBlockAllocator.Allocation<GlAllocation> allocation = this.blockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
            TransientGpuBuffer transientBuffer = new TransientGpuBuffer(this, allocation.block().glBuffer().handle(), usage, allocation.block().glBuffer().size());
            return new GpuBufferSlice(transientBuffer, allocation.offset(), allocation.size());
        }

        @Override
        public GpuBufferSlice.MappedView allocateGpuMapped(long size, long alignment, int usage, long minimumAllocation, long elementSize) {
            return this.allocateStaging(size, alignment, usage, minimumAllocation, elementSize);
        }

        @Override
        public GpuBufferSlice uploadStaging(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
            return this.uploadGpu(data, alignment, usage, minimumAllocation, elementSize);
        }

        @Override
        public GpuBufferSlice uploadGpu(List<ByteBuffer> data, long alignment, int usage, long minimumAllocation, long elementSize) {
            long totalSize = 0L;
            for (int i = 0; i < data.size(); ++i) {
                totalSize += (long)data.get(i).remaining();
                totalSize = Mth.roundToward(totalSize, alignment);
            }
            GpuBufferSlice bufferSlice = this.allocateGpu(totalSize, alignment, usage);
            int target = GlUtil.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(target, ((GlBuffer)bufferSlice.buffer()).handle());
            long ptr = GL33C.nglMapBufferRange((int)target, (long)bufferSlice.offset(), (long)totalSize, (int)38);
            long offset = 0L;
            for (int i = 0; i < data.size(); ++i) {
                ByteBuffer buffer = data.get(i);
                MemoryUtil.memCopy((long)MemoryUtil.memAddress((ByteBuffer)buffer), (long)(ptr + offset), (long)buffer.remaining());
                offset += (long)buffer.remaining();
                offset = Mth.roundToward(offset, alignment);
            }
            GL33C.glUnmapBuffer((int)target);
            GlStateManager._glBindBuffer(target, 0);
            return bufferSlice;
        }

        @Override
        public List<GpuBufferSlice> multiUploadStaging(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage) {
            return this.multiUploadGpu(data, alignment, usage);
        }

        @Override
        public List<GpuBufferSlice> multiUploadGpu(List<ByteBuffer> data, long alignment, int usage) {
            ReferenceArrayList uploadedBuffers = new ReferenceArrayList();
            uploadedBuffers.size(data.size());
            IntArrayList sortedDataIndices = IntArrayList.toList((IntStream)IntStream.range(0, data.size()));
            sortedDataIndices.sort(IntComparator.comparing(index -> ((ByteBuffer)data.get(index)).remaining()));
            int target = GlUtil.selectBufferBindTarget(usage);
            while (!sortedDataIndices.isEmpty()) {
                boolean allocatedAnything = false;
                for (int i = sortedDataIndices.size() - 1; i >= 0; --i) {
                    int bufferIndex = sortedDataIndices.getInt(i);
                    ByteBuffer currentBuffer = data.get(bufferIndex);
                    if (!this.blockAllocator.canAllocateInCurrentBlock(currentBuffer.remaining(), alignment)) continue;
                    sortedDataIndices.removeInt(i);
                    GpuBufferSlice bufferSlice = this.allocateGpu(currentBuffer.remaining(), alignment, usage);
                    uploadedBuffers.set(bufferIndex, (Object)bufferSlice);
                    GlStateManager._glBindBuffer(target, ((GlBuffer)bufferSlice.buffer()).handle());
                    long ptr = GL33C.nglMapBufferRange((int)target, (long)bufferSlice.offset(), (long)bufferSlice.length(), (int)38);
                    MemoryUtil.memCopy((long)MemoryUtil.memAddress((ByteBuffer)currentBuffer), (long)ptr, (long)bufferSlice.length());
                    GL33C.glUnmapBuffer((int)target);
                    allocatedAnything = true;
                    break;
                }
                if (allocatedAnything) continue;
                int bufferIndex = sortedDataIndices.popInt();
                ByteBuffer currentBuffer = data.get(bufferIndex);
                GpuBufferSlice bufferSlice = this.allocateGpu(currentBuffer.remaining(), alignment, usage);
                uploadedBuffers.set(bufferIndex, (Object)bufferSlice);
                GlStateManager._glBindBuffer(target, ((GlBuffer)bufferSlice.buffer()).handle());
                long ptr = GL33C.nglMapBufferRange((int)target, (long)bufferSlice.offset(), (long)bufferSlice.length(), (int)38);
                MemoryUtil.memCopy((long)MemoryUtil.memAddress((ByteBuffer)currentBuffer), (long)ptr, (long)bufferSlice.length());
                GL33C.glUnmapBuffer((int)target);
            }
            GlStateManager._glBindBuffer(target, 0);
            return uploadedBuffers;
        }

        @Environment(value=EnvType.CLIENT)
        private record GlAllocation(GlBuffer glBuffer, long hostBuffer) {
        }
    }
}

