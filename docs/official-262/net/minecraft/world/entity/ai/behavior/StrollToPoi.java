/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  org.apache.commons.lang3.mutable.MutableLong
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoi {
    public static BehaviorControl<PathfinderMob> create(MemoryModuleType<GlobalPos> memoryType, float speedModifier, int closeEnoughDist, int maxDistanceFromPoi) {
        MutableLong nextOkStartTime = new MutableLong(0L);
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.present(memoryType)).apply((Applicative)i, (walkTarget, memory) -> (level, body, timestamp) -> {
            GlobalPos pos = (GlobalPos)i.get(memory);
            if (level.dimension() != pos.dimension() || !pos.pos().closerToCenterThan(body.position(), maxDistanceFromPoi)) {
                return false;
            }
            if (timestamp <= nextOkStartTime.longValue()) {
                return true;
            }
            walkTarget.set(new WalkTarget(pos.pos(), speedModifier, closeEnoughDist));
            nextOkStartTime.setValue(timestamp + 80L);
            return true;
        }));
    }
}

