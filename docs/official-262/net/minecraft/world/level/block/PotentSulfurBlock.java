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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class PotentSulfurBlock
extends BaseEntityBlock {
    public static final int ALLOWED_WATER_BLOCKS_ABOVE = 4;
    public static final MapCodec<PotentSulfurBlock> CODEC = PotentSulfurBlock.simpleCodec(PotentSulfurBlock::new);
    public static final EnumProperty<PotentSulfurState> STATE = BlockStateProperties.POTENT_SULFUR_STATE;

    public MapCodec<PotentSulfurBlock> codec() {
        return CODEC;
    }

    public PotentSulfurBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue(STATE, PotentSulfurState.DRY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new PotentSulfurBlockEntity(worldPosition, blockState);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return PotentSulfurBlock.validBlockState(state, level, pos);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return PotentSulfurBlock.validBlockState(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    private static BlockState validBlockState(BlockState state, LevelReader level, BlockPos pos) {
        if (!level.getFluidState(pos.above()).isSourceOfType(Fluids.WATER)) {
            return (BlockState)state.setValue(STATE, PotentSulfurState.DRY);
        }
        BlockState belowState = level.getBlockState(pos.below());
        if (belowState.is(BlockTags.CAUSES_CONTINUOUS_GEYSER_ERUPTIONS) && PotentSulfurBlock.isSourceIfFluid(belowState)) {
            return (BlockState)state.setValue(STATE, PotentSulfurState.CONTINUOUS);
        }
        if (belowState.is(BlockTags.CAUSES_PERIODIC_GEYSER_ERUPTIONS) && PotentSulfurBlock.isSourceIfFluid(belowState)) {
            BlockEntity blockEntity;
            boolean isGeyser;
            boolean bl = isGeyser = state.getValue(STATE) == PotentSulfurState.ERUPTING || state.getValue(STATE) == PotentSulfurState.DORMANT;
            if (!isGeyser && (blockEntity = level.getBlockEntity(pos)) instanceof PotentSulfurBlockEntity) {
                PotentSulfurBlockEntity potentSulfurEntity = (PotentSulfurBlockEntity)blockEntity;
                potentSulfurEntity.resetCountdown();
            }
            if (state.getValue(STATE) == PotentSulfurState.ERUPTING) {
                return state;
            }
            return (BlockState)state.setValue(STATE, PotentSulfurState.DORMANT);
        }
        return (BlockState)state.setValue(STATE, PotentSulfurState.WET);
    }

    private static boolean isSourceIfFluid(BlockState belowState) {
        FluidState fluidState = belowState.getFluidState();
        return fluidState.isEmpty() || fluidState.isSource();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (state.getValue(STATE) == PotentSulfurState.ERUPTING || state.getValue(STATE) == PotentSulfurState.CONTINUOUS) {
            level.blockEvent(pos, this, 0, 0);
            level.playSound(null, pos, state.getValue(STATE) == PotentSulfurState.CONTINUOUS ? SoundEvents.GEYSER_CONTINUOUS_START : SoundEvents.GEYSER_ERUPTION_START, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(state));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STATE) == PotentSulfurState.DRY) {
            return;
        }
        if (!level.getFluidState(pos.above()).isSourceOfType(Fluids.WATER)) {
            return;
        }
        PotentSulfurBlock.spawnBubbleParticlesAt(level, random, pos.getX(), pos.getY() + 1, pos.getZ());
        PotentSulfurBlock.spawnBubbleParticlesAt(level, random, pos.getX(), pos.getY() + 1, pos.getZ());
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.NOXIOUS_GAS, SoundSource.AMBIENT, 1.0f, 1.0f, false);
        }
    }

    private static void spawnBubbleParticlesAt(Level level, RandomSource random, double x, double y, double z) {
        level.addAlwaysVisibleParticle(ParticleTypes.SULFUR_BUBBLES, x + (double)random.nextFloat(), y + (double)random.nextFloat(), z + (double)random.nextFloat(), 0.0, 0.0, 0.0);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int b0, int b1) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PotentSulfurBlockEntity) {
            PotentSulfurBlockEntity entity = (PotentSulfurBlockEntity)blockEntity;
            entity.eruptionTick = level.getGameTime();
        }
        return true;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        boolean client = level.isClientSide();
        return PotentSulfurBlock.createTickerHelper(type, BlockEntityTypes.POTENT_SULFUR, switch (blockState.getValue(STATE)) {
            default -> throw new MatchException(null, null);
            case PotentSulfurState.DRY -> null;
            case PotentSulfurState.WET -> {
                if (client) {
                    yield PotentSulfurBlockEntity.CLIENT_NOXIOUS_GAS_TICKER;
                }
                yield PotentSulfurBlockEntity.SERVER_NAUSEA_EFFECT_TICKER;
            }
            case PotentSulfurState.DORMANT -> {
                if (client) {
                    yield PotentSulfurBlockEntity.CLIENT_NOXIOUS_GAS_TICKER;
                }
                yield PotentSulfurBlockEntity.SERVER_WAITING_COUNTDOWN_TICKER.andThen(PotentSulfurBlockEntity.SERVER_NAUSEA_EFFECT_TICKER);
            }
            case PotentSulfurState.ERUPTING -> {
                if (client) {
                    yield PotentSulfurBlockEntity.CLIENT_GEYSER_PLUME_TICKER.apply(SoundEvents.GEYSER_ERUPTION_ACTIVE).andThen(PotentSulfurBlockEntity.LAUNCH_ENTITY_TICKER);
                }
                yield PotentSulfurBlockEntity.LAUNCH_ENTITY_TICKER.andThen(PotentSulfurBlockEntity.SERVER_WAITING_COUNTDOWN_TICKER);
            }
            case PotentSulfurState.CONTINUOUS -> client ? PotentSulfurBlockEntity.CLIENT_GEYSER_PLUME_TICKER.apply(SoundEvents.GEYSER_CONTINUOUS_ACTIVE).andThen(PotentSulfurBlockEntity.LAUNCH_ENTITY_TICKER) : PotentSulfurBlockEntity.LAUNCH_ENTITY_TICKER;
        });
    }
}

