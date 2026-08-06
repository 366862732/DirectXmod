/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock
extends Block
implements SimpleWaterloggedBlock {
    public static final MapCodec<WallBlock> CODEC = WallBlock.simpleCodec(WallBlock::new);
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
    public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
    public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
    public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
    public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf((Map)Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST)));
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final Function<BlockState, VoxelShape> shapes;
    private final Function<BlockState, VoxelShape> collisionShapes;
    private static final VoxelShape TEST_SHAPE_POST = Block.column(2.0, 0.0, 16.0);
    private static final Map<Direction, VoxelShape> TEST_SHAPES_WALL = Shapes.rotateHorizontal(Block.boxZ(2.0, 16.0, 0.0, 9.0));

    public MapCodec<WallBlock> codec() {
        return CODEC;
    }

    public WallBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(UP, true)).setValue(NORTH, WallSide.NONE)).setValue(EAST, WallSide.NONE)).setValue(SOUTH, WallSide.NONE)).setValue(WEST, WallSide.NONE)).setValue(WATERLOGGED, false));
        this.shapes = this.makeShapes(16.0f, 14.0f);
        this.collisionShapes = this.makeShapes(24.0f, 24.0f);
    }

    private Function<BlockState, VoxelShape> makeShapes(float postHeight, float wallTop) {
        VoxelShape post = Block.column(8.0, 0.0, postHeight);
        int width = 6;
        Map<Direction, VoxelShape> low = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, wallTop, 0.0, 11.0));
        Map<Direction, VoxelShape> tall = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, postHeight, 0.0, 11.0));
        return this.getShapeForEachState(state -> {
            VoxelShape shape = state.getValue(UP) != false ? post : Shapes.empty();
            for (Map.Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                shape = Shapes.or(shape, switch ((WallSide)state.getValue(entry.getValue())) {
                    default -> throw new MatchException(null, null);
                    case WallSide.NONE -> Shapes.empty();
                    case WallSide.LOW -> (VoxelShape)low.get(entry.getKey());
                    case WallSide.TALL -> (VoxelShape)tall.get(entry.getKey());
                });
            }
            return shape;
        }, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.collisionShapes.apply(state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    private boolean connectsTo(BlockState state, boolean faceSolid, Direction direction) {
        Block block = state.getBlock();
        boolean connectedFenceGate = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
        return state.is(BlockTags.WALLS) || !WallBlock.isExceptionForConnection(state) && faceSolid || block instanceof IronBarsBlock || connectedFenceGate;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockPos northPos = pos.north();
        BlockPos eastPos = pos.east();
        BlockPos southPos = pos.south();
        BlockPos westPos = pos.west();
        BlockPos topPos = pos.above();
        BlockState northState = level.getBlockState(northPos);
        BlockState eastState = level.getBlockState(eastPos);
        BlockState southState = level.getBlockState(southPos);
        BlockState westState = level.getBlockState(westPos);
        BlockState topState = level.getBlockState(topPos);
        boolean north = this.connectsTo(northState, northState.isFaceSturdy(level, northPos, Direction.SOUTH), Direction.SOUTH);
        boolean east = this.connectsTo(eastState, eastState.isFaceSturdy(level, eastPos, Direction.WEST), Direction.WEST);
        boolean south = this.connectsTo(southState, southState.isFaceSturdy(level, southPos, Direction.NORTH), Direction.NORTH);
        boolean west = this.connectsTo(westState, westState.isFaceSturdy(level, westPos, Direction.EAST), Direction.EAST);
        BlockState state = (BlockState)this.defaultBlockState().setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
        return this.updateShape(level, state, topPos, topState, north, east, south, west);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (directionToNeighbour == Direction.DOWN) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        if (directionToNeighbour == Direction.UP) {
            return this.topUpdate(level, state, neighbourPos, neighbourState);
        }
        return this.sideUpdate(level, pos, state, neighbourPos, neighbourState, directionToNeighbour);
    }

    private static boolean isConnected(BlockState state, Property<WallSide> northWall) {
        return state.getValue(northWall) != WallSide.NONE;
    }

    private static boolean isCovered(VoxelShape aboveShape, VoxelShape testShape) {
        return !Shapes.joinIsNotEmpty(testShape, aboveShape, BooleanOp.ONLY_FIRST);
    }

    private BlockState topUpdate(LevelReader level, BlockState state, BlockPos topPos, BlockState topNeighbour) {
        boolean north = WallBlock.isConnected(state, NORTH);
        boolean east = WallBlock.isConnected(state, EAST);
        boolean south = WallBlock.isConnected(state, SOUTH);
        boolean west = WallBlock.isConnected(state, WEST);
        return this.updateShape(level, state, topPos, topNeighbour, north, east, south, west);
    }

    private BlockState sideUpdate(LevelReader level, BlockPos pos, BlockState state, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
        Direction opposite = direction.getOpposite();
        boolean isNorthConnected = direction == Direction.NORTH ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite) : WallBlock.isConnected(state, NORTH);
        boolean isEastConnected = direction == Direction.EAST ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite) : WallBlock.isConnected(state, EAST);
        boolean isSouthConnected = direction == Direction.SOUTH ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite) : WallBlock.isConnected(state, SOUTH);
        boolean isWestConnected = direction == Direction.WEST ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite) : WallBlock.isConnected(state, WEST);
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        return this.updateShape(level, state, above, aboveState, isNorthConnected, isEastConnected, isSouthConnected, isWestConnected);
    }

    private BlockState updateShape(LevelReader level, BlockState state, BlockPos topPos, BlockState topNeighbour, boolean north, boolean east, boolean south, boolean west) {
        VoxelShape aboveShape = topNeighbour.getCollisionShape(level, topPos).getFaceShape(Direction.DOWN);
        BlockState sidesUpdatedState = this.updateSides(state, north, east, south, west, aboveShape);
        return (BlockState)sidesUpdatedState.setValue(UP, this.shouldRaisePost(sidesUpdatedState, topNeighbour, aboveShape));
    }

    private boolean shouldRaisePost(BlockState state, BlockState topNeighbour, VoxelShape aboveShape) {
        boolean hasHighWall;
        boolean hasCorner;
        boolean topNeighbourHasPost;
        boolean bl = topNeighbourHasPost = topNeighbour.getBlock() instanceof WallBlock && topNeighbour.getValue(UP) != false;
        if (topNeighbourHasPost) {
            return true;
        }
        WallSide northWall = state.getValue(NORTH);
        WallSide southWall = state.getValue(SOUTH);
        WallSide eastWall = state.getValue(EAST);
        WallSide westWall = state.getValue(WEST);
        boolean southNone = southWall == WallSide.NONE;
        boolean westNone = westWall == WallSide.NONE;
        boolean eastNone = eastWall == WallSide.NONE;
        boolean northNone = northWall == WallSide.NONE;
        boolean bl2 = hasCorner = northNone && southNone && westNone && eastNone || northNone != southNone || westNone != eastNone;
        if (hasCorner) {
            return true;
        }
        boolean bl3 = hasHighWall = northWall == WallSide.TALL && southWall == WallSide.TALL || eastWall == WallSide.TALL && westWall == WallSide.TALL;
        if (hasHighWall) {
            return false;
        }
        return topNeighbour.is(BlockTags.WALL_POST_OVERRIDE) || WallBlock.isCovered(aboveShape, TEST_SHAPE_POST);
    }

    private BlockState updateSides(BlockState state, boolean northConnection, boolean eastConnection, boolean southConnection, boolean westConnection, VoxelShape aboveShape) {
        return (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, this.makeWallState(northConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.NORTH)))).setValue(EAST, this.makeWallState(eastConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.EAST)))).setValue(SOUTH, this.makeWallState(southConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.SOUTH)))).setValue(WEST, this.makeWallState(westConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.WEST)));
    }

    private WallSide makeWallState(boolean connectsToSide, VoxelShape aboveShape, VoxelShape testShape) {
        if (connectsToSide) {
            if (WallBlock.isCovered(aboveShape, testShape)) {
                return WallSide.TALL;
            }
            return WallSide.LOW;
        }
        return WallSide.NONE;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(WATERLOGGED) == false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, WEST, SOUTH, WATERLOGGED);
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
}

