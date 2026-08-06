/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.IntIntPair
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.util.vma.Vma
 *  org.lwjgl.vulkan.VK12
 *  org.lwjgl.vulkan.VkDevice
 *  org.lwjgl.vulkan.VkPhysicalDeviceLimits
 *  org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Properties
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.HintsAndWorkarounds;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import com.mojang.blaze3d.vulkan.VulkanGpuSampler;
import com.mojang.blaze3d.vulkan.VulkanGpuSurface;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanInstance;
import com.mojang.blaze3d.vulkan.VulkanPhysicalDevice;
import com.mojang.blaze3d.vulkan.VulkanQueryPool;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import java.lang.invoke.CallSite;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Properties;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class VulkanDevice
implements GpuDeviceBackend {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ShaderSource defaultShaderSource;
    private final Map<RenderPipeline, VulkanRenderPipeline> pipelineCache = new IdentityHashMap<RenderPipeline, VulkanRenderPipeline>();
    private final Map<ShaderCompilationKey, IntermediaryShaderModule> shaderCache = new HashMap<ShaderCompilationKey, IntermediaryShaderModule>();
    private final VulkanInstance instance;
    private final VkDevice vkDevice;
    private final long vma;
    private final GlslCompiler glslCompiler = new GlslCompiler();
    private final DeviceInfo deviceInfo;
    private final VulkanQueue graphicsQueue;
    private final VulkanQueue computeQueue;
    private final VulkanQueue transferQueue;
    private final boolean isIntegratedIntelMoltenVK;
    private final VulkanCommandEncoder commandEncoder;
    private final CheckpointExtension checkpointExtension;

    public VulkanDevice(ShaderSource defaultShaderSource, VulkanInstance instance, VulkanPhysicalDevice physicalDevice, Set<String> enabledDeviceExtensions, VkDevice vkDevice, long vma, CheckpointExtension checkpointExtension) {
        this.defaultShaderSource = defaultShaderSource;
        this.instance = instance;
        this.vkDevice = vkDevice;
        this.vma = vma;
        this.checkpointExtension = checkpointExtension;
        HashSet<CallSite> extensionNames = new HashSet<CallSite>();
        for (String name : instance.getEnabledExtensions()) {
            extensionNames.add((CallSite)((Object)(name + " (I)")));
        }
        for (String name : enabledDeviceExtensions) {
            extensionNames.add((CallSite)((Object)(name + " (D)")));
        }
        VkPhysicalDeviceLimits limits = physicalDevice.vkPhysicalDeviceProperties().limits();
        VkPhysicalDeviceVulkan11Properties vk11Properties = physicalDevice.vkPhysicalDeviceVulkan11Properties();
        this.deviceInfo = new DeviceInfo(physicalDevice.deviceName(), physicalDevice.vendorName(), physicalDevice.driverInfo(), true, "Vulkan", limits.timestampPeriod(), new DeviceLimits((int)limits.maxSamplerAnisotropy(), (int)limits.minUniformBufferOffsetAlignment(), limits.maxImageDimension2D(), vk11Properties.maxMemoryAllocationSize() < 0L ? Long.MAX_VALUE : vk11Properties.maxMemoryAllocationSize(), physicalDevice.vkPhysicalDeviceMultiDrawPropertiesEXT().maxMultiDrawCount() < 0 ? Integer.MAX_VALUE : physicalDevice.vkPhysicalDeviceMultiDrawPropertiesEXT().maxMultiDrawCount(), limits.maxColorAttachments()), new DeviceFeatures(true, enabledDeviceExtensions.contains("VK_EXT_multi_draw"), false, true, true, true, true), Collections.unmodifiableSet(extensionNames), new HintsAndWorkarounds(false, false), physicalDevice.deviceType());
        IntIntPair graphicsQueueFamily = physicalDevice.graphicsQueueFamilyAndIndex();
        assert (graphicsQueueFamily != null);
        IntIntPair computeQueueFamily = physicalDevice.computeQueueFamilyAndIndex();
        IntIntPair transferQueueFamily = physicalDevice.transferQueueFamilyAndIndex();
        this.graphicsQueue = new VulkanQueue(this, graphicsQueueFamily.leftInt(), graphicsQueueFamily.rightInt());
        this.computeQueue = computeQueueFamily != null ? new VulkanQueue(this, computeQueueFamily.leftInt(), computeQueueFamily.rightInt()) : this.graphicsQueue;
        this.transferQueue = transferQueueFamily != null ? new VulkanQueue(this, transferQueueFamily.leftInt(), transferQueueFamily.rightInt()) : this.computeQueue;
        this.isIntegratedIntelMoltenVK = physicalDevice.vkPhysicalDeviceProperties().deviceType() == 1 && physicalDevice.vkPhysicalDeviceProperties().vendorID() == 32902 && physicalDevice.vkPhysicalDeviceDriverProperties().driverID() == 14;
        physicalDevice.close();
        this.commandEncoder = new VulkanCommandEncoder(this);
    }

    @Override
    public void close() {
        this.checkpointExtension.close();
        this.commandEncoder.destroy();
        this.clearPipelineCache();
        Vma.vmaDestroyAllocator((long)this.vma);
        VK12.vkDestroyDevice((VkDevice)this.vkDevice, null);
        this.instance.close();
        this.glslCompiler.close();
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    public VulkanInstance instance() {
        return this.instance;
    }

    public VkDevice vkDevice() {
        return this.vkDevice;
    }

    public VulkanQueue graphicsQueue() {
        return this.graphicsQueue;
    }

    public VulkanQueue computeQueue() {
        return this.computeQueue;
    }

    public VulkanQueue transferQueue() {
        return this.transferQueue;
    }

    public long vma() {
        return this.vma;
    }

    @Override
    public GpuSurfaceBackend createSurface(long windowHandle) {
        return new VulkanGpuSurface(this, windowHandle);
    }

    @Override
    public VulkanCommandEncoder createCommandEncoder() {
        return this.commandEncoder;
    }

    @Override
    public GpuSampler createSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        return new VulkanGpuSampler(this, addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new VulkanGpuTexture(this, usage, this.isDebuggingEnabled() && label != null ? label.get() : "", format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new VulkanGpuTexture(this, usage, this.isDebuggingEnabled() && label != null ? label : "", format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return this.createTextureView(texture, 0, texture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        return new VulkanGpuTextureView(this, (VulkanGpuTexture)texture, baseMipLevel, mipLevels);
    }

    @Override
    public VulkanGpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
        return new VulkanGpuBuffer.Direct(this, label, usage, size, this.isIntegratedIntelMoltenVK);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data) {
        GpuBuffer buffer = this.createBuffer((Supplier)label, usage | 8, (long)data.remaining());
        this.createCommandEncoder().writeToBuffer(buffer.slice(), data);
        return buffer;
    }

    @Override
    public List<String> getLastDebugMessages() {
        return List.of();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return this.instance.debug().enabled();
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSource customShaderSource) {
        ShaderSource shaderSource = customShaderSource == null ? this.defaultShaderSource : customShaderSource;
        return this.pipelineCache.computeIfAbsent(pipeline, ignored -> this.compilePipeline(pipeline, shaderSource));
    }

    protected VulkanRenderPipeline getOrCompilePipeline(RenderPipeline pipeline) {
        return this.pipelineCache.computeIfAbsent(pipeline, ignored -> this.compilePipeline(pipeline, this.defaultShaderSource));
    }

    protected IntermediaryShaderModule getOrCompileShader(Identifier id, ShaderType type, ShaderDefines defines, ShaderSource shaderSource) {
        ShaderCompilationKey key = new ShaderCompilationKey(id, type, defines);
        return this.shaderCache.computeIfAbsent(key, ignored -> this.compileShader(key, shaderSource));
    }

    private IntermediaryShaderModule compileShader(ShaderCompilationKey key, ShaderSource shaderSource) {
        String source = shaderSource.get(key.id, key.type);
        if (source == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", (Object)key.type, (Object)key.id);
            return IntermediaryShaderModule.INVALID;
        }
        String sourceWithDefines = GlslPreprocessor.injectDefines(source, key.defines);
        try {
            return this.glslCompiler.createIntermediary(key.id.toDebugFileName(), sourceWithDefines, key.type);
        }
        catch (ShaderCompileException e) {
            LOGGER.error("Couldn't compile {} shader {}: {}", new Object[]{key.type, key.id, e.getMessage()});
            return IntermediaryShaderModule.INVALID;
        }
    }

    private VulkanRenderPipeline compilePipeline(RenderPipeline pipeline, ShaderSource shaderSource) {
        IntermediaryShaderModule vertexShader = this.getOrCompileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
        IntermediaryShaderModule fragmentShader = this.getOrCompileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
        if (vertexShader == IntermediaryShaderModule.INVALID) {
            LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", (Object)pipeline.getLocation(), (Object)pipeline.getVertexShader());
            return new VulkanRenderPipeline(pipeline, this, 0L, 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L, 0L);
        }
        if (fragmentShader == IntermediaryShaderModule.INVALID) {
            LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", (Object)pipeline.getLocation(), (Object)pipeline.getFragmentShader());
            return new VulkanRenderPipeline(pipeline, this, 0L, 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L, 0L);
        }
        try {
            GlslCompiler.CompiledModules modules = this.glslCompiler.compile(this, pipeline, vertexShader, fragmentShader);
            return VulkanRenderPipeline.compile(this, modules.layout(), pipeline, modules.vertex(), modules.fragment());
        }
        catch (ShaderCompileException e) {
            LOGGER.error("Couldn't compile pipeline {}: {}", (Object)pipeline.getLocation(), (Object)e.getMessage());
            return new VulkanRenderPipeline(pipeline, this, 0L, 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L, 0L);
        }
    }

    @Override
    public void clearPipelineCache() {
        this.graphicsQueue.waitIdle();
        this.pipelineCache.values().forEach(VulkanRenderPipeline::destroy);
        this.pipelineCache.clear();
        this.shaderCache.values().forEach(IntermediaryShaderModule::close);
        this.shaderCache.clear();
    }

    @Override
    public GpuQueryPool createTimestampQueryPool(int size) {
        return new VulkanQueryPool(this, size);
    }

    @Override
    public long getTimestampNow() {
        return this.commandEncoder.getTimestampNow();
    }

    public CheckpointExtension checkpointExtension() {
        return this.checkpointExtension;
    }

    @Environment(value=EnvType.CLIENT)
    private record ShaderCompilationKey(Identifier id, ShaderType type, ShaderDefines defines) {
        @Override
        public String toString() {
            String string = String.valueOf(this.id) + " (" + String.valueOf((Object)this.type) + ")";
            if (!this.defines.isEmpty()) {
                return string + " with " + String.valueOf(this.defines);
            }
            return string;
        }
    }
}

