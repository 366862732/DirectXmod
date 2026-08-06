/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.projectile.hurtingprojectile.windcharge;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractWindCharge
extends AbstractHurtingProjectile
implements ItemSupplier {
    public static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(true, false, Optional.empty(), BuiltInRegistries.BLOCK.get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity()));
    public static final double JUMP_SCALE = 0.25;

    public AbstractWindCharge(EntityType<? extends AbstractWindCharge> type, Level level) {
        super((EntityType<? extends AbstractHurtingProjectile>)type, level);
        this.accelerationPower = 0.0;
    }

    public AbstractWindCharge(EntityType<? extends AbstractWindCharge> type, Level level, Entity owner, double x, double y, double z) {
        super(type, x, y, z, level);
        this.setOwner(owner);
        this.accelerationPower = 0.0;
    }

    protected AbstractWindCharge(EntityType<? extends AbstractWindCharge> type, double x, double y, double z, Vec3 direction, Level level) {
        super(type, x, y, z, direction, level);
        this.accelerationPower = 0.0;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        float width = this.getType().getDimensions().width() / 2.0f;
        float height = this.getType().getDimensions().height();
        float offset = 0.15f;
        return new AABB(position.x - (double)width, position.y - (double)0.15f, position.z - (double)width, position.x + (double)width, position.y - (double)0.15f + (double)height, position.z + (double)width);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity instanceof AbstractWindCharge) {
            return false;
        }
        return super.canCollideWith(entity);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof AbstractWindCharge) {
            return false;
        }
        if (entity.is(EntityTypes.END_CRYSTAL)) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        DamageSource source;
        LivingEntity livingEntity;
        Entity entity;
        super.onHitEntity(hitResult);
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Entity entity2 = this.getOwner();
        if (entity2 instanceof LivingEntity) {
            entity = (LivingEntity)entity2;
            livingEntity = entity;
        } else {
            livingEntity = null;
        }
        LivingEntity owner = livingEntity;
        entity = hitResult.getEntity();
        if (owner != null) {
            owner.setLastHurtMob(entity);
        }
        if (entity.hurtServer(serverLevel, source = this.damageSources().windCharge(this, owner), 1.0f) && entity instanceof LivingEntity) {
            LivingEntity mob = (LivingEntity)entity;
            EnchantmentHelper.doPostAttackEffects(serverLevel, mob, source);
        }
        this.explode(this.position());
    }

    @Override
    public void push(double xa, double ya, double za) {
    }

    protected abstract void explode(Vec3 var1);

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!this.level().isClientSide()) {
            Vec3i collisionNormal = hitResult.getDirection().getUnitVec3i();
            Vec3 scaledNormal = Vec3.atLowerCornerOf(collisionNormal).multiply(0.25, 0.25, 0.25);
            Vec3 explosionPos = hitResult.getLocation().add(scaledNormal);
            this.explode(explosionPos);
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected float getInertia() {
        return 1.0f;
    }

    @Override
    protected float getLiquidInertia() {
        return this.getInertia();
    }

    @Override
    protected @Nullable ParticleOptions getTrailParticle() {
        return null;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide() && this.getBlockY() > this.level().getMaxY() + 30) {
            this.explode(this.position());
            this.discard();
        } else {
            super.tick();
        }
    }
}

