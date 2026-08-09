package com.dx12.dx12;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderDefines;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

/**
 * DX12 着色器编译器：GLSL -> SPIR-V（shaderc）-> 反射 + rebind（spvc）->
 * HLSL SM5.1（spvc）。镜像官方 {@code com.mojang.blaze3d.vulkan.glsl.GlslCompiler}，
 * 仅把最后的 "创建 Vulkan shader module" 换成 "输出 HLSL 字符串"（DXBC 编译在
 * 原生层 D3DCompile 完成）。
 */
@Environment(EnvType.CLIENT)
public class Dx12ShaderCompiler implements AutoCloseable {
    private final long shaderCompiler;
    private final long shaderOptions;
    private final ShaderDefines globalDefines;

    public Dx12ShaderCompiler() {
        this.shaderCompiler = Shaderc.shaderc_compiler_initialize();
        this.shaderOptions = Shaderc.shaderc_compile_options_initialize();
        Shaderc.shaderc_compile_options_set_target_env(this.shaderOptions, 0, 0x402000);  // Vulkan 1.2
        Shaderc.shaderc_compile_options_set_auto_bind_uniforms(this.shaderOptions, true);
        Shaderc.shaderc_compile_options_set_auto_map_locations(this.shaderOptions, true);
        Shaderc.shaderc_compile_options_set_generate_debug_info(this.shaderOptions);
        Shaderc.shaderc_compile_options_set_optimization_level(this.shaderOptions, 0);
        this.globalDefines = ShaderDefines.builder()
            .define("gl_VertexID", "gl_VertexIndex")
            .define("gl_InstanceID", "gl_InstanceIndex")
            .build();
    }

