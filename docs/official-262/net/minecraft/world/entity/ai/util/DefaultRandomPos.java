/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DefaultRandomPos {
    public static @Nullable Vec3 getPos(PathfinderMob mob, int horizontalDist, int verticalDist) {
        boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
        return RandomPos.generateRandomPos(mob, () -> {
            BlockPos direction = RandomPos.generateRandomDirection(mob.getRandom(), horizontalDist, verticalDist);
            return DefaultRandomPos.generateRandomPosTowardDirection(mob, horizontalDist, restrict, direction);
        });
    }

    public static @Nullable Vec3 getPosTowards(PathfinderMob mob, int horizontalDist, int verticalDist, Vec3 towardsPos, double maxXzRadiansFromDir) {
        Vec3 dir = towardsPos.subtract(mob.getX(), mob.getY(), mob.getZ());
        boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
        return RandomPos.generateRandomPos(mob, () -> {
            BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), 0.0, horizontalDist, verticalDist, 0, dir.x, dir.z, maxXzRadiansFromDir);
            if (direction == null) {
                return null;
            }
            return DefaultRandomPos.generateRandomPosTowardDirection(mob, horizontalDist, restrict, direction);
        });
    }

    public static @Nullable Vec3 getPosAway(PathfinderMob mob, int horizontalDist, int verticalDist, Vec3 avoidPos) {
        Vec3 dirAway = mob.position().subtract(avoidPos);
        boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
        return RandomPos.generateRandomPos(mob, () -> {
            BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), 0.0, horizontalDist, verticalDist, 0, dirAway.x, dirAway.z, 1.5707963705062866);
            if (direction == null) {
                return null;
            }
            return DefaultRandomPos.generateRandomPosTowardDirection(mob, horizontalDist, restrict, direction);
        });
    }

    private static @Nullable BlockPos generateRandomPosTowardDirection(PathfinderMob mob, int horizontalDist, boolean restrict, BlockPos direction) {
        BlockPos pos = RandomPos.generateRandomPosTowardDirection(mob, horizontalDist, mob.getRandom(), direction);
        if (GoalUtils.isOutsideLimits(pos, mob) || GoalUtils.isRestricted(restrict, mob, pos) || GoalUtils.isNotStable(mob.getNavigation(), pos) || GoalUtils.hasMalus(mob, pos)) {
            return null;
        }
        return pos;
    }
}

