package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if (D3D12Bridge.hasSurface()) {
            // Camera was updated in ClientTickEvents.END_CLIENT_TICK.
            // renderFrame() dispatches to render_surface() → Present().
            D3D12Bridge.renderFrame();
        }
    }
}
