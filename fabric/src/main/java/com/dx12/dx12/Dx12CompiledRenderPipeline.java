package com.dx12.dx12;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * DX12 编译后的渲染管线：持有一条 {@link RenderPipeline} 对应的原生
 * {@code Dx12Pipeline*}（已含 D3DCompile 字节码 + root signature + PSO）。
 * 由 {@link Dx12Device#precompilePipeline} 创建，{@link Dx12Device#clearPipelineCache}
 * 统一销毁。
 */
@Environment(EnvType.CLIENT)
public final class Dx12CompiledRenderPipeline implements CompiledRenderPipeline, AutoCloseable {
    private final RenderPipeline info;
    private final long handle;          // native Dx12Pipeline*
    private final String vertexHlsl;    // 保留便于调试/后续 P5 使用
    private final String fragmentHlsl;

    public Dx12CompiledRenderPipeline(RenderPipeline info, long handle,
        String vertexHlsl, String fragmentHlsl) {
        this.info = info;
        this.handle = handle;
        this.vertexHlsl = vertexHlsl;
        this.fragmentHlsl = fragmentHlsl;
    }

    public RenderPipeline info() {
        return this.info;
    }

    public long handle() {
        return this.handle;
    }

    @Override
    public boolean isValid() {
        return this.handle != 0;
    }

    @Override
    public void close() {
        if (this.handle != 0) {
            Dx12Native.dx12DestroyPipeline(this.handle);
        }
    }
}
