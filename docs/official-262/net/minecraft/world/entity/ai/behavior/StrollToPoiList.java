/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  org.apache.commons.lang3.mutable.MutableLong
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoiList {
    public static BehaviorControl<Villager> create(MemoryModuleType<List<GlobalPos>> strollToMemoryType, float speedModifier, int closeEnoughDist, int maxDistanceFromPoi, MemoryModuleType<GlobalPos> mustBeCloseToMemoryType) {
        MutableLong nextOkStartTime = new MutableLong(0L);
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.present(strollToMemoryType), i.present(mustBeCloseToMemoryType)).apply((Applicative)i, (walkTarget, strollToMemory, mustBeCloseToMemory) -> (level, body, timestamp) -> {
            List strollTo = (List)i.get(strollToMemory);
            GlobalPos stayCloseTo = (GlobalPos)i.get(mustBeCloseToMemory);
            if (strollTo.isEmpty()) {
                return false;
            }
            GlobalPos targetPos = (GlobalPos)strollTo.get(level.getRandom().nextInt(strollTo.size()));
            if (targetPos == null || level.dimension() != targetPos.dimension() || !stayCloseTo.pos().closerToCenterThan(body.position(), maxDistanceFromPoi)) {
                return false;
            }
            if (timestamp > nextOkStartTime.longValue()) {
                walkTarget.set(new WalkTarget(targetPos.pos(), speedModifier, closeEnoughDist));
                nextOkStartTime.setValue(timestamp + 100L);
            }
            return true;
        }));
    }
}

