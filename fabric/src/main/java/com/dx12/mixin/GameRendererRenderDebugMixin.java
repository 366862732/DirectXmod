package com.dx12.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Debug mixin to trace why renderLevel() is never called.
 */
@Mixin(GameRenderer.class)
public class GameRendererRenderDebugMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"))
    private void gl4dx12$debugRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        try {
            boolean resourcesLoaded = minecraft.isGameLoadFinished();
            boolean levelNotNull = minecraft.level != null;
            long gameTime = levelNotNull ? minecraft.level.getGameTime() : -1L;
            System.out.println("[GL4DX12 DEBUG] render(): resourcesLoaded=" + resourcesLoaded
                + ", advanceGameTime=" + advanceGameTime
                + ", level=null=" + !levelNotNull
                + ", gameTime=" + gameTime);
        } catch (Exception e) {
            System.err.println("[GL4DX12 DEBUG] Error: " + e);
        }
    }
}
