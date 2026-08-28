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
    // P20 诊断：所有 pipeline 强制输出纯绿色，区分"管线问题"vs"数据问题"。
    // 默认关闭（DX12_DIAG_GREEN=1 开启），避免正常游戏时闪绿屏。
    private static final boolean DIAG_GREEN = "1".equals(System.getenv("DX12_DIAG_GREEN"));
    // 诊断白色输出：验证管线通路（尤其 mojang_logo 管线）。
    // DX12_DIAG_WHITE=1 强制 fragment shader 输出纯白色，屏幕变白=管线通，仍红=纹理/上传问题。
    private static final boolean DIAG_WHITE = "1".equals(System.getenv("DX12_DIAG_WHITE"));

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
        Dx12IntermediaryShaderModule vertex, Dx12IntermediaryShaderModule fragment,
        boolean flipY)
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
        // P28：animate_sprite 图集合成管线（blit/interpolate）的顶点 shader 用 GL
        // bottom-up 投影（ortho(0,W,0,H)）配合 vanilla 的 bottom-up sprite 坐标。
        // D3D12 的 NDC Y 与 GL 相反（NDC -1 在顶部），导致图集内容整体垂直翻转
        // （dump 证实：所有稀疏图集内容落在 y[H-h..H-1]）。修复：在顶点输出上
        // 翻转 Y，只作用于图集上传管线，不影响屏幕 GUI（其坐标是 top-down 本就正确）。
        // P31：flipY 变体——GUI 离屏纹理（物品图集 GuiItemAtlas / PIP 实体纹理，
        // 颜色附件 usage=13 且带深度附件）以 invertY 正交投影渲染到 D3D12 纹理，
        // GL 的"双翻转抵消"机制在 D3D12 下失效，导致物品图标不可见 / 实体倒置。
        // 同样注入 Y-flip（方向与 P28 一致），只作用于离屏 pass 的管线变体，
        // 不影响主世界渲染方向。
        String pipelineLocation = pipeline.getLocation().toString();
        if (pipelineLocation.contains("animate_sprite") || flipY) {
            String before = vertexHlsl;
            vertexHlsl = injectVertexYFlip(vertexHlsl);
            if (!before.equals(vertexHlsl)) {
                System.err.println("[dx12-java] " + (flipY ? "[P31]" : "[P28]")
                    + " inject Y-flip into " + pipelineLocation + " vertex HLSL"
                    + (flipY ? " (flipY variant)" : ""));
                System.err.flush();
            } else {
                System.err.println("[dx12-java] " + (flipY ? "[P31]" : "[P28]")
                    + " WARN: no gl_Position assignment found in " + pipelineLocation
                    + " vertex HLSL, Y-flip NOT injected");
                System.err.flush();
            }
        }
        if (DIAG_GREEN || DIAG_WHITE) {
            // P6 诊断：强制输出纯绿/纯白，区分"管线问题"vs"数据问题"
            // 屏幕变绿/白 => 管线通路正常，问题在纹理数据；仍红 => 管线根本问题。
            String loc = pipeline.getLocation().toString();
            System.err.println("[dx12-java] [DIAG] " + loc
                + " frag before= " + fragmentHlsl.length() + " bytes");
            String oldLenStr = String.valueOf(fragmentHlsl.length());
            fragmentHlsl = DIAG_GREEN
                ? stripFragMainAndReplace(fragmentHlsl)
                : stripFragMainAndWhite(fragmentHlsl);
            System.err.println("[dx12-java] [DIAG] " + loc
                + " frag after= " + fragmentHlsl.length() + " bytes"
                + " changed=" + (fragmentHlsl.length() != Integer.parseInt(oldLenStr)));
            String head = fragmentHlsl.length() > 120
                ? fragmentHlsl.substring(0, 120) + "..."
                : fragmentHlsl;
            System.err.println("[dx12-java] [DIAG] " + loc + " frag head=" + head);
            System.err.println("[dx12-java] [DIAG] " + loc
                + " frag forced " + (DIAG_GREEN ? "GREEN" : "WHITE") + " (all pipelines)");
        }
        // Fix S2：语义名称由 toHlsl() 通过 spvc_compiler_hlsl_add_vertex_attribute_remap 注入，
        // 基准 é vertex.inputs() (SPIR-V unique inputs após rebind), não vertexInputNames (formato).
        // BUG-01 fix: usar vertex.inputs().size() garante alinhamento com spvc remap count.
        List<String> semanticNames = new ArrayList<>();
        for (int i = 0; i < vertex.inputs().size(); ++i) {
            semanticNames.add(i == 0 ? "POSITION" : ("TEXCOORD" + (i - 1)));
        }
        return new Dx12CompiledShader(vertexHlsl, fragmentHlsl, entries, vertexInputNames, semanticNames);
    }

    @Override
    public void close() {
        Shaderc.shaderc_compile_options_release(this.shaderOptions);
        Shaderc.shaderc_compiler_release(this.shaderCompiler);
    }

    /**
     * P28：在 spvc 生成的顶点 HLSL 中，于每个 {@code gl_Position = ...;} 赋值后插入
     * {@code gl_Position.y = -gl_Position.y;}，把 GL bottom-up NDC 修正为 D3D12
     * top-down NDC（图集 blit 专用；ortho 下 w=1，clip 空间直接翻转 Y 即正确）。
     * P31：实体管线顶点 shader 多为多分支 gl_VertexID 写法，每个分支各有一个
     * gl_Position 赋值——必须翻转所有赋值，只翻第一个会导致几何错位。
     */
    private static String injectVertexYFlip(String hlsl) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?m)^([ \\t]*gl_Position\\s*=\\s*[^;]+;)$")
            .matcher(hlsl);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        int count = 0;
        while (m.find()) {
            String stmt = m.group(1);
            String indent = stmt.substring(0, stmt.length() - stmt.stripLeading().length());
            sb.append(hlsl, last, m.start());
            sb.append(stmt).append('\n').append(indent).append("gl_Position.y = -gl_Position.y;");
            last = m.end();
            count++;
        }
        if (count == 0) {
            return hlsl;
        }
        sb.append(hlsl.substring(last));
        return sb.toString();
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

    /** 同 stripFragMainAndReplace，但输出纯白色（用于诊断 mojang_logo 管线）。 */
    private static String stripFragMainAndWhite(String hlsl) {
        String[] lines = hlsl.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < lines.length) {
            String trimmed = lines[i].trim();
            if (trimmed.matches("void\\s+frag_main\\(\\).*")) {
                int braceCount = 0;
                for (char c : lines[i].toCharArray()) {
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                }
                while (braceCount <= 0 && i + 1 < lines.length) {
                    i++;
                    for (char c : lines[i].toCharArray()) {
                        if (c == '{') braceCount++;
                        else if (c == '}') braceCount--;
                    }
                }
                sb.append("void frag_main() { fragColor = float4(1.0, 1.0, 1.0, 1.0); }\n");
                i++;
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
