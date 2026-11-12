package com.xgdt.dx11.mixin;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.xgdt.dx11.config.Dx11Config;
import com.xgdt.dx11.dx11.Dx11State;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creates the GLFW window without an OpenGL context when the D3D11 backend is
 * selected, and routes vsync through the D3D11 swapchain.
 *
 * <p>The window hint must be set after all of vanilla's hints (the constructor
 * explicitly requests {@code GLFW_OPENGL_API}), hence the injection directly
 * before {@code glfwCreateWindow}.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(Window.class)
public class WindowMixin {
    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
            shift = At.Shift.BEFORE))
    private void dx11RequestNoApi(
        WindowEventHandler eventHandler,
        ScreenManager screenManager,
        DisplayData displayData,
        @Nullable String preferredFullscreenVideoMode,
        String title,
        CallbackInfo ci) {
        if (Dx11Config.shouldUseDx11()) {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        }
    }

    @Inject(method = "updateVsync", at = @At("HEAD"), cancellable = true)
    private void dx11UpdateVsync(boolean vsync, CallbackInfo ci) {
        if (Dx11Config.shouldUseDx11()) {
            // GLFW swap interval is meaningless without a GL context; the
            // D3D11 swapchain's Present(1, 0) does the actual vsync.
            Dx11State.setVsync(vsync);
            ci.cancel();
        }
    }
}
