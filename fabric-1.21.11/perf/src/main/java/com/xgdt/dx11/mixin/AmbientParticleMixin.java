package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.AmbientParticleChance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.attribute.AmbientParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds every loaded {@link AmbientParticle} probability into
 * {@link AmbientParticleChance}.
 *
 * <p>The constructor is the only place a probability enters the game: biome
 * JSON is decoded into these records during datapack load.  Tracking the
 * maximum here lets {@code ClientLevelAnimateTickMixin} skip the expensive
 * per-position attribute lookup with a single random draw.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(AmbientParticle.class)
public class AmbientParticleMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void dx11$trackProbability(
        ParticleOptions particle, float probability, CallbackInfo ci) {
        AmbientParticleChance.track(probability);
    }
}
