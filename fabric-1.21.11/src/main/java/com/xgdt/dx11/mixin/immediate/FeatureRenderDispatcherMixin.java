package com.xgdt.dx11.mixin.immediate;

import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces a flush before the two passes that must not be reordered with the
 * entity layers accumulated so far: name tags (drawn over entities) and
 * particles (which manage their own buffers).
 */
@Environment(EnvType.CLIENT)
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin {
    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Inject(
        method = "renderAllFeatures",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/NameTagFeatureRenderer;render(Lnet/minecraft/client/renderer/SubmitNodeCollection;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/gui/Font;)V"))
    private void dx11$flushBeforeNameTags(CallbackInfo ci) {
        if (ImmediateFlags.batching()) {
            this.bufferSource.endLastBatch();
        }
    }

    @Inject(
        method = "renderAllFeatures",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ParticleFeatureRenderer;render(Lnet/minecraft/client/renderer/SubmitNodeCollection;)V"))
    private void dx11$flushBeforeParticles(CallbackInfo ci) {
        if (ImmediateFlags.batching()) {
            this.bufferSource.endLastBatch();
        }
    }
}
