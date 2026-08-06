/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.vulkan.KHRDynamicRendering
 *  org.lwjgl.vulkan.KHRSynchronization2
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkBufferCopy
 *  org.lwjgl.vulkan.VkBufferCopy$Buffer
 *  org.lwjgl.vulkan.VkBufferImageCopy
 *  org.lwjgl.vulkan.VkBufferImageCopy$Buffer
 *  org.lwjgl.vulkan.VkClearAttachment
 *  org.lwjgl.vulkan.VkClearAttachment$Buffer
 *  org.lwjgl.vulkan.VkClearColorValue
 *  org.lwjgl.vulkan.VkClearDepthStencilValue
 *  org.lwjgl.vulkan.VkClearRect
 *  org.lwjgl.vulkan.VkClearRect$Buffer
 *  org.lwjgl.vulkan.VkClearValue
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkCommandBufferBeginInfo
 *  org.lwjgl.vulkan.VkDependencyInfo
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkImageCopy
 *  org.lwjgl.vulkan.VkImageCopy$Buffer
 *  org.lwjgl.vulkan.VkImageSubresourceLayers
 *  org.lwjgl.vulkan.VkImageSubresourceRange
 *  org.lwjgl.vulkan.VkMemoryBarrier2
 *  org.lwjgl.vulkan.VkMemoryBarrier2$Buffer
 *  org.lwjgl.vulkan.VkRect2D
 *  org.lwjgl.vulkan.VkRenderingAttachmentInfo
 *  org.lwjgl.vulkan.VkRenderingAttachmentInfo$Buffer
 *  org.lwjgl.vulkan.VkRenderingInfo
 *  org.lwjgl.vulkan.VkSemaphoreCreateInfo
 *  org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo
 *  org.lwjgl.vulkan.VkSemaphoreWaitInfo
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.DestructionQueue;
import com.mojang.blaze3d.vulkan.VulkanCommandPool;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanQueryPool;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import com.mojang.blaze3d.vulkan.VulkanTransientMemory;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearDepthStencilValue;
import org.lwjgl.vulkan.VkClearRect;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkMemoryBarrier2;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;

