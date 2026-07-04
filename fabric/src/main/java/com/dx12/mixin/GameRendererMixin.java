package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that intercepts GameRenderer.render() to replace
 * OpenGL rendering with our Rust/wgpu backend.
 *
 * In MC 1.21.1, the render entry point moved from LevelRenderer.renderLevel()
 * to GameRenderer.render(RenderTickCounter, boolean).
 */
@Mixin(net.minecraft.client.render.GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(
            RenderTickCounter tickCounter,
            boolean tick,
            CallbackInfo ci
    ) {
        // Log entry point
        com.dx12.Dx12Mod.LOGGER.info("GameRendererMixin: intercepting render");

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
