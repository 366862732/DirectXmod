/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class InstantaneousMobEffect
extends MobEffect {
    public InstantaneousMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean isInstantaneous() {
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplification) {
        return remainingDuration >= 1;
    }
}

