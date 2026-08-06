/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkImageSubresourceRange
 *  org.lwjgl.vulkan.VkImageViewCreateInfo
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.LongBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

@Environment(value=EnvType.CLIENT)
public class VulkanGpuTextureView
extends GpuTextureView
implements Destroyable {
    private final VulkanDevice device;
    private final long vkImageView;
    private boolean closed;

    protected VulkanGpuTextureView(VulkanDevice device, VulkanGpuTexture texture, int baseMipLevel, int mipLevels) {
        super(texture, baseMipLevel, mipLevels);
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush();){
            boolean isCubemap = (texture.usage() & 0x10) != 0;
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc((MemoryStack)stack).sType$Default();
            imageViewCreateInfo.image(texture.vkImage());
            imageViewCreateInfo.viewType(isCubemap ? 3 : 1);
            imageViewCreateInfo.format(VulkanConst.toVk(texture.getFormat()));
            VkImageSubresourceRange subresourceRange = imageViewCreateInfo.subresourceRange();
            subresourceRange.aspectMask(texture.getFormat().hasColorAspect() ? 1 : 2);
            subresourceRange.baseMipLevel(baseMipLevel);
            subresourceRange.levelCount(mipLevels);
            subresourceRange.baseArrayLayer(0);
            subresourceRange.layerCount(isCubemap ? 6 : 1);
            LongBuffer handlePtr = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateImageView((VkDevice)device.vkDevice(), (VkImageViewCreateInfo)imageViewCreateInfo, null, (LongBuffer)handlePtr), "Failed to create VkImageView");
            this.vkImageView = handlePtr.get(0);
            device.instance().debug().setObjectName(device.vkDevice(), 14, this.vkImageView, texture.getLabel());
        }
        texture.addViews();
    }

    @Override
    public void destroy() {
        VK12.vkDestroyImageView((VkDevice)this.device.vkDevice(), (long)this.vkImageView, null);
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.device.createCommandEncoder().queueForDestroy(this);
            this.texture().removeViews();
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public VulkanGpuTexture texture() {
        return (VulkanGpuTexture)super.texture();
    }

    public long vkImageView() {
        return this.vkImageView;
    }
}

