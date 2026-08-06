/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkQueryPoolCreateInfo
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.LongBuffer;
import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;

@Environment(value=EnvType.CLIENT)
public class VulkanQueryPool
implements GpuQueryPool,
Destroyable {
    private final VulkanDevice device;
    private final int size;
    private final long vkQueryPool;

    public VulkanQueryPool(VulkanDevice device, int size) {
        this.device = device;
        this.size = size;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkQueryPoolCreateInfo createInfo = VkQueryPoolCreateInfo.calloc((MemoryStack)stack).sType$Default();
            createInfo.queryType(2);
            createInfo.queryCount(size);
            LongBuffer pointer = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateQueryPool((VkDevice)device.vkDevice(), (VkQueryPoolCreateInfo)createInfo, null, (LongBuffer)pointer), "Cannot create query pool");
            this.vkQueryPool = pointer.get(0);
            VK12.vkResetQueryPool((VkDevice)device.vkDevice(), (long)this.vkQueryPool, (int)0, (int)size);
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public OptionalLong getValue(int index) {
        return this.getValues(index, 1)[0];
    }

    @Override
    public OptionalLong[] getValues(int index, int count) {
        if (index + count > this.size) {
            throw new IndexOutOfBoundsException("getValues would read out-of-bounds for an array of " + count + " starting at " + index + ", when total size is " + this.size);
        }
        OptionalLong[] result = new OptionalLong[count];
        try (MemoryStack stack = MemoryStack.stackPush();){
            LongBuffer values = stack.callocLong(2 * count);
            VulkanUtils.crashIfFailure(this.device, VK12.vkGetQueryPoolResults((VkDevice)this.device.vkDevice(), (long)this.vkQueryPool, (int)index, (int)count, (LongBuffer)values, (long)16L, (int)5), "Cannot fetch query results");
            for (int i = 0; i < count; ++i) {
                result[i] = values.get(i * 2 + 1) != 0L ? OptionalLong.of(values.get(i * 2)) : OptionalLong.empty();
            }
        }
        return result;
    }

    protected long vkQueryPool() {
        return this.vkQueryPool;
    }

    @Override
    public void close() {
        this.device.createCommandEncoder().queueForDestroy(this);
    }

    @Override
    public void destroy() {
        VK12.vkDestroyQueryPool((VkDevice)this.device.vkDevice(), (long)this.vkQueryPool, null);
    }
}

