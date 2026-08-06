/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

class BadOmenMobEffect
extends MobEffect {
    protected BadOmenMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplification) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        Raid raid;
        ServerPlayer player;
        if (mob instanceof ServerPlayer && !(player = (ServerPlayer)mob).isSpectator() && level.getDifficulty() != Difficulty.PEACEFUL && level.isVillage(player.blockPosition()) && ((raid = level.getRaidAt(player.blockPosition())) == null || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel())) {
            player.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, amplification));
            player.setRaidOmenPosition(player.blockPosition());
            return false;
        }
        return true;
    }
}

