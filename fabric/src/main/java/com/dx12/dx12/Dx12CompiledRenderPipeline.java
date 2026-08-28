package com.dx12.dx12;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * DX12 编译后的渲染管线：持有一条 {@link RenderPipeline} 对应的原生
 * {@code Dx12Pipeline*}（已含 D3DCompile 字节码 + root signature + PSO）。
 * 由 {@link Dx12Device#precompilePipeline} 创建，{@link Dx12Device#clearPipelineCache}
 * 统一销毁。
 *
 * bindings 不再在编译期固化，而是由 {@link #buildBindings()} 从
 * {@link RenderPipeline#getBindGroupLayouts()} + shader modules 动态推导，
 * 与官方 Blaze3D 的 BindGroupLayout 体系保持一致。
 */
@Environment(EnvType.CLIENT)
public final class Dx12CompiledRenderPipeline implements CompiledRenderPipeline, AutoCloseable {
    private final RenderPipeline info;
    private final long handle;                 // native Dx12Pipeline*
    private final Dx12IntermediaryShaderModule vertexShader;  // 用于 buildBindings() 推导
    private final Dx12IntermediaryShaderModule fragmentShader; // 用于 buildBindings() 推导
    private final String vertexHlsl;
    private final String fragmentHlsl;
    /** P31：翻转变体标注——为 GUI 离屏 pass（物品图集 / PIP）编译的 Y-flip 变体管线。 */
    private final boolean flipY;

    public Dx12CompiledRenderPipeline(RenderPipeline info, long handle,
        Dx12IntermediaryShaderModule vertexShader,
        Dx12IntermediaryShaderModule fragmentShader,
        String vertexHlsl, String fragmentHlsl) {
        this(info, handle, vertexShader, fragmentShader, vertexHlsl, fragmentHlsl, false);
    }

    public Dx12CompiledRenderPipeline(RenderPipeline info, long handle,
        Dx12IntermediaryShaderModule vertexShader,
        Dx12IntermediaryShaderModule fragmentShader,
        String vertexHlsl, String fragmentHlsl, boolean flipY) {
        this.info = info;
        this.handle = handle;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.vertexHlsl = vertexHlsl;
        this.fragmentHlsl = fragmentHlsl;
        this.flipY = flipY;
    }

    public RenderPipeline info() {
        return this.info;
    }

    public long handle() {
        return this.handle;
    }

    /** P31：是否为 Y-flip 变体（仅 GUI 离屏 pass 使用）。 */
    public boolean flipY() {
        return this.flipY;
    }

    /**
     * 从 pipeline info 的 BindGroupLayout + shader modules 动态推导绑定条目列表，
     * 逻辑镜像 {@link Dx12ShaderCompiler#addToBindGroup}。
     */
    public List<Dx12BindGroupEntry> buildBindings() {
        List<BindGroupLayout.UniformDescription> allUniforms =
            BindGroupLayout.flattenUniforms(this.info.getBindGroupLayouts());
        List<String> allSamplers =
            BindGroupLayout.flattenSamplers(this.info.getBindGroupLayouts());
        Set<String> allNames = new HashSet<>();
        allUniforms.forEach(u -> allNames.add(u.name()));
        allNames.addAll(allSamplers);

        List<Dx12BindGroupEntry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Dx12IntermediaryShaderModule shader : List.of(vertexShader, fragmentShader)) {
            for (Dx12IntermediaryShaderModule.SpvUniformBuffer buf : shader.uniformBuffers()) {
                String name = buf.name();
                if (!allNames.contains(name) || !seen.add(name)) continue;
                entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.UNIFORM_BUFFER, name, null));
            }
            for (Dx12IntermediaryShaderModule.SpvSampler sampler : shader.samplers()) {
                String name = sampler.name();
                if (!allNames.contains(name) || !seen.add(name)) continue;
                BindGroupLayout.UniformDescription utbDesc = allUniforms.stream()
                    .filter(d -> d.name().equals(name)).findFirst().orElse(null);
                if (utbDesc != null) {
                    entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.TEXEL_BUFFER, name,
                        utbDesc.gpuFormat()));
                } else {
                    entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.SAMPLED_IMAGE, name, null));
                }
            }
        }
        return entries;
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
