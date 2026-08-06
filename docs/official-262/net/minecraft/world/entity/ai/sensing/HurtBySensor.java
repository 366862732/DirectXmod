/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 */
package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

public class HurtBySensor
extends Sensor<LivingEntity> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
    }

    @Override
    protected void doTick(ServerLevel level, LivingEntity body) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        DamageSource damageSource = body.getLastDamageSource();
        if (damageSource != null) {
            brain.setMemory(MemoryModuleType.HURT_BY, body.getLastDamageSource());
            Entity entitySource = damageSource.getEntity();
            if (entitySource instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entitySource;
                brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, livingEntity);
            }
        } else {
            brain.eraseMemory(MemoryModuleType.HURT_BY);
        }
        brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(hurtByEntity -> {
            if (!hurtByEntity.isAlive() || hurtByEntity.level() != level) {
                brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            }
        });
    }
}

