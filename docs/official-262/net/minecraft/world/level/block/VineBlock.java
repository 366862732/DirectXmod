/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.TypedInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class VineBlock
extends Block {
    public static final MapCodec<VineBlock> CODEC = VineBlock.simpleCodec(VineBlock::new);
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream().filter(e -> e.getKey() != Direction.DOWN).collect(Util.toMap());
    private final Function<BlockState, VoxelShape> shapes;

    public MapCodec<VineBlock> codec() {
        return CODEC;
    }

    public VineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(UP, false)).setValue(NORTH, false)).setValue(EAST, false)).setValue(SOUTH, false)).setValue(WEST, false));
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> shapes = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(state -> {
            VoxelShape shape = Shapes.empty();
            for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                if (!((Boolean)state.getValue(entry.getValue())).booleanValue()) continue;
                shape = Shapes.or(shape, (VoxelShape)shapes.get(entry.getKey()));
            }
            return shape.isEmpty() ? Shapes.block() : shape;
        });
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return this.hasFaces(this.getUpdatedState(state, level, pos));
    }

    private boolean hasFaces(BlockState blockState) {
        return this.countFaces(blockState) > 0;
    }

    private int countFaces(BlockState blockState) {
        int count = 0;
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            if (!blockState.getValue(property).booleanValue()) continue;
            ++count;
        }
        return count;
    }

    private boolean canSupportAtFace(BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) {
            return false;
        }
        BlockPos relative = pos.relative(direction);
        if (VineBlock.isAcceptableNeighbour(level, relative, direction)) {
            return true;
        }
        if (direction.getAxis() != Direction.Axis.Y) {
            BooleanProperty property = PROPERTY_BY_DIRECTION.get(direction);
            BlockState aboveState = level.getBlockState(pos.above());
            return aboveState.is(this) && aboveState.getValue(property) != false;
        }
        return false;
    }

    public static boolean isAcceptableNeighbour(BlockGetter level, BlockPos neighbourPos, Direction directionToNeighbour) {
        return MultifaceBlock.canAttachTo(level, directionToNeighbour, neighbourPos, level.getBlockState(neighbourPos));
    }

    private BlockState getUpdatedState(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        if (state.getValue(UP).booleanValue()) {
            state = (BlockState)state.setValue(UP, VineBlock.isAcceptableNeighbour(level, abovePos, Direction.DOWN));
        }
        TypedInstance aboveState = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BooleanProperty property = VineBlock.getPropertyForFace(direction);
            if (!state.getValue(property).booleanValue()) continue;
            boolean canSupport = this.canSupportAtFace(level, pos, direction);
            if (!canSupport) {
                if (aboveState == null) {
                    aboveState = level.getBlockState(abovePos);
                }
                canSupport = aboveState.is(this) && ((StateHolder)((Object)aboveState)).getValue(property) != false;
            }
            state = (BlockState)state.setValue(property, canSupport);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.DOWN) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        BlockState blockState = this.getUpdatedState(state, level, pos);
        if (!this.hasFaces(blockState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return blockState;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState after;
        BlockState before;
        BlockPos belowPos;
        BlockState belowState;
        if (!level.getGameRules().get(GameRules.SPREAD_VINES).booleanValue()) {
            return;
        }
        if (random.nextInt(4) != 0) {
            return;
        }
        Direction testDirection = Direction.getRandom(random);
        BlockPos abovePos = pos.above();
        if (testDirection.getAxis().isHorizontal() && !state.getValue(VineBlock.getPropertyForFace(testDirection)).booleanValue()) {
            if (!this.canSpread(level, pos)) {
                return;
            }
            BlockPos testPos = pos.relative(testDirection);
            BlockState edgeState = level.getBlockState(testPos);
            if (edgeState.isAir()) {
                Direction cwDirection = testDirection.getClockWise();
                Direction ccwDirection = testDirection.getCounterClockWise();
                boolean cwHasConnectingFace = state.getValue(VineBlock.getPropertyForFace(cwDirection));
                boolean ccwHasConnectingFace = state.getValue(VineBlock.getPropertyForFace(ccwDirection));
                BlockPos cwTestPos = testPos.relative(cwDirection);
                BlockPos ccwTestPos = testPos.relative(ccwDirection);
                if (cwHasConnectingFace && VineBlock.isAcceptableNeighbour(level, cwTestPos, cwDirection)) {
                    level.setBlock(testPos, (BlockState)this.defaultBlockState().setValue(VineBlock.getPropertyForFace(cwDirection), true), 2);
                } else if (ccwHasConnectingFace && VineBlock.isAcceptableNeighbour(level, ccwTestPos, ccwDirection)) {
                    level.setBlock(testPos, (BlockState)this.defaultBlockState().setValue(VineBlock.getPropertyForFace(ccwDirection), true), 2);
                } else {
                    Direction opposite = testDirection.getOpposite();
                    if (cwHasConnectingFace && level.isEmptyBlock(cwTestPos) && VineBlock.isAcceptableNeighbour(level, pos.relative(cwDirection), opposite)) {
                        level.setBlock(cwTestPos, (BlockState)this.defaultBlockState().setValue(VineBlock.getPropertyForFace(opposite), true), 2);
                    } else if (ccwHasConnectingFace && level.isEmptyBlock(ccwTestPos) && VineBlock.isAcceptableNeighbour(level, pos.relative(ccwDirection), opposite)) {
                        level.setBlock(ccwTestPos, (BlockState)this.defaultBlockState().setValue(VineBlock.getPropertyForFace(opposite), true), 2);
                    } else if ((double)random.nextFloat() < 0.05 && VineBlock.isAcceptableNeighbour(level, testPos.above(), Direction.UP)) {
                        level.setBlock(testPos, (BlockState)this.defaultBlockState().setValue(UP, true), 2);
                    }
                }
            } else if (VineBlock.isAcceptableNeighbour(level, testPos, testDirection)) {
                level.setBlock(pos, (BlockState)state.setValue(VineBlock.getPropertyForFace(testDirection), true), 2);
            }
            return;
        }
        if (testDirection == Direction.UP && pos.getY() < level.getMaxY()) {
            if (this.canSupportAtFace(level, pos, testDirection)) {
                level.setBlock(pos, (BlockState)state.setValue(UP, true), 2);
                return;
            }
            if (level.isEmptyBlock(abovePos)) {
                if (!this.canSpread(level, pos)) {
                    return;
                }
                BlockState aboveState = state;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (!random.nextBoolean() && VineBlock.isAcceptableNeighbour(level, abovePos.relative(direction), direction)) continue;
                    aboveState = (BlockState)aboveState.setValue(VineBlock.getPropertyForFace(direction), false);
                }
                if (this.hasHorizontalConnection(aboveState)) {
                    level.setBlock(abovePos, aboveState, 2);
                }
                return;
            }
        }
        if (pos.getY() > level.getMinY() && ((belowState = level.getBlockState(belowPos = pos.below())).isAir() || belowState.is(this)) && (before = belowState.isAir() ? this.defaultBlockState() : belowState) != (after = this.copyRandomFaces(state, before, random)) && this.hasHorizontalConnection(after)) {
            level.setBlock(belowPos, after, 2);
        }
    }

    private BlockState copyRandomFaces(BlockState from, BlockState to, RandomSource random) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BooleanProperty propertyForFace;
            if (!random.nextBoolean() || !from.getValue(propertyForFace = VineBlock.getPropertyForFace(direction)).booleanValue()) continue;
            to = (BlockState)to.setValue(propertyForFace, true);
        }
        return to;
    }

    private boolean hasHorizontalConnection(BlockState state) {
        return state.getValue(NORTH) != false || state.getValue(EAST) != false || state.getValue(SOUTH) != false || state.getValue(WEST) != false;
    }

    private boolean canSpread(BlockGetter level, BlockPos pos) {
        int radius = 4;
        Iterable<BlockPos> iterable = BlockPos.betweenClosed(pos.getX() - 4, pos.getY() - 1, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 1, pos.getZ() + 4);
        int max = 5;
        for (BlockPos blockPos : iterable) {
            if (!level.getBlockState(blockPos).is(this) || --max > 0) continue;
            return false;
        }
        return true;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
        if (clickedState.is(this)) {
            return this.countFaces(clickedState) < PROPERTY_BY_DIRECTION.size();
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
        boolean clickedVine = clickedState.is(this);
        BlockState result = clickedVine ? clickedState : this.defaultBlockState();
        for (Direction direction : context.getNearestLookingDirections()) {
            boolean faceOccupied;
            if (direction == Direction.DOWN) continue;
            BooleanProperty face = VineBlock.getPropertyForFace(direction);
            boolean bl = faceOccupied = clickedVine && clickedState.getValue(face) != false;
            if (faceOccupied || !this.canSupportAtFace(context.getLevel(), context.getClickedPos(), direction)) continue;
            return (BlockState)result.setValue(face, true);
        }
        return clickedVine ? result : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case Rotation.CLOCKWISE_180 -> (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, state.getValue(SOUTH))).setValue(EAST, state.getValue(WEST))).setValue(SOUTH, state.getValue(NORTH))).setValue(WEST, state.getValue(EAST));
            case Rotation.COUNTERCLOCKWISE_90 -> (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, state.getValue(EAST))).setValue(EAST, state.getValue(SOUTH))).setValue(SOUTH, state.getValue(WEST))).setValue(WEST, state.getValue(NORTH));
            case Rotation.CLOCKWISE_90 -> (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, state.getValue(WEST))).setValue(EAST, state.getValue(NORTH))).setValue(SOUTH, state.getValue(EAST))).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT: {
                return (BlockState)((BlockState)state.setValue(NORTH, state.getValue(SOUTH))).setValue(SOUTH, state.getValue(NORTH));
            }
            case FRONT_BACK: {
                return (BlockState)((BlockState)state.setValue(EAST, state.getValue(WEST))).setValue(WEST, state.getValue(EAST));
            }
        }
        return super.mirror(state, mirror);
    }

    public static BooleanProperty getPropertyForFace(Direction direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }
}

