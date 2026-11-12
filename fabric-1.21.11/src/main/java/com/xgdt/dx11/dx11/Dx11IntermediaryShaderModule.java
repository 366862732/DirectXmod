package com.xgdt.dx11.dx11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * SPIR-V module plus the reflection data needed to bind it through D3D11.
 *
 * <p>This is a D3D11/SM4.0 counterpart of the 26.2 DX12 module. It only
 * reflects uniform buffers, sampled images (including texel buffers), stage
 * inputs and stage outputs; storage buffers do not exist in the 1.21.11
 * {@code GpuDevice} API.</p>
 */
@Environment(EnvType.CLIENT)
final class Dx11IntermediaryShaderModule implements AutoCloseable {
    record SpvUniformBuffer(String name, int bindingOffset) {
    }

    record SpvSampler(String name, int bindingOffset, int dimensions) {
    }

    record SpvVariable(String name, int locationOffset, int id, boolean used) {
    }

    private final String name;
    private final @Nullable ByteBuffer spirv;
    private final List<SpvUniformBuffer> uniformBuffers;
    private final List<SpvSampler> samplers;
    private final List<SpvVariable> outputs;
    private final List<SpvVariable> inputs;

    private Dx11IntermediaryShaderModule(
        String name,
        @Nullable ByteBuffer spirv,
        List<SpvUniformBuffer> uniformBuffers,
        List<SpvSampler> samplers,
        List<SpvVariable> outputs,
        List<SpvVariable> inputs
    ) {
        this.name = name;
        this.spirv = spirv;
        this.uniformBuffers = uniformBuffers;
        this.samplers = samplers;
        this.outputs = outputs;
        this.inputs = inputs;
    }

    String name() {
        return this.name;
    }

    List<SpvVariable> inputs() {
        return this.inputs;
    }

    List<SpvVariable> outputs() {
        return this.outputs;
    }

    List<SpvUniformBuffer> uniformBuffers() {
        return this.uniformBuffers;
    }

    List<SpvSampler> samplers() {
        return this.samplers;
    }

    boolean hasUniformBuffer(String resourceName) {
        return this.getUniformBuffer(resourceName) != null;
    }

    boolean hasSampler(String resourceName) {
        return this.getSampler(resourceName) != null;
    }

    /**
     * Parses SPIR-V and reflects the resources used by the shader. Output
     * locations are flattened to 0..n-1 to match the linked GL program
     * behaviour.
     */
    static Dx11IntermediaryShaderModule createFromSpirv(String filename, ByteBuffer spirv)
        throws Dx11ShaderCompileException {
        List<SpvUniformBuffer> uniformBuffers = new ArrayList<>();
        List<SpvSampler> samplers = new ArrayList<>();
        List<SpvVariable> outputs = new ArrayList<>();
        List<SpvVariable> inputs = new ArrayList<>();
        Set<Integer> usedIds = collectUsedIds(spirv);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.callocPointer(1);
            IntBuffer intReturnBuffer = stack.callocInt(1);
            throwIfError(Spvc.spvc_context_create(pointer), "Couldn't create spvc context");
            long context = pointer.get(0);
            try {
                throwIfError(
                    Spvc.spvc_context_parse_spirv(
                        context, spirv.asIntBuffer(), (long) (spirv.remaining() / 4), pointer),
                    "Couldn't parse spirv");
                long ir = pointer.get(0);
                throwIfError(
                    Spvc.spvc_context_create_compiler(context, Spvc.SPVC_BACKEND_HLSL, ir, 1, pointer),
                    "Couldn't create compiler");
                long compiler = pointer.get(0);
                PointerBuffer activeSet = stack.callocPointer(1);
                throwIfError(
                    Spvc.spvc_compiler_get_active_interface_variables(compiler, activeSet),
                    "Couldn't get active interface variables");
                if (activeSet.get(0) != 0L) {
                    throwIfError(
                        Spvc.spvc_compiler_set_enabled_interface_variables(compiler, activeSet.get(0)),
                        "Couldn't set active interface variables");
                }
                throwIfError(
                    Spvc.spvc_compiler_create_shader_resources(compiler, pointer),
                    "Couldn't create resource list");
                long spvcResources = pointer.get(0);
                PointerBuffer countPointer = stack.callocPointer(1);

                // SPVC_RESOURCE_TYPE_UNIFORM_BUFFER = 1
                throwIfError(
                    Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 1, pointer, countPointer),
                    "Couldn't list uniform buffers");
                long spvcList = pointer.get(0);
                int spvcCount = (int) countPointer.get(0);
                SpvcReflectedResource.Buffer resources = SpvcReflectedResource.create(spvcList, spvcCount);
                for (int i = 0; i < spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
                    uniformBuffers.add(new SpvUniformBuffer(resource.nameString(), bindingOffset));
                }

                // SPVC_RESOURCE_TYPE_SAMPLED_IMAGE = 7
                throwIfError(
                    Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 7, pointer, countPointer),
                    "Couldn't list sampled images");
                spvcList = pointer.get(0);
                spvcCount = (int) countPointer.get(0);
                resources = SpvcReflectedResource.create(spvcList, spvcCount);
                for (int i = 0; i < spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
                    long typeHandle = Spvc.spvc_compiler_get_type_handle(compiler, resource.type_id());
                    int dimension = Spvc.spvc_type_get_image_dimension(typeHandle);
                    samplers.add(new SpvSampler(resource.nameString(), bindingOffset, dimension));
                }

