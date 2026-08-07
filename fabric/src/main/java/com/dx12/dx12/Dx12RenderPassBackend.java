package com.dx12.dx12;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;

/**
 * D3D12 render pass backend. The render pass lifecycle (begin/end + clears) is
 * driven by the native {@code dx12BeginRenderPass} / {@code dx12EndRenderPass}.
 *
 * P3 scope: {@link #writeTimestamp} only. All draw/descriptor methods require a
 * compiled pipeline (P4) and throw {@link UnsupportedOperationException}.
 */
@Environment(EnvType.CLIENT)
public class Dx12RenderPassBackend implements RenderPassBackend {
    private final long ctx;

    Dx12RenderPassBackend(long ctx) {
        this.ctx = ctx;
    }

    @Override
    public void pushDebugGroup(Supplier<String> label) {
        // P3: no-op (native debug groups are added with the P4 pipeline work).
    }

    @Override
    public void popDebugGroup() {
        // P3: no-op.
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {
        Dx12Native.dx12WriteTimestamp(this.ctx,
            ((Dx12GpuQueryPool) pool).nativeHandle(), index);
    }

    // -----------------------------------------------------------------------
    // P4: pipeline + draw commands (not implemented until the shader layer)
    // -----------------------------------------------------------------------

    @Override
    public void setPipeline(RenderPipeline pipeline) {
        throw new UnsupportedOperationException("P4: D3D12 pipeline not yet implemented");
    }

    @Override
    public void bindTexture(String name, @Nullable GpuTextureView textureView, @Nullable GpuSampler sampler) {
        throw new UnsupportedOperationException("P4: descriptor binding not yet implemented");
    }

    @Override
    public void setUniform(String name, GpuBuffer value) {
        throw new UnsupportedOperationException("P4: descriptor binding not yet implemented");
    }

    @Override
    public void setUniform(String name, GpuBufferSlice value) {
        throw new UnsupportedOperationException("P4: descriptor binding not yet implemented");
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        throw new UnsupportedOperationException("P4: scissor not yet implemented");
    }

    @Override
    public void disableScissor() {
        throw new UnsupportedOperationException("P4: scissor not yet implemented");
    }

    @Override
    public void setVertexBuffer(int slot, @Nullable GpuBufferSlice vertexBuffer) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void setIndexBuffer(GpuBuffer indexBuffer, IndexType indexType) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void drawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void multiDrawIndexed(IntBuffer drawParameters, int instanceCount, int firstInstance, int drawCount) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void multiDrawIndexed(PointerBuffer firstIndexOffsets, IntBuffer indexCounts,
        IntBuffer vertexOffsets, int drawCount) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void drawIndexedIndirect(GpuBufferSlice commands, int drawCount) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public <T> void drawMultipleIndexed(Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer defaultIndexBuffer, @Nullable IndexType defaultIndexType,
        Collection<String> dynamicUniforms, T uniformArgument) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void multiDraw(IntBuffer drawParameters, int instanceCount, int firstInstance, int drawCount) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void multiDraw(IntBuffer firstVertices, IntBuffer vertexCounts, int drawCount) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }

    @Override
    public void drawIndirect(GpuBufferSlice commands, int drawCount) {
        throw new UnsupportedOperationException("P4: draw commands not yet implemented");
    }
}
