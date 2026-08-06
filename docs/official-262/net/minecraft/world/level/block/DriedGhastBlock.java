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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTicks;
import org.jspecify.annotations.Nullable;

public class DriedGhastBlock
extends HorizontalDirectionalBlock
implements SimpleWaterloggedBlock {
    public static final MapCodec<DriedGhastBlock> CODEC = DriedGhastBlock.simpleCodec(DriedGhastBlock::new);
    public static final int MAX_HYDRATION_LEVEL = 3;
    public static final IntegerProperty HYDRATION_LEVEL = BlockStateProperties.DRIED_GHAST_HYDRATION_LEVELS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int HYDRATION_TICK_DELAY = 5000;
    private static final VoxelShape SHAPE = Block.column(10.0, 10.0, 0.0, 10.0);

    public MapCodec<DriedGhastBlock> codec() {
        return CODEC;
    }

    public DriedGhastBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(HYDRATION_LEVEL, 0)).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HYDRATION_LEVEL, WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public int getHydrationLevel(BlockState state) {
        return state.getValue(HYDRATION_LEVEL);
    }

    private boolean isReadyToSpawn(BlockState state) {
        return this.getHydrationLevel(state) == 3;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            this.tickWaterlogged(state, level, position, random);
            return;
        }
        int hydrationLevel = this.getHydrationLevel(state);
        if (hydrationLevel > 0) {
            level.setBlock(position, (BlockState)state.setValue(HYDRATION_LEVEL, hydrationLevel - 1), 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Context.of(state));
        }
    }

    private void tickWaterlogged(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        if (!this.isReadyToSpawn(state)) {
            level.playSound(null, position, SoundEvents.DRIED_GHAST_TRANSITION, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.setBlock(position, (BlockState)state.setValue(HYDRATION_LEVEL, this.getHydrationLevel(state) + 1), 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Context.of(state));
        } else {
            this.spawnGhastling(level, position, state);
        }
    }

    private void spawnGhastling(ServerLevel level, BlockPos position, BlockState state) {
        level.removeBlock(position, false);
        HappyGhast ghastling = EntityTypes.HAPPY_GHAST.create((Level)level, EntitySpawnReason.BREEDING);
        if (ghastling != null) {
            Vec3 spawnAt = Vec3.atBottomCenterOf(position);
            ghastling.setBaby(true);
            float blockRotation = Direction.getYRot((Direction)state.getValue(FACING));
            ghastling.setYHeadRot(blockRotation);
            ghastling.snapTo(spawnAt.x(), spawnAt.y(), spawnAt.z(), blockRotation, 0.0f);
            level.addFreshEntity(ghastling);
            level.playSound(null, ghastling, SoundEvents.GHASTLING_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = (double)pos.getX() + 0.5;
        double y = (double)pos.getY() + 0.5;
        double z = (double)pos.getZ() + 0.5;
        if (!state.getValue(WATERLOGGED).booleanValue()) {
            if (random.nextInt(40) == 0 && level.getBlockState(pos.below()).is(BlockTags.TRIGGERS_AMBIENT_DRIED_GHAST_BLOCK_SOUNDS)) {
                level.playLocalSound(x, y, z, SoundEvents.DRIED_GHAST_AMBIENT, SoundSource.BLOCKS, 1.0f, 1.0f, false);
            }
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0, 0.02, 0.0);
            }
        } else {
            if (random.nextInt(40) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.DRIED_GHAST_AMBIENT_WATER, SoundSource.BLOCKS, 1.0f, 1.0f, false);
            }
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x + (double)((random.nextFloat() * 2.0f - 1.0f) / 3.0f), y + 0.4, z + (double)((random.nextFloat() * 2.0f - 1.0f) / 3.0f), 0.0, random.nextFloat(), 0.0);
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if ((state.getValue(WATERLOGGED).booleanValue() || state.getValue(HYDRATION_LEVEL) > 0) && !((LevelTicks)level.getBlockTicks()).hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 5000);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean isWaterSource = replacedFluidState.is(Fluids.WATER);
        return (BlockState)((BlockState)super.getStateForPlacement(context).setValue(WATERLOGGED, isWaterSource)).setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (state.getValue(BlockStateProperties.WATERLOGGED).booleanValue() || !fluidState.is(Fluids.WATER)) {
            return false;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            level.playSound(null, pos, SoundEvents.DRIED_GHAST_PLACE_IN_WATER, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        level.playSound(null, pos, state.getValue(WATERLOGGED) != false ? SoundEvents.DRIED_GHAST_PLACE_IN_WATER : SoundEvents.DRIED_GHAST_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}

