package com.dx12.dx12;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPass.RenderArea;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * D3D12 render pass backend（P6 draw 全链路）。
 *
 * 镜像官方 {@code VulkanRenderPass}：uniform/texture 暂存在本类 map 中，draw
 * 前按需 push 到原生瞬时描述符堆（{@link #pushDescriptors()}）；setPipeline
 * 绑定 withDepth/withoutDepth PSO；顶点/索引缓冲与 draw 命令直接下发 native。
 */
@Environment(EnvType.CLIENT)
public class Dx12RenderPassBackend implements RenderPassBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    private static final int USAGE_VERTEX = 0x20;
    private static final int USAGE_INDEX = 0x40;
    private static final int USAGE_UNIFORM = 0x80;
    private static final int USAGE_UNIFORM_TEXEL_BUFFER = 0x100;

    /** DEBUG: true 时强制所有 pass 使用 withoutDepth PSO（禁用深度测试/写入），用于排查黑屏是否由深度问题导致。 */
    private static final boolean DEBUG_DISABLE_DEPTH_TEST = false;

    private final @Nullable Dx12Device device;
    private final long ctx;
    private final @Nullable RenderArea renderArea;
    private final int outputWidth;
    private final int outputHeight;
    private final boolean hasDepth;
    /** P27：本 pass 的 color[0] 纹理 native handle（用于图集合成后 dump 验证）。 */
    private final long colorTargetHandle;
    /** P31：是否使用 shader Y-flip 变体管线（GUI 离屏 pass，由 CommandEncoder 根据 texture usage 判断）。 */
    private final boolean flipY;
    private int pushedDebugGroups = 0;

    private @Nullable Dx12CompiledRenderPipeline pipeline;
    private boolean anyDescriptorDirty = false;
    private final Map<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final Map<String, TextureViewAndSampler> textures = new HashMap<>();
    /** P22：帧计数，用于每帧打印一次 DynamicTransforms offset 诊断（确认 ring buffer rotate 生效）。 */
    private int frameCount = 0;

    private record TextureViewAndSampler(Dx12GpuTextureView view, Dx12GpuSampler sampler) {
    }

    public Dx12RenderPassBackend(@Nullable Dx12Device device, long ctx,
        @Nullable RenderArea renderArea, int outputWidth, int outputHeight,
        boolean hasDepth, long colorTargetHandle, boolean flipY) {
        this.device = device;
        this.ctx = ctx;
        this.renderArea = renderArea;
        this.outputWidth = outputWidth;
        this.outputHeight = outputHeight;
        this.hasDepth = hasDepth;
        this.colorTargetHandle = colorTargetHandle;
        this.flipY = flipY;
    }

    /** P27：当前管线的 location 字符串（如 minecraft:pipeline/animate_sprite_blit）。 */
    @Nullable
    public String pipelineLocation() {
        Dx12CompiledRenderPipeline p = this.pipeline;
        if (p == null || p.info() == null || p.info().getLocation() == null) {
            return null;
        }
        return p.info().getLocation().toString();
    }

    /** P27：本 pass 的 color[0] 纹理 native handle。 */
    public long colorTargetHandle() {
        return this.colorTargetHandle;
    }

    /** P27：本 pass 的渲染输出尺寸（用于 dump tag 区分不同图集）。 */
    public int outputWidth() {
        return this.outputWidth;
    }

    public int outputHeight() {
        return this.outputHeight;
    }

    /** P27：本 pass 的 renderArea 目标区域（图集 blit 时为 sprite 在图集中的位置）。 */
    public int areaX() {
        return this.renderArea != null ? this.renderArea.x() : 0;
    }

    public int areaY() {
        return this.renderArea != null ? this.renderArea.y() : 0;
    }

    public int areaWidth() {
        return this.renderArea != null ? this.renderArea.width() : this.outputWidth;
    }

    public int areaHeight() {
        return this.renderArea != null ? this.renderArea.height() : this.outputHeight;
    }

    @Override
    public void pushDebugGroup(Supplier<String> label) {
        this.pushedDebugGroups++;
    }

    @Override
    public void popDebugGroup() {
        this.pushedDebugGroups--;
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {
        Dx12Native.dx12WriteTimestamp(this.ctx,
            ((Dx12GpuQueryPool) pool).nativeHandle(), index);
    }

    // -----------------------------------------------------------------------
    // Pipeline + descriptors
    // -----------------------------------------------------------------------

    @Override
    public void setPipeline(RenderPipeline pipeline) {
        Dx12Device device = this.device;
        if (device == null) {
            throw new IllegalStateException("No D3D12 device bound to this render pass");
        }
        Dx12CompiledRenderPipeline compiled = device.getOrCompilePipeline(pipeline, this.flipY);
        if (compiled == null || !compiled.isValid()) {
            throw new IllegalStateException(
                "Pipeline " + pipeline.getLocation() + " is not valid (shader compilation failed)");
        }
        this.pipeline = compiled;
        this.anyDescriptorDirty = true;
        // P22：desc.hasDepth 来自渲染 pass（是否有 depth attachment），但 PSO 变体选择
        // 应基于管线本身是否有 DepthStencilState。GUI 管线没有 depthStencilState，
        // 即使 pass 有 depth attachment 也不该启用深度测试（否则 withDepth PSO 的
        // GREATER_EQUAL 测试会丢弃所有 fragment → 黑屏）。
        boolean pipelineHasDepth = compiled.info().getDepthStencilState() != null;
        boolean useDepth = this.hasDepth && pipelineHasDepth;
        boolean ok = Dx12Native.dx12SetPipeline(this.ctx, compiled.handle(), useDepth);
        // P29：诊断打印仅在 DX12_LOG_VERBOSE=1 时输出，避免每帧同步 I/O。
        if (Dx12Native.LOG_VERBOSE) {
            System.err.println("[dx12-java] setPipeline: " + pipeline.getLocation()
                    + " pso=" + Long.toHexString(compiled.handle())
                    + " passHasDepth=" + this.hasDepth + " pipelineHasDepth=" + pipelineHasDepth
                    + " useDepth=" + useDepth + " ok=" + ok);
        }
        if (!ok) {
            throw new IllegalStateException("dx12SetPipeline failed for " + pipeline.getLocation());
        }
    }

    @Override
    public void bindTexture(String name, @Nullable GpuTextureView textureView,
        @Nullable GpuSampler sampler) {
        if (textureView == null || sampler == null) {
            if (textureView != null || sampler != null) {
                throw new IllegalArgumentException("Both texture and sampler must be null or non-null");
            }
            this.textures.remove(name);
            return;
        }
        this.textures.put(name, new TextureViewAndSampler(
            (Dx12GpuTextureView) textureView, (Dx12GpuSampler) sampler));
        this.anyDescriptorDirty = true;
    }

    @Override
    public void setUniform(String name, GpuBuffer value) {
        this.uniforms.put(name, value.slice());
        this.anyDescriptorDirty = true;
    }

    @Override
    public void setUniform(String name, GpuBufferSlice value) {
        // P22 诊断：检查 uniform 数据是否含 NaN/Inf（投影矩阵等异常值会污染着色器）。
        // P29：每帧多次 GPU buffer map + 扫描开销大，仅在 DX12_LOG_VERBOSE=1 时执行。
        if (Dx12Native.LOG_VERBOSE && value != null && value.length() >= 4) {
            try (GpuBufferSlice.MappedView mv = value.buffer().map(value.offset(),
                    Math.min(value.length(), 64L), true, false)) {
                java.nio.FloatBuffer fb = mv.data().order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
                int limit = (int) Math.min(fb.remaining(), 16);
                for (int i = 0; i < limit; i++) {
                    if (fb.hasRemaining()) {
                        float fv = fb.get();
                        if (Float.isNaN(fv) || Float.isInfinite(fv)) {
                            long bufId = (value.buffer() instanceof Dx12GpuBuffer b)
                                ? b.handle() : 0L;
                            System.err.printf(
                                "[dx12-java] NaN uniform '%s' buf=%x idx=%d val=%s%n",
                                name, bufId, i,
                                Float.isNaN(fv) ? "NaN" : "Inf");
                            System.err.flush();
                        }
                    }
                }
            } catch (Exception e) {
                // map 失败（如 READONLY buffer）时忽略
            }
        }
        this.uniforms.put(name, value);
        this.anyDescriptorDirty = true;
    }

    /** 镜像官方 pushDescriptors：把当前 uniform/texture 写入原生瞬时描述符堆并绑定 root table。 */
    private void pushDescriptors() {
        if (!this.anyDescriptorDirty) {
            return;
        }
        this.anyDescriptorDirty = false;
        // P22：每进入一次 pushDescriptors 视为一帧的新绘制批次
        this.frameCount++;
        Dx12CompiledRenderPipeline pipeline = this.pipeline;
        if (pipeline == null || !pipeline.isValid()) {
            LOGGER.warn("pushDescriptors: SKIP (pipeline null or invalid)");
            return;
        }
        List<Dx12BindGroupEntry> bindings = pipeline.buildBindings();
        int count = bindings.size();
        if (count == 0) {
            LOGGER.warn("pushDescriptors: SKIP (0 bindings)");
            return;
        }
        // P16 诊断：首帧打印 binding 名称列表（P29：仅 verbose 模式输出）
        if (Dx12Native.LOG_VERBOSE && System.err instanceof java.io.PrintStream) {
            StringBuilder sb = new StringBuilder("[dx12-java] pushDesc pipeline=")
                .append(pipeline.info().getLocation()).append(" count=").append(count);
            for (int i = 0; i < count && i < 8; i++) {
                sb.append(" [").append(i).append("=").append(bindings.get(i).type())
                    .append(":").append(bindings.get(i).name()).append("]");
            }
            System.err.println(sb);
            System.err.flush();
        }
        int[] types = new int[count];
        long[] buffers = new long[count];
        long[] offsets = new long[count];
        long[] lengths = new long[count];
        int[] texelFormats = new int[count];
        long[] views = new long[count];
        for (int i = 0; i < count; i++) {
            Dx12BindGroupEntry entry = bindings.get(i);
            switch (entry.type()) {
                case UNIFORM_BUFFER -> {
                    GpuBufferSlice value = requireUniform(entry.name());
                    GpuBuffer buffer = value.buffer();
                    types[i] = 0;
                    buffers[i] = ((Dx12GpuBuffer) buffer).handle();
                    offsets[i] = value.offset();
                    lengths[i] = value.length();
                    // P20：诊断 binding 的 buffer 句柄、offset 和长度（P29：仅 verbose）
                    // P22：打印 DynamicTransforms 的 offset，验证 ring buffer rotate 是否生效
                    if (Dx12Native.LOG_VERBOSE && System.err instanceof java.io.PrintStream) {
                        System.err.printf("[dx12-java] pushDesc binding[%d]: name=%s type=UNIFORM buf=%x off=%d len=%d heapType=?%n",
                            i, entry.name(), buffers[i], (int)offsets[i], (int)lengths[i]);
                        if ("DynamicTransforms".equals(entry.name())) {
                            System.err.printf("[dx12-java] pushDesc DynamicTransforms: frame=%d off=%d buf=%x%n",
                                frameCount, (int)offsets[i], buffers[i]);
                        }
                        System.err.flush();
                    }
                }
                case SAMPLED_IMAGE -> {
                    TextureViewAndSampler texture = this.textures.get(entry.name());
                    if (texture == null) {
                        throw new IllegalStateException("Texture '" + entry.name() + "' was not bound before draw");
                    }
                    if (texture.view().isClosed()) {
                        throw new IllegalStateException("Texture '" + entry.name() + "' is closed");
                    }
                    types[i] = 1;
                    views[i] = texture.view().handle();
                    // P22：打印 Sampler0 view handle，验证纹理视图是否正常传递（P29：仅 verbose）
                    if (Dx12Native.LOG_VERBOSE && "Sampler0".equals(entry.name())
                        && System.err instanceof java.io.PrintStream) {
                        GpuTexture tex = texture.view().texture();
                        System.err.printf("[dx12-java] pushDesc Sampler0: frame=%d view=%x closed=%b fmt=%s w=%d h=%d%n",
                            frameCount, views[i], texture.view().isClosed(),
                            tex.getFormat(), tex.getWidth(0), tex.getHeight(0));
                        System.err.flush();
                    }
                }
                case TEXEL_BUFFER -> {
                    GpuBufferSlice value = requireUniform(entry.name());
                    GpuBuffer buffer = value.buffer();
                    types[i] = 2;
                    buffers[i] = ((Dx12GpuBuffer) buffer).handle();
                    offsets[i] = value.offset();
                    lengths[i] = value.length();
                    texelFormats[i] = entry.texelBufferFormat().ordinal();
                }
            }
        }
        if (!Dx12Native.dx12PushDescriptors(this.ctx, types, buffers, offsets, lengths, texelFormats, views)) {
            throw new IllegalStateException("dx12PushDescriptors failed");
        }
    }

    private GpuBufferSlice requireUniform(String name) {
        GpuBufferSlice value = this.uniforms.get(name);
        if (value == null) {
            throw new IllegalStateException("Uniform '" + name + "' was not set before draw");
        }
        GpuBuffer buffer = value.buffer();
        if (buffer.isClosed()) {
            throw new IllegalStateException("Uniform '" + name + "' buffer is closed");
        }
        return value;
    }

    // -----------------------------------------------------------------------
    // Scissor
    // -----------------------------------------------------------------------

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        // P31：flipY 模式下，shader 已将几何 Y 轴翻转，scissor 坐标需同步翻转以保持裁剪区域对齐。
        // newW = outputHeight - y - height，使 scissor 原点从左下角转为左上角（与 shader 翻转后一致）。
        int scissorY = this.flipY ? (this.outputHeight - y - height) : y;
        if (!Dx12Native.dx12SetScissor(this.ctx, x, scissorY, width, height)) {
            LOGGER.error("dx12SetScissor failed ({} {} {} {})", x, scissorY, width, height);
        }
    }

    @Override
    public void disableScissor() {
        RenderArea area = this.renderArea;
        if (area != null) {
            this.enableScissor(area.x(), area.y(), area.width(), area.height());
        } else {
            this.enableScissor(0, 0, this.outputWidth, this.outputHeight);
        }
    }

    // -----------------------------------------------------------------------
    // Vertex / index buffers + draws
    // -----------------------------------------------------------------------

    @Override
    public void setVertexBuffer(int slot, @Nullable GpuBufferSlice vertexBuffer) {
        if (vertexBuffer == null) {
            return;  // 与 Vulkan 语义一致：null 绑定不操作
        }
        GpuBuffer buffer = vertexBuffer.buffer();
        if (buffer.isClosed()) {
            throw new IllegalStateException("Vertex buffer at slot " + slot + " has been closed!");
        }
        if ((buffer.usage() & USAGE_VERTEX) == 0) {
            throw new IllegalStateException("Vertex buffer at slot " + slot + " doesn't have GpuBuffer.USAGE_VERTEX flag!");
        }
        Dx12CompiledRenderPipeline pipeline = this.pipeline;
        int stride = 0;
        if (pipeline != null) {
            RenderPipeline info = pipeline.info();
            var bindings = info.getVertexFormatBindings();
            if (bindings != null && slot < bindings.length && bindings[slot] != null) {
                stride = bindings[slot].getVertexSize();
            }
        }
        if (stride == 0) {
            throw new IllegalStateException(
                "setVertexBuffer: cannot derive stride for slot " + slot
                + " (pipeline=" + (pipeline != null ? pipeline.info().getLocation() : "null")
                + "), vertexFormatBindings is empty or slot out of range");
        }
        // P22 诊断：记录顶点缓冲大小，用于排查 SizeInBytes 不足问题（P29：仅 verbose）
        if (Dx12Native.LOG_VERBOSE) {
            long bufSize = buffer.size();
            long vbOffset = vertexBuffer.offset();
            long vbRemaining = bufSize - vbOffset;
            System.err.printf("[dx12-java] setVB slot=%d stride=%d bufSize=%d offset=%d remaining=%d pipeline=%s%n",
                slot, stride, (int)bufSize, (int)vbOffset, (int)vbRemaining,
                pipeline != null ? pipeline.info().getLocation() : "null");
            System.err.flush();
        }
        if (!Dx12Native.dx12SetVertexBuffer(this.ctx, slot,
            ((Dx12GpuBuffer) buffer).handle(), vertexBuffer.offset(), stride)) {
            throw new IllegalStateException("dx12SetVertexBuffer failed");
        }
    }

    @Override
    public void setIndexBuffer(GpuBuffer indexBuffer, IndexType indexType) {
        if (indexBuffer.isClosed()) {
            throw new IllegalStateException("Index buffer has been closed!");
        }
        if ((indexBuffer.usage() & USAGE_INDEX) == 0) {
            throw new IllegalStateException("Index buffer doesn't have GpuBuffer.USAGE_INDEX flag!");
        }
        if (!Dx12Native.dx12SetIndexBuffer(this.ctx, ((Dx12GpuBuffer) indexBuffer).handle(),
            indexType == IndexType.INT ? 1 : 0)) {
            throw new IllegalStateException("dx12SetIndexBuffer failed");
        }
    }

    @Override
    public void drawIndexed(int indexCount, int instanceCount, int firstIndex,
        int vertexOffset, int firstInstance) {
        // P17 诊断：记录每次 drawIndexed 调用（P29：仅 verbose）
        if (Dx12Native.LOG_VERBOSE && System.err instanceof java.io.PrintStream) {
            // 尝试获取当前绑定的顶点缓冲信息
            int neededVerts = indexCount; // 最坏情况：每个 index 引用一个独立顶点
            System.err.println("[dx12-java] drawIndexed pipeline=" + pipeline.info().getLocation()
                + " count=" + indexCount + " inst=" + instanceCount
                + " first=" + firstIndex + " base=" + vertexOffset
                + " neededVerts~=" + neededVerts
                + (indexCount == 0 ? " [ZERO-COUNT!]" : ""));
            System.err.flush();
        }
        this.pushDescriptors();
        if (!Dx12Native.dx12DrawIndexed(this.ctx, indexCount, instanceCount,
            firstIndex, vertexOffset, firstInstance)) {
            throw new IllegalStateException("dx12DrawIndexed failed");
        }
    }

    @Override
    public void multiDrawIndexed(IntBuffer drawParameters, int instanceCount,
        int firstInstance, int drawCount) {
        for (int i = 0; i < drawCount; i++) {
            int firstIndex = drawParameters.get();
            int indexCount = drawParameters.get();
            int baseVertex = drawParameters.get();
            this.drawIndexed(indexCount, instanceCount, firstIndex, baseVertex, firstInstance);
        }
    }

    @Override
    public void multiDrawIndexed(PointerBuffer firstIndexOffsets, IntBuffer indexCounts,
        IntBuffer vertexOffsets, int drawCount) {
        throw new UnsupportedOperationException(
            "multiDrawDirectSeparate is not supported by the D3D12 backend");
    }

    @Override
    public void drawIndexedIndirect(GpuBufferSlice commands, int drawCount) {
        this.pushDescriptors();
        if (!Dx12Native.dx12DrawIndexedIndirect(this.ctx,
            ((Dx12GpuBuffer) commands.buffer()).handle(), commands.offset(), drawCount)) {
            throw new IllegalStateException("dx12DrawIndexedIndirect failed");
        }
    }

    @Override
    public <T> void drawMultipleIndexed(Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer defaultIndexBuffer, @Nullable IndexType defaultIndexType,
        Collection<String> dynamicUniforms, T uniformArgument) {
        if (this.pipeline == null || !this.pipeline.isValid()) {
            throw new IllegalStateException("drawMultipleIndexed called without a valid pipeline");
        }
        for (RenderPass.Draw<T> draw : draws) {
            BiConsumer<T, RenderPass.UniformUploader> uploader = draw.uniformUploaderConsumer();
            if (uploader != null) {
                uploader.accept(uniformArgument, this::setUniform);
            }
            GpuBuffer indexBuffer = draw.indexBuffer() != null ? draw.indexBuffer() : defaultIndexBuffer;
            IndexType indexType = draw.indexType() != null ? draw.indexType() : defaultIndexType;
            if (indexBuffer == null || indexType == null) {
                throw new IllegalStateException("No index buffer was set for draw");
            }
            this.setIndexBuffer(indexBuffer, indexType);
            this.setVertexBuffer(draw.slot(), draw.vertexBuffer().slice());
            // P22 诊断：打印每个 Draw 的顶点缓冲信息（P29：仅 verbose）
            if (Dx12Native.LOG_VERBOSE) {
                GpuBufferSlice vbSlice = draw.vertexBuffer().slice();
                long vbBufSize = vbSlice.buffer().size();
                System.err.printf("[dx12-java] drawMulti slot=%d idxCount=%d vbBufSize=%d vbOff=%d vbLen=%d pipeline=%s%n",
                    draw.slot(), draw.indexCount(), (int)vbBufSize, (int)vbSlice.offset(), (int)vbSlice.length(),
                    this.pipeline.info().getLocation());
                System.err.flush();
            }
            this.pushDescriptors();
            this.drawIndexed(draw.indexCount(), 1, draw.firstIndex(), draw.baseVertex(), 0);
        }
    }

    @Override
    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        // P22 诊断：记录非索引 draw（DrawInstanced 路径，P29：仅 verbose）
        if (Dx12Native.LOG_VERBOSE && System.err instanceof java.io.PrintStream) {
            System.err.println("[dx12-java] draw (non-indexed) pipeline=" + pipeline.info().getLocation()
                + " vertCount=" + vertexCount + " inst=" + instanceCount
                + " first=" + firstVertex);
            System.err.flush();
        }
        this.pushDescriptors();
        if (!Dx12Native.dx12Draw(this.ctx, vertexCount, instanceCount, firstVertex, firstInstance)) {
            throw new IllegalStateException("dx12Draw failed");
        }
    }

    @Override
    public void multiDraw(IntBuffer drawParameters, int instanceCount, int firstInstance,
        int drawCount) {
        for (int i = 0; i < drawCount; i++) {
            int firstVertex = drawParameters.get();
            int vertexCount = drawParameters.get();
            this.draw(vertexCount, instanceCount, firstVertex, firstInstance);
        }
    }

    @Override
    public void multiDraw(IntBuffer firstVertices, IntBuffer vertexCounts, int drawCount) {
        throw new UnsupportedOperationException(
            "multiDrawDirectSeparate is not supported by the D3D12 backend");
    }

    @Override
    public void drawIndirect(GpuBufferSlice commands, int drawCount) {
        this.pushDescriptors();
        if (!Dx12Native.dx12DrawIndirect(this.ctx,
            ((Dx12GpuBuffer) commands.buffer()).handle(), commands.offset(), drawCount)) {
            throw new IllegalStateException("dx12DrawIndirect failed");
        }
    }
}
