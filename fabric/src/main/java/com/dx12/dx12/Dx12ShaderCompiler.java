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
import java.util.List;
import java.util.Optional;
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
        // P6 诊断：所有 pipeline 强制输出纯绿色，区分"管线问题"vs"数据问题"
        // 去掉管线名过滤，覆盖 core/terrain 等主渲染管线（之前只覆盖 gui/panorama）。
        // 屏幕变绿 => 管线通路正常，问题在 shader 数据（纹理绑定/UBO/culling 等）；
        // 仍黑屏 => 管线根本问题（draw 未提交/状态错配/swapchain 等）。
        String loc = pipeline.getLocation().toString();
        System.err.println("[dx12-java] [DIAG] " + loc
            + " frag before= " + fragmentHlsl.length() + " bytes");
        String oldLenStr = String.valueOf(fragmentHlsl.length());
        fragmentHlsl = stripFragMainAndReplace(fragmentHlsl);
        System.err.println("[dx12-java] [DIAG] " + loc
            + " frag after= " + fragmentHlsl.length() + " bytes"
            + " changed=" + (fragmentHlsl.length() != Integer.parseInt(oldLenStr)));
        // 打印修改后 shader 的前 120 字符，确认 GREEN 注入已生效
        String head = fragmentHlsl.length() > 120
            ? fragmentHlsl.substring(0, 120) + "..."
            : fragmentHlsl;
        System.err.println("[dx12-java] [DIAG] " + loc + " frag head=" + head);
        System.err.println("[dx12-java] [DIAG] " + loc + " frag forced GREEN (all pipelines)");
        // Fix S2：语义名称由 toHlsl() 通过 spvc_compiler_hlsl_add_vertex_attribute_remap 注入，
        // 此处直接按 vertex format 位置生成：location 0 → POSITION，其余 → TEXCOORD<n>。
        List<String> semanticNames = new ArrayList<>();
        for (int i = 0; i < vertexInputNames.size(); ++i) {
            semanticNames.add(i == 0 ? "POSITION" : ("TEXCOORD" + (i - 1)));
        }
        return new Dx12CompiledShader(vertexHlsl, fragmentHlsl, entries, vertexInputNames, semanticNames);
    }

    @Override
    public void close() {
        Shaderc.shaderc_compile_options_release(this.shaderOptions);
        Shaderc.shaderc_compiler_release(this.shaderCompiler);
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
                System.err.printf("[dx12-java] addToBindGroup: uniform '%s' not found in %s%n",
                    name, pipeline.getLocation());
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
                    System.err.printf("[dx12-java] addToBindGroup: UTB '%s' in %s has dim %d (expected 5)%n",
                        name, pipeline.getLocation(), sampler.dimensions());
                    throw new ShaderCompileException("UTB (" + name + ") must have type of SpvDimBuffer");
                }
                if (!entries.stream().noneMatch(e -> e.type() == Dx12BindGroupEntry.Type.TEXEL_BUFFER
                    && e.name().equals(name))) continue;
                entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.TEXEL_BUFFER, name,
                    uniformDescription.get().gpuFormat()));
                continue;
            }
            if (BindGroupLayout.flattenSamplers(pipeline.getBindGroupLayouts()).stream().noneMatch(name::equals)) {
                System.err.printf("[dx12-java] addToBindGroup: sampler '%s' not found in %s%n",
                    name, pipeline.getLocation());
                throw new ShaderCompileException("Unable to find shader defined uniform (" + name + ")");
            }
            if (sampler.dimensions() != 1 && sampler.dimensions() != 3) {
                System.err.printf("[dx12-java] addToBindGroup: sampled texture '%s' in %s has dim %d (expected 1 or 3)%n",
                    name, pipeline.getLocation(), sampler.dimensions());
                throw new ShaderCompileException(
                    "Sampled texture (" + name + ") must have type of SpvDim2D or SpvDimCube");
            }
            if (!entries.stream().noneMatch(e -> e.type() == Dx12BindGroupEntry.Type.SAMPLED_IMAGE
                && e.name().equals(name))) continue;
            entries.add(new Dx12BindGroupEntry(Dx12BindGroupEntry.Type.SAMPLED_IMAGE, name, null));
        }
    }

    /** 精确匹配含嵌套花括号（for/条件块等）的 frag_main 函数体并替换为纯绿色输出 */
    private static String stripFragMainAndReplace(String hlsl) {
        String[] lines = hlsl.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < lines.length) {
            String trimmed = lines[i].trim();
            // 匹配 "void frag_main()" 这一行（可能有空白字符），spvc 输出的 { 可能在下一行
            if (trimmed.matches("void\\s+frag_main\\(\\).*")) {
                // 从当前行开始统计括号
                int braceCount = 0;
                for (char c : lines[i].toCharArray()) {
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                }
                // 如果当前行没有开括号，继续读下一行直到找到 {
                while (braceCount <= 0 && i + 1 < lines.length) {
                    i++;
                    for (char c : lines[i].toCharArray()) {
                        if (c == '{') braceCount++;
                        else if (c == '}') braceCount--;
                    }
                }
                sb.append("void frag_main() { fragColor = float4(0.0, 1.0, 0.0, 1.0); }\n");
                i++;
                // 继续读行直到平衡所有括号
                while (i < lines.length && braceCount > 0) {
                    for (char c : lines[i].toCharArray()) {
                        if (c == '{') braceCount++;
                        else if (c == '}') braceCount--;
                    }
                    i++;
                }
                continue;
            }
            sb.append(lines[i]).append('\n');
            i++;
        }
        return sb.toString();
    }
}
