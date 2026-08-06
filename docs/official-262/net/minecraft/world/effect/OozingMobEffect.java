/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 */
package net.minecraft.world.effect;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

class OozingMobEffect
extends MobEffect {
    private static final int RADIUS_TO_CHECK_SLIMES = 2;
    public static final int SLIME_SIZE = 2;
    private final ToIntFunction<RandomSource> spawnedCount;

    protected OozingMobEffect(MobEffectCategory category, int color, ToIntFunction<RandomSource> spawnedCount) {
        super(category, color, ParticleTypes.ITEM_SLIME);
        this.spawnedCount = spawnedCount;
    }

    @VisibleForTesting
    protected static int numberOfSlimesToSpawn(int maxEntityCramming, NearbySlimes nearbySlimes, int numberRequested) {
        if (maxEntityCramming < 1) {
            return numberRequested;
        }
        return Mth.clamp(0, maxEntityCramming - nearbySlimes.count(maxEntityCramming), numberRequested);
    }

    @Override
    public void onMobRemoved(ServerLevel level, LivingEntity mob, int amplifier, Entity.RemovalReason reason) {
        if (reason != Entity.RemovalReason.KILLED) {
            return;
        }
        int requestedSlimesToSpawn = this.spawnedCount.applyAsInt(mob.getRandom());
        int maxEntityCramming = level.getGameRules().get(GameRules.MAX_ENTITY_CRAMMING);
        int numberOfSlimesToSpawn = OozingMobEffect.numberOfSlimesToSpawn(maxEntityCramming, NearbySlimes.closeTo(mob), requestedSlimesToSpawn);
        for (int i = 0; i < numberOfSlimesToSpawn; ++i) {
            this.spawnSlimeOffspring(mob.level(), mob.getX(), mob.getY() + 0.5, mob.getZ());
        }
    }

    private void spawnSlimeOffspring(Level level, double x, double y, double z) {
        Slime slime = EntityTypes.SLIME.create(level, EntitySpawnReason.TRIGGERED);
        if (slime == null) {
            return;
        }
        slime.setSize(2, true);
        slime.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
        level.addFreshEntity(slime);
    }

    @FunctionalInterface
    protected static interface NearbySlimes {
        public int count(int var1);

        private static NearbySlimes closeTo(LivingEntity mob) {
            return maxResults -> {
                ArrayList slimesNearby = new ArrayList();
                mob.level().getEntities(EntityTypes.SLIME, mob.getBoundingBox().inflate(2.0), slime -> slime != mob, slimesNearby, maxResults);
                return slimesNearby.size();
            };
        }
    }
}

