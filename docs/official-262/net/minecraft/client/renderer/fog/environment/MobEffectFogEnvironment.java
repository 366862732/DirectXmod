/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.fog.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class MobEffectFogEnvironment
extends FogEnvironment {
    public abstract Holder<MobEffect> getMobEffect();

    @Override
    public boolean providesColor() {
        return false;
    }

    @Override
    public boolean modifiesDarkness() {
        return true;
    }

    @Override
    public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
        LivingEntity livingEntity;
        return entity instanceof LivingEntity && (livingEntity = (LivingEntity)entity).hasEffect(this.getMobEffect());
    }
}

