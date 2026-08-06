/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.vulkan.NVDeviceDiagnosticCheckpoints
 *  org.lwjgl.vulkan.VkCheckpointData2NV
 *  org.lwjgl.vulkan.VkCheckpointData2NV$Buffer
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkQueue
 */
package com.mojang.blaze3d.vulkan.checkpoints;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.checkpoints.AbstractCheckpointStorage;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.NVDeviceDiagnosticCheckpoints;
import org.lwjgl.vulkan.VkCheckpointData2NV;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkQueue;

@Environment(value=EnvType.CLIENT)
public class NvidiaCheckpointExtension
implements CheckpointExtension {
    private final List<NvidiaCheckpointStorage> storages = new ArrayList<NvidiaCheckpointStorage>();

    @Override
    public CheckpointExtension.CheckpointStorage createStorage(VulkanDevice device, VulkanQueue queue, int maxFramesInFlight) {
        NvidiaCheckpointStorage storage = new NvidiaCheckpointStorage(queue, maxFramesInFlight);
        this.storages.add(storage);
        return storage;
    }

    @Override
    public List<CheckpointExtension.QueueCheckpoints> retrieveCheckpoints(boolean isDeviceLost) {
        if (!isDeviceLost) {
            return List.of();
        }
        ArrayList<CheckpointExtension.QueueCheckpoints> result = new ArrayList<CheckpointExtension.QueueCheckpoints>(this.storages.size());
        for (NvidiaCheckpointStorage storage : this.storages) {
            result.add(storage.retrieveCheckpoints());
        }
        return result;
    }

    @Override
    public void close() {
    }

    @Environment(value=EnvType.CLIENT)
    private static class NvidiaCheckpointStorage
    extends AbstractCheckpointStorage {
        protected NvidiaCheckpointStorage(VulkanQueue queue, int maxFramesInFlight) {
            super(queue, maxFramesInFlight);
        }

        @Override
        protected void recordCheckpoint(VkCommandBuffer commandBuffer, int id) {
            NVDeviceDiagnosticCheckpoints.vkCmdSetCheckpointNV((VkCommandBuffer)commandBuffer, (long)id);
        }

        public CheckpointExtension.QueueCheckpoints retrieveCheckpoints() {
            ArrayList<CheckpointExtension.StageCheckpoint> stageCheckpoints = new ArrayList<CheckpointExtension.StageCheckpoint>();
            try (MemoryStack stack = MemoryStack.stackPush();){
                IntBuffer count = stack.callocInt(1);
                NVDeviceDiagnosticCheckpoints.vkGetQueueCheckpointData2NV((VkQueue)this.queue, (IntBuffer)count, null);
                VkCheckpointData2NV.Buffer data = VkCheckpointData2NV.calloc((int)count.get(0), (MemoryStack)stack);
                for (int i = 0; i < count.get(0); ++i) {
                    ((VkCheckpointData2NV)data.get(i)).sType$Default();
                }
                NVDeviceDiagnosticCheckpoints.vkGetQueueCheckpointData2NV((VkQueue)this.queue, (IntBuffer)count, (VkCheckpointData2NV.Buffer)data);
                while (data.remaining() > 0) {
                    VkCheckpointData2NV checkpointData = (VkCheckpointData2NV)data.get();
                    AbstractCheckpointStorage.Checkpoint checkpoint = this.findCheckpoint((int)checkpointData.pCheckpointMarker());
                    if (checkpoint == null) continue;
                    stageCheckpoints.add(new CheckpointExtension.StageCheckpoint(checkpointData.stage(), checkpoint.type(), checkpoint.label()));
                }
            }
            return new CheckpointExtension.QueueCheckpoints(this.queue.address(), stageCheckpoints);
        }
    }
}

