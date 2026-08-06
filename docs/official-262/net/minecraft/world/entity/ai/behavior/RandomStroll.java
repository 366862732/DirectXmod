/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomStroll {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;
    private static final int[][] SWIM_XY_DISTANCE_TIERS = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {10, 7}};

    public static OneShot<PathfinderMob> stroll(float speedModifier) {
        return RandomStroll.stroll(speedModifier, true);
    }

    public static OneShot<PathfinderMob> stroll(float speedModifier, boolean mayStrollFromWater) {
        return RandomStroll.strollFlyOrSwim(speedModifier, body -> LandRandomPos.getPos(body, 10, 7), mayStrollFromWater ? b -> true : b -> !b.isInWater());
    }

    public static BehaviorControl<PathfinderMob> stroll(float speedModifier, int maxHorizontalDistance, int maxVerticalDistance) {
        return RandomStroll.strollFlyOrSwim(speedModifier, body -> LandRandomPos.getPos(body, maxHorizontalDistance, maxVerticalDistance), b -> true);
    }

    public static BehaviorControl<PathfinderMob> fly(float speedModifier) {
        return RandomStroll.strollFlyOrSwim(speedModifier, body -> RandomStroll.getTargetFlyPos(body, 10, 7), b -> true);
    }

    public static BehaviorControl<PathfinderMob> swim(float speedModifier) {
        return RandomStroll.strollFlyOrSwim(speedModifier, RandomStroll::getTargetSwimPos, Entity::isInWater);
    }

    private static OneShot<PathfinderMob> strollFlyOrSwim(float speedModifier, Function<PathfinderMob, Vec3> fetchTargetPos, Predicate<PathfinderMob> canRun) {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET)).apply((Applicative)i, walkTarget -> (level, body, timestamp) -> {
            if (!canRun.test((PathfinderMob)body)) {
                return false;
            }
            Optional<Vec3> pathGoalPos = Optional.ofNullable((Vec3)fetchTargetPos.apply((PathfinderMob)body));
            walkTarget.setOrErase(pathGoalPos.map(pos -> new WalkTarget((Vec3)pos, speedModifier, 0)));
            return true;
        }));
    }

    private static @Nullable Vec3 getTargetSwimPos(PathfinderMob body) {
        Vec3 fallback = null;
        Vec3 targetPos = null;
        for (int[] distance : SWIM_XY_DISTANCE_TIERS) {
            targetPos = fallback == null ? BehaviorUtils.getRandomSwimmablePos(body, distance[0], distance[1]) : body.position().add(body.position().vectorTo(fallback).normalize().multiply(distance[0], distance[1], distance[0]));
            boolean restrict = GoalUtils.mobRestricted(body, distance[0]);
            if (targetPos == null || body.level().getFluidState(BlockPos.containing(targetPos)).isEmpty() || GoalUtils.isRestricted(restrict, body, targetPos)) {
                return fallback;
            }
            fallback = targetPos;
        }
        return targetPos;
    }

    private static @Nullable Vec3 getTargetFlyPos(PathfinderMob body, int maxHorizontalDistance, int maxVerticalDistance) {
        Vec3 wanderDirection = body.getViewVector(0.0f);
        return AirAndWaterRandomPos.getPos(body, maxHorizontalDistance, maxVerticalDistance, -2, wanderDirection.x, wanderDirection.z, 1.5707963705062866);
    }
}

