/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.vulkan.EXTMultiDraw
 *  org.lwjgl.vulkan.KHRPushDescriptor
 *  org.lwjgl.vulkan.KHRSynchronization2
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkBufferViewCreateInfo
 *  org.lwjgl.vulkan.VkCommandBuffer
 *  org.lwjgl.vulkan.VkDescriptorBufferInfo
 *  org.lwjgl.vulkan.VkDescriptorBufferInfo$Buffer
 *  org.lwjgl.vulkan.VkDescriptorImageInfo
 *  org.lwjgl.vulkan.VkDescriptorImageInfo$Buffer
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkDrawIndexedIndirectCommand
 *  org.lwjgl.vulkan.VkDrawIndirectCommand
 *  org.lwjgl.vulkan.VkMultiDrawIndexedInfoEXT
 *  org.lwjgl.vulkan.VkMultiDrawInfoEXT
 *  org.lwjgl.vulkan.VkRect2D
 *  org.lwjgl.vulkan.VkRect2D$Buffer
 *  org.lwjgl.vulkan.VkViewport
 *  org.lwjgl.vulkan.VkViewport$Buffer
 *  org.lwjgl.vulkan.VkWriteDescriptorSet
 *  org.lwjgl.vulkan.VkWriteDescriptorSet$Buffer
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import com.mojang.blaze3d.vulkan.VulkanGpuSampler;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanQueryPool;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTMultiDraw;
import org.lwjgl.vulkan.KHRPushDescriptor;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferViewCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.lwjgl.vulkan.VkDrawIndirectCommand;
import org.lwjgl.vulkan.VkMultiDrawIndexedInfoEXT;
import org.lwjgl.vulkan.VkMultiDrawInfoEXT;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

