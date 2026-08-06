/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MultifaceBlock
extends Block
implements SimpleWaterloggedBlock {
    public static final MapCodec<MultifaceBlock> CODEC = MultifaceBlock.simpleCodec(MultifaceBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;
    protected static final Direction[] DIRECTIONS = Direction.values();
    private final Function<BlockState, VoxelShape> shapes;
    private final boolean canRotate;
    private final boolean canMirrorX;
    private final boolean canMirrorZ;

    protected MapCodec<? extends MultifaceBlock> codec() {
        return CODEC;
    }

    public MultifaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(MultifaceBlock.getDefaultMultifaceState(this.stateDefinition));
        this.shapes = this.makeShapes();
        this.canRotate = Direction.Plane.HORIZONTAL.stream().allMatch(this::isFaceSupported);
        this.canMirrorX = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.X).filter(this::isFaceSupported).count() % 2L == 0L;
        this.canMirrorZ = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.Z).filter(this::isFaceSupported).count() % 2L == 0L;
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> shapes = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(state -> {
            VoxelShape shape = Shapes.empty();
            for (Direction direction : DIRECTIONS) {
                if (!MultifaceBlock.hasFace(state, direction)) continue;
                shape = Shapes.or(shape, (VoxelShape)shapes.get(direction));
            }
            return shape.isEmpty() ? Shapes.block() : shape;
        }, WATERLOGGED);
    }

    public static Set<Direction> availableFaces(BlockState state) {
        if (!(state.getBlock() instanceof MultifaceBlock)) {
            return Set.of();
        }
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        for (Direction direction : Direction.values()) {
            if (!MultifaceBlock.hasFace(state, direction)) continue;
            faces.add(direction);
        }
        return faces;
    }

    public static Set<Direction> unpack(byte data) {
        EnumSet<Direction> presentDirections = EnumSet.noneOf(Direction.class);
        for (Direction direction : Direction.values()) {
            if ((data & (byte)(1 << direction.ordinal())) <= 0) continue;
            presentDirections.add(direction);
        }
        return presentDirections;
    }

    public static byte pack(Collection<Direction> directions) {
        byte code = 0;
        for (Direction direction : directions) {
            code = (byte)(code | 1 << direction.ordinal());
        }
        return code;
    }

    protected boolean isFaceSupported(Direction faceDirection) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction direction : DIRECTIONS) {
            if (!this.isFaceSupported(direction)) continue;
            builder.add(MultifaceBlock.getFaceProperty(direction));
        }
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!MultifaceBlock.hasAnyFace(state)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (!MultifaceBlock.hasFace(state, directionToNeighbour) || MultifaceBlock.canAttachTo(level, directionToNeighbour, neighbourPos, neighbourState)) {
            return state;
        }
        return MultifaceBlock.removeFace(state, MultifaceBlock.getFaceProperty(directionToNeighbour));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        boolean hasAtLeastOneFace = false;
        for (Direction directionToNeighbour : DIRECTIONS) {
            if (!MultifaceBlock.hasFace(state, directionToNeighbour)) continue;
            if (!MultifaceBlock.canAttachTo(level, pos, directionToNeighbour)) {
                return false;
            }
            hasAtLeastOneFace = true;
        }
        return hasAtLeastOneFace;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.getItemInHand().is(this.asItem()) || MultifaceBlock.hasAnyVacantFace(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        BlockState oldState = level.getBlockState(placePos);
        return Arrays.stream(context.getNearestLookingDirections()).map(direction -> this.getStateForPlacement(oldState, level, placePos, (Direction)direction)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public boolean isValidStateForPlacement(BlockGetter level, BlockState oldState, BlockPos placementPos, Direction placementDirection) {
        if (!this.isFaceSupported(placementDirection) || oldState.is(this) && MultifaceBlock.hasFace(oldState, placementDirection)) {
            return false;
        }
        BlockPos neighbourPos = placementPos.relative(placementDirection);
        return MultifaceBlock.canAttachTo(level, placementDirection, neighbourPos, level.getBlockState(neighbourPos));
    }

    public @Nullable BlockState getStateForPlacement(BlockState oldState, BlockGetter level, BlockPos placementPos, Direction placementDirection) {
        if (!this.isValidStateForPlacement(level, oldState, placementPos, placementDirection)) {
            return null;
        }
        BlockState newState = oldState.is(this) ? oldState : (oldState.getFluidState().isSourceOfType(Fluids.WATER) ? (BlockState)this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true) : this.defaultBlockState());
        return (BlockState)newState.setValue(MultifaceBlock.getFaceProperty(placementDirection), true);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        if (!this.canRotate) {
            return state;
        }
        return this.mapDirections(state, rotation::rotate);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.FRONT_BACK && !this.canMirrorX) {
            return state;
        }
        if (mirror == Mirror.LEFT_RIGHT && !this.canMirrorZ) {
            return state;
        }
        return this.mapDirections(state, mirror::mirror);
    }

    private BlockState mapDirections(BlockState state, Function<Direction, Direction> mapping) {
        BlockState newState = state;
        for (Direction direction : DIRECTIONS) {
            if (!this.isFaceSupported(direction)) continue;
            newState = (BlockState)newState.setValue(MultifaceBlock.getFaceProperty(mapping.apply(direction)), state.getValue(MultifaceBlock.getFaceProperty(direction)));
        }
        return newState;
    }

    public static boolean hasFace(BlockState state, Direction faceDirection) {
        BooleanProperty property = MultifaceBlock.getFaceProperty(faceDirection);
        return state.getValueOrElse(property, false);
    }

    public static boolean canAttachTo(BlockGetter level, BlockPos pos, Direction directionTowardsNeighbour) {
        BlockPos neighbourPos = pos.relative(directionTowardsNeighbour);
        BlockState blockState = level.getBlockState(neighbourPos);
        return MultifaceBlock.canAttachTo(level, directionTowardsNeighbour, neighbourPos, blockState);
    }

    public static boolean canAttachTo(BlockGetter level, Direction directionTowardsNeighbour, BlockPos neighbourPos, BlockState neighbourState) {
        return Block.isFaceFull(neighbourState.getBlockSupportShape(level, neighbourPos), directionTowardsNeighbour.getOpposite()) || Block.isFaceFull(neighbourState.getCollisionShape(level, neighbourPos), directionTowardsNeighbour.getOpposite());
    }

    private static BlockState removeFace(BlockState state, BooleanProperty property) {
        BlockState newState = (BlockState)state.setValue(property, false);
        if (MultifaceBlock.hasAnyFace(newState)) {
            return newState;
        }
        return Blocks.AIR.defaultBlockState();
    }

    public static BooleanProperty getFaceProperty(Direction faceDirection) {
        return PROPERTY_BY_DIRECTION.get(faceDirection);
    }

    private static BlockState getDefaultMultifaceState(StateDefinition<Block, BlockState> stateDefinition) {
        BlockState state = (BlockState)stateDefinition.any().setValue(WATERLOGGED, false);
        for (BooleanProperty faceProperty : PROPERTY_BY_DIRECTION.values()) {
            state = (BlockState)state.trySetValue(faceProperty, false);
        }
        return state;
    }

    protected static boolean hasAnyFace(BlockState state) {
        for (Direction direction : DIRECTIONS) {
            if (!MultifaceBlock.hasFace(state, direction)) continue;
            return true;
        }
        return false;
    }

    private static boolean hasAnyVacantFace(BlockState state) {
        for (Direction direction : DIRECTIONS) {
            if (MultifaceBlock.hasFace(state, direction)) continue;
            return true;
        }
        return false;
    }
}

