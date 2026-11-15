package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.xgdt.dx11.perf.immediate.BatchedBufferSource;
import java.util.SequencedMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Swaps the entity/feature buffer source for the deferred batched one.  The
 * crumbling and outline sources stay vanilla: they are small, already batched
 * per render type, and must remain separate for correctness.
 */
@Environment(EnvType.CLIENT)
@Mixin(RenderBuffers.class)
public abstract class RenderBuffersMixin {
    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;immediateWithBuffers(Ljava/util/SequencedMap;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;)Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
    private static MultiBufferSource.BufferSource dx11$batchEntityBuffers(
        SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers,
        ByteBufferBuilder sharedBuffer,
        Operation<MultiBufferSource.BufferSource> original) {
        // Installed unconditionally so a runtime toggle can start batching in
        // the existing source; the feature flag only controls the fast paths.
        return new BatchedBufferSource(sharedBuffer, fixedBuffers);
    }
}
