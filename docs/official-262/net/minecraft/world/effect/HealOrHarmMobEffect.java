/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

class HealOrHarmMobEffect
extends InstantaneousMobEffect {
    private final boolean isHarm;

    public HealOrHarmMobEffect(MobEffectCategory category, int color, boolean isHarm) {
        super(category, color);
        this.isHarm = isHarm;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (this.isHarm == mob.isInvertedHealAndHarm()) {
            mob.heal(Math.max(4 << amplification, 0));
        } else {
            mob.hurtServer(level, mob.damageSources().magic(), 6 << amplification);
        }
        return true;
    }

    @Override
    public void applyInstantaneousEffect(ServerLevel serverLevel, @Nullable Entity source, @Nullable Entity owner, LivingEntity mob, int amplification, double scale) {
        if (this.isHarm == mob.isInvertedHealAndHarm()) {
            int amount = (int)(scale * (double)(4 << amplification) + 0.5);
            mob.heal(amount);
        } else {
            int amount = (int)(scale * (double)(6 << amplification) + 0.5);
            if (source == null) {
                mob.hurtServer(serverLevel, mob.damageSources().magic(), amount);
            } else {
                mob.hurtServer(serverLevel, mob.damageSources().indirectMagic(source, owner), amount);
            }
        }
    }
}

