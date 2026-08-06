/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.mutable.MutableInt
 */
package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableInt;

public class BellBlockEntity
extends BlockEntity {
    private static final int DURATION = 50;
    private static final int GLOW_DURATION = 60;
    private static final int MIN_TICKS_BETWEEN_SEARCHES = 60;
    private static final int MAX_RESONATION_TICKS = 40;
    private static final int TICKS_BEFORE_RESONATION = 5;
    private static final int SEARCH_RADIUS = 48;
    private static final int HEAR_BELL_RADIUS = 32;
    private static final int HIGHLIGHT_RAIDERS_RADIUS = 48;
    private long lastRingTimestamp;
    public int ticks;
    public boolean shaking;
    public Direction clickDirection;
    private List<LivingEntity> nearbyEntities;
    private boolean resonating;
    private int resonationTicks;

    public BellBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.BELL, worldPosition, blockState);
    }

    @Override
    public boolean triggerEvent(int b0, int b1) {
        if (b0 == 1) {
            this.updateEntities();
            this.resonationTicks = 0;
            this.clickDirection = Direction.from3DDataValue(b1);
            this.ticks = 0;
            this.shaking = true;
            return true;
        }
        return super.triggerEvent(b0, b1);
    }

    private static void tick(Level level, BlockPos pos, BlockState state, BellBlockEntity entity, ResonationEndAction onResonationEnd) {
        if (entity.shaking) {
            ++entity.ticks;
        }
        if (entity.ticks >= 50) {
            entity.shaking = false;
            entity.ticks = 0;
        }
        if (entity.ticks >= 5 && entity.resonationTicks == 0 && BellBlockEntity.areRaidersNearby(pos, entity.nearbyEntities)) {
            entity.resonating = true;
            level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (entity.resonating) {
            if (entity.resonationTicks < 40) {
                ++entity.resonationTicks;
            } else {
                onResonationEnd.run(level, pos, entity.nearbyEntities);
                entity.resonating = false;
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BellBlockEntity entity) {
        BellBlockEntity.tick(level, pos, state, entity, BellBlockEntity::showBellParticles);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BellBlockEntity entity) {
        BellBlockEntity.tick(level, pos, state, entity, BellBlockEntity::makeRaidersGlow);
    }

    public void onHit(Direction clickDirection) {
        BlockPos bellPos = this.getBlockPos();
        this.clickDirection = clickDirection;
        if (this.shaking) {
            this.ticks = 0;
        } else {
            this.shaking = true;
        }
        this.level.blockEvent(bellPos, this.getBlockState().getBlock(), 1, clickDirection.get3DDataValue());
    }

    private void updateEntities() {
        BlockPos blockPos = this.getBlockPos();
        if (this.level.getGameTime() > this.lastRingTimestamp + 60L || this.nearbyEntities == null) {
            this.lastRingTimestamp = this.level.getGameTime();
            AABB aabb = new AABB(blockPos).inflate(48.0);
            this.nearbyEntities = this.level.getEntitiesOfClass(LivingEntity.class, aabb);
        }
        if (!this.level.isClientSide()) {
            for (LivingEntity entity : this.nearbyEntities) {
                if (!entity.isAlive() || entity.isRemoved() || !blockPos.closerToCenterThan(entity.position(), 32.0)) continue;
                entity.getBrain().setMemory(MemoryModuleType.HEARD_BELL_TIME, this.level.getGameTime());
            }
        }
    }

    private static boolean areRaidersNearby(BlockPos bellPos, List<LivingEntity> nearbyEntities) {
        for (LivingEntity entity : nearbyEntities) {
            if (!entity.isAlive() || entity.isRemoved() || !bellPos.closerToCenterThan(entity.position(), 32.0) || !entity.is(EntityTypeTags.RAIDERS)) continue;
            return true;
        }
        return false;
    }

    private static void makeRaidersGlow(Level level, BlockPos blockPos, List<LivingEntity> nearbyEntities) {
        nearbyEntities.stream().filter(e -> BellBlockEntity.isRaiderWithinRange(blockPos, e)).forEach(BellBlockEntity::glow);
    }

    private static void showBellParticles(Level level, BlockPos bellPos, List<LivingEntity> nearbyEntities) {
        MutableInt particleColor = new MutableInt(16700985);
        int nearbyRaiderCount = (int)nearbyEntities.stream().filter(p -> bellPos.closerToCenterThan(p.position(), 48.0)).count();
        nearbyEntities.stream().filter(e -> BellBlockEntity.isRaiderWithinRange(bellPos, e)).forEach(entity -> {
            float distAway = 1.0f;
            double distBtwn = Math.sqrt((entity.getX() - (double)bellPos.getX()) * (entity.getX() - (double)bellPos.getX()) + (entity.getZ() - (double)bellPos.getZ()) * (entity.getZ() - (double)bellPos.getZ()));
            double x3 = (double)((float)bellPos.getX() + 0.5f) + 1.0 / distBtwn * (entity.getX() - (double)bellPos.getX());
            double z3 = (double)((float)bellPos.getZ() + 0.5f) + 1.0 / distBtwn * (entity.getZ() - (double)bellPos.getZ());
            int particleCount = Mth.clamp((nearbyRaiderCount - 21) / -2, 3, 15);
            for (int i = 0; i < particleCount; ++i) {
                int color = particleColor.addAndGet(5);
                level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color), x3, (float)bellPos.getY() + 0.5f, z3, 0.0, 0.0, 0.0);
            }
        });
    }

    private static boolean isRaiderWithinRange(BlockPos blockPos, LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && blockPos.closerToCenterThan(entity.position(), 48.0) && entity.is(EntityTypeTags.RAIDERS);
    }

    private static void glow(LivingEntity raider) {
        raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60));
    }

    @FunctionalInterface
    private static interface ResonationEndAction {
        public void run(Level var1, BlockPos var2, List<LivingEntity> var3);
    }
}

