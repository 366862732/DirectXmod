/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.util.vma.Vma
 *  org.lwjgl.util.vma.VmaAllocationCreateInfo
 *  org.lwjgl.vulkan.VkBufferCreateInfo
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

@Environment(value=EnvType.CLIENT)
public abstract class VulkanGpuBuffer
extends GpuBuffer
implements Destroyable {
    private final long vkBuffer;

    public VulkanGpuBuffer(long vkBuffer, @GpuBuffer.Usage int usage, long size) {
        super(usage, size);
        this.vkBuffer = vkBuffer;
    }

    public long vkBuffer() {
        return this.vkBuffer;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Direct
    extends VulkanGpuBuffer {
        private boolean closed;
        protected final VulkanDevice device;
        private final long vmaAllocation;
        private int mappingRefCount;

        public Direct(VulkanDevice device, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size, boolean forceHostVisibleAllocation) {
            long vkBuffer;
            this.device = device;
            try (MemoryStack stack = MemoryStack.stackPush();){
                VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc((MemoryStack)stack).sType$Default();
                bufferCreateInfo.size(size);
                bufferCreateInfo.usage(VulkanConst.bufferUsageToVk(usage));
                bufferCreateInfo.sharingMode(0);
                bufferCreateInfo.pQueueFamilyIndices(null);
                VmaAllocationCreateInfo allocCreateInfo = VmaAllocationCreateInfo.calloc((MemoryStack)stack);
                allocCreateInfo.usage(8);
                if (forceHostVisibleAllocation) {
                    allocCreateInfo.requiredFlags(allocCreateInfo.requiredFlags() | 2);
                }
                if (VulkanUtils.hasAnyBit(usage, 3)) {
                    allocCreateInfo.requiredFlags(allocCreateInfo.requiredFlags() | 2 | 4);
                    if (VulkanUtils.hasAnyBit(usage, 1)) {
                        allocCreateInfo.preferredFlags(allocCreateInfo.preferredFlags() | 8);
                        allocCreateInfo.flags(allocCreateInfo.flags() | 0x800);
                    } else {
                        allocCreateInfo.flags(allocCreateInfo.flags() | 0x400);
                    }
                }
                if (VulkanUtils.hasAnyBit(usage, 5)) {
                    allocCreateInfo.usage(9);
                }
                LongBuffer bufferPtr = stack.callocLong(1);
                PointerBuffer allocPtr = stack.callocPointer(1);
                int result = Vma.vmaCreateBuffer((long)device.vma(), (VkBufferCreateInfo)bufferCreateInfo, (VmaAllocationCreateInfo)allocCreateInfo, (LongBuffer)bufferPtr, (PointerBuffer)allocPtr, null);
                VulkanUtils.crashIfFailure(device, result, "Failed to allocate VkBuffer");
                vkBuffer = bufferPtr.get(0);
                this.vmaAllocation = allocPtr.get(0);
                if (label != null) {
                    device.instance().debug().setObjectName(device.vkDevice(), 9, vkBuffer, label);
                }
            }
            super(vkBuffer, usage, size);
            this.closed = false;
            this.mappingRefCount = 0;
        }

        @Override
        public void destroy() {
            Vma.vmaDestroyBuffer((long)this.device.vma(), (long)this.vkBuffer(), (long)this.vmaAllocation);
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.mappingRefCount != 0) {
                throw new IllegalStateException("Attempt to close a mapped buffer");
            }
            this.device.createCommandEncoder().queueForDestroy(this);
        }

        @Override
        public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
            if (this.isClosed()) {
                throw new IllegalStateException("Buffer already closed");
            }
            if (!read && !write) {
                throw new IllegalArgumentException("At least read or write must be true");
            }
            if (read && (this.usage() & 1) == 0) {
                throw new IllegalStateException("Buffer is not readable");
            }
            if (write && (this.usage() & 2) == 0) {
                throw new IllegalStateException("Buffer is not writable");
            }
            if (offset + length > this.size()) {
                throw new IllegalArgumentException("Cannot map more data than this buffer can hold (attempting to map " + length + " bytes at offset " + offset + " from " + this.size() + " size buffer)");
            }
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Mapping buffer slice larger than 2GB is not supported");
            }
            if (offset < 0L || length < 0L) {
                throw new IllegalArgumentException("Offset or length must be positive integer values");
            }
            ++this.mappingRefCount;
            try (MemoryStack stack = MemoryStack.stackPush();){
                PointerBuffer pointer = stack.callocPointer(1);
                VulkanUtils.crashIfFailure(this.device, Vma.vmaMapMemory((long)this.device.vma(), (long)this.vmaAllocation, (PointerBuffer)pointer), "Failed to map buffer");
                ByteBuffer byteBuffer = MemoryUtil.memByteBuffer((long)(pointer.get(0) + offset), (int)((int)length));
                GpuBufferSlice.MappedView mappedView = new GpuBufferSlice.MappedView(this.slice(offset, length), byteBuffer, new Runnable(this){
                    private boolean closed;
                    final /* synthetic */ Direct this$0;
                    {
                        Direct direct = this$0;
                        Objects.requireNonNull(direct);
                        this.this$0 = direct;
                        this.closed = false;
                    }

                    @Override
                    public void run() {
                        if (this.closed) {
                            return;
                        }
                        this.closed = true;
                        --this.this$0.mappingRefCount;
                        Vma.vmaUnmapMemory((long)this.this$0.device.vma(), (long)this.this$0.vmaAllocation);
                    }
                });
                return mappedView;
            }
        }
    }
}