    /**
     * GLSL -> SPIR-V（shaderc），并立即用 spvc 反射出绑定信息。
     * 对应官方 {@code GlslCompiler.createIntermediary}。
     */
    public Dx12IntermediaryShaderModule createIntermediary(String filename, String source, ShaderType type)
        throws ShaderCompileException {
        source = GlslPreprocessor.injectDefines(source, this.globalDefines);
        int shaderType = type == ShaderType.FRAGMENT ? 1 : 0;  // SHADERC_FRAGMENT_SHADER=1, VERTEX=0
        ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, false);
        ByteBuffer filenameBuffer = MemoryUtil.memUTF8(filename);
        ByteBuffer entrypointBuffer = MemoryUtil.memUTF8("main");
        long result = Shaderc.shaderc_compile_into_spv(this.shaderCompiler, sourceBuffer,
            shaderType, filenameBuffer, entrypointBuffer, this.shaderOptions);
        try {
            int status = Shaderc.shaderc_result_get_compilation_status(result);
            if (status != 0) {
                throw new ShaderCompileException(
                    "Couldn't parse GLSL: " + Shaderc.shaderc_result_get_error_message(result));
            }
            ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
            ByteBuffer copy = MemoryUtil.memCalloc(spirv.remaining());
            MemoryUtil.memCopy(spirv, copy);
            return Dx12IntermediaryShaderModule.createFromSpirv(filename, copy);
        } finally {
            Shaderc.shaderc_result_release(result);
            MemoryUtil.memFree(entrypointBuffer);
            MemoryUtil.memFree(filenameBuffer);
            MemoryUtil.memFree(sourceBuffer);
        }
    }

    /**
     * 组装一条管线的着色器：收集绑定（addToBindGroup）-> rebind（重写
     * SPIR-V binding/location）-> 输出 HLSL。
     * 对应官方 {@code GlslCompiler.compile}。
     */
    public Dx12CompiledShader compile(RenderPipeline pipeline,
        Dx12IntermediaryShaderModule vertex, Dx12IntermediaryShaderModule fragment)
        throws ShaderCompileException {
        List<Dx12BindGroupEntry> entries = new ArrayList<>();
        addToBindGroup(entries, vertex, pipeline);
        addToBindGroup(entries, fragment, pipeline);

        List<String> vertexOutputNames = new ArrayList<>();
        for (Dx12IntermediaryShaderModule.SpvVariable variable : vertex.outputs()) {
            vertexOutputNames.add(variable.name());
        }
        List<String> vertexInputNames = new ArrayList<>();
        for (VertexFormat vertexFormat : pipeline.getVertexFormatBindings()) {
            if (vertexFormat == null) continue;
            for (VertexFormatElement attribute : vertexFormat.getElements()) {
                vertexInputNames.add(attribute.name());
            }
        }
        vertex.rebind(vertexInputNames, entries);
        fragment.rebind(vertexOutputNames, entries);

        String vertexHlsl = vertex.toHlsl(true);
        String fragmentHlsl = fragment.toHlsl(false);
        List<String> vertexShaderInputs = new ArrayList<>();
        for (Dx12IntermediaryShaderModule.SpvVariable input : vertex.inputs()) {
            vertexShaderInputs.add(input.name());
        }
        List<String> semanticNames = extractHlslSemanticNames(vertexHlsl, vertexShaderInputs);
        return new Dx12CompiledShader(vertexHlsl, fragmentHlsl, entries, vertexShaderInputs, semanticNames);
    }

    @Override
    public void close() {
        Shaderc.shaderc_compile_options_release(this.shaderOptions);
        Shaderc.shaderc_compiler_release(this.shaderCompiler);
    }

    /**
     * 从 spvc 生成的 HLSL 顶点着色器源码中提取 semantic 名称。
     * spvc 生成的格式：{@code TypeName VarName : SEMANTIC;}（行内或分行），
     * 按 vertexShaderInputs 顺序匹配，确保 semantic 列表与输入变量一一对应。
     */
    private static List<String> extractHlslSemanticNames(String vertexHlsl,
        List<String> inputNames) {
        List<String> result = new ArrayList<>();
        // spvc HLSL 输入声明格式（兼容行内和换行）：
        //   float3 Position : POSITION;
        //   float4 Color : TEXCOORD0;
        Pattern p = Pattern.compile(
            "[\\w<>\\*\\s]+?\\b([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*([A-Za-z_][A-Za-z0-9]*)\\s*;");
        Matcher m = p.matcher(vertexHlsl);
        while (m.find()) {
            String varName = m.group(1);
            String semantic = m.group(2);
            if (inputNames.contains(varName) && !result.contains(varName)) {
                result.add(semantic);
            }
        }
        // 兜底：如果正则未匹配到全部（spvc 输出格式可能不同），
        // 回退到 TEXCOORD<location> 惯例（spvc auto_bind 时的默认行为）。
        if (result.size() != inputNames.size()) {
            result.clear();
            for (int i = 0; i < inputNames.size(); i++) {
                result.add("TEXCOORD" + i);
            }
        }
        return result;
    }

    /** 镜像官方 addToBindGroup：按 shader 声明的 UBO/sampler 顺序收集绑定。 */
    private static void addToBindGroup(List<Dx12BindGroupEntry> entries,
        Dx12IntermediaryShaderModule shader, RenderPipeline pipeline) throws ShaderCompileException {
        Optional<BindGroupLayout.UniformDescription> uniformDescription;
        for (Dx12IntermediaryShaderModule.SpvUniformBuffer buffer : shader.uniformBuffers()) {
            String name = buffer.name();
            uniformDescription = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()).stream()
                .filter(d -> d.name().equals(name)).findFirst();
            if (uniformDescription.isEmpty()) {
                throw new ShaderCompileException("Unable to find shader defined uniform (" + name + ")");
            }
            if (!entries.stream().noneMatch(e -> e.type() == Dx12BindGroupEntry.Type.UNIFORM_BUFFER
                && e.name().equals(name))) continue;
            entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.UNIFORM_BUFFER, name, null));
        }
        for (Dx12IntermediaryShaderModule.SpvSampler sampler : shader.samplers()) {
            String name = sampler.name();
            uniformDescription = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()).stream()
                .filter(d -> d.name().equals(name)).findFirst();
            if (uniformDescription.isPresent()) {
                if (sampler.dimensions() != 5) {
                    throw new ShaderCompileException("UTB (" + name + ") must have type of SpvDimBuffer");
                }
                if (!entries.stream().noneMatch(e -> e.type() == Dx12BindGroupEntry.Type.TEXEL_BUFFER
                    && e.name().equals(name))) continue;
                entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.TEXEL_BUFFER, name,
                    uniformDescription.get().gpuFormat()));
                continue;
            }
            if (BindGroupLayout.flattenSamplers(pipeline.getBindGroupLayouts()).stream().noneMatch(name::equals)) {
                throw new ShaderCompileException("Unable to find shader defined uniform (" + name + ")");
            }
            if (sampler.dimensions() != 1 && sampler.dimensions() != 3) {
                throw new ShaderCompileException(
                    "Sampled texture (" + name + ") must have type of SpvDim2D or SpvDimCube");
            }
            if (!entries.stream().noneMatch(e -> e.type() == Dx12BindGroupEntry.Type.SAMPLED_IMAGE
                && e.name().equals(name))) continue;
            entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.SAMPLED_IMAGE, name, null));
        }
    }
}
