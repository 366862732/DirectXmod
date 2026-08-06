/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public interface ServerLevelAccessor
extends LevelAccessor {
    public ServerLevel getLevel();

    public DifficultyInstance getCurrentDifficultyAt(BlockPos var1);

    default public void addFreshEntityWithPassengers(Entity entity) {
        entity.getSelfAndPassengers().forEach(this::addFreshEntity);
    }
}

