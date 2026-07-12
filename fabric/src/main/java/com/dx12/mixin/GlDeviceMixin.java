package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppress the OpenGL buffer swap (glfwSwapBuffers) in surface mode.
 *
 * MC 26.1.2 refactored the swap chain: Window.updateDisplay() was removed,
 * and the GL swap now goes through GlDevice.presentFrame() → GLFW.glfwSwapBuffers().
 *
 * When D3D12 surface mode is active and the player is in-world, we cancel
 * the GL swap so that D3D12 Present() via MinecraftMixin TAIL is the sole
 * presentation on the HWND. This eliminates the double-present GPU driver
 * contention that causes TDR timeouts.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public class GlDeviceMixin {
    private static boolean glDeviceMixinApplied = false;

    @Inject(method = "presentFrame", at = @At("HEAD"), cancellable = true)
    private void onPresentFrame(CallbackInfo ci) {
        if (!glDeviceMixinApplied) {
            glDeviceMixinApplied = true;
            com.dx12.Dx12Mod.LOGGER.info("[dx12-wm] GlDeviceMixin applied — presentFrame intercepted");
        }
        if (D3D12Bridge.hasSurface()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                // Suppress the GL swap — let D3D12 Present() be the only presentation.
                // The GL context is still valid; we're just skipping the buffer swap.
                ci.cancel();
            }
        }
    }
}
