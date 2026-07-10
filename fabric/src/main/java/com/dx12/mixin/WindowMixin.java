package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses OpenGL SwapBuffers when D3D12 surface mode is active.
 *
 * In surface mode, D3D12 calls Present() on the swapchain, which makes
 * the rendered frame visible. GL's SwapBuffers would overwrite the
 * D3D12-presented frame with a stale OpenGL buffer, so we cancel it.
 */
@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "updateDisplay", at = @At("HEAD"), cancellable = true)
    private void onUpdateDisplay(CallbackInfo ci) {
        if (D3D12Bridge.hasSurface()) {
            ci.cancel();
        }
    }
}
