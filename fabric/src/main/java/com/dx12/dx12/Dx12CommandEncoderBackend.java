package com.dx12.dx12;

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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

/**
 * D3D12 {@link CommandEncoderBackend}, mirroring the official
 * {@code VulkanCommandEncoder}.
 *
 * The native context owns two command allocators + a fence. {@link #submit()}
 * executes the recorded command list, signals the fence, and waits for the
 * value-2 completion (vanilla double-buffered submit).
 *
 * P3 scope: submit / copy / clear / render-pass lifecycle / timestamp queries.
 * Draw commands arrive with the P4 pipeline layer.
 */
@Environment(EnvType.CLIENT)
public class Dx12CommandEncoderBackend implements CommandEncoderBackend {
    private final long ctx;
    private final @Nullable Dx12Device device;
    private final Dx12TransientMemory transientMemory;
    private final List<Runnable> pendingCallbacks = new ArrayList<>();
    private @Nullable Dx12RenderPassBackend currentRenderPass;

    public Dx12CommandEncoderBackend() {
        this(null);
    }

    public Dx12CommandEncoderBackend(@Nullable Dx12Device device) {
        this.device = device;
        this.ctx = Dx12Native.dx12CreateCommandEncoder();
        if (this.ctx == 0) {
            throw new IllegalStateException("dx12CreateCommandEncoder returned a null handle");
        }
        this.transientMemory = new Dx12TransientMemory(this.ctx);
        Dx12Native.dx12BeginCommandList(this.ctx);
    }

    /** Native CommandContext* handle (used by surface blit + render pass). */
    long nativeHandle() {
        return this.ctx;
    }

    private static long textureHandle(GpuTexture texture) {
        return ((Dx12GpuTexture) texture).handle();
    }

    private static long bufferHandle(GpuBuffer buffer) {
        return ((Dx12GpuBuffer) buffer).handle();
    }

    // -----------------------------------------------------------------------
    // Transient memory
    // -----------------------------------------------------------------------

    @Override
    public TransientMemory transientMemory() {
        return this.transientMemory;
    }

    // -----------------------------------------------------------------------
    // Render pass
    // -----------------------------------------------------------------------

    @Override
    public RenderPassBackend createRenderPass(RenderPassDescriptor descriptor) {
        List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments =
            descriptor.colorAttachments();
        int colorCount = colorAttachments.size();
        long[] colorTextures = new long[colorCount];
        byte[] colorClearFlags = new byte[colorCount];
        float[] clearColors = new float[colorCount * 4];
        for (int i = 0; i < colorCount; ++i) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = colorAttachments.get(i);
            if (attachment == null) {
                colorTextures[i] = 0L;  // withUnusedColorAttachment
                continue;
            }
            GpuTextureView view = attachment.textureView();
            colorTextures[i] = ((Dx12GpuTexture) view.texture()).handle();
            if (attachment.clearValue().isPresent()) {
                Vector4fc color = attachment.clearValue().get();
                colorClearFlags[i] = 1;
                clearColors[i * 4] = color.x();
                clearColors[i * 4 + 1] = color.y();
                clearColors[i * 4 + 2] = color.z();
                clearColors[i * 4 + 3] = color.w();
            }
        }

        long depthTexture = 0L;
        byte depthClearFlag = 0;
        double depthClearValue = 0.0;
        RenderPassDescriptor.Attachment<OptionalDouble> depthAttachment = descriptor.depthAttachment();
        if (depthAttachment != null) {
            depthTexture = ((Dx12GpuTexture) depthAttachment.textureView().texture()).handle();
            if (depthAttachment.clearValue().isPresent()) {
                depthClearFlag = 1;
                depthClearValue = depthAttachment.clearValue().getAsDouble();
            }
        }

