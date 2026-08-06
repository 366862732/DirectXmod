/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.system.Pointer
 *  org.lwjgl.vulkan.VkDeviceCreateInfo
 *  org.lwjgl.vulkan.VkPhysicalDeviceFeatures2
 *  org.lwjgl.vulkan.VkPhysicalDeviceProperties2
 */
package com.mojang.blaze3d.vulkan.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties2;

@Environment(value=EnvType.CLIENT)
public record VulkanPNextStruct(int sType, int structSize) {
    public long findOrCreateStructInPNextChain(VkPhysicalDeviceProperties2 properties2, MemoryStack stack) {
        return this.findOrCreateStructInPNextChain(properties2.address(), stack);
    }

    public long findOrCreateStructInPNextChain(VkPhysicalDeviceFeatures2 features2, MemoryStack stack) {
        return this.findOrCreateStructInPNextChain(features2.address(), stack);
    }

    public long findOrCreateStructInPNextChain(long pNextChain, MemoryStack stack) {
        long foundStruct = VulkanPNextStruct.findStructInPNextChain(pNextChain, this.sType);
        if (foundStruct != 0L) {
            return foundStruct;
        }
        long newStruct = stack.ncalloc(Pointer.POINTER_SIZE, 1, this.structSize);
        VkPhysicalDeviceProperties2.nsType((long)newStruct, (int)this.sType);
        VkPhysicalDeviceProperties2.npNext((long)newStruct, (long)VkPhysicalDeviceProperties2.npNext((long)pNextChain));
        VkPhysicalDeviceProperties2.npNext((long)pNextChain, (long)newStruct);
        return newStruct;
    }

    public long findStructInPNextChain(long pNextChain) {
        return VulkanPNextStruct.findStructInPNextChain(pNextChain, this.sType);
    }

    private static long findStructInPNextChain(long pNextChain, int sType) {
        while (pNextChain != 0L) {
            if (VkPhysicalDeviceProperties2.nsType((long)pNextChain) == sType) {
                return pNextChain;
            }
            pNextChain = MemoryUtil.memGetAddress((long)(pNextChain + (long)VkDeviceCreateInfo.PNEXT));
        }
        return 0L;
    }
}

