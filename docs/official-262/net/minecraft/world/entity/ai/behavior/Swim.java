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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class Swim<T extends Mob>
extends Behavior<T> {
    private final float chance;

    public Swim(float chance) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of());
        this.chance = chance;
    }

    public static <T extends Mob> boolean shouldSwim(T mob) {
        return mob.isInWater() && mob.getFluidHeight(FluidTags.WATER) > mob.getFluidJumpThreshold() || mob.isInLava();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Mob body) {
        return Swim.shouldSwim(body);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob body, long timestamp) {
        return this.checkExtraStartConditions(level, body);
    }

    @Override
    protected void tick(ServerLevel level, Mob body, long timestamp) {
        if (body.getRandom().nextFloat() < this.chance) {
            body.getJumpControl().jump();
        }
    }
}

