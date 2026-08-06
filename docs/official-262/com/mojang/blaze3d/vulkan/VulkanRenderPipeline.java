/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.Struct
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
 *  org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo$Buffer
 *  org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
 *  org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState$Buffer
 *  org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
 *  org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo
 *  org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo
 *  org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo
 *  org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
 *  org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo
 *  org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo
 *  org.lwjgl.vulkan.VkPipelineRenderingCreateInfoKHR
 *  org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
 *  org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo$Buffer
 *  org.lwjgl.vulkan.VkPipelineVertexInputDivisorStateCreateInfoEXT
 *  org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
 *  org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo
 *  org.lwjgl.vulkan.VkVertexInputAttributeDescription
 *  org.lwjgl.vulkan.VkVertexInputAttributeDescription$Buffer
 *  org.lwjgl.vulkan.VkVertexInputBindingDescription
 *  org.lwjgl.vulkan.VkVertexInputBindingDescription$Buffer
 *  org.lwjgl.vulkan.VkVertexInputBindingDivisorDescription$Buffer
 *  org.lwjgl.vulkan.VkVertexInputBindingDivisorDescriptionEXT
 *  org.lwjgl.vulkan.VkVertexInputBindingDivisorDescriptionEXT$Buffer
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfoKHR;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputDivisorStateCreateInfoEXT;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDivisorDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDivisorDescriptionEXT;

