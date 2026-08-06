/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LlamaSpit
extends Projectile {
    public LlamaSpit(EntityType<? extends LlamaSpit> type, Level level) {
        super((EntityType<? extends Projectile>)type, level);
    }

    public LlamaSpit(Level level, Llama owner) {
        this((EntityType<? extends LlamaSpit>)EntityTypes.LLAMA_SPIT, level);
        this.setOwner(owner);
        this.setPos(owner.getX() - (double)(owner.getBbWidth() + 1.0f) * 0.5 * (double)Mth.sin(owner.yBodyRot * ((float)Math.PI / 180)), owner.getEyeY() - (double)0.1f, owner.getZ() + (double)(owner.getBbWidth() + 1.0f) * 0.5 * (double)Mth.cos(owner.yBodyRot * ((float)Math.PI / 180)));
    }

    @Override
    protected double getDefaultGravity() {
        return 0.06;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = this.getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        this.hitTargetOrDeflectSelf(hitResult);
        double x = this.getX() + movement.x;
        double y = this.getY() + movement.y;
        double z = this.getZ() + movement.z;
        this.updateRotation();
        if (this.level().getBlockStates(this.getBoundingBox()).noneMatch(BlockBehaviour.BlockStateBase::isAir)) {
            this.discard();
            return;
        }
        if (this.isInWater()) {
            this.discard();
            return;
        }
        this.setDeltaMovement(movement.scale(this.getAirDrag()));
        this.applyGravity();
        this.setPos(x, y, z);
    }

    @Override
    protected float getAirDrag() {
        return 0.99f;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity entity = this.getOwner();
        if (entity instanceof LivingEntity) {
            ServerLevel serverLevel;
            LivingEntity livingOwner = (LivingEntity)entity;
            Entity target = hitResult.getEntity();
            DamageSource damageSource = this.damageSources().spit(this, livingOwner);
            Level level = this.level();
            if (level instanceof ServerLevel && target.hurtServer(serverLevel = (ServerLevel)level, damageSource, 1.0f)) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Vec3 movement = packet.getMovement();
        for (int i = 0; i < 7; ++i) {
            double k = 0.4 + 0.1 * (double)i;
            this.level().addParticle(ParticleTypes.SPIT, this.getX(), this.getY(), this.getZ(), movement.x * k, movement.y, movement.z * k);
        }
        this.setDeltaMovement(movement);
    }
}

