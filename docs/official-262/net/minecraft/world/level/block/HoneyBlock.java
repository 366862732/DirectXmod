/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HoneyBlock
extends HalfTransparentBlock {
    public static final MapCodec<HoneyBlock> CODEC = HoneyBlock.simpleCodec(HoneyBlock::new);
    private static final double SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST = 0.13;
    private static final double MIN_FALL_SPEED_TO_BE_CONSIDERED_SLIDING = 0.08;
    private static final double THROTTLE_SLIDE_SPEED_TO = 0.05;
    private static final int SLIDE_ADVANCEMENT_CHECK_INTERVAL = 20;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 15.0);

    public MapCodec<HoneyBlock> codec() {
        return CODEC;
    }

    public HoneyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static boolean doesEntityDoHoneyBlockSlideEffects(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof AbstractMinecart || entity instanceof PrimedTnt || entity instanceof AbstractBoat;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0f, 1.0f);
        if (!level.isClientSide()) {
            level.broadcastEntityEvent(entity, (byte)54);
        }
        if (entity.causeFallDamage(fallDistance, 0.2f, level.damageSources().fall())) {
            entity.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5f, this.soundType.getPitch() * 0.75f);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (this.isSlidingDown(pos, entity)) {
            this.maybeDoSlideAchievement(entity, pos);
            this.doSlideMovement(entity);
            this.maybeDoSlideEffects(level, entity);
        }
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }

    private static double getOldDeltaY(double deltaY) {
        return deltaY / (double)0.98f + 0.08;
    }

    private static double getNewDeltaY(double deltaY) {
        return (deltaY - 0.08) * (double)0.98f;
    }

    private boolean isSlidingDown(BlockPos pos, Entity entity) {
        if (entity.onGround()) {
            return false;
        }
        if (entity.getY() > (double)pos.getY() + 0.9375 - 1.0E-7) {
            return false;
        }
        if (HoneyBlock.getOldDeltaY(entity.getDeltaMovement().y) >= -0.08) {
            return false;
        }
        double dx = Math.abs((double)pos.getX() + 0.5 - entity.getX());
        double dz = Math.abs((double)pos.getZ() + 0.5 - entity.getZ());
        double overlapDistance = 0.4375 + (double)(entity.getBbWidth() / 2.0f);
        return dx + 1.0E-7 > overlapDistance || dz + 1.0E-7 > overlapDistance;
    }

    private void maybeDoSlideAchievement(Entity entity, BlockPos pos) {
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            if (entity.level().getGameTime() % 20L == 0L) {
                CriteriaTriggers.HONEY_BLOCK_SLIDE.trigger(serverPlayer, entity.level().getBlockState(pos));
            }
        }
    }

    private void doSlideMovement(Entity entity) {
        Vec3 deltaMovement = entity.getDeltaMovement();
        if (HoneyBlock.getOldDeltaY(entity.getDeltaMovement().y) < -0.13) {
            double horizontalReductionFactor = -0.05 / HoneyBlock.getOldDeltaY(entity.getDeltaMovement().y);
            entity.setDeltaMovement(new Vec3(deltaMovement.x * horizontalReductionFactor, HoneyBlock.getNewDeltaY(-0.05), deltaMovement.z * horizontalReductionFactor));
        } else {
            entity.setDeltaMovement(new Vec3(deltaMovement.x, HoneyBlock.getNewDeltaY(-0.05), deltaMovement.z));
        }
        entity.resetFallDistance();
    }

    private void maybeDoSlideEffects(Level level, Entity entity) {
        if (HoneyBlock.doesEntityDoHoneyBlockSlideEffects(entity)) {
            RandomSource random = level.getRandom();
            if (random.nextInt(5) == 0) {
                entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0f, 1.0f);
            }
            if (!level.isClientSide() && random.nextInt(5) == 0) {
                level.broadcastEntityEvent(entity, (byte)53);
            }
        }
    }

    public static void showSlideParticles(Entity entity) {
        HoneyBlock.showParticles(entity, 5);
    }

    public static void showJumpParticles(Entity entity) {
        HoneyBlock.showParticles(entity, 10);
    }

    private static void showParticles(Entity entity, int count) {
        if (!entity.level().isClientSide()) {
            return;
        }
        BlockState blockState = Blocks.HONEY_BLOCK.defaultBlockState();
        for (int i = 0; i < count; ++i) {
            entity.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState), entity.getX(), entity.getY(), entity.getZ(), 0.0, 0.0, 0.0);
        }
    }
}

