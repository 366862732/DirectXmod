package com.dx12.dx12;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 一条 {@link RenderPipeline} 的 DX12 编译产物：顶点/片元 HLSL 源码 +
 * 实际绑定资源列表（顺序决定 SPIR-V binding 0..n-1，也决定原生层
 * root signature 的 CBV b{i} / SRV t{i} / sampler s{i} 布局）+
 * 顶点着色器声明过的输入变量名（用于推导 D3D12 input layout 的 TEXCOORD 语义）。
 */
@Environment(EnvType.CLIENT)
public record Dx12CompiledShader(
    String vertexHlsl,
    String fragmentHlsl,
    List<Dx12BindGroupEntry> entries,
    List<String> vertexShaderInputs) {
}
