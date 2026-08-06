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

public class AirAndWaterRandomPos {
    public static @Nullable Vec3 getPos(PathfinderMob mob, int horizontalDist, int verticalDist, int flyingHeight, double xDir, double zDir, double maxXzRadiansDifference) {
        boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
        return RandomPos.generateRandomPos(mob, () -> AirAndWaterRandomPos.generateRandomPos(mob, horizontalDist, verticalDist, flyingHeight, xDir, zDir, maxXzRadiansDifference, restrict));
    }

    public static @Nullable BlockPos generateRandomPos(PathfinderMob mob, int horizontalDist, int verticalDist, int flyingHeight, double xDir, double zDir, double maxXzRadiansDifference, boolean restrict) {
        BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), 0.0, horizontalDist, verticalDist, flyingHeight, xDir, zDir, maxXzRadiansDifference);
        if (direction == null) {
            return null;
        }
        BlockPos pos = RandomPos.generateRandomPosTowardDirection(mob, horizontalDist, mob.getRandom(), direction);
        if (GoalUtils.isOutsideLimits(pos, mob) || GoalUtils.isRestricted(restrict, mob, pos)) {
            return null;
        }
        if (GoalUtils.hasMalus(mob, pos = RandomPos.moveUpOutOfSolid(pos, mob.level().getMaxY(), blockPos -> GoalUtils.isSolid(mob, blockPos)))) {
            return null;
        }
        return pos;
    }
}

