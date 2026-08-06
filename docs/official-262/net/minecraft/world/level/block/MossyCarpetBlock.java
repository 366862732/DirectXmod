/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.TypedInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MossyCarpetBlock
extends Block
implements BonemealableBlock {
    public static final MapCodec<MossyCarpetBlock> CODEC = MossyCarpetBlock.simpleCodec(MossyCarpetBlock::new);
    public static final BooleanProperty BASE = BlockStateProperties.BOTTOM;
    public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
    public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
    public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
    public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
    public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf((Map)Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST)));
    private final Function<BlockState, VoxelShape> shapes;

    public MapCodec<MossyCarpetBlock> codec() {
        return CODEC;
    }

    public MossyCarpetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(BASE, true)).setValue(NORTH, WallSide.NONE)).setValue(EAST, WallSide.NONE)).setValue(SOUTH, WallSide.NONE)).setValue(WEST, WallSide.NONE));
        this.shapes = this.makeShapes();
    }

    public Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> low = Shapes.rotateHorizontal(Block.boxZ(16.0, 0.0, 10.0, 0.0, 1.0));
        Map<Direction, VoxelShape> tall = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(state -> {
            VoxelShape shape = state.getValue(BASE) != false ? (VoxelShape)tall.get(Direction.DOWN) : Shapes.empty();
            for (Map.Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                switch ((WallSide)state.getValue(entry.getValue())) {
                    case NONE: {
                        break;
                    }
                    case LOW: {
                        shape = Shapes.or(shape, (VoxelShape)low.get(entry.getKey()));
                        break;
                    }
                    case TALL: {
                        shape = Shapes.or(shape, (VoxelShape)tall.get(entry.getKey()));
                    }
                }
            }
            return shape.isEmpty() ? Shapes.block() : shape;
        });
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BASE) != false ? this.shapes.apply(this.defaultBlockState()) : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        if (state.getValue(BASE).booleanValue()) {
            return !belowState.isAir();
        }
        return belowState.is(this) && belowState.getValue(BASE) != false;
    }

    private static boolean hasFaces(BlockState blockState) {
        if (blockState.getValue(BASE).booleanValue()) {
            return true;
        }
        for (EnumProperty<WallSide> property : PROPERTY_BY_DIRECTION.values()) {
            if (blockState.getValue(property) == WallSide.NONE) continue;
            return true;
        }
        return false;
    }

    private static boolean canSupportAtFace(BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.UP) {
            return false;
        }
        return MultifaceBlock.canAttachTo(level, pos, direction);
    }

    private static BlockState getUpdatedState(BlockState state, BlockGetter level, BlockPos pos, boolean createSides) {
        TypedInstance aboveState = null;
        TypedInstance belowState = null;
        createSides |= state.getValue(BASE).booleanValue();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            WallSide side;
            EnumProperty<WallSide> property = MossyCarpetBlock.getPropertyForFace(direction);
            WallSide wallSide = MossyCarpetBlock.canSupportAtFace(level, pos, direction) ? (createSides ? WallSide.LOW : state.getValue(property)) : (side = WallSide.NONE);
            if (side == WallSide.LOW) {
                if (aboveState == null) {
                    aboveState = level.getBlockState(pos.above());
                }
                if (aboveState.is(Blocks.PALE_MOSS_CARPET) && ((StateHolder)((Object)aboveState)).getValue(property) != WallSide.NONE && !((StateHolder)((Object)aboveState)).getValue(BASE).booleanValue()) {
                    side = WallSide.TALL;
                }
                if (!state.getValue(BASE).booleanValue()) {
                    if (belowState == null) {
                        belowState = level.getBlockState(pos.below());
                    }
                    if (belowState.is(Blocks.PALE_MOSS_CARPET) && ((StateHolder)((Object)belowState)).getValue(property) == WallSide.NONE) {
                        side = WallSide.NONE;
                    }
                }
            }
            state = (BlockState)state.setValue(property, side);
        }
        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return MossyCarpetBlock.getUpdatedState(this.defaultBlockState(), context.getLevel(), context.getClickedPos(), true);
    }

    public static void placeAt(LevelAccessor level, BlockPos pos, RandomSource random, @Block.UpdateFlags int updateType) {
        BlockState simpleCarpetLayer = Blocks.PALE_MOSS_CARPET.defaultBlockState();
        BlockState adjustedCarpetLayer = MossyCarpetBlock.getUpdatedState(simpleCarpetLayer, level, pos, true);
        level.setBlock(pos, adjustedCarpetLayer, updateType);
        BlockState state = MossyCarpetBlock.createTopperWithSideChance(level, pos, random::nextBoolean);
        if (!state.isAir()) {
            level.setBlock(pos.above(), state, updateType);
            BlockState updateBottomCarpet = MossyCarpetBlock.getUpdatedState(adjustedCarpetLayer, level, pos, true);
            level.setBlock(pos, updateBottomCarpet, updateType);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        if (level.isClientSide()) {
            return;
        }
        RandomSource random = level.getRandom();
        BlockState topper = MossyCarpetBlock.createTopperWithSideChance(level, pos, random::nextBoolean);
        if (!topper.isAir()) {
            level.setBlock(pos.above(), topper, 3);
        }
    }

    private static BlockState createTopperWithSideChance(BlockGetter level, BlockPos pos, BooleanSupplier sideSurvivalTest) {
        BlockPos above = pos.above();
        BlockState abovePreviousState = level.getBlockState(above);
        boolean isMossyCarpetAbove = abovePreviousState.is(Blocks.PALE_MOSS_CARPET);
        if (isMossyCarpetAbove && abovePreviousState.getValue(BASE).booleanValue() || !isMossyCarpetAbove && !abovePreviousState.canBeReplaced()) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState noCarpetBaseState = (BlockState)Blocks.PALE_MOSS_CARPET.defaultBlockState().setValue(BASE, false);
        BlockState aboveState = MossyCarpetBlock.getUpdatedState(noCarpetBaseState, level, pos.above(), true);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            EnumProperty<WallSide> property = MossyCarpetBlock.getPropertyForFace(direction);
            if (aboveState.getValue(property) == WallSide.NONE || sideSurvivalTest.getAsBoolean()) continue;
            aboveState = (BlockState)aboveState.setValue(property, WallSide.NONE);
        }
        if (MossyCarpetBlock.hasFaces(aboveState) && aboveState != abovePreviousState) {
            return aboveState;
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState blockState = MossyCarpetBlock.getUpdatedState(state, level, pos, false);
        if (!MossyCarpetBlock.hasFaces(blockState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return blockState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BASE, NORTH, EAST, SOUTH, WEST);
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
        return switch (mirror) {
            case Mirror.LEFT_RIGHT -> (BlockState)((BlockState)state.setValue(NORTH, state.getValue(SOUTH))).setValue(SOUTH, state.getValue(NORTH));
            case Mirror.FRONT_BACK -> (BlockState)((BlockState)state.setValue(EAST, state.getValue(WEST))).setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
    }

    public static @Nullable EnumProperty<WallSide> getPropertyForFace(Direction direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(BASE) != false && !MossyCarpetBlock.createTopperWithSideChance(level, pos, () -> true).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState topper = MossyCarpetBlock.createTopperWithSideChance(level, pos, () -> true);
        if (!topper.isAir()) {
            level.setBlock(pos.above(), topper, 3);
        }
    }
}

