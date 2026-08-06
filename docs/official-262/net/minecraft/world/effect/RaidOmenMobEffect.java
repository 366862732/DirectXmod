/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

class RaidOmenMobEffect
extends MobEffect {
    protected RaidOmenMobEffect(MobEffectCategory category, int color, ParticleOptions particleOptions) {
        super(category, color, particleOptions);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplification) {
        return remainingDuration == 1;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (mob instanceof ServerPlayer) {
            BlockPos raidOmenPosition;
            ServerPlayer player = (ServerPlayer)mob;
            if (!mob.isSpectator() && (raidOmenPosition = player.getRaidOmenPosition()) != null) {
                level.getRaids().createOrExtendRaid(player, raidOmenPosition);
                player.clearRaidOmenPosition();
                return false;
            }
        }
        return true;
    }
}

