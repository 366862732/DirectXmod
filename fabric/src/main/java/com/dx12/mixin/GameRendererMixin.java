package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that injects into GameRenderer to trigger wgpu rendering.
 * Uses a broad injection point to avoid method signature mismatches.
 */
@Mixin(net.minecraft.client.render.GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    private void onRender(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        D3D12Bridge.syncWindowSize(width, height);

        long hwnd = D3D12Bridge.getWindowHandle();
        if (hwnd != 0) {
            D3D12Bridge.setWindow(hwnd);
        }

        D3D12Bridge.renderFrame();
        ci.cancel();
    }
}
