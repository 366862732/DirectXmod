/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.util.vma.Vma
 *  org.lwjgl.util.vma.VmaAllocationCreateInfo
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkImageCreateInfo
 *  org.lwjgl.vulkan.VkImageMemoryBarrier
 *  org.lwjgl.vulkan.VkImageMemoryBarrier$Buffer
 *  org.lwjgl.vulkan.VkImageSubresourceRange
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.LongBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceRange;

@Environment(value=EnvType.CLIENT)
public class VulkanGpuTexture
extends GpuTexture
implements Destroyable {
    private final VulkanDevice device;
    private final long vkImage;
    private final long vmaAllocation;
    private boolean closed = false;
    private int views = 0;

    public VulkanGpuTexture(VulkanDevice device, @GpuTexture.Usage int usage, String label, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc((MemoryStack)stack).sType$Default();
            imageCreateInfo.imageType(1);
            imageCreateInfo.extent().set(width, height, 1);
            imageCreateInfo.mipLevels(mipLevels);
            imageCreateInfo.arrayLayers(depthOrLayers);
            imageCreateInfo.format(VulkanConst.toVk(format));
            imageCreateInfo.tiling(0);
            imageCreateInfo.initialLayout(0);
            imageCreateInfo.usage(VulkanConst.textureUsageToVk(usage, format));
            imageCreateInfo.sharingMode(0);
            imageCreateInfo.samples(1);
            imageCreateInfo.flags(VulkanUtils.hasAnyBit(usage, 16) ? 16 : 0);
            VmaAllocationCreateInfo allocationCreateInfo = VmaAllocationCreateInfo.calloc((MemoryStack)stack);
            allocationCreateInfo.usage(8);
            LongBuffer imageHandlePtr = stack.callocLong(1);
            PointerBuffer allocationHandlePtr = stack.callocPointer(1);
            VulkanUtils.crashIfFailure(device, Vma.vmaCreateImage((long)device.vma(), (VkImageCreateInfo)imageCreateInfo, (VmaAllocationCreateInfo)allocationCreateInfo, (LongBuffer)imageHandlePtr, (PointerBuffer)allocationHandlePtr, null), "Failed to create image");
            this.vkImage = imageHandlePtr.get(0);
            this.vmaAllocation = allocationHandlePtr.get(0);
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc((int)1, (MemoryStack)stack).sType$Default();
            barrier.oldLayout(0);
            barrier.newLayout(1);
            barrier.srcAccessMask(0);
            barrier.dstAccessMask(98304);
            barrier.srcQueueFamilyIndex(-1);
            barrier.dstQueueFamilyIndex(-1);
            barrier.image(this.vkImage);
            VkImageSubresourceRange subresourceRange = barrier.subresourceRange();
            subresourceRange.aspectMask(this.getFormat().hasColorAspect() ? 1 : 2);
            subresourceRange.baseMipLevel(0);
            subresourceRange.levelCount(this.getMipLevels());
            subresourceRange.baseArrayLayer(0);
            subresourceRange.layerCount(depthOrLayers);
            VK12.vkCmdPipelineBarrier((VkCommandBuffer)device.createCommandEncoder().textureInitCommandBuffer(), (int)1, (int)65536, (int)0, null, null, (VkImageMemoryBarrier.Buffer)barrier);
            device.instance().debug().setObjectName(device.vkDevice(), 10, this.vkImage, label);
        }
        this.addViews();
    }

    @Override
    public void destroy() {
        Vma.vmaDestroyImage((long)this.device.vma(), (long)this.vkImage, (long)this.vmaAllocation);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.removeViews();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    public void addViews() {
        ++this.views;
    }

    public void removeViews() {
        --this.views;
        if (this.views < 0) {
            throw new IllegalStateException("Too many views removed from texture");
        }
        if (this.closed && this.views == 0) {
            this.device.createCommandEncoder().queueForDestroy(this);
        }
    }

    public long vkImage() {
        return this.vkImage;
    }
}

