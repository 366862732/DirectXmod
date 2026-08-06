/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractHurtingProjectile
extends Projectile {
    public static final double INITAL_ACCELERATION_POWER = 0.1;
    public static final double DEFLECTION_SCALE = 0.5;
    public double accelerationPower = 0.1;

    protected AbstractHurtingProjectile(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super((EntityType<? extends Projectile>)type, level);
    }

    protected AbstractHurtingProjectile(EntityType<? extends AbstractHurtingProjectile> type, double x, double y, double z, Level level) {
        this(type, level);
        this.setPos(x, y, z);
    }

    public AbstractHurtingProjectile(EntityType<? extends AbstractHurtingProjectile> type, double x, double y, double z, Vec3 direction, Level level) {
        this(type, level);
        this.snapTo(x, y, z, this.getYRot(), this.getXRot());
        this.reapplyPosition();
        this.assignDirectionalMovement(direction, this.accelerationPower);
    }

    public AbstractHurtingProjectile(EntityType<? extends AbstractHurtingProjectile> type, LivingEntity mob, Vec3 direction, Level level) {
        this(type, mob.getX(), mob.getY(), mob.getZ(), direction, level);
        this.setOwner(mob);
        this.setRot(mob.getYRot(), mob.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize() * 4.0;
        if (Double.isNaN(size)) {
            size = 4.0;
        }
        return distance < (size *= 64.0) * size;
    }

    protected ClipContext.Block getClipType() {
        return ClipContext.Block.COLLIDER;
    }

    @Override
    public void tick() {
        Entity owner = this.getOwner();
        this.applyInertia();
        if (!this.level().isClientSide() && (owner != null && owner.isRemoved() || !this.level().hasChunkAt(this.blockPosition()))) {
            this.discard();
            return;
        }
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity, this.getClipType());
        Vec3 newPosition = hitResult.getType() != HitResult.Type.MISS ? hitResult.getLocation() : this.position().add(this.getDeltaMovement());
        ProjectileUtil.rotateTowardsMovement(this, 0.2f);
        this.setPos(newPosition);
        this.applyEffectsFromBlocks();
        super.tick();
        if (this.shouldBurn()) {
            this.igniteForSeconds(1.0f);
        }
        if (hitResult.getType() != HitResult.Type.MISS && this.isAlive()) {
            this.hitTargetOrDeflectSelf(hitResult);
        }
        this.createParticleTrail();
    }

    private void applyInertia() {
        float inertia;
        Vec3 movement = this.getDeltaMovement();
        Vec3 position = this.position();
        if (this.isInWater()) {
            for (int i = 0; i < 4; ++i) {
                float s = 0.25f;
                this.level().addParticle(ParticleTypes.BUBBLE, position.x - movement.x * 0.25, position.y - movement.y * 0.25, position.z - movement.z * 0.25, movement.x, movement.y, movement.z);
            }
            inertia = this.getLiquidInertia();
        } else {
            inertia = this.getInertia();
        }
        this.setDeltaMovement(movement.add(movement.normalize().scale(this.accelerationPower)).scale(inertia));
    }

    private void createParticleTrail() {
        ParticleOptions trailParticle = this.getTrailParticle();
        Vec3 position = this.position();
        if (trailParticle != null) {
            this.level().addParticle(trailParticle, position.x, position.y + 0.5, position.z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !entity.noPhysics;
    }

    protected boolean shouldBurn() {
        return true;
    }

    protected @Nullable ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }

    protected float getInertia() {
        return 0.95f;
    }

    protected float getLiquidInertia() {
        return 0.8f;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("acceleration_power", this.accelerationPower);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.accelerationPower = input.getDoubleOr("acceleration_power", 0.1);
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f;
    }

    private void assignDirectionalMovement(Vec3 direction, double speed) {
        this.setDeltaMovement(direction.normalize().scale(speed));
        this.needsSync = true;
    }

    @Override
    protected void onDeflection(boolean byAttack) {
        super.onDeflection(byAttack);
        this.accelerationPower = byAttack ? 0.1 : (this.accelerationPower *= 0.5);
    }
}

