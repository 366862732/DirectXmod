package com.dx12.dx12;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
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
    private final List<Dx12BindGroupEntry> bindings;  // 与原生 root signature 表项同序（P6 pushDescriptors 用）
    private final String vertexHlsl;    // 保留便于调试/后续 P5 使用
    private final String fragmentHlsl;

    public Dx12CompiledRenderPipeline(RenderPipeline info, long handle,
        List<Dx12BindGroupEntry> bindings, String vertexHlsl, String fragmentHlsl) {
        this.info = info;
        this.handle = handle;
        this.bindings = bindings;
        this.vertexHlsl = vertexHlsl;
        this.fragmentHlsl = fragmentHlsl;
    }

    public RenderPipeline info() {
        return this.info;
    }

    public long handle() {
        return this.handle;
    }

    /** 绑定条目（与原生 root signature 描述符表顺序一致）。 */
    public List<Dx12BindGroupEntry> bindings() {
        return this.bindings;
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