        RenderPass.RenderArea area = descriptor.renderArea;
        int x = 0, y = 0, w = 0, h = 0;
        if (area != null) {
            x = area.x();
            y = area.y();
            w = area.width();
            h = area.height();
        } else {
            // Fall back to the first attachment's size (vanilla asserts non-null).
            if (colorCount > 0) {
                GpuTextureView view = colorAttachments.get(0).textureView();
                w = view.getWidth(0);
                h = view.getHeight(0);
            }
        }

        Dx12Native.dx12BeginRenderPass(this.ctx, colorTextures, colorClearFlags, clearColors,
            depthTexture, depthClearFlag, depthClearValue, x, y, w, h);
        boolean hasDepth = depthTexture != 0L;
        Dx12RenderPassBackend pass = new Dx12RenderPassBackend(this.device, this.ctx,
            area, w, h, hasDepth);
        this.currentRenderPass = pass;
        return pass;
    }

    @Override
    public void submitRenderPass() {
        Dx12Native.dx12EndRenderPass(this.ctx);
        this.currentRenderPass = null;
    }

    // -----------------------------------------------------------------------
    // Clears
    // -----------------------------------------------------------------------

    @Override
    public void clearColorTexture(GpuTexture colorTexture, Vector4fc clearColor) {
        Dx12Native.dx12ClearColorTexture(this.ctx, textureHandle(colorTexture),
            clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w());
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor,
        GpuTexture depthTexture, double clearDepth) {
        Dx12Native.dx12ClearColorTexture(this.ctx, textureHandle(colorTexture),
            clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w());
        Dx12Native.dx12ClearDepthTexture(this.ctx, textureHandle(depthTexture), clearDepth);
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor,
        GpuTexture depthTexture, double clearDepth, int regionX, int regionY,
        int regionWidth, int regionHeight) {
        // P3 simplification: the full-texture clears below ignore the region.
        this.clearColorAndDepthTextures(colorTexture, clearColor, depthTexture, clearDepth);
    }

    @Override
    public void clearDepthTexture(GpuTexture depthTexture, double clearDepth) {
        Dx12Native.dx12ClearDepthTexture(this.ctx, textureHandle(depthTexture), clearDepth);
    }

    // -----------------------------------------------------------------------
    // Copies
    // -----------------------------------------------------------------------

    @Override
    public void writeToBuffer(GpuBufferSlice destination, ByteBuffer data) {
        GpuBufferSlice staging = this.transientMemory.uploadStaging(data, 1,
            GpuBuffer.USAGE_COPY_SRC);
        Dx12Native.dx12CopyBuffer(this.ctx, bufferHandle(staging.buffer()), staging.offset(),
            bufferHandle(destination.buffer()), destination.offset(), data.remaining());
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        Dx12Native.dx12CopyBuffer(this.ctx, bufferHandle(source.buffer()), source.offset(),
            bufferHandle(target.buffer()), target.offset(), source.length());
    }

    @Override
    public void writeToTexture(GpuTexture destination, ByteBuffer source, int mipLevel,
        int depthOrLayer, int destX, int destY, int width, int height) {
        GpuBufferSlice staging = this.transientMemory.uploadStaging(source, 1,
            GpuBuffer.USAGE_COPY_SRC);
        Dx12Native.dx12WriteToTexture(this.ctx, bufferHandle(staging.buffer()), staging.offset(),
            width, height, textureHandle(destination), mipLevel, depthOrLayer, destX, destY);
    }

    @Override
    public void copyBufferToTexture(GpuBufferSlice source, int sourceX, int sourceY,
        int sourceWidth, int sourceHeight, GpuTexture destination, int destinationX,
        int destinationY, int copyWidth, int copyHeight, int mipLevel, int arrayLayer) {
        int texelSize = destination.getFormat().blockSize();
        long skipTexels = (long) sourceX + (long) sourceY * sourceWidth;
        long skipBytes = skipTexels * texelSize;
        Dx12Native.dx12CopyBufferToTexture(this.ctx, bufferHandle(source.buffer()),
            source.offset() + skipBytes, sourceWidth, sourceHeight,
            textureHandle(destination), mipLevel, arrayLayer,
            destinationX, destinationY, copyWidth, copyHeight);
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset,
        Runnable callback, int mipLevel) {
        this.copyTextureToBuffer(source, destination, offset, callback, mipLevel,
            0, 0, source.getWidth(mipLevel), source.getHeight(mipLevel));
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset,
        Runnable callback, int mipLevel, int x, int y, int width, int height) {
        Dx12Native.dx12CopyTextureToBuffer(this.ctx, textureHandle(source), mipLevel, 0,
            x, y, width, height, bufferHandle(destination), offset);
        // Run the callback after the next submit (mirror of the destroyQueue rotate).
        this.pendingCallbacks.add(callback);
    }

    @Override
    public void copyTextureToTexture(GpuTexture source, GpuTexture destination, int mipLevel,
        int destX, int destY, int sourceX, int sourceY, int width, int height) {
        Dx12Native.dx12CopyTextureToTexture(this.ctx, textureHandle(source),
            textureHandle(destination), mipLevel, 0, sourceX, sourceY, destX, destY, width, height);
    }

    // -----------------------------------------------------------------------
    // Fences & timestamps
    // -----------------------------------------------------------------------

    @Override
    public GpuFence createFence() {
        // 官方 VulkanCommandEncoder.createFence()：捕获当前 submit index，fence
        // 在该 encoder 的下一次提交完成后完成。官方 createCommandEncoder() 返回
        // 共享 encoder，因此一次性 encoder 上创建的 fence token（queueFencedTask /
        // StagedVertexBuffer endFrame / MappableRingBuffer rotate）也随下一次提交
        // 完成。D3D12 侧用设备级队列 fence 复现该语义：目标 = 当前 queueFenceValue
        // + 1，任何 ctx 的下一次 submit 都会推进它（见 dx12WaitForFence）。
        long fenceValue = Dx12Native.dx12GetFenceValue(this.ctx);
        long target = fenceValue + 1;
        return new GpuFence() {
            private boolean completed;

            @Override
            public boolean awaitCompletion(long timeoutMs) {
                if (!this.completed) {
                    long timeoutNs = timeoutMs > Long.MAX_VALUE / 1_000_000L
                        ? Long.MAX_VALUE
                        : timeoutMs * 1_000_000L;
                    this.completed = Dx12Native.dx12WaitForFence(
                        Dx12CommandEncoderBackend.this.ctx, target, timeoutNs);
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
        Dx12Native.dx12WriteTimestamp(this.ctx, ((Dx12GpuQueryPool) pool).nativeHandle(), index);
    }

    // -----------------------------------------------------------------------
    // Submit lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void submit() {
        Dx12Native.dx12Submit(this.ctx);
        this.transientMemory.rotate();
        // Run callbacks queued by the previous frame's copyTextureToBuffer.
        List<Runnable> run = this.pendingCallbacks;
        this.pendingCallbacks.clear();
        for (Runnable callback : run) {
            callback.run();
        }
        Dx12Native.dx12BeginCommandList(this.ctx);
    }

    public void close() {
        if (this.currentRenderPass != null) {
            Dx12Native.dx12EndRenderPass(this.ctx);
            this.currentRenderPass = null;
        }
        Dx12Native.dx12EndCommandList(this.ctx);
        // 必须先销毁命令上下文（其内部等待本 ctx 所有已提交命令完成）再关闭瞬时缓冲：
        // createBuffer(data) 等一次性 encoder 在 submit 后立即 close，若先释放
        // staging/gpu buffer，GPU 可能仍在使用它们（资源飞行中释放）→ 驱动延迟错误
        // DXGI_ERROR_DEVICE_REMOVED（下一处 CreateCommittedResource 爆发）或调试层
        // 致命异常。等待后再释放则安全。
        Dx12Native.dx12DestroyCommandEncoder(this.ctx);
        this.transientMemory.close();
    }
}
