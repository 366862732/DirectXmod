package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12Device;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Debug mixin injected at GameRenderer.render(DeltaTracker, boolean) HEAD.
 * Prints resourcesLoaded / levelNotNull / gameTime so we can confirm whether
 * the official per-frame render pipeline actually fires when launched via
 * fabric-262-26.2.jar / gl4dx12-0.1.0.jar.
 *
 * P15 enhancement: adds frame counter + advanceGameTime + pause state.
 * advanceGameTime controls whether renderLevel() is called (must be true for world rendering).
 */
@Mixin(GameRenderer.class)
public class GameRendererRenderDebugMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private GameRenderState gameRenderState;

    // P15: frame counter, reset on each new game load
    private int dx12_renderFrameCount = 0;
    private int dx12_lastResourcesLoaded = -1;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void dx12_renderDebug(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        // Always print on every frame so we can confirm GameRenderer.render() fires
        boolean resourcesLoaded = this.minecraft.isGameLoadFinished();
        boolean levelNotNull = this.minecraft.level != null;
        long gameTime = levelNotNull ? this.minecraft.level.getGameTime() : -1L;
        System.err.println("[DX12] GameRenderer.render() called frame=" + dx12_renderFrameCount
            + " window=" + (this.minecraft.getWindow().getWidth() + "x" + this.minecraft.getWindow().getHeight())
            + " level=" + (levelNotNull ? "non-null" : "NULL")
            + " paused=" + this.minecraft.isPaused()
            + " resourcesLoaded=" + resourcesLoaded);
        System.err.flush();

        int rlInt = resourcesLoaded ? 1 : 0;
        int lnInt = levelNotNull ? 1 : 0;

        // Detect game load transition to reset frame counter
        if (rlInt != dx12_lastResourcesLoaded) {
            dx12_renderFrameCount = 0;
            dx12_lastResourcesLoaded = rlInt;
        }
        dx12_renderFrameCount++;

        // P15: sample every 30 frames to reduce log noise, always print frame 1
        boolean sample = (dx12_renderFrameCount == 1) || (dx12_renderFrameCount % 30 == 0);
        if (sample) {
            System.err.println("[dx12-debug] render(): frame=" + dx12_renderFrameCount
                + " resourcesLoaded=" + rlInt
                + " advanceGameTime=" + advanceGameTime
                + " levelNotNull=" + lnInt
                + " gameTime=" + gameTime
                + " backend=" + (Dx12Device.isInitialized() ? "DX12" : "NONE")
                + " paused=" + this.minecraft.isPaused());
            System.err.flush();
            // P38: 打印 Lightmap renderState 数值，供与读回的 16×16 lightmap 内容对照。
            LightmapRenderState s = this.gameRenderState.lightmapRenderState;
            System.err.println("[dx12-debug] lightmapState needsUpdate=" + s.needsUpdate
                + " skyFactor=" + s.skyFactor
                + " blockFactor=" + s.blockFactor
                + " brightness=" + s.brightness
                + " darknessScale=" + s.darknessEffectScale
                + " bossDark=" + s.bossOverlayWorldDarkening
                + " nvInt=" + s.nightVisionEffectIntensity
                + " ambient=(" + fmt(s.ambientColor) + ")"
                + " skyColor=(" + fmt(s.skyLightColor) + ")"
                + " blockTint=(" + fmt(s.blockLightTint) + ")"
                + " nvColor=(" + fmt(s.nightVisionColor) + ")"
                + " lmHandle=" + (Dx12Device.getDebugLightmapHandle() != 0L ? "captured" : "none"));
            System.err.flush();
        }

        // Always print the decision path on frame 1 (loading → loaded transition)
        if (dx12_renderFrameCount == 1) {
            System.err.println("[dx12-debug] render() frame=1: willRenderLevel="
                + (resourcesLoaded && advanceGameTime && levelNotNull));
            System.err.flush();
        }
    }

    private static String fmt(Vector3fc c) {
        if (c == null) {
            return "null";
        }
        return String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f", c.x(), c.y(), c.z());
    }
}
