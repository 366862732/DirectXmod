/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 */
package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.NearestVisibleLivingEntitySensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.animal.frog.Frog;

public class FrogAttackablesSensor
extends NearestVisibleLivingEntitySensor {
    public static final float TARGET_DETECTION_DISTANCE = 10.0f;

    @Override
    protected boolean isMatchingEntity(ServerLevel level, LivingEntity body, LivingEntity mob) {
        if (Sensor.isEntityAttackable(level, body, mob) && Frog.canEat(mob) && !this.isUnreachableAttackTarget(body, mob)) {
            return mob.closerThan(body, 10.0);
        }
        return false;
    }

    private boolean isUnreachableAttackTarget(LivingEntity body, LivingEntity mob) {
        List unreachableAttackTargets = body.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseGet(ArrayList::new);
        return unreachableAttackTargets.contains(mob.getUUID());
    }

    @Override
    protected MemoryModuleType<LivingEntity> getMemoryToSet() {
        return MemoryModuleType.NEAREST_ATTACKABLE;
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return Sets.union(super.requires(), Set.of(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS));
    }
}

