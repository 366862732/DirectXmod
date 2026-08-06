/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BaseRailBlock
extends Block
implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE_FLAT = Block.column(16.0, 0.0, 2.0);
    private static final VoxelShape SHAPE_SLOPE = Block.column(16.0, 0.0, 8.0);
    private final boolean isStraight;

    public static boolean isRail(Level level, BlockPos pos) {
        return BaseRailBlock.isRail(level.getBlockState(pos));
    }

    public static boolean isRail(BlockState state) {
        return state.is(BlockTags.RAILS) && state.getBlock() instanceof BaseRailBlock;
    }

    protected BaseRailBlock(boolean isStraight, BlockBehaviour.Properties properties) {
        super(properties);
        this.isStraight = isStraight;
    }

    protected abstract MapCodec<? extends BaseRailBlock> codec();

    public boolean isStraight() {
        return this.isStraight;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(this.getShapeProperty()).isSlope() ? SHAPE_SLOPE : SHAPE_FLAT;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return BaseRailBlock.canSupportRigidBlock(level, pos.below());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock())) {
            return;
        }
        this.updateState(state, level, pos, movedByPiston);
    }

    protected BlockState updateState(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        state = this.updateDir(level, pos, state, true);
        if (this.isStraight) {
            level.neighborChanged(state, pos, this, null, movedByPiston);
        }
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide() || !level.getBlockState(pos).is(this)) {
            return;
        }
        RailShape shape = state.getValue(this.getShapeProperty());
        if (BaseRailBlock.shouldBeRemoved(pos, level, shape)) {
            BaseRailBlock.dropResources(state, level, pos);
            level.removeBlock(pos, movedByPiston);
        } else {
            this.updateState(state, level, pos, block);
        }
    }

    private static boolean shouldBeRemoved(BlockPos pos, Level level, RailShape shape) {
        if (!BaseRailBlock.canSupportRigidBlock(level, pos.below())) {
            return true;
        }
        return switch (shape) {
            case RailShape.ASCENDING_EAST -> {
                if (!BaseRailBlock.canSupportRigidBlock(level, pos.east())) {
                    yield true;
                }
                yield false;
            }
            case RailShape.ASCENDING_WEST -> {
                if (!BaseRailBlock.canSupportRigidBlock(level, pos.west())) {
                    yield true;
                }
                yield false;
            }
            case RailShape.ASCENDING_NORTH -> {
                if (!BaseRailBlock.canSupportRigidBlock(level, pos.north())) {
                    yield true;
                }
                yield false;
            }
            case RailShape.ASCENDING_SOUTH -> {
                if (!BaseRailBlock.canSupportRigidBlock(level, pos.south())) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
    }

    protected BlockState updateDir(Level level, BlockPos pos, BlockState state, boolean first) {
        if (level.isClientSide()) {
            return state;
        }
        RailShape current = state.getValue(this.getShapeProperty());
        return new RailState(level, pos, state).place(level.hasNeighborSignal(pos), first, current).getState();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (movedByPiston) {
            return;
        }
        if (state.getValue(this.getShapeProperty()).isSlope()) {
            level.updateNeighborsAt(pos.above(), this);
        }
        if (this.isStraight) {
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean isWaterSource = replacedFluidState.is(Fluids.WATER);
        BlockState state = super.defaultBlockState();
        Direction direction = context.getHorizontalDirection();
        boolean isEastWest = direction == Direction.EAST || direction == Direction.WEST;
        return (BlockState)((BlockState)state.setValue(this.getShapeProperty(), isEastWest ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH)).setValue(WATERLOGGED, isWaterSource);
    }

    public abstract Property<RailShape> getShapeProperty();

    protected RailShape rotate(RailShape shape, Rotation rotation) {
        return switch (rotation) {
            case Rotation.CLOCKWISE_180 -> {
                switch (shape) {
                    default: {
                        throw new MatchException(null, null);
                    }
                    case NORTH_SOUTH: {
                        yield RailShape.NORTH_SOUTH;
                    }
                    case EAST_WEST: {
                        yield RailShape.EAST_WEST;
                    }
                    case ASCENDING_EAST: {
                        yield RailShape.ASCENDING_WEST;
                    }
                    case ASCENDING_WEST: {
                        yield RailShape.ASCENDING_EAST;
                    }
                    case ASCENDING_NORTH: {
                        yield RailShape.ASCENDING_SOUTH;
                    }
                    case ASCENDING_SOUTH: {
                        yield RailShape.ASCENDING_NORTH;
                    }
                    case SOUTH_EAST: {
                        yield RailShape.NORTH_WEST;
                    }
                    case SOUTH_WEST: {
                        yield RailShape.NORTH_EAST;
                    }
                    case NORTH_WEST: {
                        yield RailShape.SOUTH_EAST;
                    }
                    case NORTH_EAST: 
                }
                yield RailShape.SOUTH_WEST;
            }
            case Rotation.COUNTERCLOCKWISE_90 -> {
                switch (shape) {
                    default: {
                        throw new MatchException(null, null);
                    }
                    case NORTH_SOUTH: {
                        yield RailShape.EAST_WEST;
                    }
                    case EAST_WEST: {
                        yield RailShape.NORTH_SOUTH;
                    }
                    case ASCENDING_EAST: {
                        yield RailShape.ASCENDING_NORTH;
                    }
                    case ASCENDING_WEST: {
                        yield RailShape.ASCENDING_SOUTH;
                    }
                    case ASCENDING_NORTH: {
                        yield RailShape.ASCENDING_WEST;
                    }
                    case ASCENDING_SOUTH: {
                        yield RailShape.ASCENDING_EAST;
                    }
                    case SOUTH_EAST: {
                        yield RailShape.NORTH_EAST;
                    }
                    case SOUTH_WEST: {
                        yield RailShape.SOUTH_EAST;
                    }
                    case NORTH_WEST: {
                        yield RailShape.SOUTH_WEST;
                    }
                    case NORTH_EAST: 
                }
                yield RailShape.NORTH_WEST;
            }
            case Rotation.CLOCKWISE_90 -> {
                switch (shape) {
                    default: {
                        throw new MatchException(null, null);
                    }
                    case NORTH_SOUTH: {
                        yield RailShape.EAST_WEST;
                    }
                    case EAST_WEST: {
                        yield RailShape.NORTH_SOUTH;
                    }
                    case ASCENDING_EAST: {
                        yield RailShape.ASCENDING_SOUTH;
                    }
                    case ASCENDING_WEST: {
                        yield RailShape.ASCENDING_NORTH;
                    }
                    case ASCENDING_NORTH: {
                        yield RailShape.ASCENDING_EAST;
                    }
                    case ASCENDING_SOUTH: {
                        yield RailShape.ASCENDING_WEST;
                    }
                    case SOUTH_EAST: {
                        yield RailShape.SOUTH_WEST;
                    }
                    case SOUTH_WEST: {
                        yield RailShape.NORTH_WEST;
                    }
                    case NORTH_WEST: {
                        yield RailShape.NORTH_EAST;
                    }
                    case NORTH_EAST: 
                }
                yield RailShape.SOUTH_EAST;
            }
            default -> shape;
        };
    }

    protected RailShape mirror(RailShape shape, Mirror mirror) {
        return switch (mirror) {
            case Mirror.LEFT_RIGHT -> {
                switch (shape) {
                    case ASCENDING_NORTH: {
                        yield RailShape.ASCENDING_SOUTH;
                    }
                    case ASCENDING_SOUTH: {
                        yield RailShape.ASCENDING_NORTH;
                    }
                    case SOUTH_EAST: {
                        yield RailShape.NORTH_EAST;
                    }
                    case SOUTH_WEST: {
                        yield RailShape.NORTH_WEST;
                    }
                    case NORTH_WEST: {
                        yield RailShape.SOUTH_WEST;
                    }
                    case NORTH_EAST: {
                        yield RailShape.SOUTH_EAST;
                    }
                }
                yield shape;
            }
            case Mirror.FRONT_BACK -> {
                switch (shape) {
                    case ASCENDING_EAST: {
                        yield RailShape.ASCENDING_WEST;
                    }
                    case ASCENDING_WEST: {
                        yield RailShape.ASCENDING_EAST;
                    }
                    case SOUTH_EAST: {
                        yield RailShape.SOUTH_WEST;
                    }
                    case SOUTH_WEST: {
                        yield RailShape.SOUTH_EAST;
                    }
                    case NORTH_WEST: {
                        yield RailShape.NORTH_EAST;
                    }
                    case NORTH_EAST: {
                        yield RailShape.NORTH_WEST;
                    }
                }
                yield shape;
            }
            default -> shape;
        };
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }
}

