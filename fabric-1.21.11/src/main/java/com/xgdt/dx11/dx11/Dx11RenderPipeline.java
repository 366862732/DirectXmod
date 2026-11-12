package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** A pipeline whose HLSL has been translated and compiled to D3D11 bytecode. */
final class Dx11RenderPipeline implements CompiledRenderPipeline, Dx11Resource {
    private final Dx11Device device;
    private final RenderPipeline info;
    private final long handle;
    private final Map<String, Integer> uniformSlots;
    private final Map<String, Integer> textureSlots;
    private final Map<String, TextureFormat> texelFormats;
    private boolean closed;

    Dx11RenderPipeline(
        Dx11Device device,
        RenderPipeline info,
        long handle,
        Map<String, Integer> uniformSlots,
        Map<String, Integer> textureSlots,
        Map<String, TextureFormat> texelFormats
    ) {
        this.device = device;
        this.info = info;
        this.handle = handle;
        this.uniformSlots = uniformSlots;
        this.textureSlots = textureSlots;
        this.texelFormats = texelFormats;
    }

    RenderPipeline info() {
        return this.info;
    }

    static Dx11RenderPipeline invalid(Dx11Device device, RenderPipeline info) {
        return new Dx11RenderPipeline(
            device, info, 0L, Map.of(), Map.of(), Map.of());
    }

    long handle() {
        return this.handle;
    }

    @Nullable Integer uniformSlot(String name) {
        return this.uniformSlots.get(name);
    }

    Map<String, Integer> uniformSlots() {
        return this.uniformSlots;
    }

    @Nullable Integer textureSlot(String name) {
        return this.textureSlots.get(name);
    }

    Map<String, Integer> textureSlots() {
        return this.textureSlots;
    }

    @Nullable TextureFormat texelFormat(String name) {
        return this.texelFormats.get(name);
    }

    @Override
    public boolean isValid() {
        return this.handle != 0L && !this.closed;
    }

    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.device.unregister(this);
            if (this.handle != 0L) {
                Dx11Native.nativeDestroyPipeline(this.handle);
            }
        }
    }

    @Override
    public long nativeHandle() {
        return this.handle;
    }

    @Override
    public void destroyNative() {
        close();
    }
}
