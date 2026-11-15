package com.xgdt.dx11.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.xgdt.dx11.config.Dx11Config;
import com.xgdt.dx11.dx11.Dx11State;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.DynamicUniforms;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla {@code GlDevice} creation with the D3D11 device and
 * redirects the final swap-buffers call to the D3D11 swapchain.
 */
@Environment(EnvType.CLIENT)
@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Shadow
    private static @Nullable GpuDevice DEVICE;

    @Shadow
    private static String apiDescription;

    @Shadow
    private static @Nullable DynamicUniforms dynamicUniforms;

    @Shadow
    private static SamplerCache samplerCache;

    @Inject(method = "initRenderer", at = @At("HEAD"), cancellable = true)
    private static void dx11InitRenderer(
        long windowHandle,
        int debugVerbosity,
        boolean synchronous,
        ShaderSource shaderSource,
        boolean renderDebugLabels,
        CallbackInfo ci) {
        if (!Dx11Config.shouldUseDx11()) {
            return;
        }
        try {
            GpuDevice gpuDevice = Dx11State.init(windowHandle, shaderSource);
            DEVICE = gpuDevice;
            apiDescription = gpuDevice.getImplementationInformation();
            dynamicUniforms = new DynamicUniforms();
            samplerCache.initialize();
            ci.cancel();
        } catch (RuntimeException | Error t) {
            Dx11Config.setProbeResult("ERROR: D3D11 initialization failed: " + t);
            LoggerFactory.getLogger("gl4dx11").error(
                "[dx11] D3D11 initialization failed after the window was created with GLFW_NO_API. "
                    + "OpenGL cannot be used as a fallback in this process; on the target HD 3000, "
                    + "OpenGL 3.1 is below Minecraft 1.21.11's 3.3 requirement.", t);
            throw t;
        }
    }

    // @WrapOperation rather than @Redirect: MixinExtras 0.5.4 (bundled with
    // Fabric Loader 0.19.x) crashes while scanning a @Redirect whose @At is a
    // single-element array.  @WrapOperation is handled by a different
    // transformer and works on both 0.5.4 and 0.5.5.
    @WrapOperation(
        method = "flipFrame",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"))
    private static void dx11SwapBuffers(long windowHandle, Operation<Void> original) {
        if (Dx11Config.shouldUseDx11()) {
            Dx11State.flipFrame(windowHandle);
        } else {
            original.call(windowHandle);
        }
    }
}
