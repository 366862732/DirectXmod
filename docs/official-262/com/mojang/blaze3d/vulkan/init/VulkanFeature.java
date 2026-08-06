/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.vulkan.VkPhysicalDeviceFeatures2
 */
package com.mojang.blaze3d.vulkan.init;

import com.mojang.blaze3d.vulkan.init.VulkanPNextStruct;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;

@Environment(value=EnvType.CLIENT)
public record VulkanFeature(VulkanPNextStruct struct, String name, long offset) {
    public VulkanFeature(VulkanPNextStruct struct, String name, long offset) {
        this.name = name;
        this.struct = struct;
        this.offset = struct.sType() == 1000059000 ? offset + (long)VkPhysicalDeviceFeatures2.FEATURES : offset;
    }

    public boolean get(VkPhysicalDeviceFeatures2 features2) {
        return this.get(features2.address());
    }

    public boolean get(long pNextChain) {
        long structAddr = this.struct.findStructInPNextChain(pNextChain);
        if (structAddr == 0L) {
            return false;
        }
        return MemoryUtil.memGetInt((long)(structAddr + this.offset)) != 0;
    }

    public boolean set(VkPhysicalDeviceFeatures2 features2, boolean value) {
        return this.set(features2.address(), value);
    }

    private boolean set(long pNextChain, boolean value) {
        long structAddr = this.struct.findStructInPNextChain(pNextChain);
        if (structAddr == 0L) {
            return false;
        }
        MemoryUtil.memPutInt((long)(structAddr + this.offset), (int)(value ? 1 : 0));
        return true;
    }

    public void set(VkPhysicalDeviceFeatures2 features2, boolean value, MemoryStack stack) {
        this.set(features2.address(), value, stack);
    }

    public void set(long pNextChain, boolean value, MemoryStack stack) {
        long structAddr = this.struct.findOrCreateStructInPNextChain(pNextChain, stack);
        MemoryUtil.memPutInt((long)(structAddr + this.offset), (int)(value ? 1 : 0));
    }
}

