/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TripWireHookBlock
extends Block {
    public static final MapCodec<TripWireHookBlock> CODEC = TripWireHookBlock.simpleCodec(TripWireHookBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    protected static final int WIRE_DIST_MIN = 1;
    protected static final int WIRE_DIST_MAX = 42;
    private static final int RECHECK_PERIOD = 10;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, 10.0, 10.0, 16.0));

    public MapCodec<TripWireHookBlock> codec() {
        return CODEC;
    }

    public TripWireHookBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false)).setValue(ATTACHED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos relative = pos.relative(direction.getOpposite());
        BlockState blockState = level.getBlockState(relative);
        return direction.getAxis().isHorizontal() && blockState.isFaceSturdy(level, relative, direction);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction[] directions;
        BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(ATTACHED, false);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction direction : directions = context.getNearestLookingDirections()) {
            Direction facing;
            if (!direction.getAxis().isHorizontal() || !(state = (BlockState)state.setValue(FACING, facing = direction.getOpposite())).canSurvive(level, pos)) continue;
            return state;
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        TripWireHookBlock.calculateState(level, pos, state, false, false, -1, null);
    }

    public static void calculateState(Level level, BlockPos pos, BlockState state, boolean isBeingDestroyed, boolean canUpdate, int wireSource, @Nullable BlockState wireSourceState) {
        BlockPos testPos;
        Optional<Direction> facingOptional = state.getOptionalValue(FACING);
        if (!facingOptional.isPresent()) {
            return;
        }
        Direction direction = facingOptional.get();
        boolean wasAttached = state.getOptionalValue(ATTACHED).orElse(false);
        boolean wasPowered = state.getOptionalValue(POWERED).orElse(false);
        Block block = state.getBlock();
        boolean attached = !isBeingDestroyed;
        boolean powered = false;
        int receiverPos = 0;
        BlockState[] wireStates = new BlockState[42];
        for (int i = 1; i < 42; ++i) {
            testPos = pos.relative(direction, i);
            BlockState wireState = level.getBlockState(testPos);
            if (wireState.is(Blocks.TRIPWIRE_HOOK)) {
                if (wireState.getValue(FACING) != direction.getOpposite()) break;
                receiverPos = i;
                break;
            }
            if (wireState.is(Blocks.TRIPWIRE) || i == wireSource) {
                if (i == wireSource) {
                    wireState = (BlockState)MoreObjects.firstNonNull((Object)wireSourceState, (Object)wireState);
                }
                boolean wireArmed = wireState.getValue(TripWireBlock.DISARMED) == false;
                boolean wirePowered = wireState.getValue(TripWireBlock.POWERED);
                powered |= wireArmed && wirePowered;
                wireStates[i] = wireState;
                if (i != wireSource) continue;
                level.scheduleTick(pos, block, 10);
                attached &= wireArmed;
                continue;
            }
            wireStates[i] = null;
            attached = false;
        }
        BlockState newState = (BlockState)((BlockState)block.defaultBlockState().trySetValue(ATTACHED, attached)).trySetValue(POWERED, powered &= (attached &= receiverPos > 1));
        if (receiverPos > 0) {
            testPos = pos.relative(direction, receiverPos);
            Direction opposite = direction.getOpposite();
            level.setBlock(testPos, (BlockState)newState.setValue(FACING, opposite), 3);
            TripWireHookBlock.notifyNeighbors(block, level, testPos, opposite);
            if (!level.getBlockState(pos).is(Blocks.TRIPWIRE_HOOK)) {
                TripWireHookBlock.onRemoved(newState, level, pos);
                return;
            }
            TripWireHookBlock.emitState(level, testPos, attached, powered, wasAttached, wasPowered);
        }
        TripWireHookBlock.emitState(level, pos, attached, powered, wasAttached, wasPowered);
        if (!isBeingDestroyed) {
            level.setBlock(pos, (BlockState)newState.setValue(FACING, direction), 3);
            if (canUpdate) {
                TripWireHookBlock.notifyNeighbors(block, level, pos, direction);
            }
        }
        if (wasAttached != attached) {
            for (int i = 1; i < receiverPos; ++i) {
                BlockState testPosState;
                BlockPos testPos2 = pos.relative(direction, i);
                BlockState wireData = wireStates[i];
                if (wireData == null || !(testPosState = level.getBlockState(testPos2)).is(Blocks.TRIPWIRE) && !testPosState.is(Blocks.TRIPWIRE_HOOK)) continue;
                level.setBlock(testPos2, (BlockState)wireData.trySetValue(ATTACHED, attached), 3);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TripWireHookBlock.calculateState(level, pos, state, false, true, -1, null);
    }

    private static void emitState(Level level, BlockPos pos, boolean attached, boolean powered, boolean wasAttached, boolean wasPowered) {
        if (powered && !wasPowered) {
            level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4f, 0.6f);
            level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
        } else if (!powered && wasPowered) {
            level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4f, 0.5f);
            level.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pos);
        } else if (attached && !wasAttached) {
            level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4f, 0.7f);
            level.gameEvent(null, GameEvent.BLOCK_ATTACH, pos);
        } else if (!attached && wasAttached) {
            level.playSound(null, pos, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4f, 1.2f / (level.getRandom().nextFloat() * 0.2f + 0.9f));
            level.gameEvent(null, GameEvent.BLOCK_DETACH, pos);
        }
    }

    private static void notifyNeighbors(Block block, Level level, BlockPos pos, Direction direction) {
        Direction front = direction.getOpposite();
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, front, Direction.UP);
        level.updateNeighborsAt(pos, block, orientation);
        level.updateNeighborsAt(pos.relative(front), block, orientation);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (movedByPiston) {
            return;
        }
        TripWireHookBlock.onRemoved(state, level, pos);
    }

    private static void onRemoved(BlockState state, Level level, BlockPos pos) {
        boolean attached = state.getValue(ATTACHED);
        boolean powered = state.getValue(POWERED);
        if (attached || powered) {
            TripWireHookBlock.calculateState(level, pos, state, true, false, -1, null);
        }
        if (powered) {
            TripWireHookBlock.notifyNeighbors(state.getBlock(), level, pos, state.getValue(FACING));
        }
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWERED) != false ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(POWERED).booleanValue()) {
            return 0;
        }
        if (state.getValue(FACING) == direction) {
            return 15;
        }
        return 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ATTACHED);
    }
}

