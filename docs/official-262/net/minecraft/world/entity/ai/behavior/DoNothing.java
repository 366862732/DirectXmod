/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.behavior;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class DoNothing
implements BehaviorControl<LivingEntity> {
    private final int minDuration;
    private final int maxDuration;
    private Behavior.Status status = Behavior.Status.STOPPED;
    private long endTimestamp;

    public DoNothing(int minDuration, int maxDuration) {
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    @Override
    public Behavior.Status getStatus() {
        return this.status;
    }

    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        return Set.of();
    }

    @Override
    public final boolean tryStart(ServerLevel level, LivingEntity body, long timestamp) {
        this.status = Behavior.Status.RUNNING;
        int duration = this.minDuration + level.getRandom().nextInt(this.maxDuration + 1 - this.minDuration);
        this.endTimestamp = timestamp + (long)duration;
        return true;
    }

    @Override
    public final void tickOrStop(ServerLevel level, LivingEntity body, long timestamp) {
        if (timestamp > this.endTimestamp) {
            this.doStop(level, body, timestamp);
        }
    }

    @Override
    public final void doStop(ServerLevel level, LivingEntity body, long timestamp) {
        this.status = Behavior.Status.STOPPED;
    }

    @Override
    public String debugString() {
        return this.getClass().getSimpleName();
    }
}

