package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "extract", at = @At("RETURN"))
    private void onExtractParticles(ParticlesRenderState particlesRenderState, Frustum frustum, Camera camera, float partialTick, CallbackInfo ci) {
        if (D3D12Bridge.isD3D12Active()) {
            D3D12Bridge.captureParticles(particlesRenderState);
        }
    }
}
