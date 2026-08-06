/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.squid;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Squid
extends AgeableWaterCreature {
    public float xBodyRot;
    public float xBodyRotO;
    public float zBodyRot;
    public float zBodyRotO;
    public float tentacleMovement;
    public float oldTentacleMovement;
    public float tentacleAngle;
    public float oldTentacleAngle;
    private float speed;
    private float tentacleSpeed;
    private float rotateSpeed;
    private Vec3 movementVector = Vec3.ZERO;
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.5f, 0.5f).withEyeHeight(0.37f);

    public Squid(EntityType<? extends Squid> type, Level level) {
        super((EntityType<? extends AgeableWaterCreature>)type, level);
        this.tentacleSpeed = 1.0f / (this.random.nextFloat() + 1.0f) * 0.2f;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SquidRandomMovementGoal(this));
        this.goalSelector.addGoal(1, new SquidFleeGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SQUID_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SQUID_DEATH;
    }

    protected SoundEvent getSquirtSound() {
        return SoundEvents.SQUID_SQUIRT;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.SQUID.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.08;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.xBodyRotO = this.xBodyRot;
        this.zBodyRotO = this.zBodyRot;
        this.oldTentacleMovement = this.tentacleMovement;
        this.oldTentacleAngle = this.tentacleAngle;
        this.tentacleMovement += this.tentacleSpeed;
        if ((double)this.tentacleMovement > Math.PI * 2) {
            if (this.level().isClientSide()) {
                this.tentacleMovement = (float)Math.PI * 2;
            } else {
                this.tentacleMovement -= (float)Math.PI * 2;
                if (this.random.nextInt(10) == 0) {
                    this.tentacleSpeed = 1.0f / (this.random.nextFloat() + 1.0f) * 0.2f;
                }
                this.level().broadcastEntityEvent(this, (byte)19);
            }
        }
        if (this.isInWater()) {
            if (this.tentacleMovement < (float)Math.PI) {
                float tentacleScale = this.tentacleMovement / (float)Math.PI;
                this.tentacleAngle = Mth.sin(tentacleScale * tentacleScale * (float)Math.PI) * (float)Math.PI * 0.25f;
                if ((double)tentacleScale > 0.75) {
                    if (this.isLocalInstanceAuthoritative()) {
                        this.setDeltaMovement(this.movementVector);
                    }
                    this.rotateSpeed = 1.0f;
                } else {
                    this.rotateSpeed *= 0.8f;
                }
            } else {
                this.tentacleAngle = 0.0f;
                if (this.isLocalInstanceAuthoritative()) {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
                }
                this.rotateSpeed *= 0.99f;
            }
            Vec3 movement = this.getDeltaMovement();
            double horizontalMovement = movement.horizontalDistance();
            this.yBodyRot += (-((float)Mth.atan2(movement.x, movement.z)) * 57.295776f - this.yBodyRot) * 0.1f;
            this.setYRot(this.yBodyRot);
            this.zBodyRot += (float)Math.PI * this.rotateSpeed * 1.5f;
            this.xBodyRot += (-((float)Mth.atan2(horizontalMovement, movement.y)) * 57.295776f - this.xBodyRot) * 0.1f;
        } else {
            this.tentacleAngle = Mth.abs(Mth.sin(this.tentacleMovement)) * (float)Math.PI * 0.25f;
            if (!this.level().isClientSide()) {
                double yd = this.getDeltaMovement().y;
                yd = this.hasEffect(MobEffects.LEVITATION) ? 0.05 * (double)(this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) : (yd -= this.getGravity());
                this.setDeltaMovement(0.0, yd * (double)this.getAirDrag(), 0.0);
            }
            this.xBodyRot += (-90.0f - this.xBodyRot) * 0.02f;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (super.hurtServer(level, source, damage) && this.getLastHurtByMob() != null) {
            this.spawnInk();
            return true;
        }
        return false;
    }

    private Vec3 rotateVector(Vec3 vec) {
        Vec3 v = vec.xRot(this.xBodyRotO * ((float)Math.PI / 180));
        v = v.yRot(-this.yBodyRotO * ((float)Math.PI / 180));
        return v;
    }

    private void spawnInk() {
        this.makeSound(this.getSquirtSound());
        Vec3 pos = this.rotateVector(new Vec3(0.0, -1.0, 0.0)).add(this.getX(), this.getY(), this.getZ());
        for (int i = 0; i < 30; ++i) {
            Vec3 dir = this.rotateVector(new Vec3((double)this.random.nextFloat() * 0.6 - 0.3, -1.0, (double)this.random.nextFloat() * 0.6 - 0.3));
            float inkPosOffsetScale = this.isBaby() ? 0.1f : 0.3f;
            Vec3 dirOffset = dir.scale(inkPosOffsetScale + this.random.nextFloat() * 2.0f);
            ((ServerLevel)this.level()).sendParticles(this.getInkParticle(), pos.x, pos.y + 0.5, pos.z, 0, dirOffset.x, dirOffset.y, dirOffset.z, 0.1f);
        }
    }

    protected ParticleOptions getInkParticle() {
        return ParticleTypes.SQUID_INK;
    }

    @Override
    public void travel(Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 19) {
            this.tentacleMovement = 0.0f;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public boolean hasMovementVector() {
        return this.movementVector.lengthSqr() > (double)1.0E-5f;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData spawnGroupData = Objects.requireNonNullElseGet(groupData, () -> new AgeableMob.AgeableMobGroupData(0.05f));
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    private static class SquidRandomMovementGoal
    extends Goal {
        private final Squid squid;

        public SquidRandomMovementGoal(Squid squid) {
            this.squid = squid;
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            int noActionTime = this.squid.getNoActionTime();
            if (noActionTime > 100) {
                this.squid.movementVector = Vec3.ZERO;
            } else if (this.squid.getRandom().nextInt(SquidRandomMovementGoal.reducedTickDelay(50)) == 0 || !this.squid.wasTouchingWater || !this.squid.hasMovementVector()) {
                float angle = this.squid.getRandom().nextFloat() * ((float)Math.PI * 2);
                this.squid.movementVector = new Vec3(Mth.cos(angle) * 0.2f, -0.1f + this.squid.getRandom().nextFloat() * 0.2f, Mth.sin(angle) * 0.2f);
            }
        }
    }

    private class SquidFleeGoal
    extends Goal {
        private static final float SQUID_FLEE_SPEED = 3.0f;
        private static final float SQUID_FLEE_MIN_DISTANCE = 5.0f;
        private static final float SQUID_FLEE_MAX_DISTANCE = 10.0f;
        private int fleeTicks;
        final /* synthetic */ Squid this$0;

        private SquidFleeGoal(Squid squid) {
            Squid squid2 = squid;
            Objects.requireNonNull(squid2);
            this.this$0 = squid2;
        }

        @Override
        public boolean canUse() {
            LivingEntity entity = this.this$0.getLastHurtByMob();
            if (this.this$0.isInWater() && entity != null) {
                return this.this$0.distanceToSqr(entity) < 100.0;
            }
            return false;
        }

        @Override
        public void start() {
            this.fleeTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ++this.fleeTicks;
            LivingEntity lastHurtByMob = this.this$0.getLastHurtByMob();
            if (lastHurtByMob == null) {
                return;
            }
            Vec3 fleeTo = new Vec3(this.this$0.getX() - lastHurtByMob.getX(), this.this$0.getY() - lastHurtByMob.getY(), this.this$0.getZ() - lastHurtByMob.getZ());
            BlockState blockState = this.this$0.level().getBlockState(BlockPos.containing(this.this$0.getX() + fleeTo.x, this.this$0.getY() + fleeTo.y, this.this$0.getZ() + fleeTo.z));
            FluidState fluidState = this.this$0.level().getFluidState(BlockPos.containing(this.this$0.getX() + fleeTo.x, this.this$0.getY() + fleeTo.y, this.this$0.getZ() + fleeTo.z));
            if (fluidState.is(FluidTags.WATER) || blockState.isAir()) {
                double length = fleeTo.length();
                if (length > 0.0) {
                    fleeTo.normalize();
                    double avoidSpeed = 3.0;
                    if (length > 5.0) {
                        avoidSpeed -= (length - 5.0) / 5.0;
                    }
                    if (avoidSpeed > 0.0) {
                        fleeTo = fleeTo.scale(avoidSpeed);
                    }
                }
                if (blockState.isAir()) {
                    fleeTo = fleeTo.subtract(0.0, fleeTo.y, 0.0);
                }
                this.this$0.movementVector = new Vec3(fleeTo.x / 20.0, fleeTo.y / 20.0, fleeTo.z / 20.0);
            }
            if (this.fleeTicks % 10 == 5) {
                this.this$0.level().addParticle(ParticleTypes.BUBBLE, this.this$0.getX(), this.this$0.getY(), this.this$0.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }
}

