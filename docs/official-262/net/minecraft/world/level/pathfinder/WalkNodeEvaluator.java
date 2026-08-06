/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2BooleanMap
 *  it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class WalkNodeEvaluator
extends NodeEvaluator {
    public static final double SPACE_BETWEEN_WALL_POSTS = 0.5;
    private static final double DEFAULT_MOB_JUMP_HEIGHT = 1.125;
    private final Long2ObjectMap<PathType> pathTypesByPosCacheByMob = new Long2ObjectOpenHashMap();
    private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap();
    private final Node[] reusableNeighbors = new Node[Direction.Plane.HORIZONTAL.length()];

    @Override
    public void prepare(PathNavigationRegion level, Mob entity) {
        super.prepare(level, entity);
        entity.onPathfindingStart();
    }

    @Override
    public void done() {
        this.mob.onPathfindingDone();
        this.pathTypesByPosCacheByMob.clear();
        this.collisionCache.clear();
        super.done();
    }

    @Override
    public Node getStart() {
        BlockPos.MutableBlockPos reusablePos = new BlockPos.MutableBlockPos();
        int startY = this.mob.getBlockY();
        BlockState blockState = this.currentContext.getBlockState(reusablePos.set(this.mob.getX(), (double)startY, this.mob.getZ()));
        if (this.mob.canStandOnFluid(blockState.getFluidState())) {
            while (this.mob.canStandOnFluid(blockState.getFluidState())) {
                blockState = this.currentContext.getBlockState(reusablePos.set(this.mob.getX(), (double)(++startY), this.mob.getZ()));
            }
            --startY;
        } else if (this.canFloat() && this.mob.isInWater()) {
            while (blockState.is(Blocks.WATER) || blockState.getFluidState() == Fluids.WATER.getSource(false)) {
                blockState = this.currentContext.getBlockState(reusablePos.set(this.mob.getX(), (double)(++startY), this.mob.getZ()));
            }
            --startY;
        } else if (this.mob.onGround()) {
            startY = Mth.floor(this.mob.getY() + 0.5);
        } else {
            reusablePos.set(this.mob.getX(), this.mob.getY() + 1.0, this.mob.getZ());
            while (reusablePos.getY() > this.currentContext.level().getMinY()) {
                startY = reusablePos.getY();
                reusablePos.setY(reusablePos.getY() - 1);
                BlockState belowBlockState = this.currentContext.getBlockState(reusablePos);
                if (belowBlockState.isAir() || belowBlockState.isPathfindable(PathComputationType.LAND)) continue;
                break;
            }
        }
        BlockPos startPos = this.mob.blockPosition();
        if (!this.canStartAt(reusablePos.set(startPos.getX(), startY, startPos.getZ()))) {
            AABB mobBB = this.mob.getBoundingBox();
            if (this.canStartAt(reusablePos.set(mobBB.minX, (double)startY, mobBB.minZ)) || this.canStartAt(reusablePos.set(mobBB.minX, (double)startY, mobBB.maxZ)) || this.canStartAt(reusablePos.set(mobBB.maxX, (double)startY, mobBB.minZ)) || this.canStartAt(reusablePos.set(mobBB.maxX, (double)startY, mobBB.maxZ))) {
                return this.getStartNode(reusablePos);
            }
        }
        return this.getStartNode(new BlockPos(startPos.getX(), startY, startPos.getZ()));
    }

    protected Node getStartNode(BlockPos pos) {
        Node node = this.getNode(pos);
        node.type = this.getCachedPathType(node.x, node.y, node.z);
        node.costMalus = this.mob.getPathfindingMalus(node.type);
        return node;
    }

    protected boolean canStartAt(BlockPos pos) {
        PathType blockPathType = this.getCachedPathType(pos.getX(), pos.getY(), pos.getZ());
        return blockPathType != PathType.OPEN && this.mob.getPathfindingMalus(blockPathType) >= 0.0f;
    }

    @Override
    public Target getTarget(double x, double y, double z) {
        return this.getTargetNodeAt(x, y, z);
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node pos) {
        int p = 0;
        int jumpSize = 0;
        PathType blockPathTypeAbove = this.getCachedPathType(pos.x, pos.y + 1, pos.z);
        PathType blockPathTypeCurrent = this.getCachedPathType(pos.x, pos.y, pos.z);
        if (this.mob.getPathfindingMalus(blockPathTypeAbove) >= 0.0f && blockPathTypeCurrent != PathType.STICKY_HONEY) {
            jumpSize = Mth.floor(Math.max(1.0f, this.mob.maxUpStep()));
        }
        double posHeight = this.getFloorLevel(new BlockPos(pos.x, pos.y, pos.z));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Node node;
            this.reusableNeighbors[direction.get2DDataValue()] = node = this.findAcceptedNode(pos.x + direction.getStepX(), pos.y, pos.z + direction.getStepZ(), jumpSize, posHeight, direction, blockPathTypeCurrent);
            if (!this.isNeighborValid(node, pos)) continue;
            neighbors[p++] = node;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Node diagonalNode;
            Direction secondDirection = direction.getClockWise();
            if (!this.isDiagonalValid(pos, this.reusableNeighbors[direction.get2DDataValue()], this.reusableNeighbors[secondDirection.get2DDataValue()]) || !this.isDiagonalValid(diagonalNode = this.findAcceptedNode(pos.x + direction.getStepX() + secondDirection.getStepX(), pos.y, pos.z + direction.getStepZ() + secondDirection.getStepZ(), jumpSize, posHeight, direction, blockPathTypeCurrent))) continue;
            neighbors[p++] = diagonalNode;
        }
        return p;
    }

    protected boolean isNeighborValid(@Nullable Node neighbor, Node current) {
        return neighbor != null && !neighbor.closed && (neighbor.costMalus >= 0.0f || current.costMalus < 0.0f);
    }

    protected boolean isDiagonalValid(Node pos, @Nullable Node ew, @Nullable Node ns) {
        if (ns == null || ew == null || ns.y > pos.y || ew.y > pos.y) {
            return false;
        }
        if (ew.type == PathType.WALKABLE_DOOR || ns.type == PathType.WALKABLE_DOOR) {
            return false;
        }
        if (this.mob.getBbWidth() > 1.0f && (ew.costMalus > 0.0f || ns.costMalus > 0.0f)) {
            return false;
        }
        boolean canPassBetweenPosts = ns.type == PathType.FENCE && ew.type == PathType.FENCE && (double)this.mob.getBbWidth() < 0.5;
        return (ns.y < pos.y || ns.costMalus >= 0.0f || canPassBetweenPosts) && (ew.y < pos.y || ew.costMalus >= 0.0f || canPassBetweenPosts);
    }

    protected boolean isDiagonalValid(@Nullable Node diagonal) {
        if (diagonal == null || diagonal.closed) {
            return false;
        }
        if (diagonal.type == PathType.WALKABLE_DOOR) {
            return false;
        }
        return diagonal.costMalus >= 0.0f;
    }

    private static boolean doesBlockHavePartialCollision(PathType type) {
        return type == PathType.FENCE || type == PathType.DOOR_WOOD_CLOSED || type == PathType.DOOR_IRON_CLOSED;
    }

    private boolean canReachWithoutCollision(Node posTo) {
        AABB bb = this.mob.getBoundingBox();
        Vec3 delta = new Vec3((double)posTo.x - this.mob.getX() + bb.getXsize() / 2.0, (double)posTo.y - this.mob.getY() + bb.getYsize() / 2.0, (double)posTo.z - this.mob.getZ() + bb.getZsize() / 2.0);
        int steps = Mth.ceil(delta.length() / bb.getSize());
        delta = delta.scale(1.0f / (float)steps);
        for (int i = 1; i <= steps; ++i) {
            if (!this.hasCollisions(bb = bb.move(delta))) continue;
            return false;
        }
        return true;
    }

    protected double getFloorLevel(BlockPos pos) {
        CollisionGetter level = this.currentContext.level();
        if ((this.canFloat() || this.isAmphibious()) && level.getFluidState(pos).is(FluidTags.WATER)) {
            return (double)pos.getY() + 0.5;
        }
        return WalkNodeEvaluator.getFloorLevel(level, pos);
    }

    public static double getFloorLevel(BlockGetter level, BlockPos pos) {
        BlockPos target = pos.below();
        VoxelShape shape = level.getBlockState(target).getCollisionShape(level, target);
        return (double)target.getY() + (shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y));
    }

    protected boolean isAmphibious() {
        return false;
    }

    protected @Nullable Node findAcceptedNode(int x, int y, int z, int jumpSize, double nodeHeight, Direction travelDirection, PathType blockPathTypeCurrent) {
        Node best = null;
        BlockPos.MutableBlockPos reusablePos = new BlockPos.MutableBlockPos();
        double maxYTarget = this.getFloorLevel(reusablePos.set(x, y, z));
        if (maxYTarget - nodeHeight > this.getMobJumpHeight()) {
            return null;
        }
        PathType pathType = this.getCachedPathType(x, y, z);
        float pathCost = this.mob.getPathfindingMalus(pathType);
        if (pathCost >= 0.0f) {
            best = this.getNodeAndUpdateCostToMax(x, y, z, pathType, pathCost);
        }
        if (WalkNodeEvaluator.doesBlockHavePartialCollision(blockPathTypeCurrent) && best != null && best.costMalus >= 0.0f && !this.canReachWithoutCollision(best)) {
            best = null;
        }
        if (pathType == PathType.WALKABLE || this.isAmphibious() && pathType == PathType.WATER) {
            return best;
        }
        if ((best == null || best.costMalus < 0.0f) && jumpSize > 0 && (pathType != PathType.FENCE || this.canWalkOverFences()) && pathType != PathType.UNPASSABLE_RAIL && pathType != PathType.TRAPDOOR && pathType != PathType.POWDER_SNOW) {
            best = this.tryJumpOn(x, y, z, jumpSize, nodeHeight, travelDirection, blockPathTypeCurrent, reusablePos);
        } else if (!this.isAmphibious() && pathType == PathType.WATER && !this.canFloat()) {
            best = this.tryFindFirstNonWaterBelow(x, y, z, best);
        } else if (pathType == PathType.OPEN) {
            best = this.tryFindFirstGroundNodeBelow(x, y, z);
        } else if (WalkNodeEvaluator.doesBlockHavePartialCollision(pathType) && best == null) {
            best = this.getClosedNode(x, y, z, pathType);
        }
        return best;
    }

    private double getMobJumpHeight() {
        return Math.max(1.125, (double)this.mob.maxUpStep());
    }

    private Node getNodeAndUpdateCostToMax(int x, int y, int z, PathType pathType, float cost) {
        Node node = this.getNode(x, y, z);
        node.type = pathType;
        node.costMalus = Math.max(node.costMalus, cost);
        return node;
    }

    private Node getBlockedNode(int x, int y, int z) {
        Node node = this.getNode(x, y, z);
        node.type = PathType.BLOCKED;
        node.costMalus = -1.0f;
        return node;
    }

    private Node getClosedNode(int x, int y, int z, PathType pathType) {
        Node node = this.getNode(x, y, z);
        node.closed = true;
        node.type = pathType;
        node.costMalus = pathType.getMalus();
        return node;
    }

    private @Nullable Node tryJumpOn(int x, int y, int z, int jumpSize, double nodeHeight, Direction travelDirection, PathType blockPathTypeCurrent, BlockPos.MutableBlockPos reusablePos) {
        Node nodeAbove = this.findAcceptedNode(x, y + 1, z, jumpSize - 1, nodeHeight, travelDirection, blockPathTypeCurrent);
        if (nodeAbove == null) {
            return null;
        }
        if (this.mob.getBbWidth() >= 1.0f) {
            return nodeAbove;
        }
        if (nodeAbove.type != PathType.OPEN && nodeAbove.type != PathType.WALKABLE) {
            return nodeAbove;
        }
        double centerX = (double)(x - travelDirection.getStepX()) + 0.5;
        double centerZ = (double)(z - travelDirection.getStepZ()) + 0.5;
        double halfWidth = (double)this.mob.getBbWidth() / 2.0;
        AABB grow = new AABB(centerX - halfWidth, this.getFloorLevel(reusablePos.set(centerX, (double)(y + 1), centerZ)) + 0.001, centerZ - halfWidth, centerX + halfWidth, (double)this.mob.getBbHeight() + this.getFloorLevel(reusablePos.set((double)nodeAbove.x, (double)nodeAbove.y, (double)nodeAbove.z)) - 0.002, centerZ + halfWidth);
        return this.hasCollisions(grow) ? null : nodeAbove;
    }

    private @Nullable Node tryFindFirstNonWaterBelow(int x, int y, int z, @Nullable Node best) {
        --y;
        while (y > this.mob.level().getMinY()) {
            PathType pathTypeLocal = this.getCachedPathType(x, y, z);
            if (pathTypeLocal != PathType.WATER) {
                return best;
            }
            best = this.getNodeAndUpdateCostToMax(x, y, z, pathTypeLocal, this.mob.getPathfindingMalus(pathTypeLocal));
            --y;
        }
        return best;
    }

    private Node tryFindFirstGroundNodeBelow(int x, int y, int z) {
        for (int currentY = y - 1; currentY >= this.mob.level().getMinY(); --currentY) {
            if (y - currentY > this.mob.getMaxFallDistance()) {
                return this.getBlockedNode(x, currentY, z);
            }
            PathType pathType = this.getCachedPathType(x, currentY, z);
            float pathCost = this.mob.getPathfindingMalus(pathType);
            if (pathType == PathType.OPEN) continue;
            if (pathCost >= 0.0f) {
                return this.getNodeAndUpdateCostToMax(x, currentY, z, pathType, pathCost);
            }
            return this.getBlockedNode(x, currentY, z);
        }
        return this.getBlockedNode(x, y, z);
    }

    private boolean hasCollisions(AABB aabb) {
        return this.collisionCache.computeIfAbsent((Object)aabb, bb -> !this.currentContext.level().noCollision(this.mob, aabb));
    }

    protected PathType getCachedPathType(int x, int y, int z) {
        return (PathType)((Object)this.pathTypesByPosCacheByMob.computeIfAbsent(BlockPos.asLong(x, y, z), k -> this.getPathTypeOfMob(this.currentContext, x, y, z, this.mob)));
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
        boolean isLargeMob;
        Set<PathType> blockTypes = this.getPathTypeWithinMobBB(context, x, y, z);
        if (blockTypes.size() == 1) {
            return blockTypes.iterator().next();
        }
        if (blockTypes.contains((Object)PathType.FENCE)) {
            return PathType.FENCE;
        }
        if (blockTypes.contains((Object)PathType.UNPASSABLE_RAIL)) {
            return PathType.UNPASSABLE_RAIL;
        }
        PathType highestMalusPathTypeWithinBB = PathType.BLOCKED;
        float highestMalusWithinBB = mob.getPathfindingMalus(highestMalusPathTypeWithinBB);
        for (PathType pathType : blockTypes) {
            float malusForPathType = mob.getPathfindingMalus(pathType);
            if (malusForPathType < 0.0f) {
                return pathType;
            }
            if (!(malusForPathType >= highestMalusWithinBB)) continue;
            highestMalusWithinBB = malusForPathType;
            highestMalusPathTypeWithinBB = pathType;
        }
        PathType currentNodePathType = this.getPathType(context, x, y, z);
        boolean bl = isLargeMob = this.entityWidth > 1;
        if (isLargeMob) {
            boolean capMalusDueToCheapNode;
            boolean isCurrentNodeCheaper = mob.getPathfindingMalus(currentNodePathType) < highestMalusWithinBB;
            boolean bl2 = capMalusDueToCheapNode = isCurrentNodeCheaper && mob.getPathfindingMalus(PathType.BIG_MOBS_CLOSE_TO_DANGER) < highestMalusWithinBB;
            if (capMalusDueToCheapNode) {
                return PathType.BIG_MOBS_CLOSE_TO_DANGER;
            }
            return highestMalusPathTypeWithinBB;
        }
        if (currentNodePathType == PathType.OPEN && highestMalusPathTypeWithinBB != PathType.OPEN && highestMalusWithinBB == 0.0f) {
            return PathType.OPEN;
        }
        return highestMalusPathTypeWithinBB;
    }

    public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
        EnumSet<PathType> blockTypes = EnumSet.noneOf(PathType.class);
        for (int dx = 0; dx < this.entityWidth; ++dx) {
            for (int dy = 0; dy < this.entityHeight; ++dy) {
                for (int dz = 0; dz < this.entityDepth; ++dz) {
                    int xx = dx + x;
                    int yy = dy + y;
                    int zz = dz + z;
                    PathType blockType = this.getPathType(context, xx, yy, zz);
                    BlockPos mobPosition = this.mob.blockPosition();
                    boolean canPassDoors = this.canPassDoors();
                    if (blockType == PathType.DOOR_WOOD_CLOSED && this.canOpenDoors() && canPassDoors) {
                        blockType = PathType.WALKABLE_DOOR;
                    }
                    if (blockType == PathType.DOOR_OPEN && !canPassDoors) {
                        blockType = PathType.BLOCKED;
                    }
                    if (blockType == PathType.RAIL && this.getPathType(context, mobPosition.getX(), mobPosition.getY(), mobPosition.getZ()) != PathType.RAIL && this.getPathType(context, mobPosition.getX(), mobPosition.getY() - 1, mobPosition.getZ()) != PathType.RAIL) {
                        blockType = PathType.UNPASSABLE_RAIL;
                    }
                    blockTypes.add(blockType);
                }
            }
        }
        return blockTypes;
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        return WalkNodeEvaluator.getPathTypeStatic(context, new BlockPos.MutableBlockPos(x, y, z));
    }

    public static PathType getPathTypeStatic(Mob mob, BlockPos pos) {
        return WalkNodeEvaluator.getPathTypeStatic(new PathfindingContext(mob.level(), mob), pos.mutable());
    }

    public static PathType getPathTypeStatic(PathfindingContext context, BlockPos.MutableBlockPos pos) {
        int z;
        int y;
        int x = pos.getX();
        PathType blockPathType = context.getPathTypeFromState(x, y = pos.getY(), z = pos.getZ());
        if (blockPathType != PathType.OPEN || y < context.level().getMinY() + 1) {
            return blockPathType;
        }
        return switch (context.getPathTypeFromState(x, y - 1, z)) {
            case PathType.OPEN, PathType.WATER, PathType.LAVA, PathType.WALKABLE -> PathType.OPEN;
            case PathType.FIRE -> PathType.FIRE;
            case PathType.DAMAGING -> PathType.DAMAGING;
            case PathType.STICKY_HONEY -> PathType.STICKY_HONEY;
            case PathType.POWDER_SNOW -> PathType.ON_TOP_OF_POWDER_SNOW;
            case PathType.DAMAGE_CAUTIOUS -> PathType.DAMAGE_CAUTIOUS;
            case PathType.TRAPDOOR -> PathType.ON_TOP_OF_TRAPDOOR;
            default -> WalkNodeEvaluator.checkNeighbourBlocks(context, x, y, z, PathType.WALKABLE);
        };
    }

    public static PathType checkNeighbourBlocks(PathfindingContext context, int x, int y, int z, PathType blockPathType) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (dx == 0 && dz == 0) continue;
                    PathType pathType = context.getPathTypeFromState(x + dx, y + dy, z + dz);
                    if (pathType == PathType.DAMAGING) {
                        return PathType.DAMAGING_IN_NEIGHBOR;
                    }
                    if (pathType == PathType.FIRE || pathType == PathType.LAVA) {
                        return PathType.FIRE_IN_NEIGHBOR;
                    }
                    if (pathType == PathType.WATER) {
                        return PathType.WATER_BORDER;
                    }
                    if (pathType != PathType.DAMAGE_CAUTIOUS) continue;
                    return PathType.DAMAGE_CAUTIOUS;
                }
            }
        }
        return blockPathType;
    }

    protected static PathType getPathTypeFromState(BlockGetter level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        if (blockState.isAir()) {
            return PathType.OPEN;
        }
        if (blockState.is(BlockTags.TRAPDOORS) || blockState.is(Blocks.LILY_PAD) || blockState.is(Blocks.BIG_DRIPLEAF)) {
            return PathType.TRAPDOOR;
        }
        if (blockState.is(Blocks.POWDER_SNOW)) {
            return PathType.POWDER_SNOW;
        }
        if (blockState.is(Blocks.CACTUS) || blockState.is(Blocks.SWEET_BERRY_BUSH)) {
            return PathType.DAMAGING;
        }
        if (blockState.is(Blocks.HONEY_BLOCK)) {
            return PathType.STICKY_HONEY;
        }
        if (blockState.is(Blocks.COCOA)) {
            return PathType.COCOA;
        }
        if (blockState.is(Blocks.WITHER_ROSE) || blockState.is(BlockTags.SPELEOTHEMS)) {
            return PathType.DAMAGE_CAUTIOUS;
        }
        FluidState fluidState = blockState.getFluidState();
        if (fluidState.is(FluidTags.LAVA)) {
            return PathType.LAVA;
        }
        if (WalkNodeEvaluator.isBurningBlock(blockState)) {
            return PathType.FIRE;
        }
        if (block instanceof DoorBlock) {
            DoorBlock door = (DoorBlock)block;
            if (blockState.getValue(DoorBlock.OPEN).booleanValue()) {
                return PathType.DOOR_OPEN;
            }
            return door.type().canOpenByHand() ? PathType.DOOR_WOOD_CLOSED : PathType.DOOR_IRON_CLOSED;
        }
        if (block instanceof BaseRailBlock) {
            return PathType.RAIL;
        }
        if (block instanceof LeavesBlock) {
            return PathType.LEAVES;
        }
        if (blockState.is(BlockTags.FENCES) || blockState.is(BlockTags.WALLS) || block instanceof FenceGateBlock && !blockState.getValue(FenceGateBlock.OPEN).booleanValue()) {
            return PathType.FENCE;
        }
        if (!blockState.isPathfindable(PathComputationType.LAND)) {
            return PathType.BLOCKED;
        }
        if (fluidState.is(FluidTags.WATER)) {
            return PathType.WATER;
        }
        return PathType.OPEN;
    }
}

