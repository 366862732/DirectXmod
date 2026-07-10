package com.dx12.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dx12.D3D12Bridge;

import net.minecraft.client.Minecraft;

/**
 * Surface mode: D3D12 Present() at TAIL of Minecraft.runTick().
 *
 * MC 26.1.2 removed Window.updateDisplay() — we cannot inject there.
 * Instead, D3D12 renders + presents AFTER the entire game tick (post GL swap).
 * GL swap shows MC content; D3D12 Present() immediately replaces it.
 * With PresentMode::Immediate, flicker is minimal.
 *
 * Offscreen mode: renderFrame() in ClientTickEvents, overlay via
 * GameRendererMixin TAIL (unchanged).
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "runTick", at = @At("TAIL"))
    private void onRunTickTail(CallbackInfo ci) {
        // Only render via D3D12 swapchain when in-world (surface mode + level loaded).
        // Title screen uses offscreen mode (GL overlay) to avoid D3D12/GL contention.
        if (D3D12Bridge.hasSurface()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                // Camera was updated in ClientTickEvents.END_CLIENT_TICK.
                // renderFrame() dispatches to render_surface() → Present().
                D3D12Bridge.renderFrame();
            }
        }
    }
}
