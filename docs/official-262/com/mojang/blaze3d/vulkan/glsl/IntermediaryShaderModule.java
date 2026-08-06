/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.util.spvc.Spvc
 *  org.lwjgl.util.spvc.SpvcReflectedResource
 *  org.lwjgl.util.spvc.SpvcReflectedResource$Buffer
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkShaderModuleCreateInfo
 */
package com.mojang.blaze3d.vulkan.glsl;

import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import com.mojang.blaze3d.vulkan.glsl.SpvSampler;
import com.mojang.blaze3d.vulkan.glsl.SpvUniformBuffer;
import com.mojang.blaze3d.vulkan.glsl.SpvVariable;
import com.mojang.blaze3d.vulkan.glsl.SpvcUtil;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcReflectedResource;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

@Environment(value=EnvType.CLIENT)
public record IntermediaryShaderModule(String name, @Nullable ByteBuffer spirv, List<SpvUniformBuffer> uniformBuffers, List<SpvSampler> samplers, List<SpvVariable> outputs, List<SpvVariable> inputs) implements AutoCloseable
{
    public static final IntermediaryShaderModule INVALID = new IntermediaryShaderModule("invalid", null, new ArrayList<SpvUniformBuffer>(), new ArrayList<SpvSampler>(), new ArrayList<SpvVariable>(), new ArrayList<SpvVariable>());

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static IntermediaryShaderModule createFromSpirv(String filename, ByteBuffer spirv) throws ShaderCompileException {
        ArrayList<SpvUniformBuffer> uniformBuffers = new ArrayList<SpvUniformBuffer>();
        ArrayList<SpvSampler> samplers = new ArrayList<SpvSampler>();
        ArrayList<SpvVariable> outputs = new ArrayList<SpvVariable>();
        ArrayList<SpvVariable> inputs = new ArrayList<SpvVariable>();
        try (MemoryStack stack = MemoryStack.stackPush();){
            PointerBuffer pointer = stack.callocPointer(1);
            IntBuffer intReturnBuffer = stack.callocInt(1);
            IntermediaryShaderModule.throwIfError(Spvc.spvc_context_create((PointerBuffer)pointer), "Couldn't create spvc context");
            long context = pointer.get(0);
            try {
                int bindingOffset;
                String name;
                SpvcReflectedResource resource;
                IntermediaryShaderModule.throwIfError(Spvc.spvc_context_parse_spirv((long)context, (IntBuffer)spirv.asIntBuffer(), (long)(spirv.remaining() / 4), (PointerBuffer)pointer), "Couldn't parse spirv");
                long ir = pointer.get(0);
                IntermediaryShaderModule.throwIfError(Spvc.spvc_context_create_compiler((long)context, (int)0, (long)ir, (int)1, (PointerBuffer)pointer), "Couldn't create compiler");
                long compiler = pointer.get(0);
                IntermediaryShaderModule.throwIfError(Spvc.spvc_compiler_create_shader_resources((long)compiler, (PointerBuffer)pointer), "Couldn't create resource list");
                long spvcResources = pointer.get(0);
                PointerBuffer countPointer = stack.callocPointer(1);
                IntermediaryShaderModule.throwIfError(Spvc.spvc_resources_get_resource_list_for_type((long)spvcResources, (int)1, (PointerBuffer)pointer, (PointerBuffer)countPointer), "Couldn't list uniform buffers");
                long spvcList = pointer.get(0);
                long spvcCount = countPointer.get(0);
                SpvcReflectedResource.Buffer resources = SpvcReflectedResource.create((long)spvcList, (int)((int)spvcCount));
                int i = 0;
                while ((long)i < spvcCount) {
                    resource = (SpvcReflectedResource)resources.get(i);
                    name = resource.nameString();
                    bindingOffset = IntermediaryShaderModule.getDecorationOffset(compiler, resource, 33, intReturnBuffer);
                    uniformBuffers.add(new SpvUniformBuffer(name, bindingOffset));
                    ++i;
                }
                IntermediaryShaderModule.throwIfError(Spvc.spvc_resources_get_resource_list_for_type((long)spvcResources, (int)7, (PointerBuffer)pointer, (PointerBuffer)countPointer), "Couldn't list sampled images");
                spvcList = pointer.get(0);
                spvcCount = countPointer.get(0);
                resources = SpvcReflectedResource.create((long)spvcList, (int)((int)spvcCount));
                i = 0;
                while ((long)i < spvcCount) {
                    resource = (SpvcReflectedResource)resources.get(i);
                    name = resource.nameString();
                    bindingOffset = IntermediaryShaderModule.getDecorationOffset(compiler, resource, 33, intReturnBuffer);
                    long typeHandle = Spvc.spvc_compiler_get_type_handle((long)compiler, (int)resource.type_id());
                    int dimension = Spvc.spvc_type_get_image_dimension((long)typeHandle);
                    samplers.add(new SpvSampler(name, bindingOffset, dimension));
                    ++i;
                }
                IntermediaryShaderModule.throwIfError(Spvc.spvc_resources_get_resource_list_for_type((long)spvcResources, (int)4, (PointerBuffer)pointer, (PointerBuffer)countPointer), "Couldn't list output variables");
                spvcList = pointer.get(0);
                spvcCount = countPointer.get(0);
                resources = SpvcReflectedResource.create((long)spvcList, (int)((int)spvcCount));
                i = 0;
                while ((long)i < spvcCount) {
                    resource = (SpvcReflectedResource)resources.get(i);
                    name = resource.nameString();
                    bindingOffset = IntermediaryShaderModule.getDecorationOffset(compiler, resource, 30, intReturnBuffer);
                    outputs.add(new SpvVariable(name, bindingOffset));
                    ++i;
                }
                IntermediaryShaderModule.throwIfError(Spvc.spvc_resources_get_resource_list_for_type((long)spvcResources, (int)3, (PointerBuffer)pointer, (PointerBuffer)countPointer), "Couldn't list input variables");
                spvcList = pointer.get(0);
                spvcCount = countPointer.get(0);
                resources = SpvcReflectedResource.create((long)spvcList, (int)((int)spvcCount));
                i = 0;
                while ((long)i < spvcCount) {
                    resource = (SpvcReflectedResource)resources.get(i);
                    name = resource.nameString();
                    bindingOffset = IntermediaryShaderModule.getDecorationOffset(compiler, resource, 30, intReturnBuffer);
                    inputs.add(new SpvVariable(name, bindingOffset));
                    ++i;
                }
            }
            finally {
                Spvc.spvc_context_destroy((long)context);
            }
        }
        IntBuffer spvAsIntBuffer = spirv.asIntBuffer();
        for (int i = 0; i < outputs.size(); ++i) {
            spvAsIntBuffer.put(((SpvVariable)outputs.get(i)).locationOffset(), i);
        }
        return new IntermediaryShaderModule(filename, spirv, uniformBuffers, samplers, outputs, inputs);
    }

