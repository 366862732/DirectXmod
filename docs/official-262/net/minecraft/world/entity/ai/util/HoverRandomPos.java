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
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HoverRandomPos {
    public static @Nullable Vec3 getPos(PathfinderMob mob, int horizontalDist, int verticalDist, double xDir, double zDir, float maxXzRadiansDifference, int hoverMaxHeight, int hoverMinHeight) {
        boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
        return RandomPos.generateRandomPos(mob, () -> {
            BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), 0.0, horizontalDist, verticalDist, 0, xDir, zDir, maxXzRadiansDifference);
            if (direction == null) {
                return null;
            }
            BlockPos pos = LandRandomPos.generateRandomPosTowardDirection(mob, horizontalDist, restrict, direction);
            if (pos == null) {
                return null;
            }
            if (GoalUtils.isWater(mob, pos = RandomPos.moveUpToAboveSolid(pos, mob.getRandom().nextInt(hoverMaxHeight - hoverMinHeight + 1) + hoverMinHeight, mob.level().getMaxY(), blockPos -> GoalUtils.isSolid(mob, blockPos))) || GoalUtils.hasMalus(mob, pos)) {
                return null;
            }
            return pos;
        });
    }
}

