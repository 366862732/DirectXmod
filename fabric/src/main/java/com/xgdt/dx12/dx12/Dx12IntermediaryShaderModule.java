package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcHlslVertexAttributeRemap;
import org.lwjgl.util.spvc.SpvcReflectedResource;
/**
 * 镜像官方 {@code com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule}。
 *
 * 持有 shaderc 编译出的 SPIR-V 字节码 + spvc 反射出的绑定信息：
 * uniform buffer（SPVC 资源类型 1）、sampled image（7）、输出（4）、输入（3）。
 * {@link #rebind} 按管线绑定顺序把 SPIR-V 中的 binding/location decoration 重写；
 * {@link #toHlsl} 用 spvc 的 HLSL 后端（SM5.1）把重写后的 SPIR-V 转成 HLSL，
 * 交给原生层 D3DCompile 出 DXBC。
 */
@Environment(EnvType.CLIENT)
public record Dx12IntermediaryShaderModule(
    String name,
    @Nullable ByteBuffer spirv,
    List<SpvUniformBuffer> uniformBuffers,
    List<SpvSampler> samplers,
    List<SpvVariable> outputs,
    List<SpvVariable> inputs) implements AutoCloseable {

    /** 无有效 SPIR-V 的占位模块（官方 INVALID）。 */
    public static final Dx12IntermediaryShaderModule INVALID =
        new Dx12IntermediaryShaderModule("invalid", null, List.of(), List.of(), List.of(), List.of());

    /** SPIR-V 中 uniform buffer 块（bindingOffset 指向 binding decoration 的字节偏移）。 */
    /** static 关键字后允许的字符集合（区分 static float4 与 static_cast）。 */
    private static final java.util.Set<Character> AFTER_STATIC_ALLOWED;
    static {
        java.util.HashSet<Character> s = new java.util.HashSet<>();
        s.add(' '); s.add('\t'); s.add('\n'); s.add('\r');
        AFTER_STATIC_ALLOWED = java.util.Collections.unmodifiableSet(s);
    }
    public record SpvUniformBuffer(String name, int bindingOffset) {
    }

    /** SPIR-V 中 combined image sampler（dimensions: 1=2D, 3=Cube, 5=Buffer）。 */
    public record SpvSampler(String name, int bindingOffset, int dimensions) {
    }

    /** SPIR-V 中输入/输出变量（locationOffset 指向 location decoration 的字节偏移）。 */
    public record SpvVariable(String name, int locationOffset) {
    }

    /**
     * 用 spvc 解析 SPIR-V 并反射出 UBO/sampler/输出/输入绑定信息，
     * 随后把输出变量 location 打平为 0..n-1（与官方行为一致）。
     */
    public static Dx12IntermediaryShaderModule createFromSpirv(String filename, ByteBuffer spirv)
        throws ShaderCompileException {
        List<SpvUniformBuffer> uniformBuffers = new ArrayList<>();
        List<SpvSampler> samplers = new ArrayList<>();
        List<SpvVariable> outputs = new ArrayList<>();
        List<SpvVariable> inputs = new ArrayList<>();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.callocPointer(1);
            IntBuffer intReturnBuffer = stack.callocInt(1);
            throwIfError(Spvc.spvc_context_create(pointer), "Couldn't create spvc context");
            long context = pointer.get(0);
            try {
                throwIfError(Spvc.spvc_context_parse_spirv(context, spirv.asIntBuffer(),
                    (long) (spirv.remaining() / 4), pointer), "Couldn't parse spirv");
                long ir = pointer.get(0);
                throwIfError(Spvc.spvc_context_create_compiler(context, 0, ir, 1, pointer),
                    "Couldn't create compiler");
                long compiler = pointer.get(0);
                throwIfError(Spvc.spvc_compiler_create_shader_resources(compiler, pointer),
                    "Couldn't create resource list");
                long spvcResources = pointer.get(0);
                PointerBuffer countPointer = stack.callocPointer(1);

                // 1 = UNIFORM_BUFFER
                throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 1,
                    pointer, countPointer), "Couldn't list uniform buffers");
                long spvcList = pointer.get(0);
                long spvcCount = countPointer.get(0);
                SpvcReflectedResource.Buffer resources = SpvcReflectedResource.create(spvcList, (int) spvcCount);
                for (int i = 0; i < (int) spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    String name = resource.nameString();
                    int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
                    uniformBuffers.add(new SpvUniformBuffer(name, bindingOffset));
                }

                // 7 = SAMPLED_IMAGE
                throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 7,
                    pointer, countPointer), "Couldn't list sampled images");
                spvcList = pointer.get(0);
                spvcCount = countPointer.get(0);
                resources = SpvcReflectedResource.create(spvcList, (int) spvcCount);
                for (int i = 0; i < (int) spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    String name = resource.nameString();
                    int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
                    long typeHandle = Spvc.spvc_compiler_get_type_handle(compiler, resource.type_id());
                    int dimension = Spvc.spvc_type_get_image_dimension(typeHandle);
                    samplers.add(new SpvSampler(name, bindingOffset, dimension));
                }

                // 4 = STAGE_OUTPUT
                throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 4,
                    pointer, countPointer), "Couldn't list output variables");
                spvcList = pointer.get(0);
                spvcCount = countPointer.get(0);
                resources = SpvcReflectedResource.create(spvcList, (int) spvcCount);
                for (int i = 0; i < (int) spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    String name = resource.nameString();
                    int locationOffset = getDecorationOffset(compiler, resource, 30, intReturnBuffer);
                    outputs.add(new SpvVariable(name, locationOffset));
                }

                // 3 = STAGE_INPUT
                throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 3,
                    pointer, countPointer), "Couldn't list input variables");
                spvcList = pointer.get(0);
                spvcCount = countPointer.get(0);
                resources = SpvcReflectedResource.create(spvcList, (int) spvcCount);
                for (int i = 0; i < (int) spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    String name = resource.nameString();
                    int locationOffset = getDecorationOffset(compiler, resource, 30, intReturnBuffer);
                    inputs.add(new SpvVariable(name, locationOffset));
                }
            } finally {
                Spvc.spvc_context_destroy(context);
            }
        }
        IntBuffer spvAsIntBuffer = spirv.asIntBuffer();
        for (int i = 0; i < outputs.size(); ++i) {
            spvAsIntBuffer.put(outputs.get(i).locationOffset(), i);
        }
        return new Dx12IntermediaryShaderModule(filename, spirv, uniformBuffers, samplers, outputs, inputs);
    }

    @Override
    public void close() {
        if (this.spirv != null) {
            MemoryUtil.memFree(this.spirv);
        }
    }

    /**
     * 按管线绑定顺序重写 SPIR-V 中的绑定/位置：
     * 输入变量按 vertex format 属性名顺序编 attribLocation；UBO/sampler 按
     * {@code entries} 顺序编 binding。剩余未匹配的资源会抛异常（官方行为）。
     */
    public void rebind(List<String> inputVariables, List<Dx12BindGroupEntry> entries)
        throws ShaderCompileException {
        if (this.spirv == null) {
            throw new IllegalStateException("Attempt to use invalid shader");
        }
        IntBuffer spvAsIntBuffer = this.spirv.asIntBuffer();
        HashSet<String> remainingInputs = new HashSet<>();
        HashSet<String> remainingSamplers = new HashSet<>();
        HashSet<String> remainingUniformBuffers = new HashSet<>();
        for (SpvVariable input : this.inputs) {
            remainingInputs.add(input.name());
        }
        for (SpvUniformBuffer uniformBuffer : this.uniformBuffers) {
            remainingUniformBuffers.add(uniformBuffer.name());
        }
        for (SpvSampler sampler : this.samplers) {
            remainingSamplers.add(sampler.name());
        }
        String previousName = null;
        int attribLocation = 0;
        for (int i = 0; i < inputVariables.size(); ++i) {
            String variableName = inputVariables.get(i);
            SpvVariable inputVariable = this.getInputVariable(variableName);
            if (inputVariable == null) continue;
            if (!variableName.equals(previousName)) {
                spvAsIntBuffer.put(inputVariable.locationOffset(), attribLocation);
                remainingInputs.remove(variableName);
            }
            ++attribLocation;
            previousName = variableName;
        }
        for (int i = 0; i < entries.size(); ++i) {
            Dx12BindGroupEntry entry = entries.get(i);
            switch (entry.type()) {
                case UNIFORM_BUFFER -> {
                    SpvUniformBuffer ubo = this.getUniformBuffer(entry.name());
                    if (ubo == null) break;
                    spvAsIntBuffer.put(ubo.bindingOffset(), i);
                    remainingUniformBuffers.remove(entry.name());
                }
                case SAMPLED_IMAGE -> {
                    SpvSampler sampler = this.getSampler(entry.name());
                    if (sampler == null) break;
                    if (sampler.dimensions() != 1 && sampler.dimensions() != 3) {
                        throw new ShaderCompileException("Unsupported texture dimensions '" + sampler.dimensions()
                            + "' for sampler " + entry.name());
                    }
                    spvAsIntBuffer.put(sampler.bindingOffset(), i);
                    remainingSamplers.remove(entry.name());
                }
                case TEXEL_BUFFER -> {
                    SpvSampler sampler = this.getSampler(entry.name());
                    if (sampler == null) break;
                    if (sampler.dimensions() != 5) {
                        throw new ShaderCompileException("Unsupported texel buffer dimensions '"
                            + sampler.dimensions() + "' for sampler " + entry.name());
                    }
                    spvAsIntBuffer.put(sampler.bindingOffset(), i);
                    remainingSamplers.remove(entry.name());
                }
            }
        }
        if (!remainingInputs.isEmpty()) {
            throw new ShaderCompileException(
                "Shader expects input variables which are not being provided: " + remainingInputs);
        }
        if (!remainingUniformBuffers.isEmpty()) {
            throw new ShaderCompileException(
                "Shader expects uniform buffers which are not being provided: " + remainingUniformBuffers);
        }
        if (!remainingSamplers.isEmpty()) {
            throw new ShaderCompileException(
                "Shader expects samplers which are not being provided: " + remainingSamplers);
        }
    }

    /**
     * 用 spvc HLSL 后端（SPVC_BACKEND_HLSL=2）把（已 rebind 的）SPIR-V 转成
     * HLSL，shader model 5.1（SPVC_COMPILER_OPTION_HLSL_SHADER_MODEL=51）。
     * 原生层随后用 d3dcompiler_47 把 HLSL 编成 vs_5_1/ps_5_1 字节码。
     */
    public String toHlsl(boolean isVertex) throws ShaderCompileException {
        if (this.spirv == null) {
            throw new IllegalStateException("Attempt to use invalid shader");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.callocPointer(1);
            throwIfError(Spvc.spvc_context_create(pointer), "Couldn't create spvc context");
            long context = pointer.get(0);
            try {
                throwIfError(Spvc.spvc_context_parse_spirv(context, this.spirv.asIntBuffer(),
                    (long) (this.spirv.remaining() / 4), pointer), "Couldn't parse spirv");
                long ir = pointer.get(0);
                throwIfError(Spvc.spvc_context_create_compiler(context, Spvc.SPVC_BACKEND_HLSL, ir, 1, pointer),
                    "Couldn't create HLSL compiler");
                long compiler = pointer.get(0);
                throwIfError(Spvc.spvc_compiler_create_compiler_options(compiler, pointer),
                    "Couldn't create compiler options");
                long options = pointer.get(0);
                throwIfError(Spvc.spvc_compiler_options_set_uint(options,
                    Spvc.SPVC_COMPILER_OPTION_HLSL_SHADER_MODEL, 51), "Couldn't set HLSL shader model");
                // P6 修复：GLSL 的 gl_PointSize / gl_PointCoord 在 SPIRV-Cross HLSL
                // 后端默认不支持（SPVC_ERROR_UNSUPPORTED_SPIRV）。vanilla
                // debug_point 管线（ShaderManager 必需）在 debug_point.vsh 里写
                // gl_PointSize = LineWidth，若不启用兼容选项则编译失败 → ShaderManager
                // apply() 抛异常 → 资源包全部移除 → UI 黑屏。启用后 spvc 会把点大小
                // 输出映射到 SV_PointSize / PSIZE 语义，点坐标映射为 PSIZE/语义。
                throwIfError(Spvc.spvc_compiler_options_set_uint(options,
                    Spvc.SPVC_COMPILER_OPTION_HLSL_POINT_SIZE_COMPAT, 1),
                    "Couldn't set HLSL point size compat");
                throwIfError(Spvc.spvc_compiler_options_set_uint(options,
                    Spvc.SPVC_COMPILER_OPTION_HLSL_POINT_COORD_COMPAT, 1),
                    "Couldn't set HLSL point coord compat");
                throwIfError(Spvc.spvc_compiler_install_compiler_options(compiler, options),
                    "Couldn't install compiler options");
                // Fix S2：为顶点着色器输入注入 D3D12 语义，避免 spvc 后端不生成正确语义导致
                // CreateGraphicsPipelineState E_INVALIDARG。location 0 → POSITION，其余 → TEXCOORD<n>。
                if (isVertex && !this.inputs.isEmpty()) {
                    int inputIdx = 0;
                    int prevLoc = -1;
                    for (SpvVariable input : this.inputs) {
                        int loc = this.spirv.asIntBuffer().get(input.locationOffset());
                        if (loc != prevLoc) {
                            // BUG-02 fix: 语义按 SPIR-V location 分配（rebind 后 location 与顶点
                            // 格式元素顺序一致：0→POSITION, n→TEXCOORD(n-1)），与 Dx12Device
                            // buildVertexInputElements 的 attribLocation 对齐。原实现按反射顺序
                            // （GLSL 声明顺序）分配，text.vsh 声明 Position,Color,UV0（格式为
                            // Position,UV0,Color）→ Color 得 TEXCOORD0、UV0 得 TEXCOORD1，
                            // 与 input layout 互换：TEXCOORD0 读 UV0 数据、TEXCOORD1 读 Color
                            // 数据 → 字形采样坐标错误（GUI 文字消失/错乱）。
                            String semantic = (loc == 0) ? "POSITION" : ("TEXCOORD" + (loc - 1));
                            SpvcHlslVertexAttributeRemap remap = SpvcHlslVertexAttributeRemap.calloc(stack);
                            remap.location(loc);
                            remap.semantic(stack.UTF8(semantic));
                            throwIfError(Spvc.spvc_compiler_hlsl_add_vertex_attribute_remap(
                                compiler, remap, 1), "Couldn't add vertex attribute remap");
                            ++inputIdx;
                            prevLoc = loc;
                        }
                    }
                }
                int compileResult = Spvc.spvc_compiler_compile(compiler, pointer);
                if (compileResult != 0) {
                    // P6 诊断：附加 SPIRV-Cross 内部错误描述（如具体哪个 builtin/指令不支持），
                    // 便于定位剩余管线编译失败的具体原因。
                    String detail = Spvc.spvc_context_get_last_error_string(context);
                    throwIfError(compileResult, "Couldn't compile HLSL"
                        + (detail != null && !detail.isEmpty() ? ": " + detail : ""));
                }
                long address = pointer.get(0);
                String hlsl = MemoryUtil.memUTF8(address);
                // P7 诊断：打印 raw spvc 输出，确认 }}; 来自 spvc 还是注入逻辑
                if (name.contains("gui") || name.contains("debug") || name.contains("position")
                        || name.contains("animate") || name.contains("sprite") || name.contains("text")
                        || name.contains("entity") || name.contains("item")) {
                    System.err.println("[dx12-java] [" + name + "] RAW spvc HLSL:\n" + hlsl);
                }
                // Fix S2：语义通过 spvc_compiler_hlsl_add_vertex_attribute_remap 在编译前注入，
                // 确保 spvc HLSL 后端输出正确的 D3D12 语义（POSITION/TEXCOORD<n>）。
                String result = hlsl;
                // P7 诊断：打印 raw spvc 输出，确认语义已正确生成
                // P27：条件同步包含 animate/sprite，确保 animate_sprite_blit 的完整 HLSL 也输出
                if (name.contains("gui") || name.contains("debug") || name.contains("position")
                        || name.contains("animate") || name.contains("sprite") || name.contains("text")
                        || name.contains("entity") || name.contains("item")) {
                    System.err.println("[dx12-java] [" + name + "] spvc HLSL (no inject):\n" + result);
                }
                return result;
            } finally {
                Spvc.spvc_context_destroy(context);
            }
        }
    }

    private @Nullable SpvUniformBuffer getUniformBuffer(String name) {
        for (SpvUniformBuffer ubo : this.uniformBuffers) {
            if (ubo.name().equals(name)) {
                return ubo;
            }
        }
        return null;
    }

    private @Nullable SpvSampler getSampler(String name) {
        for (SpvSampler sampler : this.samplers) {
            if (sampler.name().equals(name)) {
                return sampler;
            }
        }
        return null;
    }

    private @Nullable SpvVariable getInputVariable(String name) {
        for (SpvVariable variable : this.inputs) {
            if (variable.name().equals(name)) {
                return variable;
            }
        }
        return null;
    }

    private static void throwIfError(int result, String message) throws ShaderCompileException {
        if (result != 0) {
            String name = switch (result) {
                case -1 -> "SPVC_ERROR_INVALID_SPIRV";
                case -2 -> "SPVC_ERROR_UNSUPPORTED_SPIRV";
                case -3 -> "SPVC_ERROR_OUT_OF_MEMORY";
                case -4 -> "SPVC_ERROR_INVALID_ARGUMENT";
                default -> Integer.toString(result);
            };
            throw new ShaderCompileException(message + " (" + name + ")");
        }
    }

    /**
     * 为单次编译创建 SPIR-V 工作副本。
     * rebind() 会原地修改 SPIR-V 的 decoration 字段，必须克隆后操作，
     * 否则同一缓存模块被多次 rebind() 后互相污染。
     */
    public Dx12IntermediaryShaderModule cloneForCompile() {
        if (this.spirv == null) return this;
        ByteBuffer dup = MemoryUtil.memCalloc(this.spirv.remaining());
        MemoryUtil.memCopy(this.spirv, dup);
        return new Dx12IntermediaryShaderModule(
            this.name, dup, this.uniformBuffers, this.samplers, this.outputs, this.inputs);
    }

    /** 读取 SPIR-V 二进制中某 decoration（33=BINDING / 30=LOCATION）的字节偏移。 */
    private static int getDecorationOffset(long compiler, SpvcReflectedResource resource,
        int decoration, IntBuffer returnBuffer) throws ShaderCompileException {
        if (!Spvc.spvc_compiler_get_binary_offset_for_decoration(compiler, resource.id(),
            decoration, returnBuffer)) {
            throw new ShaderCompileException(
                "Couldn't find byte offset for location decoration of " + resource.nameString());
        }
        return returnBuffer.get(0);
    }
}
