/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ReferenceArrayList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.vulkan.KHRSynchronization2
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkCommandBufferSubmitInfo
 *  org.lwjgl.vulkan.VkCommandBufferSubmitInfo$Buffer
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkQueue
 *  org.lwjgl.vulkan.VkSemaphoreSubmitInfo
 *  org.lwjgl.vulkan.VkSemaphoreSubmitInfo$Buffer
 *  org.lwjgl.vulkan.VkSubmitInfo2
 *  org.lwjgl.vulkan.VkSubmitInfo2$Buffer
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.lwjgl.vulkan.VkSubmitInfo2;

@Environment(value=EnvType.CLIENT)
public record VulkanQueue(VkQueue vkQueue, int queueFamilyIndex) {
    public VulkanQueue(VulkanDevice device, int queueFamilyIndex, int queueIndex) {
        this(new VkQueue(VulkanQueue.fetchVkQueue(device, queueFamilyIndex, queueIndex), device.vkDevice()), queueFamilyIndex);
    }

    private static long fetchVkQueue(VulkanDevice device, int queueFamilyIndex, int queueIndex) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            PointerBuffer queueHandlePtr = stack.callocPointer(1);
            VK12.vkGetDeviceQueue((VkDevice)device.vkDevice(), (int)queueFamilyIndex, (int)queueIndex, (PointerBuffer)queueHandlePtr);
            long l = queueHandlePtr.get(0);
            return l;
        }
    }

    public Submission beginSubmit() {
        return new Submission(this);
    }

    public void waitIdle() {
        VK12.vkQueueWaitIdle((VkQueue)this.vkQueue);
    }

    @Environment(value=EnvType.CLIENT)
    public class Submission
    implements AutoCloseable {
        private boolean closed;
        private final ReferenceArrayList<SubmitStage> stages;
        final /* synthetic */ VulkanQueue this$0;

        private Submission(VulkanQueue this$0) {
            VulkanQueue vulkanQueue = this$0;
            Objects.requireNonNull(vulkanQueue);
            this.this$0 = vulkanQueue;
            this.closed = false;
            this.stages = new ReferenceArrayList();
            this.stages.add((Object)new SubmitStage());
        }

        public void waitSemaphore(long semaphore, long value, long stageMask) {
            if (this.closed) {
                throw new IllegalStateException("Attempt to use closed Submission");
            }
            if (!((SubmitStage)this.stages.top()).commandBuffers.isEmpty() || !((SubmitStage)this.stages.top()).signals.isEmpty()) {
                this.stages.add((Object)new SubmitStage());
            }
            ((SubmitStage)this.stages.top()).waits.add(new SemaphoreOp(semaphore, value, stageMask));
        }

        public void executeCommands(VkCommandBuffer commandBuffer) {
            if (this.closed) {
                throw new IllegalStateException("Attempt to use closed Submission");
            }
            if (!((SubmitStage)this.stages.top()).signals.isEmpty()) {
                this.stages.add((Object)new SubmitStage());
            }
            ((SubmitStage)this.stages.top()).commandBuffers.add(commandBuffer);
        }

        public void signalSemaphore(long semaphore, long value, long stageMask) {
            if (this.closed) {
                throw new IllegalStateException("Attempt to use closed Submission");
            }
            ((SubmitStage)this.stages.top()).signals.add(new SemaphoreOp(semaphore, value, stageMask));
        }

        @Override
        public void close() {
            try (MemoryStack stack = MemoryStack.stackPush();){
                VkSubmitInfo2.Buffer submits = VkSubmitInfo2.calloc((int)this.stages.size(), (MemoryStack)stack);
                for (int i = 0; i < this.stages.size(); ++i) {
                    int j;
                    SubmitStage stage = (SubmitStage)this.stages.get(i);
                    ((VkSubmitInfo2.Buffer)submits.position(i)).sType$Default();
                    if (!stage.waits.isEmpty()) {
                        VkSemaphoreSubmitInfo.Buffer waits = VkSemaphoreSubmitInfo.calloc((int)stage.waits.size(), (MemoryStack)stack);
                        submits.pWaitSemaphoreInfos(waits);
                        for (j = 0; j < stage.waits.size(); ++j) {
                            SemaphoreOp wait = stage.waits.get(j);
                            ((VkSemaphoreSubmitInfo.Buffer)waits.position(j)).sType$Default();
                            waits.semaphore(wait.vkSemaphore);
                            waits.value(wait.value);
                            waits.stageMask(wait.stageMask);
                        }
                    }
                    if (!stage.commandBuffers.isEmpty()) {
                        VkCommandBufferSubmitInfo.Buffer buffers = VkCommandBufferSubmitInfo.calloc((int)stage.commandBuffers.size(), (MemoryStack)stack);
                        submits.pCommandBufferInfos(buffers);
                        for (j = 0; j < stage.commandBuffers.size(); ++j) {
                            VkCommandBuffer vkCommandBuffer = stage.commandBuffers.get(j);
                            ((VkCommandBufferSubmitInfo.Buffer)buffers.position(j)).sType$Default();
                            buffers.commandBuffer(vkCommandBuffer);
                        }
                    }
                    if (stage.signals.isEmpty()) continue;
                    VkSemaphoreSubmitInfo.Buffer signals = VkSemaphoreSubmitInfo.calloc((int)stage.signals.size(), (MemoryStack)stack);
                    submits.pSignalSemaphoreInfos(signals);
                    for (j = 0; j < stage.signals.size(); ++j) {
                        SemaphoreOp signal = stage.signals.get(j);
                        ((VkSemaphoreSubmitInfo.Buffer)signals.position(j)).sType$Default();
                        signals.semaphore(signal.vkSemaphore);
                        signals.value(signal.value);
                        signals.stageMask(signal.stageMask);
                    }
                }
                submits.position(0);
                KHRSynchronization2.vkQueueSubmit2KHR((VkQueue)this.this$0.vkQueue, (VkSubmitInfo2.Buffer)submits, (long)0L);
                this.closed = true;
            }
        }

        @Environment(value=EnvType.CLIENT)
        private record SubmitStage(List<SemaphoreOp> waits, List<VkCommandBuffer> commandBuffers, List<SemaphoreOp> signals) {
            public SubmitStage() {
                this((List<SemaphoreOp>)new ReferenceArrayList(), (List<VkCommandBuffer>)new ReferenceArrayList(), (List<SemaphoreOp>)new ReferenceArrayList());
            }
        }

        @Environment(value=EnvType.CLIENT)
        private record SemaphoreOp(long vkSemaphore, long value, long stageMask) {
        }
    }
}

