package com.dx12.dx12;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.DeviceType;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.HintsAndWorkarounds;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * D3D12 implementation of the vanilla {@link GpuDeviceBackend} factory.
 *
 * P2 scope: every resource-creating method (texture / buffer / sampler /
 * texture view) creates real D3D12 resources through dx12_mc.dll. The render
 * pipeline methods (surface / command encoder / pipeline / query pool) are
 * stubbed and arrive in P3+.
 */
@Environment(EnvType.CLIENT)
public class Dx12Device implements GpuDeviceBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    private final DeviceInfo deviceInfo;

    public Dx12Device() {
        String probe = Dx12Native.dx12CreateDevice();
        LOGGER.info("[dx12] Device probe + resource self-test: {}", probe);
        this.deviceInfo = buildDeviceInfo(parseAdapterName(probe));
    }

    // -----------------------------------------------------------------------
    // P2: real D3D12 resources
    // -----------------------------------------------------------------------

    @Override
    public GpuSampler createSampler(AddressMode addressModeU, AddressMode addressModeV,
        FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        return new Dx12GpuSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> label, @GpuTexture.Usage int usage,
        GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new Dx12GpuTexture(usage, label == null ? "" : label.get(), format, width, height,
            depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage,
        GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new Dx12GpuTexture(usage, label == null ? "" : label, format, width, height,
            depthOrLayers, mipLevels);
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return this.createTextureView(texture, 0, texture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        return new Dx12GpuTextureView(texture, baseMipLevel, mipLevels);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
        return new Dx12GpuBuffer(usage, size);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage,
        ByteBuffer data) {
        // Mirror of the official VulkanDevice.createBuffer(data): allocate a buffer
        // with COPY_DST and upload the data immediately. No command encoder yet in
        // P2, so upload through a persistent map (UPLOAD heap via MAP_WRITE).
        GpuBuffer buffer = this.createBuffer(label, usage | GpuBuffer.USAGE_COPY_DST, data.remaining());
        GpuBufferSlice.MappedView view = buffer.map(0, data.remaining(), false, true);
        try {
            view.data().put(data.duplicate());
        } finally {
            view.close();
        }
        return buffer;
    }

    // -----------------------------------------------------------------------
    // P3+: stubbed until the render layer lands
    // -----------------------------------------------------------------------

    @Override
    public GpuSurfaceBackend createSurface(long windowHandle) {
        throw new UnsupportedOperationException("P3: DXGI swapchain not yet implemented");
    }

    @Override
    public CommandEncoderBackend createCommandEncoder() {
        throw new UnsupportedOperationException("P3: command encoder not yet implemented");
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline,
        @Nullable ShaderSource shaderSource) {
        throw new UnsupportedOperationException("P4: pipeline compilation not yet implemented");
    }

    @Override
    public void clearPipelineCache() {
        // No pipeline cache in P2.
    }

    @Override
    public GpuQueryPool createTimestampQueryPool(int size) {
        throw new UnsupportedOperationException("P3: timestamp query pool not yet implemented");
    }

    @Override
    public long getTimestampNow() {
        return 0L;
    }

    @Override
    public List<String> getLastDebugMessages() {
        return List.of();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    @Override
    public void close() {
        // Resources are closed individually; the native device context is process-lifetime.
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String parseAdapterName(String probe) {
        if (probe == null || probe.isBlank()) {
            return "D3D12 adapter";
        }
        int sep = probe.indexOf(" (");
        return sep < 0 ? probe : probe.substring(0, sep);
    }

    private static DeviceInfo buildDeviceInfo(String adapterName) {
        return new DeviceInfo(
            adapterName,  // name
            "D3D12",      // vendorName
            "D3D12 driver",  // driverInfo
            true,         // isZZeroToOne: D3D12 NDC depth is 0..1
            "DX12",       // backendName
            1.0f,         // timestampPeriod (P3: read from GetTimestampFrequency)
            new DeviceLimits(16, 256, 16384, Long.MAX_VALUE, 4096, 8),
            new DeviceFeatures(true, true, true, true, true, true, true),
            Set.of(),
            new HintsAndWorkarounds(false, false),
            DeviceType.DISCRETE  // heuristic; refine in P3 via DXGI adapter info
        );
    }
}
