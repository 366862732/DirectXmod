package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.overlay.PerfOverlayStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Measures render-thread work for the debug overlay.
 *
 * <p>The overlay itself draws later in {@code Gui.render}; this hook only
 * records how long the world/UI render call took, so the overlay can compare
 * CPU time with the frame interval and the GPU timestamp.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererTimingMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void dx11$overlayRenderStart(
        DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PerfOverlayStats.beginCpuFrame();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void dx11$overlayRenderEnd(
        DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PerfOverlayStats.endCpuFrame();
    }
}
