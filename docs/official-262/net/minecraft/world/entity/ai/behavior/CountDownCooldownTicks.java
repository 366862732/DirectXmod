/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class CountDownCooldownTicks
extends Behavior<LivingEntity> {
    private final MemoryModuleType<Integer> cooldownTicks;

    public CountDownCooldownTicks(MemoryModuleType<Integer> cooldownTicks) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(cooldownTicks, (Object)((Object)MemoryStatus.VALUE_PRESENT)));
        this.cooldownTicks = cooldownTicks;
    }

    private Optional<Integer> getCooldownTickMemory(LivingEntity body) {
        return body.getBrain().getMemory(this.cooldownTicks);
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LivingEntity body, long timestamp) {
        Optional<Integer> calmDownTicks = this.getCooldownTickMemory(body);
        return calmDownTicks.isPresent() && calmDownTicks.get() > 0;
    }

    @Override
    protected void tick(ServerLevel level, LivingEntity body, long timestamp) {
        Optional<Integer> calmDownTicks = this.getCooldownTickMemory(body);
        body.getBrain().setMemory(this.cooldownTicks, calmDownTicks.get() - 1);
    }

    @Override
    protected void stop(ServerLevel level, LivingEntity body, long timestamp) {
        body.getBrain().eraseMemory(this.cooldownTicks);
    }
}

