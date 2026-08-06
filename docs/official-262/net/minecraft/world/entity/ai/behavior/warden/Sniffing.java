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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;

public class Sniffing<E extends Warden>
extends Behavior<E> {
    private static final double ANGER_FROM_SNIFFING_MAX_DISTANCE_XZ = 6.0;
    private static final double ANGER_FROM_SNIFFING_MAX_DISTANCE_Y = 20.0;

    public Sniffing(int ticks) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.IS_SNIFFING, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.NEAREST_ATTACKABLE, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.DISTURBANCE_LOCATION, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.SNIFF_COOLDOWN, (Object)((Object)MemoryStatus.REGISTERED)), ticks);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, E body, long timestamp) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, E body, long timestamp) {
        ((Entity)body).playSound(SoundEvents.WARDEN_SNIFF, 5.0f, 1.0f);
    }

    @Override
    protected void stop(ServerLevel level, E body, long timestamp) {
        if (((Entity)body).hasPose(Pose.SNIFFING)) {
            ((Entity)body).setPose(Pose.STANDING);
        }
        ((Warden)body).getBrain().eraseMemory(MemoryModuleType.IS_SNIFFING);
        ((Warden)body).getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE).filter(arg_0 -> body.canTargetEntity(arg_0)).ifPresent(entity -> {
            if (body.closerThan((Entity)entity, 6.0, 20.0)) {
                body.increaseAngerAt((Entity)entity);
            }
            if (!body.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION)) {
                WardenAi.setDisturbanceLocation(body, entity.blockPosition());
            }
        });
    }
}

