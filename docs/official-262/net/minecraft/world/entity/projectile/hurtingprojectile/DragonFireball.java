/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.projectile.hurtingprojectile;

import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DragonFireball
extends AbstractHurtingProjectile {
    public static final float SPLASH_RANGE = 4.0f;

    public DragonFireball(EntityType<? extends DragonFireball> type, Level level) {
        super((EntityType<? extends AbstractHurtingProjectile>)type, level);
    }

    public DragonFireball(Level level, LivingEntity mob, Vec3 direction) {
        super(EntityTypes.DRAGON_FIREBALL, mob, direction, level);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (hitResult.getType() == HitResult.Type.ENTITY && this.ownedBy(((EntityHitResult)hitResult).getEntity())) {
            return;
        }
        if (!this.level().isClientSide()) {
            List<LivingEntity> entitiesOfClass = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0, 2.0, 4.0));
            AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            Entity owner = this.getOwner();
            if (owner instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)owner;
                cloud.setOwner(livingEntity);
            }
            cloud.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0f));
            cloud.setRadius(3.0f);
            cloud.setDuration(600);
            cloud.setRadiusPerTick((7.0f - cloud.getRadius()) / (float)cloud.getDuration());
            cloud.setPotionDurationScale(0.25f);
            cloud.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1));
            if (!entitiesOfClass.isEmpty()) {
                for (LivingEntity entity : entitiesOfClass) {
                    double dist = this.distanceToSqr(entity);
                    if (!(dist < 16.0)) continue;
                    cloud.setPos(entity.getX(), entity.getY(), entity.getZ());
                    break;
                }
            }
            this.level().levelEvent(2006, this.blockPosition(), this.isSilent() ? -1 : 1);
            this.level().addFreshEntity(cloud);
            this.discard();
        }
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0f);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}

