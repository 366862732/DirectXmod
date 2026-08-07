package com.dx12.dx12;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Direct3D 12 {@link GpuBackend} implementation.
 *
 * Mirrors {@code com.mojang.blaze3d.vulkan.VulkanBackend}'s contract:
 * <ul>
 *   <li>{@link #setWindowHints()} runs inside GLFW's error scope before the
 *       window is created (no hints needed for DX12).</li>
 *   <li>{@link #handleWindowCreationErrors} is only called when
 *       {@code glfwCreateWindow} returns 0.</li>
 *   <li>{@link #createDevice} is called once the window exists; it verifies the
 *       D3D12 device + resource chain (P2 self-test) and still throws a
 *       {@link BackendCreationException} so the game falls back to GL/Vulkan
 *       until the render layer (P3+) lands.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class Dx12Backend implements GpuBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    @Override
    public String getName() {
        return "DX12";
    }

    @Override
    public void setWindowHints() {
        // DX12 needs no GLFW window hints (Vulkan sets NO_API; GL sets its context hints).
    }

    @Override
    public void handleWindowCreationErrors(GLFWErrorCapture.Error error) throws BackendCreationException {
        if (error != null) {
            throw new BackendCreationException(
                String.format(Locale.ROOT, "GLFW_ERROR: 0x%X", error.error()),
                BackendCreationException.Reason.GLFW_ERROR);
        }
        throw new BackendCreationException(
            "Failed to create window for DX12", BackendCreationException.Reason.GLFW_ERROR);
    }

    @Override
    public GpuDevice createDevice(long window, ShaderSource defaultShaderSource,
        GpuDebugOptions debugOptions, Runnable criticalShaderLoader) throws BackendCreationException {
        // P2: prove the whole Java -> JNI -> C++ D3D12 resource chain works by
        // creating + destroying real textures/buffers/samplers/views through a
        // Dx12Device. The render layer (P3+) is not implemented yet, so we still
        // fail with a clean BackendCreationException and let the game fall back
        // to GL/Vulkan (same safety net as P1).
        try {
            Dx12Device device = new Dx12Device();
            selfTestJavaResources(device);
        } catch (Throwable t) {
            LOGGER.error("[dx12] D3D12 resource self-test failed: {}", t.toString());
            throw new BackendCreationException(
                "DX12 resource self-test failed: " + t, BackendCreationException.Reason.OTHER);
        }
        throw new BackendCreationException(
            "DX12 resource layer verified; render layer (P3+) not yet implemented",
            BackendCreationException.Reason.OTHER);
    }

    /**
     * Exercise every P2 resource method through the real JNI path so a broken
     * native binding fails loudly here instead of during vanilla initRenderer.
     */
    private static void selfTestJavaResources(Dx12Device device) {
        // texture: RGBA8_UNORM, 64x64, TEXTURE_BINDING|RENDER_ATTACHMENT
        GpuTexture texture = device.createTexture("dx12-selftest",
            GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
            GpuFormat.RGBA8_UNORM, 64, 64, 1, 1);
        // sampler: REPEAT/REPEAT, NEAREST/NEAREST, aniso=1
        GpuSampler sampler = device.createSampler(AddressMode.REPEAT, AddressMode.REPEAT,
            FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.of(16.0));
        // buffer: MAP_WRITE, map -> write -> unmap
        GpuBuffer buffer = device.createBuffer(() -> "dx12-selftest",
            GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST, 1024);
        try (GpuBufferSlice.MappedView view = buffer.map(0, 1024, false, true)) {
            ByteBuffer data = view.data();
            data.put(0, (byte) 0x5A);
            data.put(1023, (byte) 0x5A);
            if ((data.get(0) & 0xFF) != 0x5A || (data.get(1023) & 0xFF) != 0x5A) {
                throw new IllegalStateException("buffer map write/read mismatch");
            }
        }
        // view over the texture
        device.createTextureView(texture, 0, texture.getMipLevels()).close();
        buffer.close();
        sampler.close();
        texture.close();
        LOGGER.info("[dx12] Java resource self-test OK (texture/buffer/sampler/view via JNI)");
    }
}
