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
    /** P27：图集合成 pass 结束后 dump 图集纹理（定位按钮纹理错乱）。 */
    private static final int MAX_ATLAS_DUMPS = 14;
    private static final java.util.Set<Long> gDumpedAtlas = new java.util.HashSet<>();
    /**
     * P27: 按 ctx 记录待 submit 后 dump 的图集 pass。
     * 必须按 ctx 分组 + 只消费自己 ctx 的列表：atlas 上传（uploadInitialContents）在
     * 一次性 encoder 上连续创建多个 blit/interpolate pass，若用单一 pending 会被后续
     * pass 覆盖；若用全局 static 列表会被其它 encoder 的 submit() 抢消费（此时本 ctx
     * 命令尚未提交 GPU，读回全 0）。submit() 里 dx12Submit 提交 GPU 后执行 dump，
     * deviceWaitIdle 才能读到真实内容。
     */
    private static final java.util.Map<Long, java.util.List<Dx12RenderPassBackend>>
        gPendingAtlasByCtx = new java.util.HashMap<>();

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
        int[] colorMips = new int[colorCount];
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
            // P3b：把 view 的 base mip 传给 native RTV（TextureAtlas 对 mipViews[level]
            // 逐级上传；此前 RTV 恒绑 mip0 → 图集 mip1+ 从未写入，远处大 LOD 采样
            // 到未初始化内容）。
            if (view instanceof Dx12GpuTextureView dx12View) {
                colorMips[i] = dx12View.baseMip();
            }
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
        int depthMip = 0;
        byte depthClearFlag = 0;
        double depthClearValue = 0.0;
        RenderPassDescriptor.Attachment<OptionalDouble> depthAttachment = descriptor.depthAttachment();
        if (depthAttachment != null) {
            GpuTextureView depthView = depthAttachment.textureView();
            depthTexture = ((Dx12GpuTexture) depthView.texture()).handle();
            if (depthView instanceof Dx12GpuTextureView dx12View) {
                depthMip = dx12View.baseMip();
            }
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

        Dx12Native.dx12BeginRenderPass(this.ctx, colorTextures, colorMips,
            colorClearFlags, clearColors, depthTexture, depthMip,
            depthClearFlag, depthClearValue, x, y, w, h);
        boolean hasDepth = depthTexture != 0L;
        // P31：GUI 离屏渲染判别——颜色附件 usage==13（COPY_DST|TEXTURE_BINDING|
        // RENDER_ATTACHMENT，无 COPY_SRC）且带深度附件。GuiItemAtlas / PIP 实体纹理
        // 满足；Lightmap 同为 usage=13 但无深度附件（排除）；MainTarget/RenderTarget
        // 为 usage=15（含 COPY_SRC，排除）。命中则该 pass 内所有管线选 Y-flip 变体。
        boolean flipY = false;
        if (hasDepth && colorCount > 0) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> first = colorAttachments.get(0);
            if (first != null && first.textureView() != null
                && ((Dx12GpuTexture) first.textureView().texture()).usage() == 13) {
                flipY = true;
            }
        }
        if (flipY && Dx12Native.LOG_VERBOSE) {
            System.err.println("[dx12-java] [P31] flipY render pass: "
                + w + 'x' + h + " area=" + x + ',' + y
                + " colorTex=0x" + Long.toHexString(colorTextures[0]));
            System.err.flush();
        }
        // P6 诊断：打印 pass 尺寸 + Java 调用来源（P29：getStackTrace() 开销大，
        // 仅 DX12_LOG_VERBOSE=1 时输出，避免图集上传时每帧数百次堆栈遍历）。
        if (Dx12Native.LOG_VERBOSE) {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("createRenderPass: ").append(w).append('x').append(h)
                .append(" area=").append(x).append(',').append(y)
                .append(hasDepth ? " depth=yes" : " depth=no").append(" from:");
            int shown = 0;
            for (int i = 3; i < st.length && shown < 6; ++i) {
                String cn = st[i].getClassName();
                int dot = cn.lastIndexOf('.');
                sb.append(' ').append(dot >= 0 ? cn.substring(dot + 1) : cn)
                    .append('.').append(st[i].getMethodName());
                shown++;
            }
            System.err.println("[dx12-java] " + sb);
            System.err.flush();
        }
        Dx12RenderPassBackend pass = new Dx12RenderPassBackend(this.device, this.ctx,
            area, w, h, hasDepth, colorCount > 0 ? colorTextures[0] : 0L, flipY);
        this.currentRenderPass = pass;
        return pass;
    }

    @Override
    public void submitRenderPass() {
        Dx12RenderPassBackend pass = this.currentRenderPass;
        Dx12Native.dx12EndRenderPass(this.ctx);
        this.currentRenderPass = null;
        // P27 诊断：animate_sprite_blit / interpolate 图集合成 pass 结束 → 记录待 dump 的 pass。
        // 注意：dx12EndRenderPass 只在命令列表上记录 draw，GPU 要等 submit() 的
        // dx12Submit 才真正执行。因此 dump 必须推迟到 submit() 之后执行，否则
        // dbgReadbackTexturePixels 的 deviceWaitIdle 等待不到未提交的命令，读回全 0。
        // P29：dump 含 deviceWaitIdle，整条 P27 链路仅在 DX12_LOG_VERBOSE=1 时启用。
        if (Dx12Native.LOG_VERBOSE && pass != null && gDumpedAtlas.size() < MAX_ATLAS_DUMPS) {
            String loc = pass.pipelineLocation();
            if (loc != null && (loc.contains("animate") || loc.contains("sprite"))) {
                gPendingAtlasByCtx.computeIfAbsent(this.ctx, k -> new ArrayList<>()).add(pass);
                System.err.println("[dx12-java] P27 pending atlas dump ctx=0x"
                    + Long.toHexString(this.ctx)
                    + " pass=" + loc + " colorTex=0x" + Long.toHexString(pass.colorTargetHandle())
                    + " area=" + pass.areaX() + "," + pass.areaY()
                    + " " + pass.areaWidth() + "x" + pass.areaHeight());
                System.err.flush();
            }
        }
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
        // P3b fix：GuiItemAtlas 槽位 STALE 重绘前只清该槽位矩形区域。原实现忽略
        // region 整图清空 → 滚动/翻页触发任意槽位重绘时把整张物品图集抹掉，其它
        // 已烘好的槽位图标（图集区域仍标记有效、不再触发重绘）随之消失。
        Dx12Native.dx12ClearColorTextureRegion(this.ctx, textureHandle(colorTexture),
            clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w(),
            regionX, regionY, regionWidth, regionHeight);
        Dx12Native.dx12ClearDepthTextureRegion(this.ctx, textureHandle(depthTexture),
            clearDepth, regionX, regionY, regionWidth, regionHeight);
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
        // 与官方 VulkanCommandEncoder.writeToBuffer 对齐：直接上传，不做 NaN 检测/替换。
        // 若数据含 NaN（例如投影矩阵 near/far 异常），应在源头修复，而非在上传时暴力覆盖。
        // 暴力替换会把整块顶点数据也错误地覆写，导致黑屏（P7 根因定位）。
        GpuBufferSlice staging = this.transientMemory.uploadStaging(data, 1,
            GpuBuffer.USAGE_COPY_SRC);
        Dx12Native.dx12CopyBuffer(this.ctx, bufferHandle(staging.buffer()), staging.offset(),
            bufferHandle(destination.buffer()), destination.offset(), data.remaining());
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        // P22 诊断：记录 copyToBuffer 参数，排查缓冲区大小不足
        if (System.err instanceof java.io.PrintStream) {
            System.err.printf("[dx12-java] copyToBuf srcBuf=%x srcOff=%d srcLen=%d dstBuf=%x dstOff=%d%n",
                bufferHandle(source.buffer()), (int)source.offset(), (int)source.length(),
                bufferHandle(target.buffer()), (int)target.offset());
            System.err.flush();
        }
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
        // P15 诊断：记录提交前的 fence 值，用于排查命令未提交/未完成
        long fenceBefore = Dx12Native.dx12GetFenceValue(this.ctx);
        Dx12Native.dx12Submit(this.ctx);
        this.transientMemory.rotate();
        // P27: dx12Submit 已提交 GPU → 现在读回图集才是真实内容（dbgReadbackTexturePixels
        // 内部 deviceWaitIdle 会等待刚提交的命令完成）。只消费本 ctx 的 pending 列表：
        // 别的 encoder 记录的 blit pass 命令尚未提交 GPU，读回全 0（纯黑假象）。
        // 按 color target handle 去重，最多 dump MAX_ATLAS_DUMPS 个不同图集。
        // P29：dump 含 deviceWaitIdle，仅 verbose 模式启用。
        if (Dx12Native.LOG_VERBOSE) {
            List<Dx12RenderPassBackend> atlasPasses = gPendingAtlasByCtx.remove(this.ctx);
            if (atlasPasses != null && !atlasPasses.isEmpty()) {
                for (Dx12RenderPassBackend dumpPass : atlasPasses) {
                    long h = dumpPass.colorTargetHandle();
                    if (!gDumpedAtlas.contains(h) && gDumpedAtlas.size() < MAX_ATLAS_DUMPS) {
                        gDumpedAtlas.add(h);
                        System.err.println("[dx12-java] P27 dump atlas (after submit) ctx=0x"
                            + Long.toHexString(this.ctx)
                            + " pass=" + dumpPass.pipelineLocation()
                            + " size=" + dumpPass.outputWidth() + "x" + dumpPass.outputHeight()
                            + " lastArea=" + dumpPass.areaX() + "," + dumpPass.areaY()
                            + " " + dumpPass.areaWidth() + "x" + dumpPass.areaHeight()
                            + " colorTex=0x" + Long.toHexString(h));
                        System.err.flush();
                        Dx12Native.dx12DumpTextureToFile(h,
                            "atlas_" + dumpPass.outputWidth() + "x" + dumpPass.outputHeight()
                            + "_" + Long.toHexString(h & 0xFFFF));
                    }
                }
            }
        }
        // Run callbacks queued by the previous frame's copyTextureToBuffer.
        List<Runnable> run = this.pendingCallbacks;
        this.pendingCallbacks.clear();
        for (Runnable callback : run) {
            callback.run();
        }
        Dx12Native.dx12BeginCommandList(this.ctx);
        // P15: 每 30 帧打印一次 submit 摘要（P29：仅 verbose）
        if (Dx12Native.LOG_VERBOSE && (fenceBefore % 30L) == 0) {
            System.err.println("[dx12-java] submit: frame=" + fenceBefore
                + " ctx=" + Long.toHexString(this.ctx));
            System.err.flush();
        }
    }

    public void close() {
        if (Dx12Native.LOG_VERBOSE) {
            System.err.println("[dx12-java] close: begin");
            System.err.flush();
        }
        if (this.currentRenderPass != null) {
            Dx12Native.dx12EndRenderPass(this.ctx);
            this.currentRenderPass = null;
        }
        Dx12Native.dx12EndCommandList(this.ctx);
        // 共享 encoder（device != null）由 Dx12Device.close() 负责销毁 CommandContext，
        // 此处不调用 dx12DestroyCommandEncoder，避免 CubeMap.render() 等内部调用
        // close() 时意外销毁共享 ctx 导致后续渲染使用悬空指针。
        // 临时/一次性 encoder（device == null，如 createBuffer(data)）仍调用
        // dx12DestroyCommandEncoder，保证资源在 fence 等待后安全释放。
        if (this.device == null) {
            Dx12Native.dx12DestroyCommandEncoder(this.ctx);
            if (Dx12Native.LOG_VERBOSE) {
                System.err.println("[dx12-java] close: after destroyCommandEncoder");
                System.err.flush();
            }
        }
        this.transientMemory.close();
        if (Dx12Native.LOG_VERBOSE) {
            System.err.println("[dx12-java] close: after transientMemory.close");
            System.err.flush();
        }
    }
}
