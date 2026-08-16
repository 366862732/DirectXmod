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

    private final @Nullable Dx12Device device;
    private final long ctx;
    private final @Nullable RenderArea renderArea;
    private final int outputWidth;
    private final int outputHeight;
    private final boolean hasDepth;
    private int pushedDebugGroups = 0;

    private @Nullable Dx12CompiledRenderPipeline pipeline;
    private boolean anyDescriptorDirty = false;
    private final Map<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final Map<String, TextureViewAndSampler> textures = new HashMap<>();

    private record TextureViewAndSampler(Dx12GpuTextureView view, Dx12GpuSampler sampler) {
    }

    public Dx12RenderPassBackend(@Nullable Dx12Device device, long ctx,
        @Nullable RenderArea renderArea, int outputWidth, int outputHeight,
        boolean hasDepth) {
        this.device = device;
        this.ctx = ctx;
        this.renderArea = renderArea;
        this.outputWidth = outputWidth;
        this.outputHeight = outputHeight;
        this.hasDepth = hasDepth;
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
        Dx12CompiledRenderPipeline compiled = device.getOrCompilePipeline(pipeline);
        if (compiled == null || !compiled.isValid()) {
            throw new IllegalStateException(
                "Pipeline " + pipeline.getLocation() + " is not valid (shader compilation failed)");
        }
        this.pipeline = compiled;
        this.anyDescriptorDirty = true;
        boolean ok = Dx12Native.dx12SetPipeline(this.ctx, compiled.handle(), this.hasDepth);
        System.err.println("[dx12-java] setPipeline: " + pipeline.getLocation()
                + " pso=" + Long.toHexString(compiled.handle())
                + " hasDepth=" + this.hasDepth + " ok=" + ok);
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
        this.uniforms.put(name, value);
        this.anyDescriptorDirty = true;
    }

    /** 镜像官方 pushDescriptors：把当前 uniform/texture 写入原生瞬时描述符堆并绑定 root table。 */
    private void pushDescriptors() {
        if (!this.anyDescriptorDirty) {
            return;
        }
        this.anyDescriptorDirty = false;
        Dx12CompiledRenderPipeline pipeline = this.pipeline;
        if (pipeline == null || !pipeline.isValid()) {
            System.err.println("[dx12-java] pushDescriptors: SKIP (pipeline null or invalid)");
            return;
        }
        List<Dx12BindGroupEntry> bindings = pipeline.bindings();
        int count = bindings.size();
        if (count == 0) {
            System.err.println("[dx12-java] pushDescriptors: SKIP (0 bindings)");
            return;
        }
        // P16 诊断：首帧打印 binding 名称列表
        if (System.err instanceof java.io.PrintStream) {
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
        if (!Dx12Native.dx12SetScissor(this.ctx, x, y, width, height)) {
            LOGGER.error("dx12SetScissor failed ({} {} {} {})", x, y, width, height);
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
        // P17 诊断：记录每次 drawIndexed 调用（含参数），验证 GUI pass 是否有实际 draw
        if (System.err instanceof java.io.PrintStream) {
            System.err.println("[dx12-java] drawIndexed pipeline=" + pipeline.info().getLocation()
                + " count=" + indexCount + " inst=" + instanceCount
                + " first=" + firstIndex + " base=" + vertexOffset
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
            this.pushDescriptors();
            this.drawIndexed(draw.indexCount(), 1, draw.firstIndex(), draw.baseVertex(), 0);
        }
    }

    @Override
    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
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
