/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.vulkan.AMDBufferMarker
 *  org.lwjgl.vulkan.VkCommandBuffer
 */
package com.mojang.blaze3d.vulkan.checkpoints;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.checkpoints.AbstractCheckpointStorage;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.vulkan.AMDBufferMarker;
import org.lwjgl.vulkan.VkCommandBuffer;

@Environment(value=EnvType.CLIENT)
public class AmdCheckpointExtension
implements CheckpointExtension {
    private static final long[] STAGES = new long[]{1L, 8192L};
    private final List<AmdCheckpointStorage> storages = new ArrayList<AmdCheckpointStorage>();

    @Override
    public CheckpointExtension.CheckpointStorage createStorage(VulkanDevice device, VulkanQueue queue, int maxFramesInFlight) {
        AmdCheckpointStorage storage = new AmdCheckpointStorage(device, queue, maxFramesInFlight);
        this.storages.add(storage);
        return storage;
    }

    @Override
    public List<CheckpointExtension.QueueCheckpoints> retrieveCheckpoints(boolean isDeviceLost) {
        ArrayList<CheckpointExtension.QueueCheckpoints> result = new ArrayList<CheckpointExtension.QueueCheckpoints>(this.storages.size());
        for (AmdCheckpointStorage storage : this.storages) {
            result.add(storage.retrieveCheckpoints());
        }
        return result;
    }

    @Override
    public void close() {
        for (AmdCheckpointStorage storage : this.storages) {
            storage.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class AmdCheckpointStorage
    extends AbstractCheckpointStorage
    implements AutoCloseable {
        private final VulkanGpuBuffer buffer;
        private final GpuBufferSlice.MappedView mappedView;

        protected AmdCheckpointStorage(VulkanDevice device, VulkanQueue queue, int maxFramesInFlight) {
            super(queue, maxFramesInFlight);
            this.buffer = device.createBuffer(() -> "Internal marker storage", 9, (long)(STAGES.length * 4));
            this.mappedView = this.buffer.map(true, false);
        }

        @Override
        protected void recordCheckpoint(VkCommandBuffer commandBuffer, int id) {
            for (int i = 0; i < STAGES.length; ++i) {
                AMDBufferMarker.vkCmdWriteBufferMarker2AMD((VkCommandBuffer)commandBuffer, (long)STAGES[i], (long)this.buffer.vkBuffer(), (long)(i * 4), (int)id);
            }
        }

        public CheckpointExtension.QueueCheckpoints retrieveCheckpoints() {
            ArrayList<CheckpointExtension.StageCheckpoint> stageCheckpoints = new ArrayList<CheckpointExtension.StageCheckpoint>();
            for (int i = 0; i < STAGES.length; ++i) {
                AbstractCheckpointStorage.Checkpoint checkpoint = this.findCheckpoint(this.mappedView.data().getInt(i * 4));
                if (checkpoint == null) continue;
                stageCheckpoints.add(new CheckpointExtension.StageCheckpoint(STAGES[i], checkpoint.type(), checkpoint.label()));
            }
            return new CheckpointExtension.QueueCheckpoints(this.queue.address(), stageCheckpoints);
        }

        @Override
        public void close() {
            this.mappedView.close();
            this.buffer.close();
        }
    }
}

