package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.culling.FogOcclusion;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the fog ranges vanilla writes to the {@code Fog} uniform and tracks
 * the debug fog toggle.
 *
 * <p>Fog occlusion needs the exact ends used by the terrain shader.  Deriving
 * them again would duplicate biome, weather, water, darkness and boss-fog
 * rules; {@code setupFog} already computed them by the time the level renders.
 * The constructor also calls {@code updateBuffer} with maximum ranges, which
 * simply keeps culling disabled until the first real frame.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "updateBuffer", at = @At("HEAD"))
    private void dx11$captureFogRanges(
        ByteBuffer buffer,
        int offset,
        Vector4f color,
        float environmentalStart,
        float environmentalEnd,
        float renderDistanceStart,
        float renderDistanceEnd,
        float skyEnd,
        float cloudEnd,
        CallbackInfo ci) {
        FogOcclusion.capture(
            environmentalStart,
            environmentalEnd,
            renderDistanceStart,
            renderDistanceEnd);
    }

    @Inject(method = "toggleFog", at = @At("RETURN"))
    private static void dx11$trackFogToggle(CallbackInfoReturnable<Boolean> cir) {
        FogOcclusion.setFogEnabled(cir.getReturnValueZ());
    }
}
