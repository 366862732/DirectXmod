/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.vulkan.VkCommandBuffer
 */
package com.mojang.blaze3d.vulkan.checkpoints;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.vulkan.VkCommandBuffer;

@Environment(value=EnvType.CLIENT)
public class NoopCheckpointExtension
implements CheckpointExtension {
    public static final NoopCheckpointExtension INSTANCE = new NoopCheckpointExtension();

    @Override
    public CheckpointExtension.CheckpointStorage createStorage(VulkanDevice device, VulkanQueue queue, int maxFramesInFlight) {
        return NoopCheckpointStorage.INSTANCE;
    }

    @Override
    public List<CheckpointExtension.QueueCheckpoints> retrieveCheckpoints(boolean isDeviceLost) {
        return List.of();
    }

    @Override
    public void close() {
    }

    @Environment(value=EnvType.CLIENT)
    private static class NoopCheckpointStorage
    implements CheckpointExtension.CheckpointStorage {
        private static final NoopCheckpointStorage INSTANCE = new NoopCheckpointStorage();

        private NoopCheckpointStorage() {
        }

        @Override
        public void rotate() {
        }

        @Override
        public void recordCheckpoint(VkCommandBuffer commandBuffer, CheckpointExtension.CheckpointType type, Supplier<String> label) {
        }
    }
}

