/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkSamplerCreateInfo
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.LongBuffer;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

@Environment(value=EnvType.CLIENT)
public class VulkanGpuSampler
extends GpuSampler
implements Destroyable {
    private final long vkSampler;
    private final VulkanDevice device;
    private final AddressMode addressModeU;
    private final AddressMode addressModeV;
    private final FilterMode minFilter;
    private final FilterMode magFilter;
    private final int maxAnisotropy;
    private final OptionalDouble maxLod;
    private boolean closed;

    public VulkanGpuSampler(VulkanDevice device, AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        this.device = device;
        this.addressModeU = addressModeU;
        this.addressModeV = addressModeV;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.maxAnisotropy = maxAnisotropy;
        this.maxLod = maxLod;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkSamplerCreateInfo createInfo = VkSamplerCreateInfo.calloc((MemoryStack)stack).sType$Default();
            createInfo.magFilter(VulkanConst.toVk(magFilter));
            createInfo.minFilter(VulkanConst.toVk(minFilter));
            createInfo.mipmapMode(maxLod.orElse(1000.0) > 0.25 ? 1 : 0);
            createInfo.addressModeU(VulkanConst.toVk(addressModeU));
            createInfo.addressModeV(VulkanConst.toVk(addressModeV));
            createInfo.mipLodBias(0.0f);
            createInfo.maxLod(Math.max(0.25f, (float)maxLod.orElse(1000.0)));
            createInfo.anisotropyEnable(maxAnisotropy > 1);
            createInfo.maxAnisotropy((float)maxAnisotropy);
            LongBuffer pointer = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateSampler((VkDevice)device.vkDevice(), (VkSamplerCreateInfo)createInfo, null, (LongBuffer)pointer), "Can't create sampler");
            this.vkSampler = pointer.get(0);
        }
    }

    @Override
    public void destroy() {
        VK12.vkDestroySampler((VkDevice)this.device.vkDevice(), (long)this.vkSampler, null);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.device.createCommandEncoder().queueForDestroy(this);
    }

    @Override
    public AddressMode getAddressModeU() {
        return this.addressModeU;
    }

    @Override
    public AddressMode getAddressModeV() {
        return this.addressModeV;
    }

    @Override
    public FilterMode getMinFilter() {
        return this.minFilter;
    }

    @Override
    public FilterMode getMagFilter() {
        return this.magFilter;
    }

    @Override
    public int getMaxAnisotropy() {
        return this.maxAnisotropy;
    }

    @Override
    public OptionalDouble getMaxLod() {
        return this.maxLod;
    }

    public long vkSampler() {
        return this.vkSampler;
    }
}

