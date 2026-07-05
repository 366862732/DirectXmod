package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that intercepts GameRenderer.render() to replace
 * OpenGL rendering with our Rust/wgpu backend.
 */
@Mixin(value = net.minecraft.client.render.GameRenderer.class, remap = false)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(
            RenderTickCounter tickCounter,
            boolean tick,
            CallbackInfo ci
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return; // Not ready yet, let OpenGL render
        }

        // Sync window size with wgpu renderer
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        D3D12Bridge.syncWindowSize(width, height);

        // Get window HWND for wgpu surface
        long hwnd = D3D12Bridge.getWindowHandle();
        if (hwnd != 0) {
            D3D12Bridge.setWindow(hwnd);
        }

        // Trigger wgpu rendering
        D3D12Bridge.renderFrame();

        // Cancel the original OpenGL rendering
        ci.cancel();
    }
}
