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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class LookAtTargetSink
extends Behavior<Mob> {
    public LookAtTargetSink(int minDuration, int maxDuration) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT)), minDuration, maxDuration);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob body, long timestamp) {
        return body.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).filter(pos -> pos.isVisibleBy(body)).isPresent();
    }

    @Override
    protected void stop(ServerLevel level, Mob body, long timestamp) {
        body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, Mob body, long timestamp) {
        body.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent(target -> body.getLookControl().setLookAt(target.currentPosition()));
    }
}

