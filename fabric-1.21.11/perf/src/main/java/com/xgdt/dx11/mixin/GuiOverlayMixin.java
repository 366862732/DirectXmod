package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.overlay.PerfOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the optional debug overlay above the normal HUD. */
@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class GuiOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void dx11$renderPerfOverlay(
        GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        PerfOverlay.render(graphics);
    }
}