@Environment(value=EnvType.CLIENT)
public class VulkanCommandEncoder
implements CommandEncoderBackend,
Destroyable {
    public static final int MAX_SUBMITS_IN_FLIGHT = 2;
    private final VulkanDevice device;
    private final VulkanTransientMemory transientMemory;
    private final long submitSemaphore;
    private long currentSubmitIndex = 2L;
    private long completedSubmitIndex = 0L;
    private final CheckpointExtension.CheckpointStorage checkpointStorage;
    private VulkanQueue.Submission submissionBuilder;
    private final DestructionQueue<Destroyable> destroyQueue = new DestructionQueue<Destroyable>(2, Destroyable::destroy);
    private final VulkanCommandPool[] commandPools = new VulkanCommandPool[2];
    private @Nullable VkCommandBuffer currentCommandBuffer;
    private @Nullable VulkanRenderPass currentRenderPass;

    public VulkanCommandEncoder(VulkanDevice device) {
        this.device = device;
        this.transientMemory = new VulkanTransientMemory(device, this);
        MemoryStack baseStack = MemoryStack.stackGet();
        try (MemoryStack stack = baseStack.push();){
            VkSemaphoreTypeCreateInfo semaphoreTypeCreateInfo = VkSemaphoreTypeCreateInfo.calloc((MemoryStack)stack).sType$Default();
            semaphoreTypeCreateInfo.semaphoreType(1);
            semaphoreTypeCreateInfo.initialValue(this.currentSubmitIndex - 1L);
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc((MemoryStack)stack).sType$Default();
            semaphoreCreateInfo.pNext(semaphoreTypeCreateInfo);
            LongBuffer semaphoreHandlePtr = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateSemaphore((VkDevice)device.vkDevice(), (VkSemaphoreCreateInfo)semaphoreCreateInfo, null, (LongBuffer)semaphoreHandlePtr), "Failed to create submit VkSemaphore");
            this.submitSemaphore = semaphoreHandlePtr.get(0);
        }
        for (int i = 0; i < 2; ++i) {
            this.commandPools[i] = new VulkanCommandPool(device, device.graphicsQueue());
        }
        this.checkpointStorage = device.checkpointExtension().createStorage(device, device.graphicsQueue(), 2);
        this.submissionBuilder = device.graphicsQueue().beginSubmit();
        this.transientMemory.beginSubmit();
    }

    @Override
    public void destroy() {
        this.transientMemory.endSubmit();
        this.submissionBuilder.close();
        this.device.graphicsQueue().waitIdle();
        this.destroyQueue.close();
        this.transientMemory.destroy();
        this.destroyQueue.close();
        for (int i = 0; i < 2; ++i) {
            this.commandPools[i].destroy();
        }
        VK12.vkDestroySemaphore((VkDevice)this.device.vkDevice(), (long)this.submitSemaphore, null);
    }

    public void queueForDestroy(Destroyable destroyable) {
        this.destroyQueue.add(destroyable);
    }

    private VulkanCommandPool currentCommandPool() {
        return this.commandPools[(int)(this.currentSubmitIndex % 2L)];
    }

    public VkCommandBuffer allocateAndBeginTransientCommandBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkCommandBuffer commandBuffer = this.currentCommandPool().allocateBuffer();
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc((MemoryStack)stack).sType$Default();
            beginInfo.flags(1);
            VulkanUtils.crashIfFailure(this.device, VK12.vkBeginCommandBuffer((VkCommandBuffer)commandBuffer, (VkCommandBufferBeginInfo)beginInfo), "Failed to begin VkCommandBuffer");
            VkCommandBuffer vkCommandBuffer = commandBuffer;
            return vkCommandBuffer;
        }
    }

    private VkCommandBuffer commandBuffer() {
        if (this.currentCommandBuffer != null) {
            return this.currentCommandBuffer;
        }
        if (this.currentRenderPass != null) {
            throw new IllegalStateException("Cannot start command buffer while inside RenderPass");
        }
        this.currentCommandBuffer = this.allocateAndBeginTransientCommandBuffer();
        this.submissionBuilder.executeCommands(this.currentCommandBuffer);
        return this.currentCommandBuffer;
    }

    VkCommandBuffer textureInitCommandBuffer() {
        return this.commandBuffer();
    }

    private void endCommandBuffer() {
        if (this.currentCommandBuffer == null) {
            return;
        }
        if (this.currentRenderPass != null) {
            throw new IllegalStateException("Cannot end command buffer while inside RenderPass");
        }
        VulkanUtils.crashIfFailure(this.device, VK12.vkEndCommandBuffer((VkCommandBuffer)this.currentCommandBuffer), "Failed to end VkCommandBuffer");
        this.currentCommandBuffer = null;
    }

    public void waitSemaphore(long vkSemaphore, long value, long stageMask) {
        if (this.currentRenderPass != null) {
            throw new IllegalStateException("Cannot add semaphore operation while inside RenderPass");
        }
        this.endCommandBuffer();
        this.submissionBuilder.waitSemaphore(vkSemaphore, value, stageMask);
    }

    public void execute(VkCommandBuffer commandBuffer) {
        if (this.currentRenderPass != null) {
            throw new IllegalStateException("Cannot execute command buffer while inside RenderPass");
        }
        this.endCommandBuffer();
        this.submissionBuilder.executeCommands(commandBuffer);
    }

    public void signalSemaphore(long vkSemaphore, long value, long stageMask) {
        if (this.currentRenderPass != null) {
            throw new IllegalStateException("Cannot add semaphore operation while inside RenderPass");
        }
        this.endCommandBuffer();
        this.submissionBuilder.signalSemaphore(vkSemaphore, value, stageMask);
    }

    private void memoryBarrier(MemoryStack stack) {
        VulkanCommandEncoder.memoryBarrier(this.commandBuffer(), stack);
    }

    public static void memoryBarrier(VkCommandBuffer commandBuffer, MemoryStack stack) {
        VkMemoryBarrier2.Buffer memoryBarrier = VkMemoryBarrier2.calloc((int)1, (MemoryStack)stack).sType$Default();
        memoryBarrier.srcStageMask(65536L);
        memoryBarrier.srcAccessMask(98304L);
        memoryBarrier.dstStageMask(65536L);
        memoryBarrier.dstAccessMask(98304L);
        VkDependencyInfo depInfo = VkDependencyInfo.calloc((MemoryStack)stack).sType$Default();
        depInfo.pMemoryBarriers(memoryBarrier);
        KHRSynchronization2.vkCmdPipelineBarrier2KHR((VkCommandBuffer)commandBuffer, (VkDependencyInfo)depInfo);
    }

    @Override
    public void submit() {
        this.endCommandBuffer();
        this.transientMemory.endSubmit();
        this.signalSemaphore(this.submitSemaphore, this.currentSubmitIndex, 65536L);
        this.submissionBuilder.close();
        this.submissionBuilder = this.device.graphicsQueue().beginSubmit();
        ++this.currentSubmitIndex;
        if (!this.awaitSubmitCompletion(this.currentSubmitIndex - 2L, 5000000000L)) {
            List<CheckpointExtension.QueueCheckpoints> checkpoints = this.device.checkpointExtension().retrieveCheckpoints(false);
            throw new IllegalStateException("5s timeout reached when waiting for VK semaphore: " + VulkanUtils.formatCheckpoints(checkpoints));
        }
        this.currentCommandPool().reset();
        this.destroyQueue.rotate();
        this.checkpointStorage.rotate();
        this.transientMemory.beginSubmit();
    }

    @Override
    public TransientMemory transientMemory() {
        return this.transientMemory;
    }

    @Override
    public RenderPassBackend createRenderPass(RenderPassDescriptor descriptor) {
        List<@Nullable RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments = descriptor.colorAttachments();
        @Nullable VulkanGpuTextureView[] colorTextures = new VulkanGpuTextureView[colorAttachments.size()];
        for (int i = 0; i < colorAttachments.size(); ++i) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = colorAttachments.get(i);
            colorTextures[i] = attachment != null ? (VulkanGpuTextureView)attachment.textureView() : null;
        }
        RenderPassDescriptor.Attachment<OptionalDouble> depthAttachment = descriptor.depthAttachment();
        this.device.instance().debug().beginDebugGroup(this.commandBuffer(), descriptor.label());
        this.checkpointStorage.recordCheckpoint(this.commandBuffer(), CheckpointExtension.CheckpointType.BEGIN_RENDER_PASS, descriptor.label());
        try (MemoryStack stack = MemoryStack.stackPush();){
            Object clearValue;
            int width = 0;
            int height = 0;
            if (!colorAttachments.isEmpty()) {
                for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> colorAttachment : colorAttachments) {
                    if (colorAttachment == null) continue;
                    GpuTextureView colorTexture = colorAttachment.textureView();
                    width = colorTexture.getWidth(0);
                    height = colorTexture.getHeight(0);
                }
            } else if (depthAttachment != null) {
                width = depthAttachment.textureView().getWidth(0);
                height = depthAttachment.textureView().getHeight(0);
            }
            VkRect2D vkRenderArea = VkRect2D.calloc((MemoryStack)stack);
            assert (descriptor.renderArea != null);
            vkRenderArea.extent().set(descriptor.renderArea.width(), descriptor.renderArea.height());
            vkRenderArea.offset().set(descriptor.renderArea.x(), descriptor.renderArea.y());
            VkRenderingAttachmentInfo.Buffer colorAttachmentInfo = VkRenderingAttachmentInfo.calloc((int)colorAttachments.size(), (MemoryStack)stack);
            for (int i = 0; i < colorAttachments.size(); ++i) {
                ((VkRenderingAttachmentInfo.Buffer)colorAttachmentInfo.position(i)).sType$Default();
                VulkanGpuTextureView colorTexture = colorTextures[i];
                if (colorTexture != null) {
                    colorAttachmentInfo.imageView(colorTexture.vkImageView());
                    colorAttachmentInfo.imageLayout(1);
                    colorAttachmentInfo.storeOp(0);
                    RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = colorAttachments.get(i);
                    clearValue = attachment.clearValue();
                    if (((Optional)clearValue).isPresent()) {
                        Vector4fc color = ((Optional)clearValue).get();
                        VkClearColorValue vkClearColor = VulkanUtils.putArgb(VkClearColorValue.calloc((MemoryStack)stack), color);
                        colorAttachmentInfo.loadOp(1);
                        colorAttachmentInfo.clearValue(VkClearValue.calloc((MemoryStack)stack).color(vkClearColor));
                        continue;
                    }
                    colorAttachmentInfo.loadOp(0);
                    continue;
                }
                colorAttachmentInfo.imageView(0L);
                colorAttachmentInfo.imageLayout(0);
                colorAttachmentInfo.storeOp(1);
                colorAttachmentInfo.loadOp(2);
            }
            colorAttachmentInfo.position(0);
            VkRenderingInfo renderingInfo = VkRenderingInfo.calloc((MemoryStack)stack).sType$Default();
            renderingInfo.renderArea(vkRenderArea);
            renderingInfo.layerCount(1);
            renderingInfo.viewMask(0);
            renderingInfo.pColorAttachments(colorAttachmentInfo);
            if (depthAttachment != null) {
                VkRenderingAttachmentInfo depthAttachmentInfo = VkRenderingAttachmentInfo.calloc((MemoryStack)stack).sType$Default();
                VulkanGpuTextureView vulkanDepthAttachment = (VulkanGpuTextureView)depthAttachment.textureView();
                depthAttachmentInfo.imageView(vulkanDepthAttachment.vkImageView());
                depthAttachmentInfo.imageLayout(1);
                depthAttachmentInfo.storeOp(0);
                clearValue = depthAttachment.clearValue();
                if (((OptionalDouble)clearValue).isPresent()) {
                    double color = ((OptionalDouble)clearValue).getAsDouble();
                    VkClearDepthStencilValue vkClearColor = VkClearDepthStencilValue.calloc((MemoryStack)stack).depth((float)color);
                    depthAttachmentInfo.loadOp(1);
                    depthAttachmentInfo.clearValue(VkClearValue.calloc((MemoryStack)stack).depthStencil(vkClearColor));
                } else {
                    depthAttachmentInfo.loadOp(0);
                }
                renderingInfo.pDepthAttachment(depthAttachmentInfo);
            }
            KHRDynamicRendering.vkCmdBeginRenderingKHR((VkCommandBuffer)this.commandBuffer(), (VkRenderingInfo)renderingInfo);
            this.currentRenderPass = new VulkanRenderPass(this.device, this, this.commandBuffer(), this.checkpointStorage, descriptor.renderArea, width, height, depthAttachment != null, descriptor.label());
        }
        return this.currentRenderPass;
    }

    @Override
    public void submitRenderPass() {
        if (this.currentRenderPass == null) {
            throw new IllegalStateException("Cannot submit a renderpass if one hasn't been started!");
        }
        KHRDynamicRendering.vkCmdEndRenderingKHR((VkCommandBuffer)this.commandBuffer());
        this.device.instance().debug().endDebugGroup(this.commandBuffer());
        this.checkpointStorage.recordCheckpoint(this.commandBuffer(), CheckpointExtension.CheckpointType.END_RENDER_PASS, this.currentRenderPass.getLabel());
        this.currentRenderPass = null;
        try (MemoryStack stack = MemoryStack.stackPush();){
            this.memoryBarrier(stack);
        }
    }

    private void clearColorTextureUnsynced(MemoryStack stack, GpuTexture colorTexture, Vector4fc clearColor) {
        VkClearColorValue vkClearColor = VulkanUtils.putArgb(VkClearColorValue.calloc((MemoryStack)stack), clearColor);
        VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc((MemoryStack)stack);
        subresourceRange.baseMipLevel(0);
        subresourceRange.levelCount(colorTexture.getMipLevels());
        subresourceRange.baseArrayLayer(0);
        subresourceRange.layerCount(1);
        subresourceRange.aspectMask(1);
        VK12.vkCmdClearColorImage((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuTexture)colorTexture).vkImage(), (int)1, (VkClearColorValue)vkClearColor, (VkImageSubresourceRange)subresourceRange);
    }

    public void clearDepthTextureUnsynced(MemoryStack stack, GpuTexture depthTexture, double clearDepth) {
        VkClearDepthStencilValue vkClearDepth = VkClearDepthStencilValue.calloc((MemoryStack)stack).depth((float)clearDepth);
        VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc((MemoryStack)stack);
        subresourceRange.baseMipLevel(0);
        subresourceRange.levelCount(depthTexture.getMipLevels());
        subresourceRange.baseArrayLayer(0);
        subresourceRange.layerCount(1);
        subresourceRange.aspectMask(2);
        VK12.vkCmdClearDepthStencilImage((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuTexture)depthTexture).vkImage(), (int)1, (VkClearDepthStencilValue)vkClearDepth, (VkImageSubresourceRange)subresourceRange);
    }

    @Override
    public void clearColorTexture(GpuTexture colorTexture, Vector4fc clearColor) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            this.clearColorTextureUnsynced(stack, colorTexture, clearColor);
            this.memoryBarrier(stack);
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            this.clearColorTextureUnsynced(stack, colorTexture, clearColor);
            this.clearDepthTextureUnsynced(stack, depthTexture, clearDepth);
            this.memoryBarrier(stack);
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth, int regionX, int regionY, int regionWidth, int regionHeight) {
        try (GpuTextureView colorTextureView = this.device.createTextureView(colorTexture);
             GpuTextureView depthTextureView = this.device.createTextureView(depthTexture);
             MemoryStack stack = MemoryStack.stackPush();){
            this.createRenderPass(RenderPassDescriptor.create(() -> "ClearColorDepthTextures").withColorAttachment(colorTextureView).withDepthAttachment(depthTextureView).withRenderArea(new RenderPass.RenderArea(0, 0, colorTexture.getWidth(0), colorTexture.getHeight(0))));
            assert (this.currentRenderPass != null);
            VkClearRect.Buffer rects = VkClearRect.calloc((int)1, (MemoryStack)stack);
            rects.baseArrayLayer(0);
            rects.layerCount(1);
            rects.rect().offset().set(regionX, regionY);
            rects.rect().extent().set(regionWidth, regionHeight);
            VkClearAttachment.Buffer attachments = VkClearAttachment.calloc((int)2, (MemoryStack)stack);
            VkClearValue colorClearValue = VkClearValue.calloc((MemoryStack)stack);
            VulkanUtils.putArgb(colorClearValue.color(), clearColor);
            attachments.aspectMask(1);
            attachments.clearValue(colorClearValue);
            VkClearValue depthClearValue = VkClearValue.calloc((MemoryStack)stack);
            VkClearDepthStencilValue clearValue = depthClearValue.depthStencil();
            clearValue.depth((float)clearDepth);
            attachments.position(1);
            attachments.aspectMask(2);
            attachments.clearValue(depthClearValue);
            attachments.position(0);
            VK12.vkCmdClearAttachments((VkCommandBuffer)this.commandBuffer(), (VkClearAttachment.Buffer)attachments, (VkClearRect.Buffer)rects);
            this.submitRenderPass();
        }
    }

    @Override
    public void clearDepthTexture(GpuTexture depthTexture, double clearDepth) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            this.clearDepthTextureUnsynced(stack, depthTexture, clearDepth);
            this.memoryBarrier(stack);
        }
    }

    @Override
    public void writeToBuffer(GpuBufferSlice destination, ByteBuffer data) {
        VulkanGpuBuffer destBuffer = (VulkanGpuBuffer)destination.buffer();
        GpuBufferSlice stagingBuffer = this.transientMemory.uploadStaging(data, 1L, 16);
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkBufferCopy.Buffer regions = VkBufferCopy.calloc((int)1, (MemoryStack)stack).srcOffset(stagingBuffer.offset()).dstOffset(destination.offset()).size((long)data.remaining());
            VK12.vkCmdCopyBuffer((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuBuffer)stagingBuffer.buffer()).vkBuffer(), (long)destBuffer.vkBuffer(), (VkBufferCopy.Buffer)regions);
            this.memoryBarrier(stack);
        }
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkBufferCopy.Buffer copyInfo = VkBufferCopy.calloc((int)1, (MemoryStack)stack);
            copyInfo.srcOffset(source.offset());
            copyInfo.dstOffset(target.offset());
            copyInfo.size(source.length());
            VK12.vkCmdCopyBuffer((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuBuffer)source.buffer()).vkBuffer(), (long)((VulkanGpuBuffer)target.buffer()).vkBuffer(), (VkBufferCopy.Buffer)copyInfo);
            this.memoryBarrier(stack);
        }
    }

    @Override
    public void writeToTexture(GpuTexture destination, ByteBuffer source, int mipLevel, int depthOrLayer, int destX, int destY, int width, int height) {
        GpuBufferSlice stagingBuffer = this.transientMemory.uploadStaging(source, 1L, 16);
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc((int)1, (MemoryStack)stack);
            region.bufferOffset(stagingBuffer.offset());
            region.bufferRowLength(width);
            region.bufferImageHeight(height);
            VkImageSubresourceLayers imageSubresource = region.imageSubresource();
            imageSubresource.aspectMask(1);
            imageSubresource.mipLevel(mipLevel);
            imageSubresource.baseArrayLayer(depthOrLayer);
            imageSubresource.layerCount(1);
            region.imageOffset().set(destX, destY, 0);
            region.imageExtent().set(width, height, 1);
            VK12.vkCmdCopyBufferToImage((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuBuffer)stagingBuffer.buffer()).vkBuffer(), (long)((VulkanGpuTexture)destination).vkImage(), (int)1, (VkBufferImageCopy.Buffer)region);
            this.memoryBarrier(stack);
        }
    }

    @Override
    public void copyBufferToTexture(GpuBufferSlice source, int sourceX, int sourceY, int sourceWidth, int sourceHeight, GpuTexture destination, int destinationX, int destinationY, int copyWidth, int copyHeight, int mipLevel, int arrayLayer) {
        int texelSize = destination.getFormat().blockSize();
        long skipTexels = (long)sourceX + (long)sourceY * (long)sourceWidth;
        long skipBytes = skipTexels * (long)texelSize;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc((int)1, (MemoryStack)stack);
            region.bufferOffset(source.offset() + skipBytes);
            region.bufferRowLength(sourceWidth);
            region.bufferImageHeight(sourceHeight);
            VkImageSubresourceLayers imageSubresource = region.imageSubresource();
            imageSubresource.aspectMask(1);
            imageSubresource.mipLevel(mipLevel);
            imageSubresource.baseArrayLayer(arrayLayer);
            imageSubresource.layerCount(1);
            region.imageOffset().set(destinationX, destinationY, 0);
            region.imageExtent().set(copyWidth, copyHeight, 1);
            VK12.vkCmdCopyBufferToImage((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuBuffer)source.buffer()).vkBuffer(), (long)((VulkanGpuTexture)destination).vkImage(), (int)1, (VkBufferImageCopy.Buffer)region);
            this.memoryBarrier(stack);
        }
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel) {
        this.copyTextureToBuffer(source, destination, offset, callback, mipLevel, 0, 0, source.getWidth(mipLevel), source.getHeight(mipLevel));
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel, int x, int y, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkBufferImageCopy.Buffer copy = VkBufferImageCopy.calloc((int)1, (MemoryStack)stack);
            copy.bufferOffset(offset);
            VkImageSubresourceLayers subresource = copy.imageSubresource();
            subresource.aspectMask(VulkanConst.formatAspectMask(source.getFormat()));
            subresource.mipLevel(mipLevel);
            subresource.baseArrayLayer(0);
            subresource.layerCount(1);
            copy.imageOffset().set(x, y, 0);
            copy.imageExtent().set(width, height, 1);
            copy.bufferRowLength(width);
            copy.bufferImageHeight(height);
            VK12.vkCmdCopyImageToBuffer((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuTexture)source).vkImage(), (int)1, (long)((VulkanGpuBuffer)destination).vkBuffer(), (VkBufferImageCopy.Buffer)copy);
            this.memoryBarrier(stack);
        }
        this.queueForDestroy(callback::run);
    }

    @Override
    public void copyTextureToTexture(GpuTexture source, GpuTexture destination, int mipLevel, int destX, int destY, int sourceX, int sourceY, int width, int height) {
        VulkanGpuTexture vulkanSrc = (VulkanGpuTexture)source;
        VulkanGpuTexture vulkanDst = (VulkanGpuTexture)destination;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkImageSubresourceLayers subresourceLayers = VkImageSubresourceLayers.calloc((MemoryStack)stack);
            subresourceLayers.mipLevel(mipLevel);
            subresourceLayers.baseArrayLayer(0);
            subresourceLayers.layerCount(1);
            subresourceLayers.aspectMask(VulkanConst.formatAspectMask(source.getFormat()));
            VkImageCopy.Buffer regions = VkImageCopy.calloc((int)1, (MemoryStack)stack);
            regions.srcOffset().set(sourceX, sourceY, 0);
            regions.dstOffset().set(destX, destY, 0);
            regions.extent().set(width, height, 1);
            regions.srcSubresource(subresourceLayers);
            regions.dstSubresource(subresourceLayers);
            VK12.vkCmdCopyImage((VkCommandBuffer)this.commandBuffer(), (long)vulkanSrc.vkImage(), (int)1, (long)vulkanDst.vkImage(), (int)1, (VkImageCopy.Buffer)regions);
            this.memoryBarrier(stack);
        }
    }

    private boolean awaitSubmitCompletion(long submitIndex, long timeoutNS) {
        if (this.completedSubmitIndex >= submitIndex) {
            return true;
        }
        if (submitIndex == this.currentSubmitIndex) {
            if (timeoutNS == 0L) {
                return false;
            }
            throw new IllegalStateException("Cannot wait on a fence for the current submit");
        }
        try (MemoryStack stack = MemoryStack.stackPush();){
            boolean completed;
            VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc((MemoryStack)stack).sType$Default();
            waitInfo.pSemaphores(stack.longs(this.submitSemaphore));
            waitInfo.pValues(stack.longs(submitIndex));
            waitInfo.semaphoreCount(1);
            int result = VK12.vkWaitSemaphores((VkDevice)this.device.vkDevice(), (VkSemaphoreWaitInfo)waitInfo, (long)timeoutNS);
            VulkanUtils.crashIfFailure(this.device, result, "Failed to wait for semaphore");
            boolean bl = completed = result == 0;
            if (completed) {
                this.completedSubmitIndex = submitIndex;
            }
            boolean bl2 = completed;
            return bl2;
        }
    }

    @Override
    public GpuFence createFence() {
        return new GpuFence(this){
            private final long submitIndex;
            private boolean completed;
            final /* synthetic */ VulkanCommandEncoder this$0;
            {
                VulkanCommandEncoder vulkanCommandEncoder = this$0;
                Objects.requireNonNull(vulkanCommandEncoder);
                this.this$0 = vulkanCommandEncoder;
                this.submitIndex = this.this$0.currentSubmitIndex;
                this.completed = false;
            }

            @Override
            public boolean awaitCompletion(long timeoutMs) {
                if (!this.completed) {
                    this.completed = this.this$0.awaitSubmitCompletion(this.submitIndex, timeoutMs);
                }
                return this.completed;
            }

            @Override
            public void close() {
                this.completed = true;
            }
        };
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {
        long queryPool = ((VulkanQueryPool)pool).vkQueryPool();
        VK12.vkResetQueryPool((VkDevice)this.device.vkDevice(), (long)queryPool, (int)index, (int)1);
        KHRSynchronization2.vkCmdWriteTimestamp2KHR((VkCommandBuffer)this.commandBuffer(), (long)65536L, (long)queryPool, (int)index);
    }

    public long getTimestampNow() {
        try (MemoryStack stack = MemoryStack.stackPush();){
            VulkanQueryPool queryPool = (VulkanQueryPool)this.device.createTimestampQueryPool(1);
            try {
                VkCommandBuffer commandBuffer = this.allocateAndBeginTransientCommandBuffer();
                KHRSynchronization2.vkCmdWriteTimestamp2KHR((VkCommandBuffer)commandBuffer, (long)0L, (long)queryPool.vkQueryPool(), (int)0);
                VulkanUtils.crashIfFailure(this.device, VK12.vkEndCommandBuffer((VkCommandBuffer)commandBuffer), "Failed to end VkCommandBuffer");
                try (VulkanQueue.Submission submit = this.device.graphicsQueue().beginSubmit();){
                    submit.executeCommands(commandBuffer);
                }
                LongBuffer timestampPtr = stack.callocLong(1);
                VulkanUtils.crashIfFailure(this.device, VK12.vkGetQueryPoolResults((VkDevice)this.device.vkDevice(), (long)queryPool.vkQueryPool(), (int)0, (int)1, (LongBuffer)timestampPtr, (long)0L, (int)3), "Cannot fetch current timestamp");
                long l = timestampPtr.get(0);
                if (queryPool != null) {
                    queryPool.close();
                }
                return l;
            }
            catch (Throwable throwable) {
                if (queryPool != null) {
                    try {
                        queryPool.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
    }
}

