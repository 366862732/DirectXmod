package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderDefines;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

/**
 * GLSL -> SPIR-V (shaderc) -> HLSL SM4.0 (SPIRV-Cross) for the D3D11 backend.
 *
 * <p>SM4.0 is the common denominator of Direct3D 10.0/10.1 hardware.  The
 * generated HLSL is compiled by {@code D3DCompile} in {@code dx11_mc.dll}.
 * Unlike the D3D12 backend, uniform buffers use the {@code b#} register space
 * and textures use {@code t#}; both are assigned independently.</p>
 */
@Environment(EnvType.CLIENT)
final class Dx11ShaderCompiler implements AutoCloseable {
    private static final Pattern GL_POSITION_ASSIGNMENT =
        Pattern.compile("(?m)^[ \\t]*gl_Position\\s*=\\s*[^;\\n]+;");
    // SPIRV-Cross copies the local gl_Position into the stage output exactly
    // once, at the end of main().  Transforming there keeps gl_Position in GL
    // clip space for any shader that reads it after assigning it (for example
    // rendertype_end_portal.vsh derives screen-space texture coordinates from
    // it).  The line anchor deliberately excludes the final copy.
    private static final Pattern STAGE_OUTPUT_POSITION =
        Pattern.compile("(?m)^([ \\t]*)stage_output\\.gl_Position\\s*=\\s*gl_Position\\s*;");
    private static final Pattern POINT_SIZE_ASSIGNMENT =
        Pattern.compile("\\bgl_PointSize\\s*=\\s*[^;\\n]*;");
    // glslang reserves the bare word `sampler` (it is a keyword in newer GLSL
    // dialects), but vanilla terrain.fsh uses it as a parameter name.  Rename
    // standalone occurrences only; `sampler2D` etc. are left untouched.
    private static final Pattern STANDALONE_SAMPLER =
        Pattern.compile("\\bsampler\\b");
    // SPIRV-Cross emits `Buffer<int4> CloudFaces` for isamplerBuffer.  D3D11
    // feature level 10.x has no typed buffers, so it is replaced by a fixed
    // 8192-wide R8G8B8A8_SINT texture (see passSetTexelBuffer).
    private static final Pattern TYPED_BUFFER_DECL =
        Pattern.compile("\\bBuffer<\\s*(int4|uint4|int|uint|float4|float)\\s*>\\s+(\\w+)");
    private static final Pattern TYPED_BUFFER_LOAD =
        Pattern.compile("\\b(\\w+)\\s*\\.\\s*Load\\s*\\(");
    private static final int TEXEL_BUFFER_WIDTH = 8192;

    private final long shaderCompiler;
    private final long shaderOptions;
    private final ShaderDefines globalDefines;

