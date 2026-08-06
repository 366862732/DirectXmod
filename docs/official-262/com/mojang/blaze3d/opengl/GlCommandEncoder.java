/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.opengl.ARBBaseInstance
 *  org.lwjgl.opengl.ARBDrawIndirect
 *  org.lwjgl.opengl.ARBMultiDrawIndirect
 *  org.lwjgl.opengl.GL33C
 *  org.lwjgl.system.CustomBuffer
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlFence;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlQueryPool;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.opengl.GlTransientMemory;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.opengl.VertexArrayCache;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import java.lang.runtime.SwitchBootstraps;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBBaseInstance;
import org.lwjgl.opengl.ARBDrawIndirect;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
class GlCommandEncoder
implements CommandEncoderBackend,
AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_SUBMITS_IN_FLIGHT = 2;
    private static final long NO_FENCE = 0L;
    private final GlDevice device;
    private final GlTransientMemory transientMemory;
    private final long[] fences = new long[2];
    private long currentSubmitIndex = 2L;
    private final int readFbo;
    private final int drawFbo;
    private @Nullable RenderPipeline lastPipeline;
    private @Nullable GlProgram lastProgram;
    private @Nullable VertexArrayCache.VertexArray lastVertexArray;
    private final List<@Nullable FrameBufferAttachment> renderPassColorTextures = new ArrayList<FrameBufferAttachment>();

    protected GlCommandEncoder(GlDevice device) {
        this.device = device;
        this.transientMemory = device.getDeviceInfo().features().persistentMapping() ? new GlTransientMemory.PersistentMapping(device, this) : new GlTransientMemory.Fallback(device, this);
        this.readFbo = device.directStateAccess().createFrameBufferObject();
        this.drawFbo = device.directStateAccess().createFrameBufferObject();
    }

    @Override
    public void close() {
        this.transientMemory.close();
    }

    public long currentSubmitIndex() {
        return this.currentSubmitIndex;
    }

    public int currentSubmitSlot() {
        return (int)(this.currentSubmitIndex % 2L);
    }

    @Override
    public void submit() {
        this.fences[this.currentSubmitSlot()] = GL33C.glFenceSync((int)37143, (int)0);
        ++this.currentSubmitIndex;
        if (!this.awaitSubmit(this.currentSubmitIndex - 2L, Long.MAX_VALUE)) {
            throw new IllegalStateException("Failed to wait for frame completion");
        }
        this.transientMemory.rotate();
    }

    public boolean awaitSubmit(long index, long timeoutNS) {
        if (this.currentSubmitIndex > index + 2L) {
            return true;
        }
        if (index == this.currentSubmitIndex) {
            if (timeoutNS == 0L) {
                return false;
            }
            throw new IllegalStateException("Cannot wait on a fence for the current submit");
        }
        int submitSlot = (int)(index % 2L);
        long fence = this.fences[submitSlot];
        if (fence == 0L) {
            return true;
        }
        int result = GlStateManager._glClientWaitSync(fence, 1, timeoutNS);
        if (result == 37147) {
            return false;
        }
        if (result == 37149) {
            throw new IllegalStateException("Failed to complete GPU fence: " + GlStateManager._getError());
        }
        GL33C.glDeleteSync((long)this.fences[submitSlot]);
        this.fences[submitSlot] = 0L;
        return true;
    }

    @Override
    public TransientMemory transientMemory() {
        return this.transientMemory;
    }

    @Override
    public RenderPassBackend createRenderPass(RenderPassDescriptor descriptor) {
        OptionalDouble clearValue;
        this.device.debugLabels().pushDebugGroup(descriptor.label());
        List<@Nullable RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments = descriptor.colorAttachments();
        this.renderPassColorTextures.clear();
        for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> colorAttachment : colorAttachments) {
            this.renderPassColorTextures.add(colorAttachment != null ? (GlTextureView)colorAttachment.textureView() : null);
        }
        RenderPassDescriptor.Attachment<OptionalDouble> depthAttachment = descriptor.depthAttachment();
        int fbo = this.device.frameBufferCache().getFbo(this.device.directStateAccess(), this.renderPassColorTextures, depthAttachment == null ? null : (GlTextureView)depthAttachment.textureView());
        GlStateManager._glBindFramebuffer(36160, fbo);
        assert (descriptor.renderArea != null);
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(descriptor.renderArea.x(), descriptor.renderArea.y(), descriptor.renderArea.width(), descriptor.renderArea.height());
        for (int i = 0; i < colorAttachments.size(); ++i) {
            Object clearValue2;
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = colorAttachments.get(i);
            if (attachment == null || !((Optional)(clearValue2 = attachment.clearValue())).isPresent()) continue;
            GlStateManager._colorMask(i, 15);
            GlStateManager._clearBuffer(i, (Vector4fc)((Optional)clearValue2).get());
        }
        if (depthAttachment != null && (clearValue = depthAttachment.clearValue()).isPresent()) {
            GlStateManager._depthMask(true);
            GlStateManager._clearBuffer(clearValue.getAsDouble());
        }
        int width = 0;
        int height = 0;
        if (!colorAttachments.isEmpty()) {
            for (RenderPassDescriptor.Attachment attachment : colorAttachments) {
                if (attachment == null) continue;
                GpuTextureView colorTexture = attachment.textureView();
                width = colorTexture.getWidth(0);
                height = colorTexture.getHeight(0);
            }
        } else if (depthAttachment != null) {
            width = depthAttachment.textureView().getWidth(0);
            height = depthAttachment.textureView().getHeight(0);
        }
        GlStateManager._viewport(0, 0, width, height);
        this.lastPipeline = null;
        ScissorState scissorState = new ScissorState();
        scissorState.enable(descriptor.renderArea.x(), descriptor.renderArea.y(), descriptor.renderArea.width(), descriptor.renderArea.height());
        return new GlRenderPass(this, this.device, depthAttachment != null, this.renderPassColorTextures.size(), scissorState);
    }

    @Override
    public void clearColorTexture(GpuTexture colorTexture, Vector4fc clearColor) {
        this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)colorTexture).id, 0, 0, 36160);
        GL33C.glClearColor((float)clearColor.x(), (float)clearColor.y(), (float)clearColor.z(), (float)clearColor.w());
        GlStateManager._disableScissorTest();
        GlStateManager._colorMask(15);
        GlStateManager._clear(16384);
        GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
        GlStateManager._glBindFramebuffer(36160, 0);
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth) {
        int fbo = this.device.frameBufferCache().getFbo(this.device.directStateAccess(), Collections.singletonList((GlTexture)colorTexture), (GlTexture)depthTexture);
        GlStateManager._glBindFramebuffer(36160, fbo);
        GlStateManager._disableScissorTest();
        GL33C.glClearDepth((double)clearDepth);
        GL33C.glClearColor((float)clearColor.x(), (float)clearColor.y(), (float)clearColor.z(), (float)clearColor.w());
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(15);
        GlStateManager._clear(16640);
        GlStateManager._glBindFramebuffer(36160, 0);
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth, int regionX, int regionY, int regionWidth, int regionHeight) {
        int fbo = this.device.frameBufferCache().getFbo(this.device.directStateAccess(), Collections.singletonList((GlTexture)colorTexture), (GlTexture)depthTexture);
        GlStateManager._glBindFramebuffer(36160, fbo);
        GlStateManager._scissorBox(regionX, regionY, regionWidth, regionHeight);
        GlStateManager._enableScissorTest();
        GL33C.glClearDepth((double)clearDepth);
        GL33C.glClearColor((float)clearColor.x(), (float)clearColor.y(), (float)clearColor.z(), (float)clearColor.w());
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(15);
        GlStateManager._clear(16640);
        GlStateManager._glBindFramebuffer(36160, 0);
    }

    @Override
    public void clearDepthTexture(GpuTexture depthTexture, double clearDepth) {
        this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)depthTexture).id, 0, 36160);
        GL33C.glDrawBuffer((int)0);
        GL33C.glClearDepth((double)clearDepth);
        GlStateManager._depthMask(true);
        GlStateManager._disableScissorTest();
        GlStateManager._clear(256);
        GL33C.glDrawBuffer((int)36064);
        GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
        GlStateManager._glBindFramebuffer(36160, 0);
    }

    @Override
    public void writeToBuffer(GpuBufferSlice slice, ByteBuffer data) {
        GlBuffer buffer = (GlBuffer)slice.buffer();
        buffer.checkCanBeUsed();
        this.device.directStateAccess().bufferSubData(buffer.handle(), slice.offset(), data, buffer.usage());
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        GlBuffer sourceBuffer = (GlBuffer)source.buffer();
        GlBuffer targetBuffer = (GlBuffer)target.buffer();
        sourceBuffer.checkCanBeUsed();
        targetBuffer.checkCanBeUsed();
        this.device.directStateAccess().copyBufferSubData(sourceBuffer.handle(), targetBuffer.handle(), source.offset(), target.offset(), source.length());
    }

    @Override
    public void writeToTexture(GpuTexture destination, ByteBuffer source, int mipLevel, int depthOrLayer, int destX, int destY, int width, int height) {
        int target;
        if ((destination.usage() & 0x10) != 0) {
            target = GlConst.CUBEMAP_TARGETS[depthOrLayer % 6];
            GL33C.glBindTexture((int)34067, (int)((GlTexture)destination).id);
        } else {
            target = 3553;
            GlStateManager._bindTexture(((GlTexture)destination).id);
        }
        GlStateManager._pixelStore(3314, width);
        GlStateManager._pixelStore(3316, 0);
        GlStateManager._pixelStore(3315, 0);
        GlStateManager._pixelStore(3317, destination.getFormat().componentCount());
        GlStateManager._texSubImage2D(target, mipLevel, destX, destY, width, height, GlConst.toGlExternalId(destination.getFormat()), 5121, source);
    }

    @Override
    public void copyBufferToTexture(GpuBufferSlice source, int sourceX, int sourceY, int sourceWidth, int sourceHeight, GpuTexture destination, int destinationX, int destinationY, int copyWidth, int copyHeight, int mipLevel, int arrayLayer) {
        int target;
        if ((destination.usage() & 0x10) != 0) {
            target = GlConst.CUBEMAP_TARGETS[arrayLayer % 6];
            GL33C.glBindTexture((int)34067, (int)((GlTexture)destination).id);
        } else {
            target = 3553;
            GlStateManager._bindTexture(((GlTexture)destination).id);
        }
        int texelSize = destination.getFormat().blockSize();
        long skipTexels = (long)sourceX + (long)sourceY * (long)sourceWidth;
        long skipBytes = skipTexels * (long)texelSize;
        GlBuffer sourceGlBuffer = (GlBuffer)source.buffer();
        GlStateManager._glBindBuffer(35052, sourceGlBuffer.handle());
        GlStateManager._pixelStore(3314, sourceWidth);
        GlStateManager._pixelStore(32878, sourceHeight);
        GlStateManager._pixelStore(3316, 0);
        GlStateManager._pixelStore(3315, 0);
        GlStateManager._pixelStore(3317, destination.getFormat().byteAlignment());
        GlStateManager._texSubImage2D(target, mipLevel, destinationX, destinationY, copyWidth, copyHeight, GlConst.toGlExternalId(destination.getFormat()), GlConst.toGlType(destination.getFormat()), source.offset() + skipBytes);
        GlStateManager._glBindBuffer(35052, 0);
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel) {
        this.copyTextureToBuffer(source, destination, offset, callback, mipLevel, 0, 0, source.getWidth(mipLevel), source.getHeight(mipLevel));
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel, int x, int y, int width, int height) {
        ((GlBuffer)destination).checkCanBeUsed();
        GlStateManager.clearGlErrors();
        this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)source).glId(), 0, mipLevel, 36008);
        GlStateManager._glBindBuffer(35051, ((GlBuffer)destination).handle());
        GlStateManager._pixelStore(3330, width);
        GlStateManager._readPixels(x, y, width, height, GlConst.toGlExternalId(source.getFormat()), GlConst.toGlType(source.getFormat()), offset);
        RenderSystem.queueFencedTask(callback);
        GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, mipLevel);
        GlStateManager._glBindFramebuffer(36008, 0);
        GlStateManager._glBindBuffer(35051, 0);
        int error = GlStateManager._getError();
        if (error != 0) {
            throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + source.getLabel() + ": GL error " + error);
        }
    }

    @Override
    public void copyTextureToTexture(GpuTexture source, GpuTexture destination, int mipLevel, int destX, int destY, int sourceX, int sourceY, int width, int height) {
        GlStateManager.clearGlErrors();
        GlStateManager._disableScissorTest();
        boolean isDepth = source.getFormat().hasDepthAspect();
        int sourceId = ((GlTexture)source).glId();
        int destId = ((GlTexture)destination).glId();
        this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, isDepth ? 0 : sourceId, isDepth ? sourceId : 0, 0, 0);
        this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, isDepth ? 0 : destId, isDepth ? destId : 0, 0, 0);
        this.device.directStateAccess().blitFrameBuffers(this.readFbo, this.drawFbo, sourceX, sourceY, width, height, destX, destY, width, height, isDepth ? 256 : 16384, 9728);
        int error = GlStateManager._getError();
        if (error != 0) {
            throw new IllegalStateException("Couldn't perform copyToTexture for texture " + source.getLabel() + " to " + destination.getLabel() + ": GL error " + error);
        }
    }

    public void presentTexture(GpuTextureView textureView, int swapchainWidth, int swapchainHeight) {
        int destY = Math.max(0, swapchainHeight - textureView.getHeight(0));
        int copyWidth = Math.min(swapchainWidth, textureView.getWidth(0));
        int copyHeight = Math.min(swapchainHeight, textureView.getHeight(0));
        GlStateManager._disableScissorTest();
        GlStateManager._viewport(0, 0, textureView.getWidth(0), textureView.getHeight(0));
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(15);
        this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)textureView.texture()).glId(), 0, 0, 0);
        this.device.directStateAccess().blitFrameBuffers(this.drawFbo, 0, 0, 0, copyWidth, copyHeight, 0, destY, copyWidth, copyHeight + destY, 16384, 9728);
    }

    @Override
    public GpuFence createFence() {
        return new GlFence(this);
    }

    protected <T> void executeDrawMultiple(GlRenderPass renderPass, Collection<RenderPass.Draw<T>> draws, @Nullable GpuBuffer defaultIndexBuffer, @Nullable IndexType defaultIndexType, Collection<String> dynamicUniforms, T uniformArgument) {
        if (!this.trySetup(renderPass, dynamicUniforms)) {
            return;
        }
        if (defaultIndexType == null) {
            defaultIndexType = IndexType.SHORT;
        }
        for (RenderPass.Draw<T> draw : draws) {
            BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer;
            IndexType indexType = draw.indexType() == null ? defaultIndexType : draw.indexType();
            renderPass.setIndexBuffer(draw.indexBuffer() == null ? defaultIndexBuffer : draw.indexBuffer(), indexType);
            renderPass.setVertexBuffer(draw.slot(), draw.vertexBuffer().slice());
            if (GlRenderPass.VALIDATION) {
                if (renderPass.indexBuffer == null) {
                    throw new IllegalStateException("Missing index buffer");
                }
                ((GlBuffer)renderPass.indexBuffer).checkCanBeUsed();
                if (renderPass.indexBuffer.isClosed()) {
                    throw new IllegalStateException("Index buffer has been closed!");
                }
                if (draw.slot() < 0 || draw.slot() >= 16) {
                    throw new IllegalStateException("Vertex buffer slot must be between 0 and 16");
                }
                if (renderPass.vertexBuffers[draw.slot()] != null) {
                    ((GlBuffer)renderPass.vertexBuffers[draw.slot()].buffer()).checkCanBeUsed();
                }
                if (renderPass.vertexBuffers[draw.slot()] == null) {
                    throw new IllegalStateException("Missing vertex buffer at slot " + draw.slot());
                }
                if (renderPass.vertexBuffers[draw.slot()].buffer().isClosed()) {
                    throw new IllegalStateException("Vertex buffer at slot " + draw.slot() + " has been closed!");
                }
            }
            if ((uniformUploaderConsumer = draw.uniformUploaderConsumer()) != null) {
                uniformUploaderConsumer.accept(uniformArgument, (name, buffer) -> {
                    block3: {
                        ((GlBuffer)buffer.buffer()).checkCanBeUsed();
                        Uniform patt1$temp = renderPass.pipeline.program().getUniform(name);
                        if (patt1$temp instanceof Uniform.Ubo) {
                            int blockBinding;
                            Uniform.Ubo $b$0 = (Uniform.Ubo)patt1$temp;
                            try {
                                int patt2$temp;
                                int tmp0$ = patt2$temp = $b$0.blockBinding();
                                if (!true) break block3;
                                blockBinding = patt2$temp;
                            }
                            catch (Throwable throwable) {
                                throw new MatchException(throwable.toString(), throwable);
                            }
                            GL33C.glBindBufferRange((int)35345, (int)blockBinding, (int)((GlBuffer)buffer.buffer()).handle(), (long)buffer.offset(), (long)buffer.length());
                        }
                    }
                });
            }
            if (renderPass.vertexBufferDirty) {
                this.lastVertexArray = this.device.vertexArrayCache().bindVertexArray(renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, null);
                renderPass.vertexBufferDirty = false;
            }
            this.drawFromBuffers(renderPass, draw.baseVertex(), draw.firstIndex(), draw.indexCount(), indexType, renderPass.pipeline, 1, 0);
        }
    }

    private void validateDraw(GlRenderPass renderPass, @Nullable IndexType indexType) {
        if (GlRenderPass.VALIDATION) {
            if (indexType != null) {
                if (renderPass.indexBuffer == null) {
                    throw new IllegalStateException("Missing index buffer");
                }
                ((GlBuffer)renderPass.indexBuffer).checkCanBeUsed();
                if (renderPass.indexBuffer.isClosed()) {
                    throw new IllegalStateException("Index buffer has been closed!");
                }
                if ((renderPass.indexBuffer.usage() & 0x40) == 0) {
                    throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
                }
            }
            GlRenderPipeline pipeline = renderPass.pipeline;
            for (int i = 0; i < 16; ++i) {
                if (renderPass.vertexBuffers[i] == null && pipeline != null && pipeline.info().getVertexFormatBindings()[i] != null) {
                    throw new IllegalStateException("Vertex format contains elements but vertex buffer at slot " + i + " is null");
                }
                if (renderPass.vertexBuffers[i] == null) continue;
                ((GlBuffer)renderPass.vertexBuffers[i].buffer()).checkCanBeUsed();
            }
        }
    }

    protected void executeDraw(GlRenderPass renderPass, int baseVertex, int firstIndex, int drawCount, @Nullable IndexType indexType, int instanceCount, int firstInstance) {
        if (!this.trySetup(renderPass, Collections.emptyList())) {
            return;
        }
        this.validateDraw(renderPass, indexType);
        this.lastVertexArray = this.device.vertexArrayCache().bindVertexArray(renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, renderPass.vertexBufferDirty ? null : this.lastVertexArray);
        renderPass.vertexBufferDirty = false;
        this.drawFromBuffers(renderPass, baseVertex, firstIndex, drawCount, indexType, renderPass.pipeline, instanceCount, firstInstance);
    }

    public void executeDraws(GlRenderPass renderPass, @Nullable IndexType indexType, @Nullable PointerBuffer firstIndexOffsets, IntBuffer indexCounts, IntBuffer vertexOffsets, int drawCount) {
        if (!this.trySetup(renderPass, Collections.emptyList())) {
            return;
        }
        this.validateDraw(renderPass, indexType);
        this.lastVertexArray = this.device.vertexArrayCache().bindVertexArray(renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, renderPass.vertexBufferDirty ? null : this.lastVertexArray);
        renderPass.vertexBufferDirty = false;
        if (indexType == null) {
            GL33C.nglMultiDrawArrays((int)GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), (long)MemoryUtil.memAddress((IntBuffer)vertexOffsets), (long)MemoryUtil.memAddress((IntBuffer)indexCounts), (int)drawCount);
        } else {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle());
            assert (firstIndexOffsets != null);
            GL33C.nglMultiDrawElementsBaseVertex((int)GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), (long)MemoryUtil.memAddress((IntBuffer)indexCounts), (int)GlConst.toGl(indexType), (long)MemoryUtil.memAddress((CustomBuffer)firstIndexOffsets), (int)drawCount, (long)MemoryUtil.memAddress((IntBuffer)vertexOffsets));
        }
    }

    protected void executeDrawIndirect(GlRenderPass renderPass, @Nullable IndexType indexType, GlBuffer commands, long offset, int drawCount) {
        if (!this.trySetup(renderPass, Collections.emptyList())) {
            return;
        }
        this.validateDraw(renderPass, indexType);
        this.lastVertexArray = this.device.vertexArrayCache().bindVertexArray(renderPass.pipeline.info().getVertexFormatBindings(), renderPass.vertexBuffers, renderPass.vertexBufferDirty ? null : this.lastVertexArray);
        renderPass.vertexBufferDirty = false;
        GlStateManager._glBindBuffer(36671, commands.handle());
        if (indexType == null) {
            if (drawCount > 1) {
                ARBMultiDrawIndirect.glMultiDrawArraysIndirect((int)GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), (long)offset, (int)drawCount, (int)0);
            } else {
                ARBDrawIndirect.glDrawArraysIndirect((int)GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), (long)offset);
            }
        } else {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle());
            if (drawCount > 1) {
                ARBMultiDrawIndirect.glMultiDrawElementsIndirect((int)GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), (int)GlConst.toGl(indexType), (long)offset, (int)drawCount, (int)0);
            } else {
                ARBDrawIndirect.glDrawElementsIndirect((int)GlConst.toGl(renderPass.pipeline.info().getPrimitiveTopology()), (int)GlConst.toGl(indexType), (long)offset);
            }
        }
    }

    private void drawFromBuffers(GlRenderPass renderPass, int baseVertex, int firstIndex, int drawCount, @Nullable IndexType indexType, GlRenderPipeline pipeline, int instanceCount, int firstInstance) {
        if (indexType != null) {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle());
            if (firstInstance > 0) {
                ARBBaseInstance.glDrawElementsInstancedBaseVertexBaseInstance((int)GlConst.toGl(pipeline.info().getPrimitiveTopology()), (int)drawCount, (int)GlConst.toGl(indexType), (long)((long)firstIndex * (long)indexType.bytes), (int)instanceCount, (int)baseVertex, (int)firstInstance);
            } else {
                GL33C.glDrawElementsInstancedBaseVertex((int)GlConst.toGl(pipeline.info().getPrimitiveTopology()), (int)drawCount, (int)GlConst.toGl(indexType), (long)((long)firstIndex * (long)indexType.bytes), (int)instanceCount, (int)baseVertex);
            }
        } else if (firstInstance > 0) {
            ARBBaseInstance.glDrawArraysInstancedBaseInstance((int)GlConst.toGl(pipeline.info().getPrimitiveTopology()), (int)baseVertex, (int)drawCount, (int)instanceCount, (int)firstInstance);
        } else {
            GL33C.glDrawArraysInstanced((int)GlConst.toGl(pipeline.info().getPrimitiveTopology()), (int)baseVertex, (int)drawCount, (int)instanceCount);
        }
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private boolean trySetup(GlRenderPass renderPass, Collection<String> dynamicUniforms) {
        boolean differentProgram;
        if (GlRenderPass.VALIDATION) {
            if (renderPass.pipeline == null) {
                throw new IllegalStateException("Can't draw without a render pipeline");
            }
            if (renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
                throw new IllegalStateException("Pipeline contains invalid shader program");
            }
            for (BindGroupLayout.UniformDescription uniformDescription : BindGroupLayout.flattenUniforms(renderPass.pipeline.info().getBindGroupLayouts())) {
                GpuBufferSlice value = renderPass.uniforms.get(uniformDescription.name());
                if (dynamicUniforms.contains(uniformDescription.name())) continue;
                if (value == null) {
                    throw new IllegalStateException("Missing uniform " + uniformDescription.name() + " (should be " + String.valueOf((Object)uniformDescription.type()) + ")");
                }
                ((GlBuffer)value.buffer()).checkCanBeUsed();
                if (uniformDescription.type() == UniformType.UNIFORM_BUFFER) {
                    if (value.buffer().isClosed()) {
                        throw new IllegalStateException("Uniform buffer " + uniformDescription.name() + " is already closed");
                    }
                    if ((value.buffer().usage() & 0x80) == 0) {
                        throw new IllegalStateException("Uniform buffer " + uniformDescription.name() + " must have GpuBuffer.USAGE_UNIFORM");
                    }
                }
                if (uniformDescription.type() != UniformType.TEXEL_BUFFER) continue;
                if (value.offset() != 0L) throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                if (value.length() != value.buffer().size()) {
                    throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                }
                if ((value.buffer().usage() & 0x100) == 0) {
                    throw new IllegalStateException("Uniform texel buffer " + uniformDescription.name() + " must have GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER");
                }
                if (uniformDescription.gpuFormat() != null) continue;
                throw new IllegalStateException("Invalid uniform texel buffer " + uniformDescription.name() + " (missing a texture format)");
            }
            for (Map.Entry entry : renderPass.pipeline.program().getUniforms().entrySet()) {
                if (!(entry.getValue() instanceof Uniform.Sampler)) continue;
                String name = (String)entry.getKey();
                GlRenderPass.TextureViewAndSampler viewAndSampler = renderPass.samplers.get(name);
                if (viewAndSampler == null) {
                    throw new IllegalStateException("Missing sampler " + name);
                }
                GlTextureView textureView = viewAndSampler.view();
                if (textureView.isClosed()) {
                    throw new IllegalStateException("Texture view " + name + " (" + textureView.texture().getLabel() + ") has been closed!");
                }
                if ((textureView.texture().usage() & 4) == 0) {
                    throw new IllegalStateException("Texture view " + name + " (" + textureView.texture().getLabel() + ") must have USAGE_TEXTURE_BINDING!");
                }
                if (!viewAndSampler.sampler().isClosed()) continue;
                throw new IllegalStateException("Sampler for " + name + " (" + textureView.texture().getLabel() + ") has been closed!");
            }
            if (renderPass.pipeline.info().wantsDepthTexture() && !renderPass.hasDepthTexture()) {
                LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", (Object)renderPass.pipeline.info().getLocation());
            }
        } else {
            if (renderPass.pipeline == null) return false;
            if (renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
                return false;
            }
        }
        RenderPipeline pipeline = renderPass.pipeline.info();
        GlProgram glProgram = renderPass.pipeline.program();
        this.applyPipelineState(pipeline);
        boolean bl = differentProgram = this.lastProgram != glProgram;
        if (differentProgram) {
            GlStateManager._glUseProgram(glProgram.getProgramId());
            this.lastProgram = glProgram;
        }
        block11: for (Map.Entry<String, Uniform> entry : glProgram.getUniforms().entrySet()) {
            int target;
            int location;
            int n;
            int n2;
            Uniform uniform;
            String name = entry.getKey();
            boolean isDirty = renderPass.dirtyUniforms.contains(name);
            Objects.requireNonNull(entry.getValue());
            int n3 = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Uniform.Ubo.class, Uniform.Utb.class, Uniform.Sampler.class}, (Uniform)uniform, n3)) {
                default: {
                    throw new MatchException(null, null);
                }
                case 0: {
                    Uniform.Ubo ubo = (Uniform.Ubo)uniform;
                    try {
                        int n4 = n2 = ubo.blockBinding();
                    }
                    catch (Throwable throwable) {
                        throw new MatchException(throwable.toString(), throwable);
                    }
                }
                int blockBinding = n2;
                if (!isDirty) continue block11;
                GpuBufferSlice bufferView = renderPass.uniforms.get(name);
                GL33C.glBindBufferRange((int)35345, (int)blockBinding, (int)((GlBuffer)bufferView.buffer()).handle(), (long)bufferView.offset(), (long)bufferView.length());
                continue block11;
                case 1: {
                    GpuFormat format;
                    int samplerIndex;
                    int location2;
                    int n5;
                    Uniform.Utb utb = (Uniform.Utb)uniform;
                    {
                        GpuFormat gpuFormat;
                        int n6 = n5 = utb.location();
                        location2 = n5;
                        n6 = n5 = utb.samplerIndex();
                        samplerIndex = n5;
                        format = gpuFormat = utb.format();
                        n6 = n5 = utb.texture();
                    }
                    int texture = n5;
                    if (differentProgram || isDirty) {
                        GlStateManager._glUniform1i(location2, samplerIndex);
                    }
                    GlStateManager._activeTexture(33984 + samplerIndex);
                    GL33C.glBindTexture((int)35882, (int)texture);
                    if (!isDirty) continue block11;
                    GpuBufferSlice bufferView2 = renderPass.uniforms.get(name);
                    GL33C.glTexBuffer((int)35882, (int)GlConst.toGlInternalId(format), (int)((GlBuffer)bufferView2.buffer()).handle());
                    continue block11;
                }
                case 2: 
            }
            Uniform.Sampler sampler = (Uniform.Sampler)uniform;
            {
                int n7 = n = sampler.location();
                location = n;
                n7 = n = sampler.samplerIndex();
            }
            int samplerIndex = n;
            GlRenderPass.TextureViewAndSampler viewAndSampler = renderPass.samplers.get(name);
            if (viewAndSampler == null) continue;
            GlTextureView textureView = viewAndSampler.view();
            if (differentProgram || isDirty) {
                GlStateManager._glUniform1i(location, samplerIndex);
            }
            GlStateManager._activeTexture(33984 + samplerIndex);
            GlTexture texture = textureView.texture();
            if ((texture.usage() & 0x10) != 0) {
                target = 34067;
                GL33C.glBindTexture((int)34067, (int)texture.id);
            } else {
                target = 3553;
                GlStateManager._bindTexture(texture.id);
            }
            GL33C.glBindSampler((int)samplerIndex, (int)viewAndSampler.sampler().getId());
            GlStateManager._texParameter(target, 33084, textureView.baseMipLevel());
            GlStateManager._texParameter(target, 33085, textureView.baseMipLevel() + textureView.mipLevels() - 1);
        }
        renderPass.dirtyUniforms.clear();
        int[] drawBuffers = new int[renderPass.colorAttachmentCount];
        for (int i = 0; i < renderPass.colorAttachmentCount; ++i) {
            drawBuffers[i] = 36064 + i;
        }
        GL33C.glDrawBuffers((int[])drawBuffers);
        if (renderPass.isScissorEnabled()) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(renderPass.getScissorX(), renderPass.getScissorY(), renderPass.getScissorWidth(), renderPass.getScissorHeight());
            return true;
        }
        GlStateManager._disableScissorTest();
        return true;
    }

    private void applyPipelineState(RenderPipeline pipeline) {
        ColorTargetState state;
        int i;
        if (this.lastPipeline == pipeline) {
            return;
        }
        this.lastPipeline = pipeline;
        DepthStencilState depthStencilState = pipeline.getDepthStencilState();
        if (depthStencilState != null) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GlConst.toGl(depthStencilState.depthTest()));
            GlStateManager._depthMask(depthStencilState.writeDepth());
            if (depthStencilState.depthBiasConstant() != 0.0f || depthStencilState.depthBiasScaleFactor() != 0.0f) {
                GlStateManager._polygonOffset(depthStencilState.depthBiasScaleFactor(), depthStencilState.depthBiasConstant());
                GlStateManager._enablePolygonOffset();
            } else {
                GlStateManager._disablePolygonOffset();
            }
        } else {
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
            GlStateManager._disablePolygonOffset();
        }
        if (pipeline.isCull()) {
            GlStateManager._enableCull();
        } else {
            GlStateManager._disableCull();
        }
        @Nullable ColorTargetState[] colorTargetStates = pipeline.getColorTargetStates();
        for (i = 0; i < colorTargetStates.length; ++i) {
            state = colorTargetStates[i];
            if (state == null) continue;
            if (state.blendFunction().isPresent()) {
                GlStateManager._enableBlend(i);
                BlendFunction blendFunction = state.blendFunction().get();
                GlStateManager._blendFuncSeparate(GlConst.toGl(blendFunction.color().sourceFactor()), GlConst.toGl(blendFunction.color().destFactor()), GlConst.toGl(blendFunction.alpha().sourceFactor()), GlConst.toGl(blendFunction.alpha().destFactor()));
                GlStateManager._blendEquationSeparate(GlConst.toGl(blendFunction.color().op()), GlConst.toGl(blendFunction.alpha().op()));
                continue;
            }
            GlStateManager._disableBlend(i);
        }
        GlStateManager._polygonMode(1032, GlConst.toGl(pipeline.getPolygonMode()));
        for (i = 0; i < colorTargetStates.length; ++i) {
            state = colorTargetStates[i];
            if (state == null) continue;
            GlStateManager._colorMask(i, state.writeMask());
        }
    }

    @Override
    public void submitRenderPass() {
        GlStateManager._glBindFramebuffer(36160, 0);
        this.device.debugLabels().popDebugGroup();
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {
        ((GlQueryPool)pool).writeTimestamp(index);
    }
}

