/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetAwayFrom {
    public static BehaviorControl<PathfinderMob> pos(MemoryModuleType<BlockPos> memory, float speedModifier, int desiredDistance, boolean interruptCurrentWalk) {
        return SetWalkTargetAwayFrom.create(memory, speedModifier, desiredDistance, interruptCurrentWalk, Vec3::atBottomCenterOf);
    }

    public static OneShot<PathfinderMob> entity(MemoryModuleType<? extends Entity> memory, float speedModifier, int desiredDistance, boolean interruptCurrentWalk) {
        return SetWalkTargetAwayFrom.create(memory, speedModifier, desiredDistance, interruptCurrentWalk, Entity::position);
    }

    private static <T> OneShot<PathfinderMob> create(MemoryModuleType<T> walkAwayFromMemory, float speedModifier, int desiredDistance, boolean interruptCurrentWalk, Function<T, Vec3> toPosition) {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.present(walkAwayFromMemory)).apply((Applicative)i, (walkTarget, walkAwayFrom) -> (level, body, timestamp) -> {
            Vec3 avoidDirection;
            Vec3 currentDirection;
            Vec3 avoidPosition;
            Optional target = i.tryGet(walkTarget);
            if (target.isPresent() && !interruptCurrentWalk) {
                return false;
            }
            Vec3 bodyPosition = body.position();
            if (!bodyPosition.closerThan(avoidPosition = (Vec3)toPosition.apply(i.get(walkAwayFrom)), desiredDistance)) {
                return false;
            }
            if (target.isPresent() && ((WalkTarget)target.get()).getSpeedModifier() == speedModifier && (currentDirection = ((WalkTarget)target.get()).getTarget().currentPosition().subtract(bodyPosition)).dot(avoidDirection = avoidPosition.subtract(bodyPosition)) < 0.0) {
                return false;
            }
            for (int j = 0; j < 10; ++j) {
                Vec3 fleeToPos = LandRandomPos.getPosAway(body, 16, 7, avoidPosition);
                if (fleeToPos == null) continue;
                walkTarget.set(new WalkTarget(fleeToPos, speedModifier, 0));
                break;
            }
            return true;
        }));
    }
}

