/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LandRandomPos {
    public static @Nullable Vec3 getPos(PathfinderMob mob, int horizontalDist, int verticalDist) {
        return LandRandomPos.getPos(mob, horizontalDist, verticalDist, mob::getWalkTargetValue);
    }

    public static @Nullable Vec3 getPos(PathfinderMob mob, int horizontalDist, int verticalDist, ToDoubleFunction<BlockPos> positionWeight) {
        boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
        return RandomPos.generateRandomPos(() -> {
            BlockPos direction = RandomPos.generateRandomDirection(mob.getRandom(), horizontalDist, verticalDist);
            BlockPos pos = LandRandomPos.generateRandomPosTowardDirection(mob, horizontalDist, restrict, direction);
            if (pos == null) {
                return null;
            }
            return LandRandomPos.movePosUpOutOfSolid(mob, pos);
        }, positionWeight);
    }

    public static @Nullable Vec3 getPosTowards(PathfinderMob mob, int horizontalDist, int verticalDist, Vec3 towardsPos) {
        Vec3 dir = towardsPos.subtract(mob.getX(), mob.getY(), mob.getZ());
        boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
        return LandRandomPos.getPosInDirection(mob, 0.0, horizontalDist, verticalDist, dir, restrict);
    }

    public static @Nullable Vec3 getPosAway(PathfinderMob mob, int horizontalDist, int verticalDist, Vec3 avoidPos) {
        return LandRandomPos.getPosAway(mob, 0.0, horizontalDist, verticalDist, avoidPos);
    }

    public static @Nullable Vec3 getPosAway(PathfinderMob mob, double minHorizontalDist, double maxHorizontalDist, int verticalDist, Vec3 avoidPos) {
        Vec3 dirAway = mob.position().subtract(avoidPos);
        if (dirAway.length() == 0.0) {
            dirAway = new Vec3(mob.getRandom().nextDouble() - 0.5, 0.0, mob.getRandom().nextDouble() - 0.5);
        }
        boolean restrict = GoalUtils.mobRestricted(mob, maxHorizontalDist);
        return LandRandomPos.getPosInDirection(mob, minHorizontalDist, maxHorizontalDist, verticalDist, dirAway, restrict);
    }

    private static @Nullable Vec3 getPosInDirection(PathfinderMob mob, double minHorizontalDist, double maxHorizontalDist, int verticalDist, Vec3 dir, boolean restrict) {
        return RandomPos.generateRandomPos(mob, () -> {
            BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), minHorizontalDist, maxHorizontalDist, verticalDist, 0, dir.x, dir.z, 1.5707963705062866);
            if (direction == null) {
                return null;
            }
            BlockPos pos = LandRandomPos.generateRandomPosTowardDirection(mob, maxHorizontalDist, restrict, direction);
            if (pos == null) {
                return null;
            }
            return LandRandomPos.movePosUpOutOfSolid(mob, pos);
        });
    }

    public static @Nullable BlockPos movePosUpOutOfSolid(PathfinderMob mob, BlockPos pos) {
        if (GoalUtils.isWater(mob, pos = RandomPos.moveUpOutOfSolid(pos, mob.level().getMaxY(), blockPos -> GoalUtils.isSolid(mob, blockPos))) || GoalUtils.hasMalus(mob, pos)) {
            return null;
        }
        return pos;
    }

    public static @Nullable BlockPos generateRandomPosTowardDirection(PathfinderMob mob, double horizontalDist, boolean restrict, BlockPos direction) {
        BlockPos pos = RandomPos.generateRandomPosTowardDirection(mob, horizontalDist, mob.getRandom(), direction);
        if (GoalUtils.isOutsideLimits(pos, mob) || GoalUtils.isRestricted(restrict, mob, pos) || GoalUtils.isNotStable(mob.getNavigation(), pos)) {
            return null;
        }
        return pos;
    }
}

