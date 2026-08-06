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
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.vulkan.VkCommandBuffer;

@Environment(value=EnvType.CLIENT)
public interface CheckpointExtension
extends AutoCloseable {
    public CheckpointStorage createStorage(VulkanDevice var1, VulkanQueue var2, int var3);

    public List<QueueCheckpoints> retrieveCheckpoints(boolean var1);

    @Override
    public void close();

    @Environment(value=EnvType.CLIENT)
    public record StageCheckpoint(long stage, CheckpointType type, String label) {
    }

    @Environment(value=EnvType.CLIENT)
    public record QueueCheckpoints(long queue, List<StageCheckpoint> checkpoints) {
    }

    @Environment(value=EnvType.CLIENT)
    public static enum CheckpointType {
        BEGIN_RENDER_PASS,
        END_RENDER_PASS;

    }

    @Environment(value=EnvType.CLIENT)
    public static interface CheckpointStorage {
        public void rotate();

        public void recordCheckpoint(VkCommandBuffer var1, CheckpointType var2, Supplier<String> var3);
    }
}

