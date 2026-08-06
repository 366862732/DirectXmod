/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FrogspawnBlock
extends Block {
    public static final MapCodec<FrogspawnBlock> CODEC = FrogspawnBlock.simpleCodec(FrogspawnBlock::new);
    private static final int MIN_TADPOLES_SPAWN = 2;
    private static final int MAX_TADPOLES_SPAWN = 5;
    private static final int DEFAULT_MIN_HATCH_TICK_DELAY = 3600;
    private static final int DEFAULT_MAX_HATCH_TICK_DELAY = 12000;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.5);
    private static int minHatchTickDelay = 3600;
    private static int maxHatchTickDelay = 12000;

    public MapCodec<FrogspawnBlock> codec() {
        return CODEC;
    }

    public FrogspawnBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return FrogspawnBlock.mayPlaceOn(level, pos.below());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, FrogspawnBlock.getFrogspawnHatchDelay(level.getRandom()));
    }

    private static int getFrogspawnHatchDelay(RandomSource random) {
        return random.nextInt(minHatchTickDelay, maxHatchTickDelay);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.canSurvive(state, level, pos)) {
            this.destroyBlock(level, pos);
            return;
        }
        this.hatchFrogspawn(level, pos, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.is(EntityTypes.FALLING_BLOCK)) {
            this.destroyBlock(level, pos);
        }
    }

    private static boolean mayPlaceOn(BlockGetter level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        FluidState fluidAbove = level.getFluidState(pos.above());
        return (fluidState.is(FluidTags.SUPPORTS_FROGSPAWN) || level.getBlockState(pos).is(BlockTags.SUPPORTS_FROGSPAWN)) && fluidAbove.is(Fluids.EMPTY);
    }

    private void hatchFrogspawn(ServerLevel level, BlockPos pos, RandomSource random) {
        this.destroyBlock(level, pos);
        level.playSound(null, pos, SoundEvents.FROGSPAWN_HATCH, SoundSource.BLOCKS, 1.0f, 1.0f);
        this.spawnTadpoles(level, pos, random);
    }

    private void destroyBlock(Level level, BlockPos pos) {
        level.destroyBlock(pos, false);
    }

    private void spawnTadpoles(ServerLevel level, BlockPos pos, RandomSource random) {
        int tadpoleAmount = random.nextInt(2, 6);
        for (int i = 1; i <= tadpoleAmount; ++i) {
            Tadpole tadpole = EntityTypes.TADPOLE.create((Level)level, EntitySpawnReason.BREEDING);
            if (tadpole == null) continue;
            double xPos = (double)pos.getX() + this.getRandomTadpolePositionOffset(random);
            double zPos = (double)pos.getZ() + this.getRandomTadpolePositionOffset(random);
            int yRot = random.nextInt(1, 361);
            tadpole.snapTo(xPos, (double)pos.getY() - 0.5, zPos, yRot, 0.0f);
            tadpole.setPersistenceRequired();
            level.addFreshEntity(tadpole);
        }
    }

    private double getRandomTadpolePositionOffset(RandomSource random) {
        double tadpoleHitboxCenter = 0.2f;
        return Mth.clamp(random.nextDouble(), (double)0.2f, 0.7999999970197678);
    }

    @VisibleForTesting
    public static void setHatchDelay(int minDelay, int maxDelay) {
        minHatchTickDelay = minDelay;
        maxHatchTickDelay = maxDelay;
    }

    @VisibleForTesting
    public static void setDefaultHatchDelay() {
        minHatchTickDelay = 3600;
        maxHatchTickDelay = 12000;
    }
}

