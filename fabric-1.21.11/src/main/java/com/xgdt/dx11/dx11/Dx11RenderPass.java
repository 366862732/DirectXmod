package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xgdt.dx11.config.Dx11Config;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * D3D11 render pass.  Pipeline state is applied immediately; uniforms and
 * textures are (re)bound at draw time so the pass behaves like the GL
 * implementation even when the caller changes bindings between draws.
 */
final class Dx11RenderPass implements RenderPass {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    private final Dx11Device device;
    private final long handle;
    private final boolean debugLogging = Dx11Config.debugLogging();
    private final Map<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final Map<String, TextureViewAndSampler> samplers = new HashMap<>();
    private @Nullable Dx11RenderPipeline pipeline;
    private @Nullable Dx11Buffer vertexBuffer;
    private @Nullable Dx11Buffer indexBuffer;
    private VertexFormat.@Nullable IndexType indexType;
    private boolean closed;
    private long drawCount;
    private long bindingLogCount;

    Dx11RenderPass(Dx11Device device, long handle) {
        this.device = device;
        this.handle = handle;
    }

    @Override
    public void pushDebugGroup(Supplier<String> label) {
        // D3D11 has no debug marker without an annotation extension.
    }

    @Override
    public void popDebugGroup() {
    }

    @Override
    public void setPipeline(RenderPipeline pipeline) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }
        this.pipeline = this.device.getOrCompilePipeline(pipeline);
        if (this.pipeline.isValid()) {
            Dx11Native.nativePassSetPipeline(this.handle, this.pipeline.handle());
        } else {
            LOGGER.error("[dx11] Pipeline {} is invalid", pipeline.getLocation());
        }
    }

    @Override
    public void bindTexture(
        String name, @Nullable GpuTextureView view, @Nullable GpuSampler sampler) {
        if (sampler == null) {
            this.samplers.remove(name);
        } else {
            this.samplers.put(
                name, new TextureViewAndSampler((Dx11TextureView) view, (Dx11Sampler) sampler));
        }
    }

    @Override
    public void setUniform(String name, GpuBuffer buffer) {
        this.setUniform(name, buffer.slice());
    }

    @Override
    public void setUniform(String name, GpuBufferSlice slice) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }
        int alignment = this.device.getUniformOffsetAlignment();
        if (slice.offset() % alignment != 0L) {
            throw new IllegalArgumentException(
                "Uniform buffer offset must be aligned to " + alignment);
        }
        this.uniforms.put(name, slice);
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        Dx11Native.nativePassSetScissor(this.handle, true, x, y, width, height);
    }

    @Override
    public void disableScissor() {
        Dx11Native.nativePassSetScissor(this.handle, false, 0, 0, 0, 0);
    }

    @Override
    public void setVertexBuffer(int slot, GpuBuffer buffer) {
        if (slot != 0) {
            throw new IllegalArgumentException("Vertex buffer slot is out of range: " + slot);
        }
        this.vertexBuffer = (Dx11Buffer) buffer;
        Dx11Native.nativePassSetVertexBuffer(this.handle, this.vertexBuffer.handle());
    }

    @Override
    public void setIndexBuffer(GpuBuffer buffer, VertexFormat.IndexType indexType) {
        this.indexBuffer = (Dx11Buffer) buffer;
        this.indexType = indexType;
        Dx11Native.nativePassSetIndexBuffer(
            this.handle, this.indexBuffer.handle(), indexType == VertexFormat.IndexType.INT);
    }

    @Override
    public void drawIndexed(int baseVertex, int firstIndex, int indexCount, int instanceCount) {
        this.applyBindings();
        final long draw = ++this.drawCount;
        if (this.debugLogging && draw <= 20) {
            LOGGER.info(
                "[dx11] drawIndexed #{} pipeline={} baseVertex={} firstIndex={} count={} instances={}",
                draw,
                this.pipeline == null ? "?" : this.pipeline.info().getLocation(),
                baseVertex,
                firstIndex,
                indexCount,
                instanceCount);
        }
        Dx11Native.nativePassDrawIndexed(
            this.handle, firstIndex, indexCount, baseVertex, instanceCount);
    }

    @Override
    public <T> void drawMultipleIndexed(
        Collection<Draw<T>> draws,
        @Nullable GpuBuffer defaultIndexBuffer,
        VertexFormat.@Nullable IndexType defaultIndexType,
        Collection<String> uniformNames,
        T uniformOwner) {
        VertexFormat.IndexType fallbackType =
            defaultIndexType == null ? VertexFormat.IndexType.SHORT : defaultIndexType;
        for (Draw<T> draw : draws) {
            GpuBuffer drawIndexBuffer =
                draw.indexBuffer() == null ? defaultIndexBuffer : draw.indexBuffer();
            if (drawIndexBuffer != null) {
                this.setIndexBuffer(
                    drawIndexBuffer,
                    draw.indexType() == null ? fallbackType : draw.indexType());
            }
            this.setVertexBuffer(draw.slot(), draw.vertexBuffer());
            BiConsumer<T, UniformUploader> uploader = draw.uniformUploaderConsumer();
            if (uploader != null) {
                uploader.accept(uniformOwner, this::setUniform);
            }
            this.drawIndexed(0, draw.firstIndex(), draw.indexCount(), 1);
        }
    }

    @Override
    public void draw(int firstVertex, int vertexCount) {
        this.applyBindings();
        final long draw = ++this.drawCount;
        if (this.debugLogging && draw <= 20) {
            LOGGER.info(
                "[dx11] draw #{} pipeline={} firstVertex={} count={}",
                draw,
                this.pipeline == null ? "?" : this.pipeline.info().getLocation(),
                firstVertex,
                vertexCount);
        }
        Dx11Native.nativePassDraw(this.handle, firstVertex, vertexCount, 1);
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            Dx11Native.nativeDestroyRenderPass(this.handle);
        }
    }

    private void applyBindings() {
        Dx11RenderPipeline current = this.pipeline;
        if (current == null || !current.isValid()) {
            throw new IllegalStateException("Pipeline contains invalid shader program");
        }

        final boolean log = this.debugLogging && this.bindingLogCount < 3;
        if (log) {
            ++this.bindingLogCount;
            LOGGER.info(
                "[dx11] bindings for {}: pipeline uniforms={} pass uniforms={} pipeline textures={} pass textures={}",
                current.info().getLocation(),
                current.uniformSlots(),
                this.uniforms.keySet(),
                current.textureSlots(),
                this.samplers.keySet());
        }

        // Upload every uniform the pass has set that the compiled shaders
        // actually reference.  Iterating the pipeline's declared uniform list
        // is not enough: vanilla binds uniforms that the pipeline itself does
        // not declare (notably "Globals" from RenderSystem.bindDefaultUniforms)
        // and only the compiled binding list knows about them.  The D3D12
        // backend does the same through pipeline.buildBindings().
        for (Map.Entry<String, GpuBufferSlice> entry : this.uniforms.entrySet()) {
            String name = entry.getKey();
            GpuBufferSlice slice = entry.getValue();
            Dx11Buffer buffer = (Dx11Buffer) slice.buffer();
            TextureFormat texelFormat = current.texelFormat(name);
            if (texelFormat != null) {
                Integer slot = current.textureSlot(name);
                if (log) {
                    LOGGER.info(
                        "[dx11]   texel buffer '{}' slot={} format={}",
                        name, slot, texelFormat);
                }
                if (slot != null) {
                    Dx11Native.nativePassSetTexelBuffer(
                        this.handle,
                        slot,
                        buffer.handle(),
                        texelFormat.ordinal());
                }
                continue;
            }
            Integer slot = current.uniformSlot(name);
            if (log) {
                LOGGER.info("[dx11]   uniform '{}' slot={}", name, slot);
            }
            if (slot != null) {
                Dx11Native.nativePassSetUniform(
                    this.handle,
                    slot,
                    buffer.handle(),
                    slice.offset(),
                    slice.length());
            }
        }

        // Bind sampled textures and explicitly unbind slots that this pipeline
        // declares but the pass does not provide, so a stale binding from a
        // previous draw cannot leak in.
        for (Map.Entry<String, Integer> entry : current.textureSlots().entrySet()) {
            String name = entry.getKey();
            if (current.texelFormat(name) != null) {
                continue;  // already bound by the uniform loop
            }
            TextureViewAndSampler binding = this.samplers.get(name);
            if (log) {
                LOGGER.info(
                    "[dx11]   texture '{}' slot={} present={}",
                    name, entry.getValue(), binding != null);
            }
            if (binding != null) {
                Dx11Native.nativePassBindTexture(
                    this.handle,
                    entry.getValue(),
                    binding.view == null ? 0L : binding.view.handle(),
                    binding.sampler == null ? 0L : binding.sampler.handle());
            } else {
                Dx11Native.nativePassBindTexture(this.handle, entry.getValue(), 0L, 0L);
            }
        }
    }

    private record TextureViewAndSampler(
        @Nullable Dx11TextureView view, @Nullable Dx11Sampler sampler) {
    }
}
