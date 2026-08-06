/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetFromBlockMemory {
    public static OneShot<Villager> create(MemoryModuleType<GlobalPos> memoryType, float speedModifier, int closeEnoughDist, int tooFarDistance, int tooLongUnreachableDuration) {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE), i.absent(MemoryModuleType.WALK_TARGET), i.present(memoryType)).apply((Applicative)i, (cantReachSince, walkTarget, memory) -> (level, body, timestamp) -> {
            GlobalPos targetPos = (GlobalPos)i.get(memory);
            Optional cantReachTargetSince = i.tryGet(cantReachSince);
            if (targetPos.dimension() != level.dimension() || cantReachTargetSince.isPresent() && level.getGameTime() - (Long)cantReachTargetSince.get() > (long)tooLongUnreachableDuration) {
                body.releasePoi(memoryType);
                memory.erase();
                cantReachSince.set(timestamp);
            } else if (targetPos.pos().distManhattan(body.blockPosition()) > tooFarDistance) {
                Vec3 towardsTargetPos = null;
                int tries = 0;
                int MAX_TRIES = 1000;
                while (towardsTargetPos == null || BlockPos.containing(towardsTargetPos).distManhattan(body.blockPosition()) > tooFarDistance) {
                    towardsTargetPos = DefaultRandomPos.getPosTowards(body, 15, 7, Vec3.atBottomCenterOf(targetPos.pos()), 1.5707963705062866);
                    if (++tries != 1000) continue;
                    body.releasePoi(memoryType);
                    memory.erase();
                    cantReachSince.set(timestamp);
                    return true;
                }
                walkTarget.set(new WalkTarget(towardsTargetPos, speedModifier, closeEnoughDist));
            } else if (targetPos.pos().distManhattan(body.blockPosition()) > closeEnoughDist) {
                walkTarget.set(new WalkTarget(targetPos.pos(), speedModifier, closeEnoughDist));
            }
            return true;
        }));
    }
}