    @Override
    public void close() {
        MemoryUtil.memFree((ByteBuffer)this.spirv);
    }

    public void rebind(List<String> inputVariables, List<VulkanBindGroupLayout.Entry> entries) throws ShaderCompileException {
        int i;
        if (this.spirv == null) {
            throw new IllegalStateException("Attempt to use invalid shader");
        }
        IntBuffer spvAsIntBuffer = this.spirv.asIntBuffer();
        HashSet<String> remainingInputs = new HashSet<String>();
        HashSet<String> remainingSamplers = new HashSet<String>();
        HashSet<String> remainingUniformBuffers = new HashSet<String>();
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
        for (i = 0; i < inputVariables.size(); ++i) {
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
        block9: for (i = 0; i < entries.size(); ++i) {
            VulkanBindGroupLayout.Entry entry = entries.get(i);
            switch (entry.type()) {
                case UNIFORM_BUFFER: {
                    SpvUniformBuffer ubo = this.getUniformBuffer(entry.name());
                    if (ubo == null) continue block9;
                    spvAsIntBuffer.put(ubo.bindingOffset(), i);
                    remainingUniformBuffers.remove(entry.name());
                    continue block9;
                }
                case SAMPLED_IMAGE: {
                    SpvSampler sampler = this.getSampler(entry.name());
                    if (sampler == null) continue block9;
                    if (sampler.dimensions() != 1 && sampler.dimensions() != 3) {
                        throw new ShaderCompileException("Unsupported texture dimensions '" + SpvcUtil.imageDimensionToString(sampler.dimensions()) + "' for sampler " + entry.name());
                    }
                    spvAsIntBuffer.put(sampler.bindingOffset(), i);
                    remainingSamplers.remove(entry.name());
                    continue block9;
                }
                case TEXEL_BUFFER: {
                    SpvSampler sampler = this.getSampler(entry.name());
                    if (sampler == null) continue block9;
                    if (sampler.dimensions() != 5) {
                        throw new ShaderCompileException("Unsupported texel buffer dimensions '" + SpvcUtil.imageDimensionToString(sampler.dimensions()) + "' for sampler " + entry.name());
                    }
                    spvAsIntBuffer.put(sampler.bindingOffset(), i);
                    remainingSamplers.remove(entry.name());
                }
            }
        }
        if (!remainingInputs.isEmpty()) {
            throw new ShaderCompileException("Shader expects input variables which are not being provided: " + String.valueOf(remainingInputs));
        }
        if (!remainingUniformBuffers.isEmpty()) {
            throw new ShaderCompileException("Shader expects uniform buffers which are not being provided: " + String.valueOf(remainingUniformBuffers));
        }
        if (!remainingSamplers.isEmpty()) {
            throw new ShaderCompileException("Shader expects samplers which are not being provided: " + String.valueOf(remainingSamplers));
        }
    }

    public long createVulkanShaderModule(VulkanDevice device) {
        if (this.spirv == null) {
            throw new IllegalStateException("Attempt to use invalid shader");
        }
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc((MemoryStack)stack).sType$Default().pCode(this.spirv);
            LongBuffer pointer = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateShaderModule((VkDevice)device.vkDevice(), (VkShaderModuleCreateInfo)info, null, (LongBuffer)pointer), "Can't compile " + this.name);
            device.instance().debug().setObjectName(device.vkDevice(), 15, pointer.get(0), () -> this.name);
            long l = pointer.get(0);
            return l;
        }
    }

    private @Nullable SpvUniformBuffer getUniformBuffer(String name) {
        for (SpvUniformBuffer ubo : this.uniformBuffers) {
            if (!ubo.name().equals(name)) continue;
            return ubo;
        }
        return null;
    }

    private @Nullable SpvSampler getSampler(String name) {
        for (SpvSampler sampler : this.samplers) {
            if (!sampler.name().equals(name)) continue;
            return sampler;
        }
        return null;
    }

    private @Nullable SpvVariable getInputVariable(String name) {
        for (SpvVariable variable : this.inputs) {
            if (!variable.name().equals(name)) continue;
            return variable;
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

    private static int getDecorationOffset(long compiler, SpvcReflectedResource resource, int decoration, IntBuffer returnBuffer) throws ShaderCompileException {
        if (!Spvc.spvc_compiler_get_binary_offset_for_decoration((long)compiler, (int)resource.id(), (int)decoration, (IntBuffer)returnBuffer)) {
            throw new ShaderCompileException("Couldn't find byte offset for location decoration of " + resource.nameString());
        }
        return returnBuffer.get(0);
    }
}

