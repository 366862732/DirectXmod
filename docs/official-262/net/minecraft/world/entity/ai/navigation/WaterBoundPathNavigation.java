/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class WaterBoundPathNavigation
extends PathNavigation {
    private boolean allowBreaching;

    public WaterBoundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.allowBreaching = this.mob.is(EntityTypes.DOLPHIN);
        this.nodeEvaluator = new SwimNodeEvaluator(this.allowBreaching);
        this.nodeEvaluator.setCanPassDoors(false);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.allowBreaching || this.mob.isInLiquid();
    }

    @Override
    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.mob.getY(0.5), this.mob.getZ());
    }

    @Override
    protected double getGroundY(Vec3 target) {
        return target.y;
    }

    @Override
    protected boolean canMoveDirectly(Vec3 startPos, Vec3 stopPos) {
        return WaterBoundPathNavigation.isClearForMovementBetween(this.mob, startPos, stopPos, false);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        return !this.level.getBlockState(pos).isSolidRender();
    }

    @Override
    public void setCanFloat(boolean canFloat) {
    }

    @Override
    public boolean canNavigateGround() {
        return false;
    }

    @Override
    public float getMaxVerticalDistanceToWaypoint() {
        return 0.5f;
    }
}

