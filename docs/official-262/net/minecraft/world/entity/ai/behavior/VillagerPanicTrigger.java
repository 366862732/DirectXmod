/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

public class VillagerPanicTrigger
extends Behavior<Villager> {
    public VillagerPanicTrigger() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return VillagerPanicTrigger.isHurt(body) || VillagerPanicTrigger.hasHostile(body);
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        if (VillagerPanicTrigger.isHurt(body) || VillagerPanicTrigger.hasHostile(body)) {
            Brain<Villager> brain = body.getBrain();
            if (!brain.isActive(Activity.PANIC)) {
                brain.eraseMemory(MemoryModuleType.PATH);
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
                brain.eraseMemory(MemoryModuleType.BREED_TARGET);
                brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
            }
            brain.setActiveActivityIfPossible(Activity.PANIC);
        }
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        if (timestamp % 100L == 0L) {
            body.spawnGolemIfNeeded(level, timestamp, 3);
        }
    }

    public static boolean hasHostile(LivingEntity myBody) {
        return myBody.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
    }

    public static boolean isHurt(LivingEntity myBody) {
        return myBody.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }
}

