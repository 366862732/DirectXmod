package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.culling.VisibleSections;
import com.xgdt.dx11.perf.region.RegionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detaches region arenas when the renderer rebuilds all sections or changes
 * level.  Detaching (rather than force-closing) keeps any mesh that is still
 * visible for a frame valid; its arena closes when the last slice is freed.
 *
 * <p>The visible-section snapshot is dropped at the same points so entity and
 * upload culling never use coordinates from a previous world.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public class LevelRendererLifecycleMixin {
    @Inject(method = "allChanged", at = @At("HEAD"))
    private void dx11$clearRegionsOnRebuild(CallbackInfo ci) {
        RegionManager.clear();
        VisibleSections.clear();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void dx11$clearRegionsOnLevelChange(ClientLevel level, CallbackInfo ci) {
        RegionManager.clear();
        VisibleSections.clear();
    }
}
