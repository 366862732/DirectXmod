/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WitherSkull
extends AbstractHurtingProjectile {
    private static final EntityDataAccessor<Boolean> DATA_DANGEROUS = SynchedEntityData.defineId(WitherSkull.class, EntityDataSerializers.BOOLEAN);
    private static final boolean DEFAULT_DANGEROUS = false;

    public WitherSkull(EntityType<? extends WitherSkull> type, Level level) {
        super((EntityType<? extends AbstractHurtingProjectile>)type, level);
    }

    public WitherSkull(Level level, LivingEntity mob, Vec3 direction) {
        super(EntityTypes.WITHER_SKULL, mob, direction, level);
    }

    @Override
    protected float getInertia() {
        return this.isDangerous() ? 0.73f : super.getInertia();
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public float getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid, float resistance) {
        if (this.isDangerous() && WitherBoss.canDestroy(block)) {
            return Math.min(0.8f, resistance);
        }
        return resistance;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        boolean wasHurt;
        super.onHitEntity(hitResult);
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Entity entity = hitResult.getEntity();
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity) {
            LivingEntity livingOwner = (LivingEntity)owner;
            DamageSource damageSource = this.damageSources().witherSkull(this, livingOwner);
            wasHurt = entity.hurtServer(serverLevel, damageSource, 8.0f);
            if (wasHurt) {
                if (entity.isAlive()) {
                    EnchantmentHelper.doPostAttackEffects(serverLevel, entity, damageSource);
                } else {
                    livingOwner.heal(5.0f);
                }
            }
        } else {
            wasHurt = entity.hurtServer(serverLevel, this.damageSources().magic(), 5.0f);
        }
        if (wasHurt && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            int witherSeconds = 0;
            if (this.level().getDifficulty() == Difficulty.NORMAL) {
                witherSeconds = 10;
            } else if (this.level().getDifficulty() == Difficulty.HARD) {
                witherSeconds = 40;
            }
            if (witherSeconds > 0) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * witherSeconds, 1), this.getEffectSource());
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.level().explode((Entity)this, this.getX(), this.getY(), this.getZ(), 1.0f, false, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_DANGEROUS, false);
    }

    public boolean isDangerous() {
        return this.entityData.get(DATA_DANGEROUS);
    }

    public void setDangerous(boolean value) {
        this.entityData.set(DATA_DANGEROUS, value);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("dangerous", this.isDangerous());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setDangerous(input.getBooleanOr("dangerous", false));
    }
}