                // SPVC_RESOURCE_TYPE_STAGE_OUTPUT = 4
                throwIfError(
                    Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 4, pointer, countPointer),
                    "Couldn't list output variables");
                spvcList = pointer.get(0);
                spvcCount = (int) countPointer.get(0);
                resources = SpvcReflectedResource.create(spvcList, spvcCount);
                for (int i = 0; i < spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    int locationOffset = getDecorationOffset(compiler, resource, 30, intReturnBuffer);
                    outputs.add(new SpvVariable(
                        resource.nameString(), locationOffset, resource.id(),
                        usedIds.contains(resource.id())));
                }

                // SPVC_RESOURCE_TYPE_STAGE_INPUT = 3
                throwIfError(
                    Spvc.spvc_resources_get_resource_list_for_type(spvcResources, 3, pointer, countPointer),
                    "Couldn't list input variables");
                spvcList = pointer.get(0);
                spvcCount = (int) countPointer.get(0);
                resources = SpvcReflectedResource.create(spvcList, spvcCount);
                for (int i = 0; i < spvcCount; ++i) {
                    SpvcReflectedResource resource = resources.get(i);
                    int locationOffset = getDecorationOffset(compiler, resource, 30, intReturnBuffer);
                    inputs.add(new SpvVariable(
                        resource.nameString(), locationOffset, resource.id(),
                        usedIds.contains(resource.id())));
                }
            } finally {
                Spvc.spvc_context_destroy(context);
            }
        }

        IntBuffer spvAsIntBuffer = spirv.asIntBuffer();
        for (int i = 0; i < outputs.size(); ++i) {
            spvAsIntBuffer.put(outputs.get(i).locationOffset(), i);
        }
        return new Dx11IntermediaryShaderModule(
            filename, spirv, uniformBuffers, samplers, outputs, inputs);
    }

    @Override
    public void close() {
        if (this.spirv != null) {
            MemoryUtil.memFree(this.spirv);
        }
    }

    /**
     * Assigns sequential locations to the stage outputs and returns their
     * names in location order.
     *
     * <p>The vertex stage owns this mapping.  The fragment stage is remapped
     * to the same locations by name, so a fragment shader that reads a
     * non-prefix subset of the vertex outputs still matches the vertex
     * shader's output signature.</p>
     */
    List<String> rebindOutputs() throws Dx11ShaderCompileException {
        if (this.spirv == null) {
            throw new IllegalStateException("Attempt to use invalid shader");
        }
        IntBuffer spvAsIntBuffer = this.spirv.asIntBuffer();
        List<String> names = new ArrayList<>(this.outputs.size());
        for (int i = 0; i < this.outputs.size(); ++i) {
            SpvVariable output = this.outputs.get(i);
            spvAsIntBuffer.put(output.locationOffset(), i);
            names.add(output.name());
        }
        return names;
    }

    /**
     * Rewrites input locations and resource bindings in this SPIR-V module.
     *
     * <p>For the vertex stage, input locations are compacted to the order of
     * the linked inputs (location 0 is POSITION, location n is
     * TEXCOORD(n-1)).  For the fragment stage, {@code useInputIndex} makes
     * each input use its index in {@code inputVariables} (the vertex output
     * list), which is exactly the location assigned by
     * {@link #rebindOutputs()}.  Uniform buffers use the {@code b#} namespace
     * and sampled images / texel buffers use the {@code t#} namespace, exactly
     * like the native render pass expects.</p>
     */
    List<String> rebind(
        List<String> inputVariables,
        List<Dx11CompiledShader.Binding> entries,
        boolean useInputIndex)
        throws Dx11ShaderCompileException {
        if (this.spirv == null) {
            throw new IllegalStateException("Attempt to use invalid shader");
        }
        IntBuffer spvAsIntBuffer = this.spirv.asIntBuffer();
        Set<String> remainingInputs = new HashSet<>();
        Set<String> remainingUniformBuffers = new HashSet<>();
        Set<String> remainingSamplers = new HashSet<>();
        for (SpvVariable input : this.inputs) {
            if (input.used()) {
                remainingInputs.add(input.name());
            }
        }
        for (SpvUniformBuffer buffer : this.uniformBuffers) {
            remainingUniformBuffers.add(buffer.name());
        }
        for (SpvSampler sampler : this.samplers) {
            remainingSamplers.add(sampler.name());
        }

        Set<String> matchedInputs = new HashSet<>();
        String previousName = null;
        int attribLocation = 0;
        List<String> linkedInputs = new ArrayList<>();
        for (int index = 0; index < inputVariables.size(); ++index) {
            String variableName = inputVariables.get(index);
            SpvVariable inputVariable = this.getInputVariable(variableName);
            if (inputVariable == null) {
                continue;
            }
            if (!variableName.equals(previousName)) {
                spvAsIntBuffer.put(
                    inputVariable.locationOffset(), useInputIndex ? index : attribLocation);
                remainingInputs.remove(variableName);
                matchedInputs.add(variableName);
                linkedInputs.add(variableName);
            }
            ++attribLocation;
            previousName = variableName;
        }

        if (useInputIndex) {
            // Unused fragment inputs are stripped from the HLSL afterwards.
            // Give the ones without a matching vertex output a unique location
            // so the stripper can identify them without colliding with a live
            // input.
            int nextLocation = inputVariables.size();
            for (SpvVariable input : this.inputs) {
                if (!input.used() && !matchedInputs.contains(input.name())) {
                    spvAsIntBuffer.put(input.locationOffset(), nextLocation++);
                }
            }
        }

        for (Dx11CompiledShader.Binding entry : entries) {
            switch (entry.type()) {
                case UNIFORM_BUFFER -> {
                    SpvUniformBuffer buffer = this.getUniformBuffer(entry.name());
                    if (buffer != null) {
                        spvAsIntBuffer.put(buffer.bindingOffset(), entry.slot());
                        remainingUniformBuffers.remove(entry.name());
                    }
                }
                case SAMPLED_IMAGE, TEXEL_BUFFER -> {
                    SpvSampler sampler = this.getSampler(entry.name());
                    if (sampler != null) {
                        spvAsIntBuffer.put(sampler.bindingOffset(), entry.slot());
                        remainingSamplers.remove(entry.name());
                    }
                }
            }
        }

        // A resource that the pipeline did not declare cannot be bound by the
        // render pass. Surface it as a compile error instead of silently
        // reading zeroes at runtime.
        if (!remainingInputs.isEmpty()) {
            throw new Dx11ShaderCompileException(
                this.name + " expects input variables which are not being provided: " + remainingInputs);
        }
        if (!remainingUniformBuffers.isEmpty()) {
            throw new Dx11ShaderCompileException(
                this.name + " expects uniform buffers which are not being provided: " + remainingUniformBuffers);
        }
        if (!remainingSamplers.isEmpty()) {
            throw new Dx11ShaderCompileException(
                this.name + " expects samplers which are not being provided: " + remainingSamplers);
        }
        return linkedInputs;
    }

    /**
     * Emits HLSL through SPIRV-Cross' HLSL backend using shader model 4.0.
     * SM4.0 is the common denominator of Direct3D 10.0 and 10.1 hardware.
     */
    String toHlsl(boolean isVertex) throws Dx11ShaderCompileException {
        if (this.spirv == null) {
            throw new IllegalStateException("Attempt to use invalid shader");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.callocPointer(1);
            throwIfError(Spvc.spvc_context_create(pointer), "Couldn't create spvc context");
            long context = pointer.get(0);
            try {
                throwIfError(
                    Spvc.spvc_context_parse_spirv(
                        context, this.spirv.asIntBuffer(), (long) (this.spirv.remaining() / 4), pointer),
                    "Couldn't parse spirv");
                long ir = pointer.get(0);
                throwIfError(
                    Spvc.spvc_context_create_compiler(context, Spvc.SPVC_BACKEND_HLSL, ir, 1, pointer),
                    "Couldn't create HLSL compiler");
                long compiler = pointer.get(0);
                PointerBuffer activeSet = stack.callocPointer(1);
                throwIfError(
                    Spvc.spvc_compiler_get_active_interface_variables(compiler, activeSet),
                    "Couldn't get active interface variables");
                if (activeSet.get(0) != 0L) {
                    throwIfError(
                        Spvc.spvc_compiler_set_enabled_interface_variables(compiler, activeSet.get(0)),
                        "Couldn't set active interface variables");
                }
                throwIfError(
                    Spvc.spvc_compiler_create_compiler_options(compiler, pointer),
                    "Couldn't create compiler options");
                long options = pointer.get(0);
                throwIfError(
                    Spvc.spvc_compiler_options_set_uint(
                        options, Spvc.SPVC_COMPILER_OPTION_HLSL_SHADER_MODEL, 40),
                    "Couldn't set HLSL shader model");
                throwIfError(
                    Spvc.spvc_compiler_install_compiler_options(compiler, options),
                    "Couldn't install compiler options");

                if (isVertex && !this.inputs.isEmpty()) {
                    int prevLoc = -1;
                    for (SpvVariable input : this.inputs) {
                        int loc = this.spirv.asIntBuffer().get(input.locationOffset());
                        if (loc != prevLoc) {
                            String semantic = loc == 0 ? "POSITION" : ("TEXCOORD" + (loc - 1));
                            SpvcHlslVertexAttributeRemap remap = SpvcHlslVertexAttributeRemap.calloc(stack);
                            remap.location(loc);
                            remap.semantic(stack.UTF8(semantic));
                            throwIfError(
                                Spvc.spvc_compiler_hlsl_add_vertex_attribute_remap(compiler, remap, 1),
                                "Couldn't add vertex attribute remap");
                            prevLoc = loc;
                        }
                    }
                }

                int compileResult = Spvc.spvc_compiler_compile(compiler, pointer);
                if (compileResult != 0) {
                    String detail = Spvc.spvc_context_get_last_error_string(context);
                    throwIfError(
                        compileResult,
                        "Couldn't compile HLSL"
                            + (detail != null && !detail.isEmpty() ? ": " + detail : ""));
                }
                String hlsl = MemoryUtil.memUTF8(pointer.get(0));
                // SPIRV-Cross emits SM5.1-style `register(tN, spaceN)`
                // qualifiers for some inputs.  SM4.0 accepts only `register(tN)`.
                hlsl = hlsl.replaceAll(
                    "register\\(([abctsu]\\d+)\\s*,\\s*space\\d+\\)", "register($1)");
                if (!isVertex) {
                    hlsl = this.stripUnusedFragmentInputs(hlsl);
                }
                return hlsl;
            } finally {
                Spvc.spvc_context_destroy(context);
            }
        }
    }

    /**
     * Removes stage inputs that the fragment shader never reads from the
     * generated HLSL input struct and its {@code stage_input} assignments.
     *
     * <p>SPIRV-Cross' active-interface filter leaves unused stage inputs in
     * {@code SPIRV_Cross_Input}.  D3D11 rejects a pixel-shader input signature
     * that is not a subset of the vertex-shader output signature, so they must
     * be removed here.</p>
     */
    private String stripUnusedFragmentInputs(String hlsl) {
        Set<String> unusedNames = new HashSet<>();
        Set<Integer> unusedLocations = new HashSet<>();
        Set<Integer> usedLocations = new HashSet<>();
        IntBuffer spvAsIntBuffer = this.spirv == null ? null : this.spirv.asIntBuffer();
        for (SpvVariable input : this.inputs) {
            if (input.used()) {
                if (spvAsIntBuffer != null) {
                    usedLocations.add(spvAsIntBuffer.get(input.locationOffset()));
                }
            } else {
                unusedNames.add(input.name());
                if (spvAsIntBuffer != null) {
                    unusedLocations.add(spvAsIntBuffer.get(input.locationOffset()));
                }
            }
        }
        if (unusedNames.isEmpty()) {
            return hlsl;
        }

        String marker = "struct SPIRV_Cross_Input";
        int structStart = hlsl.indexOf(marker);
        if (structStart < 0) {
            return hlsl;
        }
        int open = hlsl.indexOf('{', structStart);
        int close = open < 0 ? -1 : hlsl.indexOf("};", open);
        if (open < 0 || close < 0) {
            return hlsl;
        }

        List<String> keptLines = new ArrayList<>();
        Set<String> removedMembers = new HashSet<>();
        for (String line : hlsl.substring(open + 1, close).split("\n", -1)) {
            String member = structMemberName(line);
            String semantic = structMemberSemantic(line);
            boolean remove = member != null && unusedNames.contains(member);
            if (!remove && semantic != null && semantic.startsWith("TEXCOORD")) {
                try {
                    int location = Integer.parseInt(semantic.substring("TEXCOORD".length()));
                    remove = unusedLocations.contains(location)
                        && !usedLocations.contains(location);
                } catch (NumberFormatException ignored) {
                    // Not a numbered TEXCOORD semantic; leave the member alone.
                }
            }
            if (remove && member != null) {
                removedMembers.add(member);
            } else {
                keptLines.add(line);
            }
        }
        if (removedMembers.isEmpty()) {
            return hlsl;
        }

        String result;
        boolean allRemoved = keptLines.stream().allMatch(String::isBlank);
        if (allRemoved) {
            // No inputs left at all: drop the empty struct and the parameter.
            result = hlsl.substring(0, structStart) + hlsl.substring(close + 2);
            result = result.replace(
                "SPIRV_Cross_Output main(SPIRV_Cross_Input stage_input)",
                "SPIRV_Cross_Output main()");
        } else {
            result = hlsl.substring(0, open + 1)
                + String.join("\n", keptLines)
                + hlsl.substring(close);
        }
        for (String member : removedMembers) {
            result = result.replaceAll(
                "(?m)^[ \\t]*[A-Za-z_][A-Za-z0-9_]*\\s*=\\s*stage_input\\."
                    + Pattern.quote(member) + "\\s*;[ \\t]*\\n?",
                "");
        }
        return result;
    }

    private static @Nullable String structMemberName(String line) {
        String declaration = structMemberDeclaration(line);
        if (declaration == null) {
            return null;
        }
        int space = declaration.lastIndexOf(' ');
        if (space < 0) {
            return null;
        }
        String name = declaration.substring(space + 1).strip();
        int bracket = name.indexOf('[');
        if (bracket >= 0) {
            name = name.substring(0, bracket);
        }
        return name.isEmpty() ? null : name;
    }

    private static @Nullable String structMemberSemantic(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String semantic = line.substring(colon + 1).strip();
        int semicolon = semantic.indexOf(';');
        if (semicolon >= 0) {
            semantic = semantic.substring(0, semicolon).strip();
        }
        return semantic.isEmpty() ? null : semantic;
    }

    private static @Nullable String structMemberDeclaration(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String declaration = line.substring(0, colon).strip();
        return declaration.isEmpty() ? null : declaration;
    }

    Dx11IntermediaryShaderModule cloneForCompile() {
        if (this.spirv == null) {
            return this;
        }
        ByteBuffer duplicate = MemoryUtil.memCalloc(this.spirv.remaining());
        MemoryUtil.memCopy(this.spirv, duplicate);
        return new Dx11IntermediaryShaderModule(
            this.name, duplicate, this.uniformBuffers, this.samplers, this.outputs, this.inputs);
    }

    private @Nullable SpvUniformBuffer getUniformBuffer(String resourceName) {
        for (SpvUniformBuffer buffer : this.uniformBuffers) {
            if (buffer.name().equals(resourceName)) {
                return buffer;
            }
        }
        return null;
    }

    private @Nullable SpvSampler getSampler(String resourceName) {
        for (SpvSampler sampler : this.samplers) {
            if (sampler.name().equals(resourceName)) {
                return sampler;
            }
        }
        return null;
    }

    private @Nullable SpvVariable getInputVariable(String resourceName) {
        for (SpvVariable input : this.inputs) {
            if (input.name().equals(resourceName)) {
                return input;
            }
        }
        return null;
    }

    private static int getDecorationOffset(
        long compiler, SpvcReflectedResource resource, int decoration, IntBuffer returnBuffer)
        throws Dx11ShaderCompileException {
        if (!Spvc.spvc_compiler_get_binary_offset_for_decoration(
            compiler, resource.id(), decoration, returnBuffer)) {
            throw new Dx11ShaderCompileException(
                "Couldn't find byte offset for decoration " + decoration + " of " + resource.nameString());
        }
        return returnBuffer.get(0);
    }

    /**
     * Finds the result IDs that are actually referenced by the shader body.
     * Stage interface variables are pointers, so their reads appear as
     * {@code OpLoad}/{@code OpAccessChain} operands.  Decorations, names and
     * the entry-point interface list are not uses.
     */
    private static Set<Integer> collectUsedIds(ByteBuffer spirv) {
        // ByteBuffer.duplicate() resets the byte order to BIG_ENDIAN, so it
        // must be restored explicitly; SPIR-V words are little-endian.
        IntBuffer words = spirv.duplicate().order(spirv.order()).asIntBuffer();
        Set<Integer> used = new HashSet<>();
        int index = 5;  // SPIR-V header
        while (index < words.limit()) {
            int instruction = words.get(index);
            int size = instruction >>> 16;
            int opcode = instruction & 0xFFFF;
            if (size <= 0 || index + size > words.limit()) {
                break;
            }
            switch (opcode) {
                case 60, 61, 65, 68, 70, 83 -> // OpImageTexelPointer, OpLoad,
                    // OpAccessChain, OpArrayLength, OpInBoundsAccessChain,
                    // OpCopyObject: the pointer/base is operand 3.
                    used.add(words.get(index + 3));
                case 62, 63, 64 -> { // OpStore, OpCopyMemory, OpCopyMemorySized
                    used.add(words.get(index + 1));
                    used.add(words.get(index + 2));
                }
                case 57 -> { // OpFunctionCall: arguments start at operand 4
                    for (int i = 4; i < size; ++i) {
                        used.add(words.get(index + i));
                    }
                }
                case 245 -> { // OpPhi: (value, parent) pairs start at operand 3
                    for (int i = 3; i + 1 < size; i += 2) {
                        used.add(words.get(index + i));
                    }
                }
                default -> {
                }
            }
            index += size;
        }
        return used;
    }

    private static void throwIfError(int result, String message) throws Dx11ShaderCompileException {
        if (result != 0) {
            String errorName = switch (result) {
                case -1 -> "SPVC_ERROR_INVALID_SPIRV";
                case -2 -> "SPVC_ERROR_UNSUPPORTED_SPIRV";
                case -3 -> "SPVC_ERROR_OUT_OF_MEMORY";
                case -4 -> "SPVC_ERROR_INVALID_ARGUMENT";
                default -> Integer.toString(result);
            };
            throw new Dx11ShaderCompileException(message + " (" + errorName + ")");
        }
    }
}
