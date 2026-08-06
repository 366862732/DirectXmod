package com.dx12.dx12;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import java.util.Locale;
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
 *   <li>{@link #createDevice} is called once the window exists; a success
 *       immediately triggers {@code device.getDeviceInfo()} and
 *       {@code RenderSystem.initRenderer} (which creates samplers/buffers),
 *       so P1 intentionally fails after probing D3D12 to let the game fall
 *       back to GL/Vulkan. P2+ returns a real {@link GpuDevice}.</li>
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
        // P1 hook validation: prove the vanilla backend-selection loop reaches us
        // AND the JNI chain into the Rust D3D12 layer works, then fail so the game
        // falls back to GL/Vulkan. Do not return a GpuDevice until the resource
        // layer (P2+) is implemented, or initRenderer will crash on unimplemented
        // sampler/buffer/texture creation.
        try {
            String info = Dx12Native.dx12CreateDevice();
            LOGGER.info("[dx12] D3D12 probe OK (createDevice called by vanilla): {}", info);
        } catch (Throwable t) {
            LOGGER.error("[dx12] D3D12 probe failed: {}", t.toString());
        }
        throw new BackendCreationException(
            "DX12 backend is under construction (P1 hook validation only)",
            BackendCreationException.Reason.OTHER);
    }
}
