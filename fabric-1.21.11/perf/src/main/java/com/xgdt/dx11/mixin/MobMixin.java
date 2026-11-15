package com.xgdt.dx11.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xgdt.dx11.perf.MicroStats;
import com.xgdt.dx11.perf.PerfState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.breeze.Breeze;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops client-side mobs from attempting server-owned ambient sounds.
 *
 * <p>{@code Mob.baseTick} contains
 * {@code if (isAlive() && random.nextInt(1000) < ambientSoundTime++) playAmbientSound()}.
 * The sound itself is broadcast by the server, so a client-side entity runs the
 * RNG check and occasionally a client-only sound for nothing.  Short-circuiting
 * the liveness expression on the client skips both the {@code nextInt} and the
 * increment for every mob, every tick.</p>
 *
 * <p>{@link Breeze} is the one exception: it plays its ambient sound locally,
 * so it keeps the vanilla behaviour.  This is the same exception Lithium
 * applies.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(Mob.class)
public class MobMixin {
    @ModifyExpressionValue(
        method = "baseTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;isAlive()Z"))
    private boolean dx11$skipClientAmbientSound(boolean alive) {
        if (!alive) {
            return false;
        }
        if (!PerfState.layerActive() || !PerfState.config().skipClientAmbientSounds) {
            return true;
        }
        Mob self = (Mob) (Object) this;
        if (!self.level().isClientSide()) {
            return true;
        }
        if (self instanceof Breeze) {
            return true;
        }
        MicroStats.ambientSoundSkipped();
        return false;
    }
}
