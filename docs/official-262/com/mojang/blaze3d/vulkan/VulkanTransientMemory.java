/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.Pair
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntComparator
 *  it.unimi.dsi.fastutil.objects.ReferenceArrayList
 *  it.unimi.dsi.fastutil.objects.ReferenceReferenceImmutablePair
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.util.vma.Vma
 *  org.lwjgl.util.vma.VmaAllocationCreateInfo
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkBufferCopy
 *  org.lwjgl.vulkan.VkBufferCopy$Buffer
 *  org.lwjgl.vulkan.VkBufferCreateInfo
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkMemoryHeap
 *  org.lwjgl.vulkan.VkMemoryType
 *  org.lwjgl.vulkan.VkPhysicalDevice
 *  org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.util.TransientBlockAllocator;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceReferenceImmutablePair;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryHeap;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

@Environment(value=EnvType.CLIENT)
public class VulkanTransientMemory
implements TransientMemory,
Destroyable {
    private static final long BLOCK_SIZE = 524288L;
    private static final long MAX_CPU_ALIGNMENT = 16L;
    private static final long MAX_GPU_ALIGNMENT = Long.highestOneBit(Long.MAX_VALUE);
    private static final int BUFFER_USAGE_BITS = 471;
    private final VulkanDevice device;
    private final VulkanCommandEncoder encoder;
    private final boolean useDeviceMemoryForMappedGpuStaging;
    private final TransientBlockAllocator<Long> cpuBlockAllocator = new TransientBlockAllocator<Long>(524288L, 16L, TransientBlockAllocator.Allocator.create(MemoryUtil::nmemAlloc, MemoryUtil::nmemFree));
    private final TransientBlockAllocator<VulkanAllocation> stagingBlockAllocator;
    private final TransientBlockAllocator<VulkanAllocation> gpuBlockAllocator;
    private final TransientBlockAllocator<Pair<VulkanAllocation, VulkanAllocation>> gpuMappedBlockAllocator;
    private long submitIndex = 0L;
    private boolean anyCommandRecorded = false;
    private @Nullable VkCommandBuffer commandBuffer;

    VulkanTransientMemory(VulkanDevice device, VulkanCommandEncoder encoder) {
        this.device = device;
        this.encoder = encoder;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkDevice vkDevice = device.vkDevice();
            VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc((MemoryStack)stack);
            VK12.vkGetPhysicalDeviceMemoryProperties((VkPhysicalDevice)vkDevice.getPhysicalDevice(), (VkPhysicalDeviceMemoryProperties)memoryProperties);
            int heapCount = memoryProperties.memoryHeapCount();
            int typeCount = memoryProperties.memoryTypeCount();
            int largestDeviceLocalHeapIndex = -1;
            long largestDeviceLocalHeapSize = -1L;
            for (int i = 0; i < heapCount; ++i) {
                VkMemoryHeap heapProperties = memoryProperties.memoryHeaps(i);
                if (!VulkanUtils.hasAnyBit(heapProperties.flags(), 1) || heapProperties.size() < largestDeviceLocalHeapSize) continue;
                largestDeviceLocalHeapIndex = i;
                largestDeviceLocalHeapSize = heapProperties.size();
            }
            assert (largestDeviceLocalHeapIndex != -1);
            boolean largestHeapIsHostVisibleAndCoherent = false;
            for (int i = 0; i < typeCount; ++i) {
                VkMemoryType typeProperties = memoryProperties.memoryTypes(i);
                if (typeProperties.heapIndex() != largestDeviceLocalHeapIndex || !VulkanUtils.hasAllBits(typeProperties.propertyFlags(), 6)) continue;
                largestHeapIsHostVisibleAndCoherent = true;
                break;
            }
            this.useDeviceMemoryForMappedGpuStaging = largestHeapIsHostVisibleAndCoherent;
        }
        this.stagingBlockAllocator = new TransientBlockAllocator<VulkanAllocation>(524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(size -> this.allocateVulkanBlock(size, true), this::freeVulkanBlock));
        this.gpuBlockAllocator = new TransientBlockAllocator<VulkanAllocation>(524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(size -> this.allocateVulkanBlock(size, false), this::queueFreeVulkanBlock));
        this.gpuMappedBlockAllocator = new TransientBlockAllocator<Pair>(524288L, MAX_GPU_ALIGNMENT, TransientBlockAllocator.Allocator.create(this::allocateGpuMappedVulkanBlock, this::freeGpuMappedVulkanBlock), this::recordGpuMappedCopy);
    }

    @Override
    public void destroy() {
        this.cpuBlockAllocator.close();
        this.stagingBlockAllocator.close();
        this.gpuBlockAllocator.close();
        this.gpuMappedBlockAllocator.close();
    }

    public void beginSubmit() {
        assert (this.commandBuffer == null);
        this.commandBuffer = this.encoder.allocateAndBeginTransientCommandBuffer();
        this.encoder.execute(this.commandBuffer);
        this.anyCommandRecorded = false;
    }

    public void endSubmit() {
        this.cpuBlockAllocator.rotate().run();
        this.encoder.queueForDestroy(this.stagingBlockAllocator.rotate()::run);
        if (this.useDeviceMemoryForMappedGpuStaging) {
            this.encoder.queueForDestroy(this.gpuBlockAllocator.rotate()::run);
        } else {
            this.gpuBlockAllocator.rotate().run();
        }
        this.gpuMappedBlockAllocator.rotate();
        assert (this.commandBuffer != null);
        if (this.anyCommandRecorded) {
            try (MemoryStack stack = MemoryStack.stackPush();){
                VulkanCommandEncoder.memoryBarrier(this.commandBuffer, stack);
            }
        }
        VK12.vkEndCommandBuffer((VkCommandBuffer)this.commandBuffer);
        this.commandBuffer = null;
        ++this.submitIndex;
    }

    private void recordGpuMappedCopy(Pair<VulkanAllocation, VulkanAllocation> block) {
        if (block.first() == block.second()) {
            return;
        }
        assert (((VulkanAllocation)block.first()).size == ((VulkanAllocation)block.second()).size);
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkBufferCopy.Buffer region = VkBufferCopy.calloc((int)1, (MemoryStack)stack);
            region.srcOffset(0L);
            region.dstOffset(0L);
            region.size(((VulkanAllocation)block.first()).size);
            assert (this.commandBuffer != null);
            VK12.vkCmdCopyBuffer((VkCommandBuffer)this.commandBuffer, (long)((VulkanAllocation)block.first()).vkBuffer, (long)((VulkanAllocation)block.second()).vkBuffer, (VkBufferCopy.Buffer)region);
            this.anyCommandRecorded = true;
        }
    }

    private VulkanAllocation allocateVulkanBlock(long size, boolean staging) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc((MemoryStack)stack).sType$Default();
            bufferCreateInfo.size(size);
            bufferCreateInfo.usage(471);
            bufferCreateInfo.sharingMode(0);
            bufferCreateInfo.pQueueFamilyIndices(null);
            VmaAllocationCreateInfo allocCreateInfo = VmaAllocationCreateInfo.calloc((MemoryStack)stack);
            if (staging) {
                allocCreateInfo.usage(9);
            } else {
                allocCreateInfo.usage(8);
            }
            if (this.useDeviceMemoryForMappedGpuStaging || staging) {
                allocCreateInfo.requiredFlags(6);
                allocCreateInfo.flags(1024);
            }
            LongBuffer bufferPtr = stack.callocLong(1);
            PointerBuffer allocPtr = stack.callocPointer(1);
            int result = Vma.vmaCreateBuffer((long)this.device.vma(), (VkBufferCreateInfo)bufferCreateInfo, (VmaAllocationCreateInfo)allocCreateInfo, (LongBuffer)bufferPtr, (PointerBuffer)allocPtr, null);
            VulkanUtils.crashIfFailure(this.device, result, "Failed to allocate VkBuffer");
            PointerBuffer hostPtrPtr = stack.callocPointer(1);
            if (staging || this.useDeviceMemoryForMappedGpuStaging) {
                VulkanUtils.crashIfFailure(this.device, Vma.vmaMapMemory((long)this.device.vma(), (long)allocPtr.get(0), (PointerBuffer)hostPtrPtr), "Failed to map buffer");
            }
            this.device.instance().debug().setObjectName(this.device.vkDevice(), 9, bufferPtr.get(0), "Vulkan Transient Memory Buffer");
            VulkanAllocation vulkanAllocation = new VulkanAllocation(bufferPtr.get(0), allocPtr.get(0), hostPtrPtr.get(0), size);
            return vulkanAllocation;
        }
    }

    private void queueFreeVulkanBlock(VulkanAllocation allocation) {
        this.encoder.queueForDestroy(() -> this.freeVulkanBlock(allocation));
    }

    private void freeVulkanBlock(VulkanAllocation allocation) {
        Vma.vmaDestroyBuffer((long)this.device.vma(), (long)allocation.vkBuffer, (long)allocation.vmaAllocation);
    }

    private Pair<VulkanAllocation, VulkanAllocation> allocateGpuMappedVulkanBlock(long size) {
        assert (size >= 524288L);
        assert (size >= this.gpuBlockAllocator.blockSize());
        if (this.useDeviceMemoryForMappedGpuStaging) {
            TransientBlockAllocator.Allocation<VulkanAllocation> block = this.gpuBlockAllocator.allocate(size, 16L, size, 1L);
            assert (block.offset() == 0L);
            return new ReferenceReferenceImmutablePair((Object)block.block(), (Object)block.block());
        }
        assert (size >= this.stagingBlockAllocator.blockSize());
        TransientBlockAllocator.Allocation<VulkanAllocation> stagingBlock = this.stagingBlockAllocator.allocate(size, 16L, size, 1L);
        TransientBlockAllocator.Allocation<VulkanAllocation> gpuBlock = this.gpuBlockAllocator.allocate(size, 16L, size, 1L);
        return new ReferenceReferenceImmutablePair((Object)stagingBlock.block(), (Object)gpuBlock.block());
    }

    private void freeGpuMappedVulkanBlock(Pair<VulkanAllocation, VulkanAllocation> allocations) {
    }

    @Override
    public ByteBuffer allocateCpu(long size, long alignment, long minimumAllocation, long elementSize) {
        assert (size <= Integer.MAX_VALUE);
        TransientBlockAllocator.Allocation<Long> alloc = this.cpuBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
        return MemoryUtil.memByteBuffer((long)(alloc.block() + alloc.offset()), (int)((int)alloc.size()));
    }

    @Override
    public GpuBufferSlice.MappedView allocateStaging(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        assert (size <= Integer.MAX_VALUE);
        TransientBlockAllocator.Allocation<VulkanAllocation> alloc = this.stagingBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
        TransientGpuBuffer apiBuffer = new TransientGpuBuffer(this, alloc.block().vkBuffer, usage, (int)alloc.block().size, this.submitIndex);
        ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer((long)(alloc.block().hostPtr + alloc.offset()), (int)((int)alloc.size()));
        return new GpuBufferSlice.MappedView(new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size()), cpuBuffer, () -> {});
    }

    @Override
    public GpuBufferSlice allocateGpu(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        assert (size <= Integer.MAX_VALUE);
        TransientBlockAllocator.Allocation<VulkanAllocation> alloc = this.gpuBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
        TransientGpuBuffer apiBuffer = new TransientGpuBuffer(this, alloc.block().vkBuffer, usage, (int)alloc.block().size, this.submitIndex);
        return new GpuBufferSlice(apiBuffer, alloc.offset(), alloc.size());
    }

    @Override
    public GpuBufferSlice.MappedView allocateGpuMapped(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        assert (size <= Integer.MAX_VALUE);
        TransientBlockAllocator.Allocation<Pair<VulkanAllocation, VulkanAllocation>> alloc = this.gpuMappedBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
        TransientGpuBuffer apiBuffer = new TransientGpuBuffer(this, ((VulkanAllocation)alloc.block().second()).vkBuffer, usage, (int)((VulkanAllocation)alloc.block().first()).size, this.submitIndex);
        ByteBuffer cpuBuffer = MemoryUtil.memByteBuffer((long)(((VulkanAllocation)alloc.block().first()).hostPtr + alloc.offset()), (int)((int)alloc.size()));
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
        TransientBlockAllocator<VulkanAllocation> allocatorInUse = staging ? this.stagingBlockAllocator : this.gpuMappedBlockAllocator;
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
    private record VulkanAllocation(long vkBuffer, long vmaAllocation, long hostPtr, long size) {
    }

    @Environment(value=EnvType.CLIENT)
    private class TransientGpuBuffer
    extends VulkanGpuBuffer {
        private boolean closed;
        private final long bufferSubmitIndex;
        final /* synthetic */ VulkanTransientMemory this$0;

        public TransientGpuBuffer(VulkanTransientMemory vulkanTransientMemory, @GpuBuffer.Usage long vkBuffer, int usage, int size, long bufferSubmitIndex) {
            VulkanTransientMemory vulkanTransientMemory2 = vulkanTransientMemory;
            Objects.requireNonNull(vulkanTransientMemory2);
            this.this$0 = vulkanTransientMemory2;
            super(vkBuffer, usage, size);
            this.closed = false;
            this.bufferSubmitIndex = bufferSubmitIndex;
        }

        @Override
        public void destroy() {
        }

        @Override
        public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
            throw new IllegalStateException("Cannot map transient buffer");
        }

        @Override
        public boolean isClosed() {
            if (this.closed) {
                return true;
            }
            this.closed = this.bufferSubmitIndex < this.this$0.submitIndex;
            return this.closed;
        }

        @Override
        public void close() {
            this.closed = true;
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
}

