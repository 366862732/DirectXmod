/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.behavior;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public interface BehaviorControl<E extends LivingEntity> {
    public Behavior.Status getStatus();

    public Set<MemoryModuleType<?>> getRequiredMemories();

    public boolean tryStart(ServerLevel var1, E var2, long var3);

    public void tickOrStop(ServerLevel var1, E var2, long var3);

    public void doStop(ServerLevel var1, E var2, long var3);

    public String debugString();
}

