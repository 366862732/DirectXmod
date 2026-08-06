/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.TracyClient
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.TracyGpuProfiler;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GpuDevice {
    private final GpuDeviceBackend backend;
    private final Runnable criticalShaderLoader;
    private final @Nullable TracyGpuProfiler profiler;

    public GpuDevice(GpuDeviceBackend backend, Runnable criticalShaderLoader) {
        this.backend = backend;
        this.criticalShaderLoader = criticalShaderLoader;
        this.profiler = TracyClient.isAvailable() ? new TracyGpuProfiler(this) : null;
    }

    public GpuSurface createSurface(long windowHandle) {
        return new GpuSurface(this.backend.createSurface(windowHandle));
    }

    public CommandEncoder createCommandEncoder() {
        return new CommandEncoder(this.profiler, this.backend, this.backend.createCommandEncoder());
    }

    public GpuSampler createSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        int maxSupportedAnisotropy = this.getDeviceInfo().limits().maxAnisotropy();
        if (maxAnisotropy < 1 || maxAnisotropy > maxSupportedAnisotropy) {
            throw new IllegalArgumentException("maxAnisotropy out of range; must be >= 1 and <= " + maxSupportedAnisotropy + ", but was " + maxAnisotropy);
        }
        return this.backend.createSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    public GpuTexture createTexture(@Nullable Supplier<String> label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        this.verifyTextureCreationArgs(usage, width, height, depthOrLayers, mipLevels);
        return this.backend.createTexture(label, usage, format, width, height, depthOrLayers, mipLevels);
    }

    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        this.verifyTextureCreationArgs(usage, width, height, depthOrLayers, mipLevels);
        return this.backend.createTexture(label, usage, format, width, height, depthOrLayers, mipLevels);
    }

    private void verifyTextureCreationArgs(@GpuTexture.Usage int usage, int width, int height, int depthOrLayers, int mipLevels) {
        boolean isCubemap;
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be at least 1");
        }
        int maxDimension = Math.max(width, height);
        int maxMipSupported = Mth.log2(maxDimension) + 1;
        if (mipLevels > maxMipSupported) {
            throw new IllegalArgumentException("mipLevels must be at most " + maxMipSupported + " for a texture of width " + width + " and height " + height + " (asked for " + mipLevels + " mipLevels)");
        }
        if (depthOrLayers < 1) {
            throw new IllegalArgumentException("depthOrLayers must be at least 1");
        }
        boolean bl = isCubemap = (usage & 0x10) != 0;
        if (isCubemap) {
            if (width != height) {
                throw new IllegalArgumentException("Cubemap compatible textures must be square, but size is " + width + "x" + height);
            }
            if (depthOrLayers % 6 != 0) {
                throw new IllegalArgumentException("Cubemap compatible textures must have a layer count with a multiple of 6, was " + depthOrLayers);
            }
            if (depthOrLayers > 6) {
                throw new UnsupportedOperationException("Array textures are not yet supported");
            }
        } else if (depthOrLayers > 1) {
            throw new UnsupportedOperationException("Array or 3D textures are not yet supported");
        }
    }

    public GpuTextureView createTextureView(GpuTexture texture) {
        this.verifyTextureViewCreationArgs(texture, 0, texture.getMipLevels());
        return this.backend.createTextureView(texture, 0, texture.getMipLevels());
    }

    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        this.verifyTextureViewCreationArgs(texture, baseMipLevel, mipLevels);
        return this.backend.createTextureView(texture, baseMipLevel, mipLevels);
    }

    private void verifyTextureViewCreationArgs(GpuTexture texture, int baseMipLevel, int mipLevels) {
        if (texture.isClosed()) {
            throw new IllegalArgumentException("Can't create texture view with closed texture");
        }
        if (baseMipLevel < 0 || baseMipLevel + mipLevels > texture.getMipLevels()) {
            throw new IllegalArgumentException(mipLevels + " mip levels starting from " + baseMipLevel + " would be out of range for texture with only " + texture.getMipLevels() + " mip levels");
        }
    }

    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
        if (size <= 0L) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        }
        return this.backend.createBuffer(label, usage, size);
    }

    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data) {
        if (!data.hasRemaining()) {
            throw new IllegalArgumentException("Buffer source must not be empty");
        }
        return this.backend.createBuffer(label, usage, data);
    }

    public List<String> getLastDebugMessages() {
        return this.backend.getLastDebugMessages();
    }

    public boolean isDebuggingEnabled() {
        return this.backend.isDebuggingEnabled();
    }

    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline) {
        return this.precompilePipeline(pipeline, null);
    }

    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSource shaderSource) {
        return this.backend.precompilePipeline(pipeline, shaderSource);
    }

    public void clearPipelineCache() {
        this.backend.clearPipelineCache();
    }

    public void loadCriticalShaders() {
        this.criticalShaderLoader.run();
    }

    public void close() {
        this.backend.close();
    }

    public GpuQueryPool createTimestampQueryPool(int size) {
        return this.backend.createTimestampQueryPool(size);
    }

    protected long getTimestampNow() {
        return this.backend.getTimestampNow();
    }

    public DeviceInfo getDeviceInfo() {
        return this.backend.getDeviceInfo();
    }
}