@Environment(value=EnvType.CLIENT)
public record VulkanRenderPipeline(RenderPipeline info, VulkanDevice device, long withDepthPipeline, long withoutDepthPipeline, long pipelineLayout, VulkanBindGroupLayout layout, long vertexModule, long fragmentModule) implements CompiledRenderPipeline,
Destroyable
{
    public static final long INVALID_PIPELINE = 0L;

    @Override
    public boolean isValid() {
        return this.withDepthPipeline != 0L;
    }

    public static VulkanRenderPipeline compile(VulkanDevice device, VulkanBindGroupLayout layout, RenderPipeline pipeline, long vertexModule, long fragmentModule) {
        long pipelineLayout;
        try (MemoryStack stack = MemoryStack.stackPush();){
            VkPipelineLayoutCreateInfo createInfo = VkPipelineLayoutCreateInfo.calloc((MemoryStack)stack).sType$Default().pSetLayouts(stack.longs(layout.handle()));
            LongBuffer pointer = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreatePipelineLayout((VkDevice)device.vkDevice(), (VkPipelineLayoutCreateInfo)createInfo, null, (LongBuffer)pointer), "Can't create pipeline for " + String.valueOf(pipeline.getLocation()));
            pipelineLayout = pointer.get(0);
            device.instance().debug().setObjectName(device.vkDevice(), 17, pipelineLayout, () -> "Pipeline layout for " + String.valueOf(pipeline.getLocation()));
        }
        stack = MemoryStack.stackPush();
        try {
            long withoutDepthPipeline;
            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc((int)2, (MemoryStack)stack);
            ByteBuffer nameMain = stack.UTF8((CharSequence)"main");
            VkPipelineShaderStageCreateInfo vertexStage = VkPipelineShaderStageCreateInfo.calloc((MemoryStack)stack).sType$Default().stage(1).module(vertexModule).pName(nameMain);
            VkPipelineShaderStageCreateInfo fragmentStage = VkPipelineShaderStageCreateInfo.calloc((MemoryStack)stack).sType$Default().stage(16).module(fragmentModule).pName(nameMain);
            ((VkPipelineShaderStageCreateInfo.Buffer)((VkPipelineShaderStageCreateInfo.Buffer)shaderStages.put((Struct)vertexStage)).put((Struct)fragmentStage)).flip();
            @Nullable VertexFormat[] vertexBindings = pipeline.getVertexFormatBindings();
            VkVertexInputAttributeDescription.Buffer vertexAttributeDescriptions = VkVertexInputAttributeDescription.calloc((int)vertexBindings.length, (MemoryStack)stack);
            VkVertexInputBindingDescription.Buffer vertexBindingDescriptions = VkVertexInputBindingDescription.calloc((int)vertexBindings.length, (MemoryStack)stack);
            VkVertexInputBindingDivisorDescriptionEXT.Buffer vertexBindingDivisorDescriptions = VkVertexInputBindingDivisorDescriptionEXT.calloc((int)vertexBindings.length, (MemoryStack)stack);
            int attribLocation = 0;
            for (int i = 0; i < vertexBindings.length; ++i) {
                VertexFormat bindings = vertexBindings[i];
                if (bindings == null) continue;
                VkVertexInputBindingDescription bindingDescription = VkVertexInputBindingDescription.calloc((MemoryStack)stack).binding(i).stride(bindings.getVertexSize()).inputRate(bindings.getStepRate() > 0 ? 1 : 0);
                vertexBindingDescriptions.put((Struct)bindingDescription);
                if (bindings.getStepRate() > 0) {
                    VkVertexInputBindingDivisorDescriptionEXT divisorBinding = VkVertexInputBindingDivisorDescriptionEXT.calloc((MemoryStack)stack).binding(i).divisor(bindings.getStepRate());
                    vertexBindingDivisorDescriptions.put((Struct)divisorBinding);
                }
                for (VertexFormatElement element : bindings.getElements()) {
                    VkVertexInputAttributeDescription attributeDescription = VkVertexInputAttributeDescription.calloc((MemoryStack)stack).location(attribLocation).binding(i).offset(element.offset()).format(VulkanConst.toVk(element.format()));
                    vertexAttributeDescriptions.put((Struct)attributeDescription);
                    ++attribLocation;
                }
            }
            vertexAttributeDescriptions.flip();
            vertexBindingDescriptions.flip();
            vertexBindingDivisorDescriptions.flip();
            VkPipelineVertexInputDivisorStateCreateInfoEXT vertexInputDivisorState = VkPipelineVertexInputDivisorStateCreateInfoEXT.calloc((MemoryStack)stack).sType$Default().pVertexBindingDivisors((VkVertexInputBindingDivisorDescription.Buffer)vertexBindingDivisorDescriptions);
            VkPipelineVertexInputStateCreateInfo vertexInputState = VkPipelineVertexInputStateCreateInfo.calloc((MemoryStack)stack).sType$Default().pVertexAttributeDescriptions(vertexAttributeDescriptions).pVertexBindingDescriptions(vertexBindingDescriptions);
            if (vertexInputDivisorState.vertexBindingDivisorCount() > 0) {
                vertexInputState.pNext(vertexInputDivisorState);
            }
            VkPipelineInputAssemblyStateCreateInfo inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc((MemoryStack)stack).sType$Default().topology(VulkanConst.toVk(pipeline.getPrimitiveTopology()));
            VkPipelineRasterizationStateCreateInfo rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc((MemoryStack)stack).sType$Default().polygonMode(VulkanConst.toVk(pipeline.getPolygonMode())).cullMode(pipeline.isCull() ? 2 : 0).frontFace(1).lineWidth(1.0f);
            VkPipelineDepthStencilStateCreateInfo depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc((MemoryStack)stack).sType$Default();
            if (pipeline.getDepthStencilState() != null) {
                rasterizationState.depthBiasEnable(pipeline.getDepthStencilState().depthBiasConstant() != 0.0f && pipeline.getDepthStencilState().depthBiasScaleFactor() != 0.0f);
                rasterizationState.depthBiasConstantFactor(pipeline.getDepthStencilState().depthBiasConstant());
                rasterizationState.depthBiasSlopeFactor(pipeline.getDepthStencilState().depthBiasScaleFactor());
                depthStencilState.depthTestEnable(true);
                depthStencilState.depthWriteEnable(pipeline.getDepthStencilState().writeDepth());
                depthStencilState.depthCompareOp(VulkanConst.toVk(pipeline.getDepthStencilState().depthTest()));
            }
            @Nullable ColorTargetState[] colorTargetStates = pipeline.getColorTargetStates();
            VkPipelineColorBlendAttachmentState.Buffer blendAttachments = VkPipelineColorBlendAttachmentState.calloc((int)colorTargetStates.length, (MemoryStack)stack);
            for (ColorTargetState colorTargetState : colorTargetStates) {
                blendAttachments.colorWriteMask(colorTargetState != null ? VulkanConst.toVk(colorTargetState) : 0);
                if (colorTargetState != null && colorTargetState.blendFunction().isPresent()) {
                    VulkanRenderPipeline.applyBlendInformation(blendAttachments, colorTargetState.blendFunction().get());
                }
                blendAttachments.position(blendAttachments.position() + 1);
            }
            blendAttachments.position(0);
            VkPipelineColorBlendStateCreateInfo colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc((MemoryStack)stack).sType$Default().pAttachments(blendAttachments);
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc((MemoryStack)stack).sType$Default().scissorCount(1).viewportCount(1);
            VkPipelineMultisampleStateCreateInfo multisampleState = VkPipelineMultisampleStateCreateInfo.calloc((MemoryStack)stack).sType$Default().rasterizationSamples(1).sampleShadingEnable(false);
            VkPipelineDynamicStateCreateInfo dynamicStateInfo = VkPipelineDynamicStateCreateInfo.calloc((MemoryStack)stack).sType$Default().pDynamicStates(stack.ints(1, 0));
            VkPipelineRenderingCreateInfoKHR renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc((MemoryStack)stack).sType$Default();
            IntBuffer colorAttachmentFormats = stack.mallocInt(colorTargetStates.length);
            for (int i = 0; i < colorTargetStates.length; ++i) {
                ColorTargetState colorTargetState = colorTargetStates[i];
                colorAttachmentFormats.put(i, colorTargetState != null ? VulkanConst.toVk(colorTargetState.format()) : 0);
            }
            renderingInfo.pColorAttachmentFormats(colorAttachmentFormats);
            renderingInfo.depthAttachmentFormat(126);
            VkGraphicsPipelineCreateInfo.Buffer createInfo = VkGraphicsPipelineCreateInfo.calloc((int)1, (MemoryStack)stack).sType$Default().flags(0).pStages(shaderStages).pVertexInputState(vertexInputState).pInputAssemblyState(inputAssemblyState).pRasterizationState(rasterizationState).pDepthStencilState(depthStencilState).pColorBlendState(colorBlendState).pViewportState(viewportState).pMultisampleState(multisampleState).pDynamicState(dynamicStateInfo).layout(pipelineLayout).pNext(renderingInfo);
            LongBuffer pointer = stack.callocLong(1);
            VulkanUtils.crashIfFailure(device, VK12.vkCreateGraphicsPipelines((VkDevice)device.vkDevice(), (long)0L, (VkGraphicsPipelineCreateInfo.Buffer)createInfo, null, (LongBuffer)pointer), "Can't compile pipeline " + String.valueOf(pipeline.getLocation()));
            long withDepthPipeline = pointer.get(0);
            device.instance().debug().setObjectName(device.vkDevice(), 19, withDepthPipeline, () -> "Pipeline " + String.valueOf(pipeline.getLocation()));
            if (pipeline.getDepthStencilState() == null) {
                renderingInfo.depthAttachmentFormat(0);
                VulkanUtils.crashIfFailure(device, VK12.vkCreateGraphicsPipelines((VkDevice)device.vkDevice(), (long)0L, (VkGraphicsPipelineCreateInfo.Buffer)createInfo, null, (LongBuffer)pointer), "Can't compile pipeline " + String.valueOf(pipeline.getLocation()));
                withoutDepthPipeline = pointer.get(0);
                device.instance().debug().setObjectName(device.vkDevice(), 19, withoutDepthPipeline, () -> "Pipeline " + String.valueOf(pipeline.getLocation()));
            } else {
                withoutDepthPipeline = 0L;
            }
            VulkanRenderPipeline vulkanRenderPipeline = new VulkanRenderPipeline(pipeline, device, withDepthPipeline, withoutDepthPipeline, pipelineLayout, layout, vertexModule, fragmentModule);
            return vulkanRenderPipeline;
        }
        finally {
            if (stack != null) {
                stack.close();
            }
        }
    }

    @Override
    public void destroy() {
        if (this.withDepthPipeline == 0L) {
            return;
        }
        VK12.vkDestroyPipeline((VkDevice)this.device.vkDevice(), (long)this.withoutDepthPipeline, null);
        VK12.vkDestroyPipeline((VkDevice)this.device.vkDevice(), (long)this.withDepthPipeline, null);
        VK12.vkDestroyPipelineLayout((VkDevice)this.device.vkDevice(), (long)this.pipelineLayout, null);
        VK12.vkDestroyDescriptorSetLayout((VkDevice)this.device.vkDevice(), (long)this.layout.handle(), null);
        VK12.vkDestroyShaderModule((VkDevice)this.device.vkDevice(), (long)this.vertexModule, null);
        VK12.vkDestroyShaderModule((VkDevice)this.device.vkDevice(), (long)this.fragmentModule, null);
    }

    private static void applyBlendInformation(VkPipelineColorBlendAttachmentState.Buffer blendAttachments, BlendFunction blendFunction) {
        blendAttachments.blendEnable(true).colorBlendOp(VulkanConst.toVk(blendFunction.color().op())).alphaBlendOp(VulkanConst.toVk(blendFunction.alpha().op())).dstAlphaBlendFactor(VulkanConst.toVk(blendFunction.alpha().destFactor())).dstColorBlendFactor(VulkanConst.toVk(blendFunction.color().destFactor())).srcAlphaBlendFactor(VulkanConst.toVk(blendFunction.alpha().sourceFactor())).srcColorBlendFactor(VulkanConst.toVk(blendFunction.color().sourceFactor()));
    }
}

