/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jspecify.annotations.Nullable;

public class AmphibiousNodeEvaluator
extends WalkNodeEvaluator {
    private final boolean prefersShallowSwimming;
    private float oldWalkableCost;
    private float oldWaterBorderCost;

    public AmphibiousNodeEvaluator(boolean prefersShallowSwimming) {
        this.prefersShallowSwimming = prefersShallowSwimming;
    }

    @Override
    public void prepare(PathNavigationRegion level, Mob entity) {
        super.prepare(level, entity);
        entity.setPathfindingMalus(PathType.WATER, 0.0f);
        this.oldWalkableCost = entity.getPathfindingMalus(PathType.WALKABLE);
        entity.setPathfindingMalus(PathType.WALKABLE, 6.0f);
        this.oldWaterBorderCost = entity.getPathfindingMalus(PathType.WATER_BORDER);
        entity.setPathfindingMalus(PathType.WATER_BORDER, 4.0f);
    }

    @Override
    public void done() {
        this.mob.setPathfindingMalus(PathType.WALKABLE, this.oldWalkableCost);
        this.mob.setPathfindingMalus(PathType.WATER_BORDER, this.oldWaterBorderCost);
        super.done();
    }

    @Override
    public Node getStart() {
        if (!this.mob.isInWater()) {
            return super.getStart();
        }
        return this.getStartNode(new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ)));
    }

    @Override
    public Target getTarget(double x, double y, double z) {
        return this.getTargetNodeAt(x, y + 0.5, z);
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node pos) {
        int numValidNeighbors = super.getNeighbors(neighbors, pos);
        PathType blockPathTypeAbove = this.getCachedPathType(pos.x, pos.y + 1, pos.z);
        PathType blockPathTypeCurrent = this.getCachedPathType(pos.x, pos.y, pos.z);
        int jumpSize = this.mob.getPathfindingMalus(blockPathTypeAbove) >= 0.0f && blockPathTypeCurrent != PathType.STICKY_HONEY ? Mth.floor(Math.max(1.0f, this.mob.maxUpStep())) : 0;
        double posHeight = this.getFloorLevel(new BlockPos(pos.x, pos.y, pos.z));
        Node upNode = this.findAcceptedNode(pos.x, pos.y + 1, pos.z, Math.max(0, jumpSize - 1), posHeight, Direction.UP, blockPathTypeCurrent);
        Node downNode = this.findAcceptedNode(pos.x, pos.y - 1, pos.z, jumpSize, posHeight, Direction.DOWN, blockPathTypeCurrent);
        if (this.isVerticalNeighborValid(upNode, pos)) {
            neighbors[numValidNeighbors++] = upNode;
        }
        if (this.isVerticalNeighborValid(downNode, pos) && blockPathTypeCurrent != PathType.TRAPDOOR) {
            neighbors[numValidNeighbors++] = downNode;
        }
        for (int i = 0; i < numValidNeighbors; ++i) {
            Node neighbor = neighbors[i];
            if (neighbor.type != PathType.WATER || !this.prefersShallowSwimming || neighbor.y >= this.mob.level().getSeaLevel() - 10) continue;
            neighbor.costMalus += 1.0f;
        }
        return numValidNeighbors;
    }

    private boolean isVerticalNeighborValid(@Nullable Node verticalNode, Node pos) {
        return this.isNeighborValid(verticalNode, pos) && verticalNode.type == PathType.WATER;
    }

    @Override
    protected boolean isAmphibious() {
        return true;
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        PathType blockPathType = context.getPathTypeFromState(x, y, z);
        if (blockPathType == PathType.WATER) {
            BlockPos.MutableBlockPos reusablePos = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                reusablePos.set(x, y, z).move(direction);
                PathType pathType = context.getPathTypeFromState(reusablePos.getX(), reusablePos.getY(), reusablePos.getZ());
                if (pathType != PathType.BLOCKED) continue;
                return PathType.WATER_BORDER;
            }
            return PathType.WATER;
        }
        return super.getPathType(context, x, y, z);
    }
}