    Dx11ShaderCompiler() {
        this.shaderCompiler = Shaderc.shaderc_compiler_initialize();
        this.shaderOptions = Shaderc.shaderc_compile_options_initialize();
        if (this.shaderCompiler == 0L || this.shaderOptions == 0L) {
            throw new IllegalStateException("Could not initialize shaderc");
        }
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
     * Compiles one shader stage and reflects its resources.
     *
     * @param defines the pipeline's defines (already merged with the global
     *                {@code gl_VertexID}/{@code gl_InstanceID} mapping)
     */
    Dx11IntermediaryShaderModule createIntermediary(
        String filename, String source, ShaderType type, ShaderDefines defines)
        throws Dx11ShaderCompileException {
        source = GlslPreprocessor.injectDefines(source, this.globalDefines.withOverrides(defines));
        // D3D11 SM4.0 has no point size; GLSL 330 requires the assignment for
        // point sprites, but SPIRV-Cross cannot represent it in SM4.0.
        source = POINT_SIZE_ASSIGNMENT.matcher(source).replaceAll("");
        // glslang treats the bare identifier `sampler` as a reserved keyword.
        source = STANDALONE_SAMPLER.matcher(source).replaceAll("gl4dx_sampler");
        int shaderType = type == ShaderType.FRAGMENT ? 1 : 0;  // SHADERC_FRAGMENT_SHADER=1
        ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, false);
        ByteBuffer filenameBuffer = MemoryUtil.memUTF8(filename);
        ByteBuffer entrypointBuffer = MemoryUtil.memUTF8("main");
        long result = Shaderc.shaderc_compile_into_spv(
            this.shaderCompiler, sourceBuffer, shaderType, filenameBuffer,
            entrypointBuffer, this.shaderOptions);
        if (result == 0L) {
            // shaderc returns null when it cannot even start a compilation
            // (for example when the compiler or options are invalid).  Every
            // result accessor would dereference that null pointer, so fail
            // here instead of crashing the JVM.
            MemoryUtil.memFree(entrypointBuffer);
            MemoryUtil.memFree(filenameBuffer);
            MemoryUtil.memFree(sourceBuffer);
            throw new Dx11ShaderCompileException(
                "shaderc could not compile " + filename + " (no compilation result)");
        }
        try {
            int status = Shaderc.shaderc_result_get_compilation_status(result);
            if (status != 0) {
                throw new Dx11ShaderCompileException(
                    "Couldn't parse GLSL (" + filename + "): "
                        + Shaderc.shaderc_result_get_error_message(result));
            }
            ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
            if (spirv == null || spirv.remaining() == 0) {
                throw new Dx11ShaderCompileException(
                    "shaderc returned no SPIR-V for " + filename);
            }
            ByteBuffer copy = MemoryUtil.memCalloc(spirv.remaining());
            MemoryUtil.memCopy(spirv, copy);
            return Dx11IntermediaryShaderModule.createFromSpirv(filename, copy);
        } finally {
            Shaderc.shaderc_result_release(result);
            MemoryUtil.memFree(entrypointBuffer);
            MemoryUtil.memFree(filenameBuffer);
            MemoryUtil.memFree(sourceBuffer);
        }
    }

    /**
     * Collects the bindings of both stages, rewrites SPIR-V locations/bindings
     * and emits HLSL for each stage.
     */
    Dx11CompiledShader compile(
        RenderPipeline pipeline,
        Dx11IntermediaryShaderModule vertex,
        Dx11IntermediaryShaderModule fragment)
        throws Dx11ShaderCompileException {
        List<Dx11CompiledShader.Binding> bindings = new ArrayList<>();
        addToBindGroup(bindings, vertex, pipeline);
        addToBindGroup(bindings, fragment, pipeline);

        List<String> vertexInputNames =
            new ArrayList<>(pipeline.getVertexFormat().getElementAttributeNames());

        Dx11IntermediaryShaderModule vertexWork = vertex.cloneForCompile();
        Dx11IntermediaryShaderModule fragmentWork = fragment.cloneForCompile();
        List<String> linkedInputs;
        String vertexHlsl;
        String fragmentHlsl;
        try {
            List<String> vertexOutputNames = vertexWork.rebindOutputs();
            linkedInputs = vertexWork.rebind(vertexInputNames, bindings, false);
            fragmentWork.rebind(vertexOutputNames, bindings, true);

            vertexHlsl = vertexWork.toHlsl(true);
            fragmentHlsl = fragmentWork.toHlsl(false);
        } finally {
            // cloneForCompile() allocates a private SPIR-V copy; release it
            // after the HLSL has been emitted so repeated shader reloads do
            // not leak native memory.
            if (vertexWork != vertex) {
                vertexWork.close();
            }
            if (fragmentWork != fragment) {
                fragmentWork.close();
            }
        }
        vertexHlsl = injectVertexTransform(vertexHlsl);
        vertexHlsl = rewriteTypedBuffers(vertexHlsl);
        fragmentHlsl = rewriteTypedBuffers(fragmentHlsl);

        List<String> semanticNames = new ArrayList<>();
        for (int i = 0; i < linkedInputs.size(); ++i) {
            semanticNames.add(i == 0 ? "POSITION" : ("TEXCOORD" + (i - 1)));
        }

        int uniformSlotCount = 0;
        for (Dx11CompiledShader.Binding binding : bindings) {
            if (binding.type() == Dx11CompiledShader.Type.UNIFORM_BUFFER) {
                uniformSlotCount = Math.max(uniformSlotCount, binding.slot() + 1);
            }
        }
        // The native layer grows a slot on demand; a modest initial size avoids
        // repeated re-creation for the common small UBOs.
        int[] uniformSizes = new int[uniformSlotCount];
        for (int i = 0; i < uniformSizes.length; ++i) {
            uniformSizes[i] = 1024;
        }

        return new Dx11CompiledShader(
            vertexHlsl, fragmentHlsl, bindings, linkedInputs, semanticNames, uniformSizes);
    }

    @Override
    public void close() {
        Shaderc.shaderc_compile_options_release(this.shaderOptions);
        Shaderc.shaderc_compiler_release(this.shaderCompiler);
    }

    private static void addToBindGroup(
        List<Dx11CompiledShader.Binding> entries,
        Dx11IntermediaryShaderModule shader,
        RenderPipeline pipeline)
        throws Dx11ShaderCompileException {
        for (Dx11IntermediaryShaderModule.SpvUniformBuffer buffer : shader.uniformBuffers()) {
            String name = buffer.name();
            if (findBinding(entries, name) != null) {
                continue;
            }
            int slot = nextSlot(entries, Dx11CompiledShader.Type.UNIFORM_BUFFER);
            entries.add(new Dx11CompiledShader.Binding(
                Dx11CompiledShader.Type.UNIFORM_BUFFER, name, slot, null));
        }

        for (Dx11IntermediaryShaderModule.SpvSampler sampler : shader.samplers()) {
            String name = sampler.name();
            if (findBinding(entries, name) != null) {
                continue;
            }
            RenderPipeline.UniformDescription description = findUniform(pipeline, name);
            if (description != null && description.type() == com.mojang.blaze3d.shaders.UniformType.TEXEL_BUFFER) {
                if (sampler.dimensions() != 5) {  // SpvDimBuffer
                    throw new Dx11ShaderCompileException(
                        "UTB (" + name + ") must have type of SpvDimBuffer in " + pipeline.getLocation());
                }
                TextureFormat format = description.textureFormat();
                if (format == null) {
                    throw new Dx11ShaderCompileException(
                        "UTB (" + name + ") has no texture format in " + pipeline.getLocation());
                }
                int slot = nextSlot(entries, Dx11CompiledShader.Type.TEXEL_BUFFER);
                entries.add(new Dx11CompiledShader.Binding(
                    Dx11CompiledShader.Type.TEXEL_BUFFER, name, slot, format));
            } else {
                if (sampler.dimensions() != 1 && sampler.dimensions() != 3) {
                    throw new Dx11ShaderCompileException(
                        "Sampled texture (" + name + ") must have type of SpvDim2D or SpvDimCube in "
                            + pipeline.getLocation());
                }
                int slot = nextSlot(entries, Dx11CompiledShader.Type.SAMPLED_IMAGE);
                entries.add(new Dx11CompiledShader.Binding(
                    Dx11CompiledShader.Type.SAMPLED_IMAGE, name, slot, null));
            }
        }
    }

    private static RenderPipeline.@Nullable UniformDescription findUniform(
        RenderPipeline pipeline, String name) {
        for (RenderPipeline.UniformDescription description : pipeline.getUniforms()) {
            if (description.name().equals(name)) {
                return description;
            }
        }
        return null;
    }

    private static Dx11CompiledShader.@Nullable Binding findBinding(
        List<Dx11CompiledShader.Binding> entries, String name) {
        for (Dx11CompiledShader.Binding entry : entries) {
            if (entry.name().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    private static int nextSlot(
        List<Dx11CompiledShader.Binding> entries, Dx11CompiledShader.Type type) {
        boolean uniform = type == Dx11CompiledShader.Type.UNIFORM_BUFFER;
        int next = 0;
        for (Dx11CompiledShader.Binding entry : entries) {
            boolean entryUniform = entry.type() == Dx11CompiledShader.Type.UNIFORM_BUFFER;
            if (entryUniform == uniform) {
                next = Math.max(next, entry.slot() + 1);
            }
        }
        return next;
    }

    /**
     * Injects the GL -> D3D clip-space conversion at the point where the local
     * {@code gl_Position} is copied to the stage output.
     *
     * <p>The Y flip makes render-target memory match GL's bottom-up layout.
     * The Z transform maps GL's {@code -w..w} clip range to D3D's {@code 0..w}
     * range.  Doing this at the final copy keeps {@code gl_Position} in GL clip
     * space while the shader body runs, so shaders such as
     * {@code rendertype_end_portal.vsh} that derive screen-space coordinates
     * from it still see the original value.</p>
     *
     * <p>If SPIRV-Cross did not emit the final copy (possible for unusual or
     * modded shaders), fall back to transforming every real
     * {@code gl_Position = ...;} assignment.</p>
     */
    static String injectVertexTransform(String hlsl) {
        Matcher output = STAGE_OUTPUT_POSITION.matcher(hlsl);
        StringBuilder builder = new StringBuilder();
        int last = 0;
        boolean found = false;
        while (output.find()) {
            String indent = output.group(1);
            builder.append(hlsl, last, output.start());
            appendClipSpaceTransform(builder, indent);
            builder.append(hlsl, output.start(), output.end());
            last = output.end();
            found = true;
        }
        if (found) {
            builder.append(hlsl.substring(last));
            return builder.toString();
        }

        Matcher matcher = GL_POSITION_ASSIGNMENT.matcher(hlsl);
        last = 0;
        while (matcher.find()) {
            String statement = matcher.group();
            String indent = statement.substring(
                0, statement.length() - statement.stripLeading().length());
            builder.append(hlsl, last, matcher.end());
            builder.append('\n');
            appendClipSpaceTransform(builder, indent);
            last = matcher.end();
        }
        if (last == 0) {
            return hlsl;
        }
        builder.append(hlsl.substring(last));
        return builder.toString();
    }

    private static void appendClipSpaceTransform(StringBuilder builder, String indent) {
        builder.append(indent).append("gl_Position.y = -gl_Position.y;\n")
            .append(indent)
            .append("gl_Position.z = (gl_Position.z + gl_Position.w) * 0.5;\n");
    }

    /**
     * Replaces SM5 typed-buffer declarations/loads with the emulated
     * {@code Texture2D<int4>} / {@code Texture2D<float4>} layout.
     */
    static String rewriteTypedBuffers(String hlsl) {
        Matcher declarations = TYPED_BUFFER_DECL.matcher(hlsl);
        Map<String, String> elementTypes = new LinkedHashMap<>();
        StringBuilder declared = new StringBuilder();
        int last = 0;
        while (declarations.find()) {
            String element = declarations.group(1);
            String name = declarations.group(2);
            String replacement;
            if (element.endsWith("4")) {
                replacement = "Texture2D<" + element + "> " + name;
            } else {
                replacement = "Texture2D<" + element + "4> " + name;
            }
            declared.append(hlsl, last, declarations.start()).append(replacement);
            last = declarations.end();
            elementTypes.put(name, element);
        }
        if (last == 0) {
            return hlsl;
        }
        declared.append(hlsl.substring(last));
        String rewritten = declared.toString();

        for (Map.Entry<String, String> entry : elementTypes.entrySet()) {
            String name = entry.getKey();
            Pattern load = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\.\\s*Load\\s*\\(");
            Matcher matcher = load.matcher(rewritten);
            StringBuilder builder = new StringBuilder();
            int cursor = 0;
            while (matcher.find()) {
                int open = rewritten.indexOf('(', matcher.start());
                int close = findMatchingParen(rewritten, open);
                if (close < 0) {
                    break;
                }
                String indexExpression = rewritten.substring(open + 1, close);
                String wrapped = name + ".Load(int3((" + indexExpression + ") & "
                    + (TEXEL_BUFFER_WIDTH - 1) + ", (" + indexExpression + ") >> 13, 0))";
                builder.append(rewritten, cursor, matcher.start()).append(wrapped);
                cursor = close + 1;
                matcher.region(cursor, rewritten.length());
            }
            if (cursor != 0) {
                builder.append(rewritten.substring(cursor));
                rewritten = builder.toString();
            }
        }
        return rewritten;
    }

    private static int findMatchingParen(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '(') {
                ++depth;
            } else if (c == ')') {
                --depth;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
