/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Target;
import org.jspecify.annotations.Nullable;

public class SwimNodeEvaluator
extends NodeEvaluator {
    private final boolean allowBreaching;
    private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap();

    public SwimNodeEvaluator(boolean allowBreaching) {
        this.allowBreaching = allowBreaching;
    }

    @Override
    public void prepare(PathNavigationRegion level, Mob entity) {
        super.prepare(level, entity);
        this.pathTypesByPosCache.clear();
    }

    @Override
    public void done() {
        super.done();
        this.pathTypesByPosCache.clear();
    }

    @Override
    public Node getStart() {
        return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ));
    }

    @Override
    public Target getTarget(double x, double y, double z) {
        return this.getTargetNodeAt(x, y, z);
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node pos) {
        int count = 0;
        EnumMap nodes = Maps.newEnumMap(Direction.class);
        for (Direction direction : Direction.values()) {
            Node node = this.findAcceptedNode(pos.x + direction.getStepX(), pos.y + direction.getStepY(), pos.z + direction.getStepZ());
            nodes.put(direction, node);
            if (!this.isNodeValid(node)) continue;
            neighbors[count++] = node;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Node diagonalNode;
            Direction secondDirection = direction.getClockWise();
            if (!SwimNodeEvaluator.hasMalus((Node)nodes.get(direction)) || !SwimNodeEvaluator.hasMalus((Node)nodes.get(secondDirection)) || !this.isNodeValid(diagonalNode = this.findAcceptedNode(pos.x + direction.getStepX() + secondDirection.getStepX(), pos.y, pos.z + direction.getStepZ() + secondDirection.getStepZ()))) continue;
            neighbors[count++] = diagonalNode;
        }
        return count;
    }

    protected boolean isNodeValid(@Nullable Node node) {
        return node != null && !node.closed;
    }

    private static boolean hasMalus(@Nullable Node node) {
        return node != null && node.costMalus >= 0.0f;
    }

    protected @Nullable Node findAcceptedNode(int x, int y, int z) {
        float pathCost;
        Node best = null;
        PathType pathType = this.getCachedBlockType(x, y, z);
        if ((this.allowBreaching && pathType == PathType.BREACH || pathType == PathType.WATER) && (pathCost = this.mob.getPathfindingMalus(pathType)) >= 0.0f) {
            best = this.getNode(x, y, z);
            best.type = pathType;
            best.costMalus = Math.max(best.costMalus, pathCost);
            if (this.currentContext.level().getFluidState(new BlockPos(x, y, z)).isEmpty()) {
                best.costMalus += 8.0f;
            }
        }
        return best;
    }

    protected PathType getCachedBlockType(int x, int y, int z) {
        return (PathType)((Object)this.pathTypesByPosCache.computeIfAbsent(BlockPos.asLong(x, y, z), k -> this.getPathType(this.currentContext, x, y, z)));
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        return this.getPathTypeOfMob(context, x, y, z, this.mob);
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = x; xx < x + this.entityWidth; ++xx) {
            for (int yy = y; yy < y + this.entityHeight; ++yy) {
                for (int zz = z; zz < z + this.entityDepth; ++zz) {
                    BlockState blockState = context.getBlockState(pos.set(xx, yy, zz));
                    FluidState fluidState = blockState.getFluidState();
                    BlockState belowState = context.getBlockState((BlockPos)pos.below());
                    if (fluidState.isEmpty() && belowState.isPathfindable(PathComputationType.WATER) && blockState.isAir()) {
                        return PathType.BREACH;
                    }
                    if (fluidState.is(FluidTags.WATER)) continue;
                    return PathType.BLOCKED;
                }
            }
        }
        BlockState blockState = context.getBlockState(pos);
        if (blockState.isPathfindable(PathComputationType.WATER)) {
            return PathType.WATER;
        }
        return PathType.BLOCKED;
    }
}

