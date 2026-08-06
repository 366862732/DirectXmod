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
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.ExperimentalRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RedStoneWireBlock
extends Block {
    public static final MapCodec<RedStoneWireBlock> CODEC = RedStoneWireBlock.simpleCodec(RedStoneWireBlock::new);
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf((Map)Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST)));
    private static final int[] COLORS = Util.make(new int[16], list -> {
        for (int i = 0; i <= 15; ++i) {
            float power;
            float red = power * 0.6f + ((power = (float)i / 15.0f) > 0.0f ? 0.4f : 0.3f);
            float green = Mth.clamp(power * power * 0.7f - 0.5f, 0.0f, 1.0f);
            float blue = Mth.clamp(power * power * 0.6f - 0.7f, 0.0f, 1.0f);
            list[i] = ARGB.colorFromFloat(1.0f, red, green, blue);
        }
    });
    private static final float PARTICLE_DENSITY = 0.2f;
    private final Function<BlockState, VoxelShape> shapes;
    private final BlockState crossState;
    private final RedstoneWireEvaluator evaluator = new DefaultRedstoneWireEvaluator(this);
    private boolean shouldSignal = true;

    public MapCodec<RedStoneWireBlock> codec() {
        return CODEC;
    }

    public RedStoneWireBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(NORTH, RedstoneSide.NONE)).setValue(EAST, RedstoneSide.NONE)).setValue(SOUTH, RedstoneSide.NONE)).setValue(WEST, RedstoneSide.NONE)).setValue(POWER, 0));
        this.shapes = this.makeShapes();
        this.crossState = (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(NORTH, RedstoneSide.SIDE)).setValue(EAST, RedstoneSide.SIDE)).setValue(SOUTH, RedstoneSide.SIDE)).setValue(WEST, RedstoneSide.SIDE);
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        boolean height = true;
        int width = 10;
        VoxelShape dot = Block.column(10.0, 0.0, 1.0);
        Map<Direction, VoxelShape> floor = Shapes.rotateHorizontal(Block.boxZ(10.0, 0.0, 1.0, 0.0, 8.0));
        Map<Direction, VoxelShape> up = Shapes.rotateHorizontal(Block.boxZ(10.0, 16.0, 0.0, 1.0));
        return this.getShapeForEachState(state -> {
            VoxelShape shape = dot;
            for (Map.Entry<Direction, EnumProperty<RedstoneSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                shape = switch ((RedstoneSide)state.getValue(entry.getValue())) {
                    default -> throw new MatchException(null, null);
                    case RedstoneSide.UP -> Shapes.or(shape, (VoxelShape)floor.get(entry.getKey()), (VoxelShape)up.get(entry.getKey()));
                    case RedstoneSide.SIDE -> Shapes.or(shape, (VoxelShape)floor.get(entry.getKey()));
                    case RedstoneSide.NONE -> shape;
                };
            }
            return shape;
        }, POWER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.getConnectionState(context.getLevel(), this.crossState, context.getClickedPos());
    }

    private BlockState getConnectionState(BlockGetter level, BlockState state, BlockPos pos) {
        boolean eastWestEmpty;
        boolean wasDot = RedStoneWireBlock.isDot(state);
        state = this.getMissingConnections(level, (BlockState)this.defaultBlockState().setValue(POWER, state.getValue(POWER)), pos);
        if (wasDot && RedStoneWireBlock.isDot(state)) {
            return state;
        }
        boolean north = state.getValue(NORTH).isConnected();
        boolean south = state.getValue(SOUTH).isConnected();
        boolean east = state.getValue(EAST).isConnected();
        boolean west = state.getValue(WEST).isConnected();
        boolean northSouthEmpty = !north && !south;
        boolean bl = eastWestEmpty = !east && !west;
        if (!west && northSouthEmpty) {
            state = (BlockState)state.setValue(WEST, RedstoneSide.SIDE);
        }
        if (!east && northSouthEmpty) {
            state = (BlockState)state.setValue(EAST, RedstoneSide.SIDE);
        }
        if (!north && eastWestEmpty) {
            state = (BlockState)state.setValue(NORTH, RedstoneSide.SIDE);
        }
        if (!south && eastWestEmpty) {
            state = (BlockState)state.setValue(SOUTH, RedstoneSide.SIDE);
        }
        return state;
    }

    private BlockState getMissingConnections(BlockGetter level, BlockState state, BlockPos pos) {
        boolean canConnectUp = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (((RedstoneSide)state.getValue(PROPERTY_BY_DIRECTION.get(direction))).isConnected()) continue;
            RedstoneSide sideConnection = this.getConnectingSide(level, pos, direction, canConnectUp);
            state = (BlockState)state.setValue(PROPERTY_BY_DIRECTION.get(direction), sideConnection);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.DOWN) {
            if (!this.canSurviveOn(level, neighbourPos, neighbourState)) {
                return Blocks.AIR.defaultBlockState();
            }
            return state;
        }
        if (directionToNeighbour == Direction.UP) {
            return this.getConnectionState(level, state, pos);
        }
        RedstoneSide sideConnection = this.getConnectingSide(level, pos, directionToNeighbour);
        if (sideConnection.isConnected() == ((RedstoneSide)state.getValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour))).isConnected() && !RedStoneWireBlock.isCross(state)) {
            return (BlockState)state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), sideConnection);
        }
        return this.getConnectionState(level, (BlockState)((BlockState)this.crossState.setValue(POWER, state.getValue(POWER))).setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), sideConnection), pos);
    }

    private static boolean isCross(BlockState state) {
        return state.getValue(NORTH).isConnected() && state.getValue(SOUTH).isConnected() && state.getValue(EAST).isConnected() && state.getValue(WEST).isConnected();
    }

    private static boolean isDot(BlockState state) {
        return !state.getValue(NORTH).isConnected() && !state.getValue(SOUTH).isConnected() && !state.getValue(EAST).isConnected() && !state.getValue(WEST).isConnected();
    }

    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, @Block.UpdateFlags int updateFlags, int updateLimit) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide value = (RedstoneSide)state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (value == RedstoneSide.NONE || level.getBlockState(blockPos.setWithOffset((Vec3i)pos, direction)).is(this)) continue;
            blockPos.move(Direction.DOWN);
            BlockState blockStateDown = level.getBlockState(blockPos);
            if (blockStateDown.is(this)) {
                Vec3i neighborPos = blockPos.relative(direction.getOpposite());
                level.neighborShapeChanged(direction.getOpposite(), blockPos, (BlockPos)neighborPos, level.getBlockState((BlockPos)neighborPos), updateFlags, updateLimit);
            }
            blockPos.setWithOffset((Vec3i)pos, direction).move(Direction.UP);
            BlockState blockStateUp = level.getBlockState(blockPos);
            if (!blockStateUp.is(this)) continue;
            Vec3i neighborPos = blockPos.relative(direction.getOpposite());
            level.neighborShapeChanged(direction.getOpposite(), blockPos, (BlockPos)neighborPos, level.getBlockState((BlockPos)neighborPos), updateFlags, updateLimit);
        }
    }

    private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction) {
        return this.getConnectingSide(level, pos, direction, !level.getBlockState(pos.above()).isRedstoneConductor(level, pos));
    }

    private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction, boolean canConnectUp) {
        BlockPos relativePos = pos.relative(direction);
        BlockState relativeState = level.getBlockState(relativePos);
        if (canConnectUp) {
            boolean isPlaceableAbove;
            boolean bl = isPlaceableAbove = relativeState.getBlock() instanceof TrapDoorBlock || this.canSurviveOn(level, relativePos, relativeState);
            if (isPlaceableAbove && RedStoneWireBlock.shouldConnectTo(level.getBlockState(relativePos.above()))) {
                if (relativeState.isFaceSturdy(level, relativePos, direction.getOpposite())) {
                    return RedstoneSide.UP;
                }
                return RedstoneSide.SIDE;
            }
        }
        if (RedStoneWireBlock.shouldConnectTo(relativeState, direction) || !relativeState.isRedstoneConductor(level, relativePos) && RedStoneWireBlock.shouldConnectTo(level.getBlockState(relativePos.below()))) {
            return RedstoneSide.SIDE;
        }
        return RedstoneSide.NONE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return this.canSurviveOn(level, below, belowState);
    }

    private boolean canSurviveOn(BlockGetter level, BlockPos relativePos, BlockState relativeState) {
        return relativeState.isFaceSturdy(level, relativePos, Direction.UP) || relativeState.is(Blocks.HOPPER);
    }

    private void updatePowerStrength(Level level, BlockPos pos, BlockState state, @Nullable Orientation orientation, boolean shapeUpdateWiresAroundInitialPosition) {
        if (RedStoneWireBlock.useExperimentalEvaluator(level)) {
            new ExperimentalRedstoneWireEvaluator(this).updatePowerStrength(level, pos, state, orientation, shapeUpdateWiresAroundInitialPosition);
        } else {
            this.evaluator.updatePowerStrength(level, pos, state, orientation, shapeUpdateWiresAroundInitialPosition);
        }
    }

    public int getBlockSignal(Level level, BlockPos pos) {
        this.shouldSignal = false;
        int blockSignal = level.getBestNeighborSignal(pos);
        this.shouldSignal = true;
        return blockSignal;
    }

    private void checkCornerChangeAt(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        level.updateNeighborsAt(pos, this);
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock()) || level.isClientSide()) {
            return;
        }
        this.updatePowerStrength(level, pos, state, null, true);
        for (Direction direction : Direction.Plane.VERTICAL) {
            level.updateNeighborsAt(pos.relative(direction), this);
        }
        this.updateNeighborsOfNeighboringWires(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (movedByPiston) {
            return;
        }
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
        }
        this.updatePowerStrength(level, pos, state, null, false);
        this.updateNeighborsOfNeighboringWires(level, pos);
    }

    private void updateNeighborsOfNeighboringWires(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            this.checkCornerChangeAt(level, pos.relative(direction));
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos target = pos.relative(direction);
            if (level.getBlockState(target).isRedstoneConductor(level, target)) {
                this.checkCornerChangeAt(level, target.above());
                continue;
            }
            this.checkCornerChangeAt(level, target.below());
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        if (block == this && RedStoneWireBlock.useExperimentalEvaluator(level)) {
            return;
        }
        if (state.canSurvive(level, pos)) {
            this.updatePowerStrength(level, pos, state, orientation, false);
        } else {
            RedStoneWireBlock.dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    private static boolean useExperimentalEvaluator(Level level) {
        return level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!this.shouldSignal) {
            return 0;
        }
        return state.getSignal(level, pos, direction);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!this.shouldSignal || direction == Direction.DOWN) {
            return 0;
        }
        int power = this.ownSignal(state, level, pos);
        if (power == 0) {
            return 0;
        }
        if (direction == Direction.UP || ((RedstoneSide)this.getConnectionState(level, state, pos).getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite()))).isConnected()) {
            return power;
        }
        return 0;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWER);
    }

    protected static boolean shouldConnectTo(BlockState blockState) {
        return RedStoneWireBlock.shouldConnectTo(blockState, null);
    }

    protected static boolean shouldConnectTo(BlockState blockState, @Nullable Direction direction) {
        if (blockState.is(Blocks.REDSTONE_WIRE)) {
            return true;
        }
        if (blockState.is(Blocks.REPEATER)) {
            Direction repeaterDirection = (Direction)blockState.getValue(RepeaterBlock.FACING);
            return repeaterDirection == direction || repeaterDirection.getOpposite() == direction;
        }
        if (blockState.is(Blocks.OBSERVER)) {
            return direction == blockState.getValue(ObserverBlock.FACING);
        }
        return blockState.isSignalSource() && direction != null;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return this.shouldSignal;
    }

    public static int getColorForPower(int power) {
        return COLORS[power];
    }

    private static void spawnParticlesAlongLine(Level level, RandomSource random, BlockPos pos, int color, Direction side, Direction along, float from, float to) {
        float span = to - from;
        if (random.nextFloat() >= 0.2f * span) {
            return;
        }
        float sideOfBlock = 0.4375f;
        float positionOnLine = from + span * random.nextFloat();
        double x = 0.5 + (double)(0.4375f * (float)side.getStepX()) + (double)(positionOnLine * (float)along.getStepX());
        double y = 0.5 + (double)(0.4375f * (float)side.getStepY()) + (double)(positionOnLine * (float)along.getStepY());
        double z = 0.5 + (double)(0.4375f * (float)side.getStepZ()) + (double)(positionOnLine * (float)along.getStepZ());
        level.addParticle(new DustParticleOptions(color, 1.0f), (double)pos.getX() + x, (double)pos.getY() + y, (double)pos.getZ() + z, 0.0, 0.0, 0.0);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int power = state.getValue(POWER);
        if (power == 0) {
            return;
        }
        block4: for (Direction horizontal : Direction.Plane.HORIZONTAL) {
            RedstoneSide connection = (RedstoneSide)state.getValue(PROPERTY_BY_DIRECTION.get(horizontal));
            switch (connection) {
                case UP: {
                    RedStoneWireBlock.spawnParticlesAlongLine(level, random, pos, COLORS[power], horizontal, Direction.UP, -0.5f, 0.5f);
                }
                case SIDE: {
                    RedStoneWireBlock.spawnParticlesAlongLine(level, random, pos, COLORS[power], Direction.DOWN, horizontal, 0.0f, 0.5f);
                    continue block4;
                }
            }
            RedStoneWireBlock.spawnParticlesAlongLine(level, random, pos, COLORS[power], Direction.DOWN, horizontal, 0.0f, 0.3f);
        }
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }
        if (RedStoneWireBlock.isCross(state) || RedStoneWireBlock.isDot(state)) {
            BlockState newState = RedStoneWireBlock.isCross(state) ? this.defaultBlockState() : this.crossState;
            newState = (BlockState)newState.setValue(POWER, state.getValue(POWER));
            if ((newState = this.getConnectionState(level, newState, pos)) != state) {
                level.setBlock(pos, newState, 3);
                this.updatesOnShapeChange(level, pos, state, newState);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private void updatesOnShapeChange(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, null, Direction.UP);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos relativePos = pos.relative(direction);
            if (((RedstoneSide)oldState.getValue(PROPERTY_BY_DIRECTION.get(direction))).isConnected() == ((RedstoneSide)newState.getValue(PROPERTY_BY_DIRECTION.get(direction))).isConnected() || !level.getBlockState(relativePos).isRedstoneConductor(level, relativePos)) continue;
            level.updateNeighborsAtExceptFromFacing(relativePos, newState.getBlock(), direction.getOpposite(), ExperimentalRedstoneUtils.withFront(orientation, direction));
        }
    }
}

