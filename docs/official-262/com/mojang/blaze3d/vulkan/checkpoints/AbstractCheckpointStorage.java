/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkQueue
 */
package com.mojang.blaze3d.vulkan.checkpoints;

import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkQueue;

@Environment(value=EnvType.CLIENT)
abstract class AbstractCheckpointStorage
implements CheckpointExtension.CheckpointStorage {
    protected final VkQueue queue;
    private final int maxFramesInFlight;
    private int frame;
    private final Frame[] checkpointsByFrame;
    private int nextCheckpointId;

    protected AbstractCheckpointStorage(VulkanQueue queue, int maxFramesInFlight) {
        this.queue = queue.vkQueue();
        this.maxFramesInFlight = maxFramesInFlight;
        this.checkpointsByFrame = new Frame[maxFramesInFlight];
        for (int i = 0; i < maxFramesInFlight; ++i) {
            this.checkpointsByFrame[i] = new Frame(new ArrayList<Checkpoint>());
        }
    }

    @Override
    public void rotate() {
        this.frame = (this.frame + 1) % this.maxFramesInFlight;
        this.checkpointsByFrame[this.frame].checkpoints.clear();
    }

    @Override
    public void recordCheckpoint(VkCommandBuffer commandBuffer, CheckpointExtension.CheckpointType type, Supplier<String> label) {
        int id = this.nextCheckpointId++;
        this.checkpointsByFrame[this.frame].checkpoints.add(new Checkpoint(id, label.get(), type));
        this.recordCheckpoint(commandBuffer, id);
    }

    protected abstract void recordCheckpoint(VkCommandBuffer var1, int var2);

    protected @Nullable Checkpoint findCheckpoint(int id) {
        for (Frame frame : this.checkpointsByFrame) {
            for (Checkpoint checkpoint : frame.checkpoints) {
                if (checkpoint.id() != id) continue;
                return checkpoint;
            }
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    private record Frame(List<Checkpoint> checkpoints) {
    }

    @Environment(value=EnvType.CLIENT)
    protected record Checkpoint(int id, String label, CheckpointExtension.CheckpointType type) {
    }
}

