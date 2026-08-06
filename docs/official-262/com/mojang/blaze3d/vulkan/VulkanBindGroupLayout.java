/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.Struct
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
 *  org.lwjgl.vulkan.VkDescriptorSetLayoutBinding$Buffer
 *  org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
 *  org.lwjgl.vulkan.VkDevice
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.LongBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

@Environment(value=EnvType.CLIENT)
public record VulkanBindGroupLayout(long handle, List<Entry> entries) {
    public static final VulkanBindGroupLayout INVALID_LAYOUT = new VulkanBindGroupLayout(0L, List.of());

    public static VulkanBindGroupLayout create(VulkanDevice device, List<Entry> entries, String name) {
        long layoutHandle;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc((int)entries.size(), (MemoryStack)stack);
            for (int i = 0; i < entries.size(); ++i) {
                VkDescriptorSetLayoutBinding vkDescriptorSetLayoutBinding = VkDescriptorSetLayoutBinding.calloc((MemoryStack)stack);
                VkDescriptorSetLayoutBinding binding = vkDescriptorSetLayoutBinding.descriptorType(switch (entries.get(i).type().ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 0 -> 6;
                    case 1 -> 1;
                    case 2 -> 4;
                }).descriptorCount(1).binding(i).stageFlags(17);
                bindings.put((Struct)binding);
            }
            bindings.flip();
            VkDescriptorSetLayoutCreateInfo setCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc((MemoryStack)stack).sType$Default().flags(1).pBindings(bindings);
            LongBuffer pointer = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateDescriptorSetLayout((VkDevice)device.vkDevice(), (VkDescriptorSetLayoutCreateInfo)setCreateInfo, null, (LongBuffer)pointer), "Can't set layout for " + name);
            layoutHandle = pointer.get(0);
        }
        return new VulkanBindGroupLayout(layoutHandle, entries);
    }

    @Environment(value=EnvType.CLIENT)
    public record Entry(VulkanBindGroupEntryType type, String name, @Nullable GpuFormat texelBufferFormat) {
    }

    @Environment(value=EnvType.CLIENT)
    public static enum VulkanBindGroupEntryType {
        UNIFORM_BUFFER,
        SAMPLED_IMAGE,
        TEXEL_BUFFER;

    }
}

