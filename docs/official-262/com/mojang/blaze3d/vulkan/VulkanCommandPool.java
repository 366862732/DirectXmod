/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.CustomBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkCommandBufferAllocateInfo
 *  org.lwjgl.vulkan.VkCommandPoolCreateInfo
 *  org.lwjgl.vulkan.VkDevice
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.LongBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

@Environment(value=EnvType.CLIENT)
public class VulkanCommandPool
implements Destroyable {
    private static final int BUFFER_ALLOC_COUNT = 32;
    private static final int HANDLE_BUFFER_BLOCK_SIZE = 512;
    private final VulkanDevice device;
    private final long commandPool;
    private PointerBuffer allocatedBuffers;

    public VulkanCommandPool(VulkanDevice device, VulkanQueue queue) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc((MemoryStack)stack).sType$Default();
            commandPoolCreateInfo.flags(1);
            commandPoolCreateInfo.queueFamilyIndex(queue.queueFamilyIndex());
            LongBuffer commandPoolHandlePtr = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateCommandPool((VkDevice)device.vkDevice(), (VkCommandPoolCreateInfo)commandPoolCreateInfo, null, (LongBuffer)commandPoolHandlePtr), "Failed to create VkCommandPool");
            this.commandPool = commandPoolHandlePtr.get(0);
        }
        this.allocatedBuffers = MemoryUtil.memAllocPointer((int)512);
        this.allocatedBuffers.limit(0);
    }

    @Override
    public void destroy() {
        this.release();
        this.allocatedBuffers.free();
        VK12.vkDestroyCommandPool((VkDevice)this.device.vkDevice(), (long)this.commandPool, null);
    }

    public void release() {
        this.allocatedBuffers.rewind();
        if (this.allocatedBuffers.hasRemaining()) {
            VK12.vkFreeCommandBuffers((VkDevice)this.device.vkDevice(), (long)this.commandPool, (PointerBuffer)this.allocatedBuffers);
            this.allocatedBuffers.clear();
            MemoryUtil.memSet((CustomBuffer)this.allocatedBuffers, (int)0);
        }
        VK12.vkResetCommandPool((VkDevice)this.device.vkDevice(), (long)this.commandPool, (int)1);
        this.allocatedBuffers.limit(0);
    }

    public void reset() {
        VK12.vkResetCommandPool((VkDevice)this.device.vkDevice(), (long)this.commandPool, (int)0);
        this.allocatedBuffers.rewind();
    }

    private void allocateMoreBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush();){
            if (this.allocatedBuffers.capacity() - this.allocatedBuffers.limit() < 32) {
                PointerBuffer newBuffer = MemoryUtil.memRealloc((PointerBuffer)this.allocatedBuffers, (int)(this.allocatedBuffers.capacity() + 512));
                newBuffer.limit(this.allocatedBuffers.limit());
                this.allocatedBuffers = newBuffer;
            }
            VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc((MemoryStack)stack).sType$Default();
            allocateInfo.commandPool(this.commandPool);
            allocateInfo.level(0);
            allocateInfo.commandBufferCount(32);
            this.allocatedBuffers.limit(this.allocatedBuffers.limit() + 32);
            PointerBuffer buffers = (PointerBuffer)this.allocatedBuffers.slice(0, 32);
            VulkanUtils.crashIfFailure(this.device, VK12.vkAllocateCommandBuffers((VkDevice)this.device.vkDevice(), (VkCommandBufferAllocateInfo)allocateInfo, (PointerBuffer)buffers), "Failed to allocate VkCommandBuffers");
        }
    }

    public VkCommandBuffer allocateBuffer() {
        if (!this.allocatedBuffers.hasRemaining()) {
            this.allocateMoreBuffers();
        }
        return new VkCommandBuffer(this.allocatedBuffers.get(), this.device.vkDevice());
    }
}

