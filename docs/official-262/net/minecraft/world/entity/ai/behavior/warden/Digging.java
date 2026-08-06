/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;

public class Digging<E extends Warden>
extends Behavior<E> {
    public Digging(int ticks) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), ticks);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, E body, long timestamp) {
        return ((Entity)body).getRemovalReason() == null;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E body) {
        return ((Entity)body).onGround() || ((Entity)body).isInWater() || ((Entity)body).isInLava();
    }

    @Override
    protected void start(ServerLevel level, E body, long timestamp) {
        if (((Entity)body).onGround()) {
            ((Entity)body).setPose(Pose.DIGGING);
            ((Entity)body).playSound(SoundEvents.WARDEN_DIG, 5.0f, 1.0f);
        } else {
            ((Entity)body).playSound(SoundEvents.WARDEN_AGITATED, 5.0f, 1.0f);
            this.stop(level, body, timestamp);
        }
    }

    @Override
    protected void stop(ServerLevel level, E body, long timestamp) {
        if (((Entity)body).getRemovalReason() == null) {
            ((LivingEntity)body).remove(Entity.RemovalReason.DISCARDED);
        }
    }
}

