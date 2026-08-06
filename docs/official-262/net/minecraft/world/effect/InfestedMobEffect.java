/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.world.effect;

import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

class InfestedMobEffect
extends MobEffect {
    private final float chanceToSpawn;
    private final ToIntFunction<RandomSource> spawnedCount;

    protected InfestedMobEffect(MobEffectCategory category, int color, float chanceToSpawn, ToIntFunction<RandomSource> spawnedCount) {
        super(category, color, ParticleTypes.INFESTED);
        this.chanceToSpawn = chanceToSpawn;
        this.spawnedCount = spawnedCount;
    }

    @Override
    public void onMobHurt(ServerLevel level, LivingEntity mob, int amplifier, DamageSource source, float damage) {
        if (mob.getRandom().nextFloat() <= this.chanceToSpawn) {
            int count = this.spawnedCount.applyAsInt(mob.getRandom());
            for (int i = 0; i < count; ++i) {
                this.spawnSilverfish(level, mob, mob.getX(), mob.getY() + (double)mob.getBbHeight() / 2.0, mob.getZ());
            }
        }
    }

    private void spawnSilverfish(ServerLevel level, LivingEntity mob, double x, double y, double z) {
        Silverfish silverfish = EntityTypes.SILVERFISH.create((Level)level, EntitySpawnReason.TRIGGERED);
        if (silverfish == null) {
            return;
        }
        RandomSource random = mob.getRandom();
        float angle = 1.5707964f;
        float randomAngle = Mth.randomBetween(random, -1.5707964f, 1.5707964f);
        Vector3f viewDirection = mob.getLookAngle().toVector3f().mul(0.3f).mul(1.0f, 1.5f, 1.0f).rotateY(randomAngle);
        silverfish.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
        silverfish.setDeltaMovement(new Vec3((Vector3fc)viewDirection));
        level.addFreshEntity(silverfish);
        silverfish.playSound(SoundEvents.SILVERFISH_HURT);
    }
}

