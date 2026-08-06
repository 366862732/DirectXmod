/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class FlyNodeEvaluator
extends WalkNodeEvaluator {
    private final Long2ObjectMap<PathType> pathTypeByPosCache = new Long2ObjectOpenHashMap();
    private static final float SMALL_MOB_SIZE = 1.0f;
    private static final float SMALL_MOB_INFLATED_START_NODE_BOUNDING_BOX = 1.1f;
    private static final int MAX_START_NODE_CANDIDATES = 10;

    @Override
    public void prepare(PathNavigationRegion level, Mob entity) {
        super.prepare(level, entity);
        this.pathTypeByPosCache.clear();
        entity.onPathfindingStart();
    }

    @Override
    public void done() {
        this.mob.onPathfindingDone();
        this.pathTypeByPosCache.clear();
        super.done();
    }

    @Override
    public Node getStart() {
        BlockPos startPos;
        int startY;
        if (this.canFloat() && this.mob.isInWater()) {
            startY = this.mob.getBlockY();
            BlockPos.MutableBlockPos reusableBlockPos = new BlockPos.MutableBlockPos(this.mob.getX(), (double)startY, this.mob.getZ());
            BlockState state = this.currentContext.getBlockState(reusableBlockPos);
            while (state.is(Blocks.WATER)) {
                reusableBlockPos.set(this.mob.getX(), (double)(++startY), this.mob.getZ());
                state = this.currentContext.getBlockState(reusableBlockPos);
            }
        } else {
            startY = Mth.floor(this.mob.getY() + 0.5);
        }
        if (!this.canStartAt(startPos = BlockPos.containing(this.mob.getX(), startY, this.mob.getZ()))) {
            for (BlockPos testedPosition : this.iteratePathfindingStartNodeCandidatePositions(this.mob)) {
                if (!this.canStartAt(testedPosition)) continue;
                return super.getStartNode(testedPosition);
            }
        }
        return super.getStartNode(startPos);
    }

    @Override
    protected boolean canStartAt(BlockPos pos) {
        PathType blockPathType = this.getCachedPathType(pos.getX(), pos.getY(), pos.getZ());
        return this.mob.getPathfindingMalus(blockPathType) >= 0.0f;
    }

    @Override
    public Target getTarget(double x, double y, double z) {
        return this.getTargetNodeAt(x, y, z);
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node pos) {
        Node southWestDown;
        Node northWestDown;
        Node southEastDown;
        Node northEastDown;
        Node southWestUp;
        Node northWestUp;
        Node southEastUp;
        Node northEastUp;
        Node southWest;
        Node northWest;
        Node southEast;
        Node northEast;
        Node northDown;
        Node eastDown;
        Node westDown;
        Node southDown;
        Node northUp;
        Node eastUp;
        Node westUp;
        Node southUp;
        Node down;
        Node up;
        Node north;
        Node east;
        Node west;
        int count = 0;
        Node south = this.findAcceptedNode(pos.x, pos.y, pos.z + 1);
        if (this.isOpen(south)) {
            neighbors[count++] = south;
        }
        if (this.isOpen(west = this.findAcceptedNode(pos.x - 1, pos.y, pos.z))) {
            neighbors[count++] = west;
        }
        if (this.isOpen(east = this.findAcceptedNode(pos.x + 1, pos.y, pos.z))) {
            neighbors[count++] = east;
        }
        if (this.isOpen(north = this.findAcceptedNode(pos.x, pos.y, pos.z - 1))) {
            neighbors[count++] = north;
        }
        if (this.isOpen(up = this.findAcceptedNode(pos.x, pos.y + 1, pos.z))) {
            neighbors[count++] = up;
        }
        if (this.isOpen(down = this.findAcceptedNode(pos.x, pos.y - 1, pos.z))) {
            neighbors[count++] = down;
        }
        if (this.isOpen(southUp = this.findAcceptedNode(pos.x, pos.y + 1, pos.z + 1)) && this.hasMalus(south) && this.hasMalus(up)) {
            neighbors[count++] = southUp;
        }
        if (this.isOpen(westUp = this.findAcceptedNode(pos.x - 1, pos.y + 1, pos.z)) && this.hasMalus(west) && this.hasMalus(up)) {
            neighbors[count++] = westUp;
        }
        if (this.isOpen(eastUp = this.findAcceptedNode(pos.x + 1, pos.y + 1, pos.z)) && this.hasMalus(east) && this.hasMalus(up)) {
            neighbors[count++] = eastUp;
        }
        if (this.isOpen(northUp = this.findAcceptedNode(pos.x, pos.y + 1, pos.z - 1)) && this.hasMalus(north) && this.hasMalus(up)) {
            neighbors[count++] = northUp;
        }
        if (this.isOpen(southDown = this.findAcceptedNode(pos.x, pos.y - 1, pos.z + 1)) && this.hasMalus(south) && this.hasMalus(down)) {
            neighbors[count++] = southDown;
        }
        if (this.isOpen(westDown = this.findAcceptedNode(pos.x - 1, pos.y - 1, pos.z)) && this.hasMalus(west) && this.hasMalus(down)) {
            neighbors[count++] = westDown;
        }
        if (this.isOpen(eastDown = this.findAcceptedNode(pos.x + 1, pos.y - 1, pos.z)) && this.hasMalus(east) && this.hasMalus(down)) {
            neighbors[count++] = eastDown;
        }
        if (this.isOpen(northDown = this.findAcceptedNode(pos.x, pos.y - 1, pos.z - 1)) && this.hasMalus(north) && this.hasMalus(down)) {
            neighbors[count++] = northDown;
        }
        if (this.isOpen(northEast = this.findAcceptedNode(pos.x + 1, pos.y, pos.z - 1)) && this.hasMalus(north) && this.hasMalus(east)) {
            neighbors[count++] = northEast;
        }
        if (this.isOpen(southEast = this.findAcceptedNode(pos.x + 1, pos.y, pos.z + 1)) && this.hasMalus(south) && this.hasMalus(east)) {
            neighbors[count++] = southEast;
        }
        if (this.isOpen(northWest = this.findAcceptedNode(pos.x - 1, pos.y, pos.z - 1)) && this.hasMalus(north) && this.hasMalus(west)) {
            neighbors[count++] = northWest;
        }
        if (this.isOpen(southWest = this.findAcceptedNode(pos.x - 1, pos.y, pos.z + 1)) && this.hasMalus(south) && this.hasMalus(west)) {
            neighbors[count++] = southWest;
        }
        if (this.isOpen(northEastUp = this.findAcceptedNode(pos.x + 1, pos.y + 1, pos.z - 1)) && this.hasMalus(northEast) && this.hasMalus(north) && this.hasMalus(east) && this.hasMalus(up) && this.hasMalus(northUp) && this.hasMalus(eastUp)) {
            neighbors[count++] = northEastUp;
        }
        if (this.isOpen(southEastUp = this.findAcceptedNode(pos.x + 1, pos.y + 1, pos.z + 1)) && this.hasMalus(southEast) && this.hasMalus(south) && this.hasMalus(east) && this.hasMalus(up) && this.hasMalus(southUp) && this.hasMalus(eastUp)) {
            neighbors[count++] = southEastUp;
        }
        if (this.isOpen(northWestUp = this.findAcceptedNode(pos.x - 1, pos.y + 1, pos.z - 1)) && this.hasMalus(northWest) && this.hasMalus(north) && this.hasMalus(west) && this.hasMalus(up) && this.hasMalus(northUp) && this.hasMalus(westUp)) {
            neighbors[count++] = northWestUp;
        }
        if (this.isOpen(southWestUp = this.findAcceptedNode(pos.x - 1, pos.y + 1, pos.z + 1)) && this.hasMalus(southWest) && this.hasMalus(south) && this.hasMalus(west) && this.hasMalus(up) && this.hasMalus(southUp) && this.hasMalus(westUp)) {
            neighbors[count++] = southWestUp;
        }
        if (this.isOpen(northEastDown = this.findAcceptedNode(pos.x + 1, pos.y - 1, pos.z - 1)) && this.hasMalus(northEast) && this.hasMalus(north) && this.hasMalus(east) && this.hasMalus(down) && this.hasMalus(northDown) && this.hasMalus(eastDown)) {
            neighbors[count++] = northEastDown;
        }
        if (this.isOpen(southEastDown = this.findAcceptedNode(pos.x + 1, pos.y - 1, pos.z + 1)) && this.hasMalus(southEast) && this.hasMalus(south) && this.hasMalus(east) && this.hasMalus(down) && this.hasMalus(southDown) && this.hasMalus(eastDown)) {
            neighbors[count++] = southEastDown;
        }
        if (this.isOpen(northWestDown = this.findAcceptedNode(pos.x - 1, pos.y - 1, pos.z - 1)) && this.hasMalus(northWest) && this.hasMalus(north) && this.hasMalus(west) && this.hasMalus(down) && this.hasMalus(northDown) && this.hasMalus(westDown)) {
            neighbors[count++] = northWestDown;
        }
        if (this.isOpen(southWestDown = this.findAcceptedNode(pos.x - 1, pos.y - 1, pos.z + 1)) && this.hasMalus(southWest) && this.hasMalus(south) && this.hasMalus(west) && this.hasMalus(down) && this.hasMalus(southDown) && this.hasMalus(westDown)) {
            neighbors[count++] = southWestDown;
        }
        return count;
    }

    private boolean hasMalus(@Nullable Node node) {
        return node != null && node.costMalus >= 0.0f;
    }

    private boolean isOpen(@Nullable Node node) {
        return node != null && !node.closed;
    }

    protected @Nullable Node findAcceptedNode(int x, int y, int z) {
        Node best = null;
        PathType pathType = this.getCachedPathType(x, y, z);
        float pathCost = this.mob.getPathfindingMalus(pathType);
        if (pathCost >= 0.0f) {
            best = this.getNode(x, y, z);
            best.type = pathType;
            best.costMalus = Math.max(best.costMalus, pathCost);
            if (pathType == PathType.WALKABLE) {
                best.costMalus += 1.0f;
            }
        }
        return best;
    }

    @Override
    protected PathType getCachedPathType(int x, int y, int z) {
        return (PathType)((Object)this.pathTypeByPosCache.computeIfAbsent(BlockPos.asLong(x, y, z), key -> this.getPathTypeOfMob(this.currentContext, x, y, z, this.mob)));
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        PathType blockPathType = context.getPathTypeFromState(x, y, z);
        if (blockPathType == PathType.OPEN && y >= context.level().getMinY() + 1) {
            BlockPos belowPos = new BlockPos(x, y - 1, z);
            PathType belowType = context.getPathTypeFromState(belowPos.getX(), belowPos.getY(), belowPos.getZ());
            if (belowType == PathType.FIRE || belowType == PathType.LAVA) {
                blockPathType = PathType.FIRE;
            } else if (belowType == PathType.DAMAGING) {
                blockPathType = PathType.DAMAGING;
            } else if (belowType == PathType.COCOA) {
                blockPathType = PathType.COCOA;
            } else if (belowType == PathType.FENCE) {
                if (!belowPos.equals(context.mobPosition())) {
                    blockPathType = PathType.FENCE;
                }
            } else {
                PathType pathType = blockPathType = belowType == PathType.WALKABLE || belowType == PathType.OPEN || belowType == PathType.WATER ? PathType.OPEN : PathType.WALKABLE;
            }
        }
        if (blockPathType == PathType.WALKABLE || blockPathType == PathType.OPEN) {
            blockPathType = FlyNodeEvaluator.checkNeighbourBlocks(context, x, y, z, blockPathType);
        }
        return blockPathType;
    }

    private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(Mob mob) {
        boolean isSmallMob;
        AABB boundingBox = mob.getBoundingBox();
        boolean bl = isSmallMob = boundingBox.getSize() < 1.0;
        if (!isSmallMob) {
            return List.of(BlockPos.containing(boundingBox.minX, mob.getBlockY(), boundingBox.minZ), BlockPos.containing(boundingBox.minX, mob.getBlockY(), boundingBox.maxZ), BlockPos.containing(boundingBox.maxX, mob.getBlockY(), boundingBox.minZ), BlockPos.containing(boundingBox.maxX, mob.getBlockY(), boundingBox.maxZ));
        }
        double zPadding = Math.max(0.0, (double)1.1f - boundingBox.getZsize());
        double xPadding = Math.max(0.0, (double)1.1f - boundingBox.getXsize());
        double yPadding = Math.max(0.0, (double)1.1f - boundingBox.getYsize());
        AABB inflatedBoundingBox = boundingBox.inflate(xPadding, yPadding, zPadding);
        return BlockPos.randomBetweenClosed(mob.getRandom(), 10, Mth.floor(inflatedBoundingBox.minX), Mth.floor(inflatedBoundingBox.minY), Mth.floor(inflatedBoundingBox.minZ), Mth.floor(inflatedBoundingBox.maxX), Mth.floor(inflatedBoundingBox.maxY), Mth.floor(inflatedBoundingBox.maxZ));
    }
}

