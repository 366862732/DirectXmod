/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.mutable.MutableInt
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

public class PortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBehaviour.StatePredicate FRAME = (state, level, pos) -> state.is(Blocks.OBSIDIAN);
    private static final float SAFE_TRAVEL_MAX_ENTITY_XY = 4.0f;
    private static final double SAFE_TRAVEL_MAX_VERTICAL_DELTA = 1.0;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private final int numPortalBlocks;
    private final BlockPos bottomLeft;
    private final int height;
    private final int width;

    private PortalShape(Direction.Axis axis, int portalBlockCount, Direction rightDir, BlockPos bottomLeft, int width, int height) {
        this.axis = axis;
        this.numPortalBlocks = portalBlockCount;
        this.rightDir = rightDir;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    public static Optional<PortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos pos, Direction.Axis preferredAxis) {
        return PortalShape.findPortalShape(level, pos, shape -> shape.isValid() && shape.numPortalBlocks == 0, preferredAxis);
    }

    public static Optional<PortalShape> findPortalShape(LevelAccessor level, BlockPos pos, Predicate<PortalShape> isValid, Direction.Axis preferredAxis) {
        Optional<PortalShape> firstAxis = Optional.of(PortalShape.findAnyShape(level, pos, preferredAxis)).filter(isValid);
        if (firstAxis.isPresent()) {
            return firstAxis;
        }
        Direction.Axis otherAxis = preferredAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Optional.of(PortalShape.findAnyShape(level, pos, otherAxis)).filter(isValid);
    }

    public static PortalShape findAnyShape(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = PortalShape.calculateBottomLeft(level, rightDir, pos);
        if (bottomLeft == null) {
            return new PortalShape(axis, 0, rightDir, pos, 0, 0);
        }
        int width = PortalShape.calculateWidth(level, bottomLeft, rightDir);
        if (width == 0) {
            return new PortalShape(axis, 0, rightDir, bottomLeft, 0, 0);
        }
        MutableInt portalBlockCountOutput = new MutableInt();
        int height = PortalShape.calculateHeight(level, bottomLeft, rightDir, width, portalBlockCountOutput);
        return new PortalShape(axis, portalBlockCountOutput.intValue(), rightDir, bottomLeft, width, height);
    }

    private static @Nullable BlockPos calculateBottomLeft(BlockGetter level, Direction rightDir, BlockPos pos) {
        int minY = Math.max(level.getMinY(), pos.getY() - 21);
        while (pos.getY() > minY && PortalShape.isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }
        Direction leftDir = rightDir.getOpposite();
        int edge = PortalShape.getDistanceUntilEdgeAboveFrame(level, pos, leftDir) - 1;
        if (edge < 0) {
            return null;
        }
        return pos.relative(leftDir, edge);
    }

    private static int calculateWidth(BlockGetter level, BlockPos bottomLeft, Direction rightDir) {
        int width = PortalShape.getDistanceUntilEdgeAboveFrame(level, bottomLeft, rightDir);
        if (width < 2 || width > 21) {
            return 0;
        }
        return width;
    }

    private static int getDistanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int width = 0; width <= 21; ++width) {
            blockPos.set(pos).move(direction, width);
            BlockState blockState = level.getBlockState(blockPos);
            if (!PortalShape.isEmpty(blockState)) {
                if (!FRAME.test(blockState, level, blockPos)) break;
                return width;
            }
            BlockState belowState = level.getBlockState(blockPos.move(Direction.DOWN));
            if (!FRAME.test(belowState, level, blockPos)) break;
        }
        return 0;
    }

    private static int calculateHeight(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width, MutableInt portalBlockCount) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int height = PortalShape.getDistanceUntilTop(level, bottomLeft, rightDir, pos, width, portalBlockCount);
        if (height < 3 || height > 21 || !PortalShape.hasTopFrame(level, bottomLeft, rightDir, pos, width, height)) {
            return 0;
        }
        return height;
    }

    private static boolean hasTopFrame(BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockPos.MutableBlockPos pos, int width, int height) {
        for (int i = 0; i < width; ++i) {
            BlockPos.MutableBlockPos framePos = pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
            if (FRAME.test(level.getBlockState(framePos), level, framePos)) continue;
            return false;
        }
        return true;
    }

    private static int getDistanceUntilTop(BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockPos.MutableBlockPos pos, int width, MutableInt portalBlockCount) {
        for (int height = 0; height < 21; ++height) {
            pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, -1);
            if (!FRAME.test(level.getBlockState(pos), level, pos)) {
                return height;
            }
            pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, width);
            if (!FRAME.test(level.getBlockState(pos), level, pos)) {
                return height;
            }
            for (int i = 0; i < width; ++i) {
                pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
                BlockState state = level.getBlockState(pos);
                if (!PortalShape.isEmpty(state)) {
                    return height;
                }
                if (!state.is(Blocks.NETHER_PORTAL)) continue;
                portalBlockCount.increment();
            }
        }
        return 21;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(Blocks.NETHER_PORTAL);
    }

    public boolean isValid() {
        return this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    public void createPortalBlocks(LevelAccessor level) {
        BlockState portalState = (BlockState)Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1)).forEach(pos -> level.setBlock((BlockPos)pos, portalState, 18));
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static Vec3 getRelativePosition(BlockUtil.FoundRectangle largestRectangleAround, Direction.Axis axis, Vec3 position, EntityDimensions dimensions) {
        double relativeUp;
        double relativeRight;
        double width = (double)largestRectangleAround.axis1Size - (double)dimensions.width();
        double height = (double)largestRectangleAround.axis2Size - (double)dimensions.height();
        BlockPos bottomMin = largestRectangleAround.minCorner;
        if (width > 0.0) {
            double bottomStart = (double)bottomMin.get(axis) + (double)dimensions.width() / 2.0;
            relativeRight = Mth.clamp(Mth.inverseLerp(position.get(axis) - bottomStart, 0.0, width), 0.0, 1.0);
        } else {
            relativeRight = 0.5;
        }
        if (height > 0.0) {
            Direction.Axis heightAxis = Direction.Axis.Y;
            relativeUp = Mth.clamp(Mth.inverseLerp(position.get(heightAxis) - (double)bottomMin.get(heightAxis), 0.0, height), 0.0, 1.0);
        } else {
            relativeUp = 0.0;
        }
        Direction.Axis forwardAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        double relativeForward = position.get(forwardAxis) - ((double)bottomMin.get(forwardAxis) + 0.5);
        return new Vec3(relativeRight, relativeUp, relativeForward);
    }

    public static Vec3 findCollisionFreePosition(Vec3 bottomCenter, ServerLevel serverLevel, Entity entity, EntityDimensions dimensions) {
        if (dimensions.width() > 4.0f || dimensions.height() > 4.0f) {
            return bottomCenter;
        }
        double halfHeight = (double)dimensions.height() / 2.0;
        Vec3 center = bottomCenter.add(0.0, halfHeight, 0.0);
        VoxelShape allowedCenters = Shapes.create(AABB.ofSize(center, dimensions.width(), 0.0, dimensions.width()).expandTowards(0.0, 1.0, 0.0).inflate(1.0E-6));
        Optional<Vec3> collisionFreePosition = serverLevel.findFreePosition(entity, allowedCenters, center, dimensions.width(), dimensions.height(), dimensions.width());
        Optional<Vec3> collisionFreeBottomCenter = collisionFreePosition.map(vec -> vec.subtract(0.0, halfHeight, 0.0));
        return collisionFreeBottomCenter.orElse(bottomCenter);
    }
}

