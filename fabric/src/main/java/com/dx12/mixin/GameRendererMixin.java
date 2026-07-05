package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin that redirects getWindow() calls in GameRenderer to trigger wgpu rendering.
 */
@Mixin(net.minecraft.client.render.GameRenderer.class)
public class GameRendererMixin {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getWindow()Lnet/minecraft/class_928;"))
    private static Object redirectGetWindow(MinecraftClient mc) {
        if (mc != null && mc.getWindow() != null) {
            int width = mc.getWindow().getWidth();
            int height = mc.getWindow().getHeight();
            D3D12Bridge.syncWindowSize(width, height);

            long hwnd = D3D12Bridge.getWindowHandle();
            if (hwnd != 0) {
                D3D12Bridge.setWindow(hwnd);
            }

            D3D12Bridge.renderFrame();
        }
        return mc.getWindow();
    }
}