@Environment(value=EnvType.CLIENT)
public class VulkanRenderPass
implements RenderPassBackend {
    public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE;
    private final VulkanDevice device;
    private final VulkanCommandEncoder encoder;
    private final CheckpointExtension.CheckpointStorage checkpointStorage;
    private final @Nullable RenderPass.RenderArea renderArea;
    private final int outputWidth;
    private final int outputHeight;
    private final boolean hasDepth;
    private final Supplier<String> label;
    protected int pushedDebugGroups = 0;
    private final VkCommandBuffer commandBuffer;
    protected @Nullable VulkanRenderPipeline pipeline;
    private boolean anyDescriptorDirty = false;
    protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap();
    protected final HashMap<String, TextureViewAndSampler> textures = new HashMap();

    public VulkanRenderPass(VulkanDevice device, VulkanCommandEncoder encoder, VkCommandBuffer commandBuffer, CheckpointExtension.CheckpointStorage checkpointStorage, RenderPass.RenderArea renderArea, int outputWidth, int outputHeight, boolean hasDepth, Supplier<String> label) {
        this.device = device;
        this.encoder = encoder;
        this.commandBuffer = commandBuffer;
        this.checkpointStorage = checkpointStorage;
        this.renderArea = renderArea;
        this.outputWidth = outputWidth;
        this.outputHeight = outputHeight;
        this.hasDepth = hasDepth;
        this.label = label;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkViewport.Buffer viewport = VkViewport.calloc((int)1, (MemoryStack)stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float)outputWidth);
            viewport.height((float)outputHeight);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);
            VK12.vkCmdSetViewport((VkCommandBuffer)this.commandBuffer(), (int)0, (VkViewport.Buffer)viewport);
            VulkanRenderPass.setScissor(stack, this.commandBuffer(), renderArea.x(), renderArea.y(), renderArea.width(), renderArea.height());
        }
    }

    private VkCommandBuffer commandBuffer() {
        return this.commandBuffer;
    }

    @Override
    public void pushDebugGroup(Supplier<String> label) {
        ++this.pushedDebugGroups;
        this.device.instance().debug().beginDebugGroup(this.commandBuffer(), label);
    }

    @Override
    public void popDebugGroup() {
        if (this.pushedDebugGroups == 0) {
            throw new IllegalStateException("Can't pop more debug groups than was pushed!");
        }
        --this.pushedDebugGroups;
        this.device.instance().debug().endDebugGroup(this.commandBuffer());
    }

    @Override
    public void setPipeline(RenderPipeline pipeline) {
        this.pipeline = this.device.getOrCompilePipeline(pipeline);
        if (!this.pipeline.isValid()) {
            throw new IllegalStateException("Pipeline is not valid (may contain invalid shaders?)");
        }
        this.anyDescriptorDirty = true;
        VK12.vkCmdBindPipeline((VkCommandBuffer)this.commandBuffer(), (int)0, (long)(this.hasDepth ? this.pipeline.withDepthPipeline() : this.pipeline.withoutDepthPipeline()));
    }

    @Override
    public void bindTexture(String name, @Nullable GpuTextureView textureView, @Nullable GpuSampler sampler) {
        if (textureView == null || sampler == null) {
            if (textureView != null || sampler != null) {
                throw new IllegalArgumentException();
            }
            this.textures.remove(name);
            return;
        }
        this.textures.put(name, new TextureViewAndSampler((VulkanGpuTextureView)textureView, (VulkanGpuSampler)sampler));
        this.anyDescriptorDirty = true;
    }

    @Override
    public void setUniform(String name, GpuBuffer value) {
        this.uniforms.put(name, value.slice());
        this.anyDescriptorDirty = true;
    }

    @Override
    public void setUniform(String name, GpuBufferSlice value) {
        this.uniforms.put(name, value);
        this.anyDescriptorDirty = true;
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            VulkanRenderPass.setScissor(stack, this.commandBuffer(), x, y, width, height);
        }
    }

    private static void setScissor(MemoryStack stack, VkCommandBuffer commandBuffer, int x, int y, int width, int height) {
        VkRect2D.Buffer scissor = VkRect2D.calloc((int)1, (MemoryStack)stack);
        scissor.offset().set(x, y);
        scissor.extent().set(width, height);
        VK12.vkCmdSetScissor((VkCommandBuffer)commandBuffer, (int)0, (VkRect2D.Buffer)scissor);
    }

    @Override
    public void disableScissor() {
        if (this.renderArea != null) {
            this.enableScissor(this.renderArea.x(), this.renderArea.y(), this.renderArea.width(), this.renderArea.height());
        } else {
            this.enableScissor(0, 0, this.outputWidth, this.outputHeight);
        }
    }

    @Override
    public void setVertexBuffer(int slot, @Nullable GpuBufferSlice vertexBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            long buffer = vertexBuffer != null ? ((VulkanGpuBuffer)vertexBuffer.buffer()).vkBuffer() : 0L;
            long offset = vertexBuffer != null ? vertexBuffer.offset() : 0L;
            VK12.vkCmdBindVertexBuffers((VkCommandBuffer)this.commandBuffer(), (int)slot, (LongBuffer)stack.longs(buffer), (LongBuffer)stack.longs(offset));
        }
    }

    @Override
    public void setIndexBuffer(GpuBuffer indexBuffer, IndexType indexType) {
        int type = switch (indexType) {
            default -> throw new MatchException(null, null);
            case IndexType.SHORT -> 0;
            case IndexType.INT -> 1;
        };
        VK12.vkCmdBindIndexBuffer((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuBuffer)indexBuffer).vkBuffer(), (long)0L, (int)type);
    }

    @Override
    public void drawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
        if (this.pipeline == null || !this.pipeline.isValid()) {
            throw new IllegalStateException("Pipeline is missing or not valid");
        }
        this.pushDescriptors();
        VK12.vkCmdDrawIndexed((VkCommandBuffer)this.commandBuffer(), (int)indexCount, (int)instanceCount, (int)firstIndex, (int)vertexOffset, (int)firstInstance);
    }

    @Override
    public void multiDrawIndexed(IntBuffer drawParameters, int instanceCount, int firstInstance, int drawCount) {
        if (this.pipeline == null || !this.pipeline.isValid()) {
            throw new IllegalStateException("Pipeline is missing or not valid");
        }
        this.pushDescriptors();
        EXTMultiDraw.nvkCmdDrawMultiIndexedEXT((VkCommandBuffer)this.commandBuffer(), (int)drawCount, (long)MemoryUtil.memAddress((IntBuffer)drawParameters), (int)instanceCount, (int)firstInstance, (int)VkMultiDrawIndexedInfoEXT.SIZEOF, (long)0L);
    }

    @Override
    public void multiDrawIndexed(PointerBuffer firstIndexOffsets, IntBuffer indexCounts, IntBuffer vertexOffsets, int drawCount) {
        throw new UnsupportedOperationException("Vulkan does not support the multiDrawDirectSeparate device feature");
    }

    @Override
    public void drawIndexedIndirect(GpuBufferSlice commands, int drawCount) {
        if (this.pipeline == null || !this.pipeline.isValid()) {
            throw new IllegalStateException("Pipeline is missing or not valid");
        }
        this.pushDescriptors();
        VK12.vkCmdDrawIndexedIndirect((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuBuffer)commands.buffer()).vkBuffer(), (long)commands.offset(), (int)drawCount, (int)VkDrawIndexedIndirectCommand.SIZEOF);
    }

    @Override
    public <T> void drawMultipleIndexed(Collection<RenderPass.Draw<T>> draws, @Nullable GpuBuffer defaultIndexBuffer, @Nullable IndexType defaultIndexType, Collection<String> dynamicUniforms, T uniformArgument) {
        for (RenderPass.Draw<T> draw : draws) {
            BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer = draw.uniformUploaderConsumer();
            if (uniformUploaderConsumer != null) {
                uniformUploaderConsumer.accept(uniformArgument, this::setUniform);
            }
            assert (draw.indexBuffer() != null || defaultIndexBuffer != null);
            assert (draw.indexType() != null || defaultIndexType != null);
            this.setIndexBuffer(draw.indexBuffer() == null ? defaultIndexBuffer : draw.indexBuffer(), draw.indexType() == null ? defaultIndexType : draw.indexType());
            this.setVertexBuffer(draw.slot(), draw.vertexBuffer().slice());
            this.drawIndexed(draw.indexCount(), 1, draw.firstIndex(), draw.baseVertex(), 0);
        }
    }

    @Override
    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        if (this.pipeline == null || !this.pipeline.isValid()) {
            return;
        }
        this.pushDescriptors();
        VK12.vkCmdDraw((VkCommandBuffer)this.commandBuffer(), (int)vertexCount, (int)instanceCount, (int)firstVertex, (int)firstInstance);
    }

    @Override
    public void multiDraw(IntBuffer drawParameters, int instanceCount, int firstInstance, int drawCount) {
        if (this.pipeline == null || !this.pipeline.isValid()) {
            throw new IllegalStateException("Pipeline is missing or not valid");
        }
        this.pushDescriptors();
        EXTMultiDraw.nvkCmdDrawMultiEXT((VkCommandBuffer)this.commandBuffer(), (int)drawCount, (long)MemoryUtil.memAddress((IntBuffer)drawParameters), (int)instanceCount, (int)firstInstance, (int)VkMultiDrawInfoEXT.SIZEOF);
    }

    @Override
    public void multiDraw(IntBuffer firstVertices, IntBuffer vertexCounts, int drawCount) {
        throw new UnsupportedOperationException("Vulkan does not support the multiDrawDirectSeparate device feature");
    }

    @Override
    public void drawIndirect(GpuBufferSlice commands, int drawCount) {
        if (this.pipeline == null || !this.pipeline.isValid()) {
            throw new IllegalStateException("Pipeline is missing or not valid");
        }
        this.pushDescriptors();
        VK12.vkCmdDrawIndirect((VkCommandBuffer)this.commandBuffer(), (long)((VulkanGpuBuffer)commands.buffer()).vkBuffer(), (long)commands.offset(), (int)drawCount, (int)VkDrawIndirectCommand.SIZEOF);
    }

    private void pushDescriptors() {
        if (!this.anyDescriptorDirty) {
            return;
        }
        if (VALIDATION) {
            for (BindGroupLayout.UniformDescription uniform : BindGroupLayout.flattenUniforms(this.pipeline.info().getBindGroupLayouts())) {
                GpuBufferSlice value = this.uniforms.get(uniform.name());
                if (value == null) {
                    throw new IllegalStateException("Missing uniform " + uniform.name() + " (should be " + String.valueOf((Object)uniform.type()) + ")");
                }
                if (uniform.type() == UniformType.UNIFORM_BUFFER) {
                    if (value.buffer().isClosed()) {
                        throw new IllegalStateException("Uniform buffer " + uniform.name() + " is already closed");
                    }
                    if ((value.buffer().usage() & 0x80) == 0) {
                        throw new IllegalStateException("Uniform buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM");
                    }
                }
                if (uniform.type() != UniformType.TEXEL_BUFFER) continue;
                if (value.offset() != 0L || value.length() != value.buffer().size()) {
                    throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                }
                if ((value.buffer().usage() & 0x100) == 0) {
                    throw new IllegalStateException("Uniform texel buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER");
                }
                if (uniform.gpuFormat() != null) continue;
                throw new IllegalStateException("Invalid uniform texel buffer " + uniform.name() + " (missing a texture format)");
            }
        }
        assert (this.pipeline != null);
        VulkanBindGroupLayout layout = this.pipeline.layout();
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc((int)layout.entries().size(), (MemoryStack)stack);
            for (int i = 0; i < layout.entries().size(); ++i) {
                Record value;
                VulkanBindGroupLayout.Entry entry = layout.entries().get(i);
                VkWriteDescriptorSet set = ((VkWriteDescriptorSet)writes.get()).sType$Default();
                set.dstBinding(i);
                set.dstArrayElement(0);
                set.descriptorCount(1);
                if (entry.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER) {
                    GpuBufferSlice buffer = this.uniforms.get(entry.name());
                    if (buffer == null) {
                        throw new IllegalStateException("Missing uniform " + entry.name() + " (should be " + String.valueOf((Object)entry.type()) + ")");
                    }
                    VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc((int)1, (MemoryStack)stack);
                    bufferInfo.buffer(((VulkanGpuBuffer)buffer.buffer()).vkBuffer());
                    bufferInfo.offset(buffer.offset());
                    bufferInfo.range(buffer.length());
                    set.descriptorType(6);
                    set.pBufferInfo(bufferInfo);
                    continue;
                }
                if (entry.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.SAMPLED_IMAGE) {
                    value = this.textures.get(entry.name());
                    if (value == null) {
                        throw new IllegalStateException("Missing sampler " + entry.name());
                    }
                    VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc((int)1, (MemoryStack)stack);
                    imageInfo.sampler(((TextureViewAndSampler)value).sampler.vkSampler());
                    imageInfo.imageView(((TextureViewAndSampler)value).view.vkImageView());
                    imageInfo.imageLayout(1);
                    set.descriptorType(1);
                    set.pImageInfo(imageInfo);
                    continue;
                }
                if (entry.type() != VulkanBindGroupLayout.VulkanBindGroupEntryType.TEXEL_BUFFER) continue;
                value = this.uniforms.get(entry.name());
                if (value == null) {
                    throw new IllegalStateException("Missing uniform " + entry.name() + " (should be " + String.valueOf((Object)entry.type()) + ")");
                }
                LongBuffer bufferViewPtr = stack.callocLong(1);
                try (MemoryStack memoryStack = stack.push();){
                    assert (entry.texelBufferFormat() != null);
                    VkBufferViewCreateInfo viewCreateInfo = VkBufferViewCreateInfo.calloc((MemoryStack)stack).sType$Default();
                    viewCreateInfo.buffer(((VulkanGpuBuffer)((GpuBufferSlice)value).buffer()).vkBuffer());
                    viewCreateInfo.offset(((GpuBufferSlice)value).offset());
                    viewCreateInfo.range(((GpuBufferSlice)value).length());
                    viewCreateInfo.format(VulkanConst.toVk(entry.texelBufferFormat()));
                    VulkanUtils.crashIfFailure(this.device, VK12.vkCreateBufferView((VkDevice)this.device.vkDevice(), (VkBufferViewCreateInfo)viewCreateInfo, null, (LongBuffer)bufferViewPtr), "Couldn't create buffer view for texel buffer");
                    long bufferViewHandle = bufferViewPtr.get(0);
                    this.encoder.queueForDestroy(() -> VK12.vkDestroyBufferView((VkDevice)this.device.vkDevice(), (long)bufferViewHandle, null));
                }
                set.descriptorType(4);
                set.pTexelBufferView(bufferViewPtr);
            }
            KHRPushDescriptor.vkCmdPushDescriptorSetKHR((VkCommandBuffer)this.commandBuffer(), (int)0, (long)this.pipeline.pipelineLayout(), (int)0, (VkWriteDescriptorSet.Buffer)((VkWriteDescriptorSet.Buffer)writes.flip()));
        }
        this.anyDescriptorDirty = false;
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {
        long queryPool = ((VulkanQueryPool)pool).vkQueryPool();
        VK12.vkResetQueryPool((VkDevice)this.device.vkDevice(), (long)queryPool, (int)index, (int)1);
        KHRSynchronization2.vkCmdWriteTimestamp2KHR((VkCommandBuffer)this.commandBuffer(), (long)65536L, (long)queryPool, (int)index);
    }

    public Supplier<String> getLabel() {
        return this.label;
    }

    @Environment(value=EnvType.CLIENT)
    protected record TextureViewAndSampler(VulkanGpuTextureView view, VulkanGpuSampler sampler) {
    }
}

