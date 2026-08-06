/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class ThrowableProjectile
extends Projectile {
    private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25f;

    protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
        super((EntityType<? extends Projectile>)type, level);
    }

    protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> type, double x, double y, double z, Level level) {
        this(type, level);
        this.setPos(x, y, z);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        if (this.tickCount < 2 && distance < 12.25) {
            return false;
        }
        double size = this.getBoundingBox().getSize() * 4.0;
        if (Double.isNaN(size)) {
            size = 4.0;
        }
        return distance < (size *= 64.0) * size;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return true;
    }

    @Override
    public void tick() {
        this.handleFirstTickBubbleColumn();
        this.applyGravity();
        this.applyInertia();
        HitResult result = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        Vec3 newPosition = result.getType() != HitResult.Type.MISS ? result.getLocation() : this.position().add(this.getDeltaMovement());
        this.setPos(newPosition);
        this.updateRotation();
        this.applyEffectsFromBlocks();
        super.tick();
        if (result.getType() != HitResult.Type.MISS && this.isAlive()) {
            this.hitTargetOrDeflectSelf(result);
        }
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
            inertia = 0.8f;
        } else {
            inertia = this.getAirDrag();
        }
        this.setDeltaMovement(movement.scale(inertia));
    }

    @Override
    protected float getAirDrag() {
        return 0.99f;
    }

    private void handleFirstTickBubbleColumn() {
        if (this.firstTick) {
            for (BlockPos pos : BlockPos.betweenClosed(this.getBoundingBox())) {
                BlockState state = this.level().getBlockState(pos);
                if (!state.is(Blocks.BUBBLE_COLUMN)) continue;
                state.entityInside(this.level(), pos, this, InsideBlockEffectApplier.NOOP, true);
            }
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }
}

