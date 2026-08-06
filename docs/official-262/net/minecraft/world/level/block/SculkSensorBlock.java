/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SculkSensorBlock
extends BaseEntityBlock
implements SimpleWaterloggedBlock {
    public static final MapCodec<SculkSensorBlock> CODEC = SculkSensorBlock.simpleCodec(SculkSensorBlock::new);
    public static final int ACTIVE_TICKS = 30;
    public static final int COOLDOWN_TICKS = 10;
    public static final EnumProperty<SculkSensorPhase> PHASE = BlockStateProperties.SCULK_SENSOR_PHASE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 8.0);
    private static final float[] RESONANCE_PITCH_BEND = Util.make(new float[16], arr -> {
        int[] toneMap = new int[]{0, 0, 2, 4, 6, 7, 9, 10, 12, 14, 15, 18, 19, 21, 22, 24};
        for (int i = 0; i < 16; ++i) {
            arr[i] = NoteBlock.getPitchFromNote(toneMap[i]);
        }
    });

    public MapCodec<? extends SculkSensorBlock> codec() {
        return CODEC;
    }

    public SculkSensorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PHASE, SculkSensorPhase.INACTIVE)).setValue(POWER, 0)).setValue(WATERLOGGED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        FluidState replacedFluidState = context.getLevel().getFluidState(pos);
        return (BlockState)this.defaultBlockState().setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (SculkSensorBlock.getPhase(state) != SculkSensorPhase.ACTIVE) {
            if (SculkSensorBlock.getPhase(state) == SculkSensorPhase.COOLDOWN) {
                level.setBlock(pos, (BlockState)state.setValue(PHASE, SculkSensorPhase.INACTIVE), 3);
                if (!state.getValue(WATERLOGGED).booleanValue()) {
                    level.playSound(null, pos, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.8f);
                }
            }
            return;
        }
        SculkSensorBlock.deactivate(level, pos, state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        BlockEntity blockEntity;
        if (!level.isClientSide() && SculkSensorBlock.canActivate(onState) && !entity.is(EntityTypes.WARDEN) && (blockEntity = level.getBlockEntity(pos)) instanceof SculkSensorBlockEntity) {
            SculkSensorBlockEntity sculkSensor = (SculkSensorBlockEntity)blockEntity;
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                if (sculkSensor.getVibrationUser().canReceiveVibration(serverLevel, pos, GameEvent.STEP, GameEvent.Context.of(onState))) {
                    sculkSensor.getListener().forceScheduleVibration(serverLevel, GameEvent.STEP, GameEvent.Context.of(entity), entity.position());
                }
            }
        }
        super.stepOn(level, pos, onState, entity);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide() || state.is(oldState.getBlock())) {
            return;
        }
        if (state.getValue(POWER) > 0 && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.setBlock(pos, (BlockState)state.setValue(POWER, 0), 18);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (SculkSensorBlock.getPhase(state) == SculkSensorPhase.ACTIVE) {
            SculkSensorBlock.updateNeighbours(level, pos, state);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    private static void updateNeighbours(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        level.updateNeighborsAt(pos, block);
        level.updateNeighborsAt(pos.below(), block);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new SculkSensorBlockEntity(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return SculkSensorBlock.createTickerHelper(type, BlockEntityTypes.SCULK_SENSOR, (innerLevel, pos, state, entity) -> VibrationSystem.Ticker.tick(innerLevel, entity.getVibrationData(), entity.getVibrationUser()));
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWER);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.UP) {
            return state.getSignal(level, pos, direction);
        }
        return 0;
    }

    public static SculkSensorPhase getPhase(BlockState state) {
        return state.getValue(PHASE);
    }

    public static boolean canActivate(BlockState state) {
        return SculkSensorBlock.getPhase(state) == SculkSensorPhase.INACTIVE;
    }

    public static void deactivate(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, (BlockState)((BlockState)state.setValue(PHASE, SculkSensorPhase.COOLDOWN)).setValue(POWER, 0), 3);
        level.scheduleTick(pos, state.getBlock(), 10);
        SculkSensorBlock.updateNeighbours(level, pos, state);
    }

    @VisibleForTesting
    public int getActiveTicks() {
        return 30;
    }

    public void activate(@Nullable Entity sourceEntity, Level level, BlockPos pos, BlockState state, int calculatedPower, int vibrationFrequency) {
        level.setBlock(pos, (BlockState)((BlockState)state.setValue(PHASE, SculkSensorPhase.ACTIVE)).setValue(POWER, calculatedPower), 3);
        level.scheduleTick(pos, state.getBlock(), this.getActiveTicks());
        SculkSensorBlock.updateNeighbours(level, pos, state);
        SculkSensorBlock.tryResonateVibration(sourceEntity, level, pos, vibrationFrequency);
        level.gameEvent(sourceEntity, GameEvent.SCULK_SENSOR_TENDRILS_CLICKING, pos);
        if (!state.getValue(WATERLOGGED).booleanValue()) {
            level.playSound(null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.8f);
        }
    }

    public static void tryResonateVibration(@Nullable Entity sourceEntity, Level level, BlockPos pos, int vibrationFrequency) {
        for (Direction direction : Direction.values()) {
            BlockPos relativePos = pos.relative(direction);
            BlockState blockState = level.getBlockState(relativePos);
            if (!blockState.is(BlockTags.VIBRATION_RESONATORS)) continue;
            level.gameEvent(VibrationSystem.getResonanceEventByFrequency(vibrationFrequency), relativePos, GameEvent.Context.of(sourceEntity, blockState));
            float pitch = RESONANCE_PITCH_BEND[vibrationFrequency];
            level.playSound(null, relativePos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, pitch);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (SculkSensorBlock.getPhase(state) != SculkSensorPhase.ACTIVE) {
            return;
        }
        Direction dir = Direction.getRandom(random);
        if (dir == Direction.UP || dir == Direction.DOWN) {
            return;
        }
        double x = (double)pos.getX() + 0.5 + (dir.getStepX() == 0 ? 0.5 - random.nextDouble() : (double)dir.getStepX() * 0.6);
        double y = (double)pos.getY() + 0.25;
        double z = (double)pos.getZ() + 0.5 + (dir.getStepZ() == 0 ? 0.5 - random.nextDouble() : (double)dir.getStepZ() * 0.6);
        double ya = (double)random.nextFloat() * 0.04;
        level.addParticle(DustColorTransitionOptions.SCULK_TO_REDSTONE, x, y, z, 0.0, ya, 0.0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE, POWER, WATERLOGGED);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SculkSensorBlockEntity) {
            SculkSensorBlockEntity sculk = (SculkSensorBlockEntity)entity;
            return SculkSensorBlock.getPhase(state) == SculkSensorPhase.ACTIVE ? sculk.getLastVibrationFrequency() : 0;
        }
        return 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (dropExperience) {
            this.tryDropExperience(level, pos, tool, ConstantInt.of(5));
        }
    }
}

