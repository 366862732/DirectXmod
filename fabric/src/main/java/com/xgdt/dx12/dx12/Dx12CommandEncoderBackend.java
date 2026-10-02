package com.xgdt.dx12.dx12;

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
    /**
     * P42：标记当前 submit() 走的是 ASYNC 路径。
     * P33 fix：命令列表的 begin / allocator reset 现在由**主线程**在帧开始时负责
     * （{@link #ensureListOpen()} → dx12BeginCommandList，内部按 fenceValue-2 等待
     * allocator 不再被 GPU 占用）。渲染线程只做 Close + ExecuteCommandLists + Present，
     * 因此不会与主线程的录制竞争同一 allocator。
     */
    private boolean inAsyncSubmit = false;
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

    /** P38 诊断：submit()（共享 encoder = 每帧）后读回 16×16 lightmap，验证内容与朝向。 */
    private static final int MAX_LIGHTMAP_DUMPS = 5;
    private static final int LIGHTMAP_DUMP_INTERVAL = 120;
    private static int dx12DebugLightmapTick = 0;
    private static int dx12DebugLightmapDumps = 0;

    // -----------------------------------------------------------------------
    // P3 帧时间插桩（仅 DX12_PROF=1 时启用，正常游玩零开销）
    // 目的：量化 submit() 内部各同步阶段（RECORDING_READY 握手 / 等提交完成）
    // 与帧间隔的耗时，用于判断多帧飞行 FrameManager 的实际可回收收益。
    // -----------------------------------------------------------------------
    private static final int PROF_INTERVAL = 300;
    private static long gProfAsyncFrames = 0;
    private static long gProfLastSubmitNs = 0;
    private static long gProfIntervalNs = 0;
    private static long gProfRecordingReadyNs = 0;
    private static long gProfWaitSubmitNs = 0;
    private static long gProfTotalNs = 0;

    public Dx12CommandEncoderBackend() {
        this(null);
    }

    public Dx12CommandEncoderBackend(@Nullable Dx12Device device) {
        this.device = device;
        this.ctx = Dx12Native.dx12CreateCommandEncoder();
        if (this.ctx == 0) {
            throw new IllegalStateException("dx12CreateCommandEncoder returned a null handle");
        }
        this.transientMemory = new Dx12TransientMemory(this.ctx, this::ensureListOpen);
        // P33 fix：在构造函数中打开命令列表。beginCommandList 现在是幂等的
        // （C++ 侧若 listOpen=1 则跳过 Reset），因此 submit() 可安全再次调用而
        // 不会触发 allocator Reset E_FAIL。这确保了 self-test 等一次性路径的
        // recording 方法（writeToBuffer 等）能在 listOpen=1 的状态下执行。
        Dx12Native.dx12BeginCommandList(this.ctx);
    }

    /** Native CommandContext* handle (used by surface blit + render pass). */
    long nativeHandle() {
        return this.ctx;
    }

    /**
     * P33：懒加载打开命令列表。dx12BeginCommandList 是幂等的（C++ 侧若 listOpen=1
     * 则跳过 Reset），因此重复调用安全；列表未打开时会执行 allocator reset（内部按
     * fenceValue-2 等待 GPU 释放该 allocator）并 Reset 命令列表，随后录制命令。
     */
    private void ensureListOpen() {
        if (!Dx12Native.dx12IsListOpen(this.ctx)) {
            // P33 fix：ASYNC 路径下 begin/Reset 由主线程负责（渲染线程不再触碰
            // allocator/list），因此这里直接打开即可，不存在与渲染线程竞争的问题。
            Dx12Native.dx12BeginCommandList(this.ctx);
        }
    }

    private static long textureHandle(GpuTexture texture) {
        return ((Dx12GpuTexture) texture).handle();
    }

    private static long bufferHandle(GpuBuffer buffer) {
        return ((Dx12GpuBuffer) buffer).handle();
    }

    /** P3 插桩：纳秒 -> 毫秒，保留两位小数。 */
    private static double profMs(double ns) {
        return Math.round(ns * 1e-6 * 100.0) / 100.0;
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

        // P33：确保命令列表已打开
        this.ensureListOpen();
        Dx12Native.dx12BeginRenderPass(this.ctx, colorTextures, colorMips,
            colorClearFlags, clearColors, depthTexture, depthMip,
            depthClearFlag, depthClearValue, x, y, w, h);
        boolean hasDepth = depthTexture != 0L;
        // P28/P31：识别「离屏图集合成 pass」（GuiItemAtlas / PictureInPicture），其特征是
        // color[0] 的 usage == 13 = COPY_DST|TEXTURE_BINDING|RENDER_ATTACHMENT（GpuTexture 位常量
        // 1|4|8）且带 depth attachment（color depth usage==9，lightmap 则无 depth）。
        // 这类 pass 用 invertY=true 的正交投影渲染，其内容随后以 GL 自底向上的 UV 约定采样，
        // 在 D3D12（NDC Y 向上）下需要 flipY 变体管线（shader 注入 gl_Position.y 取反）修正。
        //
        // 注意：绝不能用 usage == 15（=COPY_DST|COPY_SRC|TEXTURE_BINDING|RENDER_ATTACHMENT，主窗口
        // MainTarget / TextureAtlas 的通用特征）。此前误用 15 导致每帧主窗口 pass 被判定为
        // 「离屏图集 pass」，对 item_cutout/entity_cutout 注入 Y 翻转并翻转 scissor，使 GUI 图标
        // 镜像/错位、按钮底图裁剪区错乱（实测日志中 854x480 主 pass 命中 4920 次）。
        boolean flipY = false;
        if (hasDepth && colorCount > 0) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> first = colorAttachments.get(0);
            if (first != null && first.textureView() != null) {
                int usage = ((Dx12GpuTexture) first.textureView().texture()).usage();
                if (usage == 13) {
                    flipY = true;
                    System.err.println("[dx12-java] [P28] flipY=true for offscreen GUI atlas/PIP pass ("
                        + w + "x" + h + ")");
                    System.err.flush();
                }
            }
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
        // P33：确保命令列表已打开
        this.ensureListOpen();
        Dx12Native.dx12ClearColorTexture(this.ctx, textureHandle(colorTexture),
            clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w());
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor,
        GpuTexture depthTexture, double clearDepth) {
        // P33：确保命令列表已打开
        this.ensureListOpen();
        Dx12Native.dx12ClearColorTexture(this.ctx, textureHandle(colorTexture),
            clearColor.x(), clearColor.y(), clearColor.z(), clearColor.w());
        Dx12Native.dx12ClearDepthTexture(this.ctx, textureHandle(depthTexture), clearDepth);
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor,
        GpuTexture depthTexture, double clearDepth, int regionX, int regionY,
        int regionWidth, int regionHeight) {
        // P33：确保命令列表已打开
        this.ensureListOpen();
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
        // P33：确保命令列表已打开
        this.ensureListOpen();
        Dx12Native.dx12ClearDepthTexture(this.ctx, textureHandle(depthTexture), clearDepth);
    }

    // -----------------------------------------------------------------------
    // Copies
    // -----------------------------------------------------------------------

    @Override
    public void writeToBuffer(GpuBufferSlice destination, ByteBuffer data) {
        // P33：确保命令列表已打开
        this.ensureListOpen();
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
        // P33：确保命令列表已打开
        this.ensureListOpen();
        // P22 诊断：记录 copyToBuffer 参数，排查缓冲区大小不足（P29：仅 verbose）。
        // 注意：copyToBuffer 由图集/区块/实体的 StagingBuffer 上传路径每帧调用数十次，
        // 且 System.err.printf 会绕过 log4j 直写控制台，无条件打印会造成每帧同步 I/O
        // （P53 采样中表现为 writeBytes 热点）——必须门控。
        if (Dx12Native.LOG_VERBOSE) {
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
        // P33：确保命令列表已打开
        this.ensureListOpen();
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
        // P33：确保命令列表已打开
        this.ensureListOpen();
        Dx12Native.dx12CopyTextureToBuffer(this.ctx, textureHandle(source), mipLevel, 0,
            x, y, width, height, bufferHandle(destination), offset);
        // Run the callback after the next submit (mirror of the destroyQueue rotate).
        this.pendingCallbacks.add(callback);
    }

    @Override
    public void copyTextureToTexture(GpuTexture source, GpuTexture destination, int mipLevel,
        int destX, int destY, int sourceX, int sourceY, int width, int height) {
        // P33：确保命令列表已打开
        this.ensureListOpen();
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
        // P33：确保命令列表已打开
        this.ensureListOpen();
        Dx12Native.dx12WriteTimestamp(this.ctx, ((Dx12GpuQueryPool) pool).nativeHandle(), index);
    }

    // -----------------------------------------------------------------------
    // Submit lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void submit() {
        // P33 async：独立渲染线程流水线
        //
        // 流程：
        //   1. 记录当前 queue fence 值（= 上一帧的 submitQueueFence）
        //   2. 请求渲染线程开始新帧（通知它主线程已录制完毕）
        //   3. 等待渲染线程发 RECORDING_READY
        //   4. 主线程继续录制命令（render pass / clear / copy / draw）
        //   5. 通知渲染线程命令已就绪（set gEvtCommandsReady）
        //   6. 等待渲染线程完成提交 + present（submits + signal fence + present）
        //   7. 执行 post-submit 清理（transientMemory rotate、callback 等）
        //
        // P33 fix（帧序倒置修复）：allocator Reset + command list begin 由**主线程**
        //       在帧开始时完成（ensureListOpen → dx12BeginCommandList）。渲染线程
        //       只负责 Close + ExecuteCommandLists + Present，绝不再 Reset 列表——
        //       否则会丢弃主线程已录制的整帧命令，back buffer 永远不被写入，
        //       Present 只能显示未初始化内容（窗口闪烁各种颜色的根因）。

        // P15 诊断：记录提交前的 fence 值
        long fenceBefore = Dx12Native.dx12GetFenceValue(this.ctx);

        // 步骤 2：请求渲染线程开始新帧
        boolean asyncStarted = Dx12Native.dx12AsyncRenderBeginFrame(this.ctx);
        if (!asyncStarted) {
            if (Dx12Native.LOG_VERBOSE) {
                System.err.println("[dx12-java] submit: SYNC fallback (no active surface or previous frame pending) fence=" + fenceBefore);
                System.err.flush();
            }
            // 无 active surface（初始化阶段或窗口未创建），回退到同步路径
            // 同步路径：确保命令列表已打开（幂等：已打开则跳过 Reset），录制命令、提交
            Dx12Native.dx12BeginCommandList(this.ctx);
            Dx12Native.dx12Submit(this.ctx);
            this.transientMemory.rotate();
            Dx12Device.dumpDebugLightmap();
            List<Runnable> run = this.pendingCallbacks;
            this.pendingCallbacks.clear();
            for (Runnable callback : run) { callback.run(); }
            return;
        }

        // 异步路径：命令已由上层框架在本次 submit() 之前录制完毕，
        // 渲染线程只负责 Close + Execute + Present。
        // 注意：不在此调用 dx12BeginCommandList（列表已由录制阶段打开）。
        this.inAsyncSubmit = true;
        if (Dx12Native.LOG_VERBOSE) {
            System.err.println("[dx12-java] submit: ASYNC path fence=" + fenceBefore);
            System.err.flush();
        }

        // P3 插桩：记录本帧起点与相对上一帧的间隔（仅 DX12_PROF=1 生效）。
        final boolean prof = Dx12Native.PROF;
        final long profStart = System.nanoTime();
        if (prof) {
            // 累加而非覆盖：否则只统计到最后一个间隔。
            if (gProfLastSubmitNs != 0) gProfIntervalNs += profStart - gProfLastSubmitNs;
            gProfLastSubmitNs = profStart;
        }

        // 步骤 3：等待渲染线程到达 RECORDING_READY。
        // 用 native 阻塞等待（WaitForSingleObject）取代原 sleep(1) 轮询：后者每帧在
        // 帧关键路径上引入约 1ms 的调度器休眠开销；阻塞等待为微秒级。超时保护不变。
        long profT0 = prof ? System.nanoTime() : 0L;
        boolean recordingReady = Dx12Native.dx12AsyncRenderWaitRecordingReady(this.ctx, 5_000L);
        if (prof) gProfRecordingReadyNs += System.nanoTime() - profT0;
        if (!recordingReady) {
            // P41：渲染线程卡死，降级到 SYNC 模式避免整个游戏崩溃
            System.err.println("[dx12] [P41] asyncRenderWaitRecordingReady timeout after 5000ms — falling back to SYNC, ctx=0x"
                + Long.toHexString(this.ctx));
            System.err.flush();
            // 清除 gAsyncRenderCtx，防止下一个 asyncBeginFrame 被 "previous frame not complete" 拒绝
            Dx12Native.dx12ClearAsyncRenderCtx();
            Dx12Native.dx12BeginCommandList(this.ctx);
            Dx12Native.dx12Submit(this.ctx);
            this.transientMemory.rotate();
            List<Runnable> run = this.pendingCallbacks;
            this.pendingCallbacks.clear();
            for (Runnable callback : run) { callback.run(); }
            return;
        }

        // 步骤 4：命令已由上层框架在此 submit() 之前录制完毕（render pass、clear、copy、draw）
        //         无需额外操作。

        // 步骤 5：通知渲染线程所有命令已入队
        Dx12Native.dx12AsyncSendCommandsReady(this.ctx);

        // 步骤 6：等待渲染线程完成提交 + present
        long profT1 = prof ? System.nanoTime() : 0L;
        boolean completed = Dx12Native.dx12AsyncRenderWaitComplete(this.ctx, 10000);
        if (prof) gProfWaitSubmitNs += System.nanoTime() - profT1;
        if (!completed) {
            System.err.println("[dx12] [P33] asyncRenderWaitComplete timeout! ctx=0x"
                + Long.toHexString(this.ctx));
            System.err.flush();
        }

        // 步骤 7：post-submit 清理（与旧同步 submit 保持一致）
        this.transientMemory.rotate();
        // P27: dx12Submit 已提交 GPU → 现在读回图集才是真实内容。
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
        // P38 诊断：共享 encoder（device != null）的 submit 对应整帧提交；此时 lightmap 的
        // "Update light" pass 已执行，读回 16×16 光照纹理是真实内容。最多 dump MAX_LIGHTMAP_DUMPS 次、
        // 间隔 LIGHTMAP_DUMP_INTERVAL 帧，降低 waitIdle 同步气泡与日志量。
        if (this.device != null && Dx12Native.LOG_VERBOSE
            && dx12DebugLightmapDumps < MAX_LIGHTMAP_DUMPS) {
            long lmh = Dx12Device.getDebugLightmapHandle();
            if (lmh != 0L) {
                ++dx12DebugLightmapTick;
                // 整帧 submit 满 LIGHTMAP_DUMP_INTERVAL 次触发一次（近似每 ~120 帧）
                if (dx12DebugLightmapTick % LIGHTMAP_DUMP_INTERVAL == 0) {
                    ++dx12DebugLightmapDumps;
                    System.err.println("[dx12-java] P38 dump lightmap #" + dx12DebugLightmapDumps
                        + " tex=0x" + Long.toHexString(lmh) + " after submit");
                    System.err.flush();
                    Dx12Native.dx12DumpTextureToFile(lmh, "lightmap_" + dx12DebugLightmapDumps);
                }
            }
        }
        // Run callbacks queued by the previous frame's copyTextureToBuffer.
        List<Runnable> run = this.pendingCallbacks;
        this.pendingCallbacks.clear();
        for (Runnable callback : run) {
            callback.run();
        }
        // P15: 每 30 帧打印一次 submit 摘要
        if (Dx12Native.LOG_VERBOSE && (fenceBefore % 30L) == 0) {
            System.err.println("[dx12-java] submit(async): frame=" + fenceBefore
                + " ctx=" + Long.toHexString(this.ctx));
            System.err.flush();
        }
        // P3 插桩：每 PROF_INTERVAL 帧输出一次各阶段耗时均值（ms）。
        if (prof) {
            ++gProfAsyncFrames;
            gProfTotalNs += System.nanoTime() - profStart;
            if (gProfAsyncFrames % PROF_INTERVAL == 0) {
                double inv = 1.0 / PROF_INTERVAL;
                System.err.println("[dx12-java] P3 prof n=" + PROF_INTERVAL
                    + " frameInterval=" + profMs(gProfIntervalNs * inv)
                    + "ms recordingReady=" + profMs(gProfRecordingReadyNs * inv)
                    + "ms waitSubmit=" + profMs(gProfWaitSubmitNs * inv)
                    + "ms submitTotal=" + profMs(gProfTotalNs * inv) + "ms");
                // P3 绘制热路径：判断帧时间花在 mod 绘制路径还是 MC 自身逻辑。
                System.err.println("[dx12-java] P3 draw: batches/frame=" + (Dx12RenderPassBackend.gProfMultiBatches * inv)
                    + " draws/frame=" + (Dx12RenderPassBackend.gProfMultiDraws * inv)
                    + " batchPath=" + profMs(Dx12RenderPassBackend.gProfMultiPathNs * inv) + "ms"
                    + " drawIndexedCalls/frame=" + (Dx12RenderPassBackend.gProfDrawIndexedCalls * inv)
                    + " drawIndexed=" + profMs(Dx12RenderPassBackend.gProfDrawIndexedNs * inv) + "ms");
                // P3b：并行批量路径四段拆分（主线程串行 vs worker 并行）。
                System.err.println("[dx12-java] P3 batch: build=" + profMs(Dx12RenderPassBackend.gProfBuildNs * inv)
                    + "ms prepare=" + profMs(Dx12RenderPassBackend.gProfPrepareNs * inv)
                    + "ms workers=" + profMs(Dx12RenderPassBackend.gProfWorkersNs * inv)
                    + "ms execute=" + profMs(Dx12RenderPassBackend.gProfExecuteNs * inv)
                    + "ms fallbacks=" + (Dx12RenderPassBackend.gProfFallbacks * inv));
                System.err.flush();
                Dx12RenderPassBackend.gProfMultiBatches = 0;
                Dx12RenderPassBackend.gProfMultiDraws = 0;
                Dx12RenderPassBackend.gProfMultiPathNs = 0;
                Dx12RenderPassBackend.gProfDrawIndexedCalls = 0;
                Dx12RenderPassBackend.gProfDrawIndexedNs = 0;
                Dx12RenderPassBackend.gProfBuildNs = 0;
                Dx12RenderPassBackend.gProfPrepareNs = 0;
                Dx12RenderPassBackend.gProfWorkersNs = 0;
                Dx12RenderPassBackend.gProfExecuteNs = 0;
                Dx12RenderPassBackend.gProfFallbacks = 0;
                gProfIntervalNs = 0; gProfRecordingReadyNs = 0;
                gProfWaitSubmitNs = 0; gProfTotalNs = 0;
            }
        }
        this.inAsyncSubmit = false;
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
