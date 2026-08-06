/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Guardian
extends Monster {
    protected static final int ATTACK_TIME = 80;
    private static final EntityDataAccessor<Boolean> DATA_ID_MOVING = SynchedEntityData.defineId(Guardian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ID_ATTACK_TARGET = SynchedEntityData.defineId(Guardian.class, EntityDataSerializers.INT);
    private float clientSideTailAnimation;
    private float clientSideTailAnimationO;
    private float clientSideTailAnimationSpeed;
    private float clientSideSpikesAnimation;
    private float clientSideSpikesAnimationO;
    private @Nullable LivingEntity clientSideCachedAttackTarget;
    private int clientSideAttackTime;
    private boolean clientSideTouchedGround;
    protected @Nullable RandomStrollGoal randomStrollGoal;

    public Guardian(EntityType<? extends Guardian> type, Level level) {
        super((EntityType<? extends Monster>)type, level);
        this.xpReward = 10;
        this.setPathfindingMalus(PathType.WATER, 0.0f);
        this.moveControl = new GuardianMoveControl<Guardian>(this);
        this.clientSideTailAnimationO = this.clientSideTailAnimation = this.random.nextFloat();
    }

    @Override
    protected void registerGoals() {
        MoveTowardsRestrictionGoal goal = new MoveTowardsRestrictionGoal(this, 1.0);
        this.randomStrollGoal = new RandomStrollGoal(this, 1.0, 80);
        this.goalSelector.addGoal(4, new GuardianAttackGoal(this));
        this.goalSelector.addGoal(5, goal);
        this.goalSelector.addGoal(7, this.randomStrollGoal);
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Guardian.class, 12.0f, 0.01f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.randomStrollGoal.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        goal.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<LivingEntity>(this, LivingEntity.class, 10, true, false, new GuardianAttackSelector(this)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.MAX_HEALTH, 30.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID_MOVING, false);
        entityData.define(DATA_ID_ATTACK_TARGET, 0);
    }

    public boolean isMoving() {
        return this.entityData.get(DATA_ID_MOVING);
    }

    public void setMoving(boolean value) {
        this.entityData.set(DATA_ID_MOVING, value);
    }

    public int getAttackDuration() {
        return 80;
    }

    private void setActiveAttackTarget(int entityId) {
        this.entityData.set(DATA_ID_ATTACK_TARGET, entityId);
    }

    public boolean hasActiveAttackTarget() {
        return this.entityData.get(DATA_ID_ATTACK_TARGET) != 0;
    }

    public @Nullable LivingEntity getActiveAttackTarget() {
        if (!this.hasActiveAttackTarget()) {
            return null;
        }
        if (this.level().isClientSide()) {
            if (this.clientSideCachedAttackTarget != null) {
                return this.clientSideCachedAttackTarget;
            }
            Entity entity = this.level().getEntity(this.entityData.get(DATA_ID_ATTACK_TARGET));
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity;
                this.clientSideCachedAttackTarget = livingEntity = (LivingEntity)entity;
                return this.clientSideCachedAttackTarget;
            }
            return null;
        }
        return this.getTarget();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ID_ATTACK_TARGET.equals(accessor)) {
            this.clientSideAttackTime = 0;
            this.clientSideCachedAttackTarget = null;
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.GUARDIAN_AMBIENT : SoundEvents.GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isInWater() ? SoundEvents.GUARDIAN_HURT : SoundEvents.GUARDIAN_HURT_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isInWater() ? SoundEvents.GUARDIAN_DEATH : SoundEvents.GUARDIAN_DEATH_LAND;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return 10.0f + level.getPathfindingCostFromLightLevels(pos);
        }
        return super.getWalkTargetValue(pos, level);
    }

    @Override
    public void aiStep() {
        if (this.isAlive()) {
            if (this.level().isClientSide()) {
                this.clientSideTailAnimationO = this.clientSideTailAnimation;
                if (!this.isInWater()) {
                    this.clientSideTailAnimationSpeed = 2.0f;
                    Vec3 movement = this.getDeltaMovement();
                    if (movement.y > 0.0 && this.clientSideTouchedGround && !this.isSilent()) {
                        this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getFlopSound(), this.getSoundSource(), 1.0f, 1.0f, false);
                    }
                    this.clientSideTouchedGround = movement.y < 0.0 && this.level().loadedAndEntityCanStandOn(this.blockPosition().below(), this);
                } else {
                    this.clientSideTailAnimationSpeed = this.isMoving() ? (this.clientSideTailAnimationSpeed < 0.5f ? 4.0f : (this.clientSideTailAnimationSpeed += (0.5f - this.clientSideTailAnimationSpeed) * 0.1f)) : (this.clientSideTailAnimationSpeed += (0.125f - this.clientSideTailAnimationSpeed) * 0.2f);
                }
                this.clientSideTailAnimation += this.clientSideTailAnimationSpeed;
                this.clientSideSpikesAnimationO = this.clientSideSpikesAnimation;
                this.clientSideSpikesAnimation = !this.isInWater() ? this.random.nextFloat() : (this.isMoving() ? (this.clientSideSpikesAnimation += (0.0f - this.clientSideSpikesAnimation) * 0.25f) : (this.clientSideSpikesAnimation += (1.0f - this.clientSideSpikesAnimation) * 0.06f));
                if (this.isMoving() && this.isInWater()) {
                    Vec3 viewVector = this.getViewVector(0.0f);
                    for (int i = 0; i < 2; ++i) {
                        this.level().addParticle(ParticleTypes.BUBBLE, this.getRandomX(0.5) - viewVector.x * 1.5, this.getRandomY() - viewVector.y * 1.5, this.getRandomZ(0.5) - viewVector.z * 1.5, 0.0, 0.0, 0.0);
                    }
                }
                if (this.hasActiveAttackTarget()) {
                    LivingEntity attackTarget;
                    if (this.clientSideAttackTime < this.getAttackDuration()) {
                        ++this.clientSideAttackTime;
                    }
                    if ((attackTarget = this.getActiveAttackTarget()) != null) {
                        this.getLookControl().setLookAt(attackTarget, 90.0f, 90.0f);
                        this.getLookControl().tick();
                        double at = this.getAttackAnimationScale(0.0f);
                        double dx = attackTarget.getX() - this.getX();
                        double dy = attackTarget.getY(0.5) - this.getEyeY();
                        double dz = attackTarget.getZ() - this.getZ();
                        double dd = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        dx /= dd;
                        dy /= dd;
                        dz /= dd;
                        double dist = this.random.nextDouble();
                        while (dist < dd) {
                            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + dx * (dist += 1.8 - at + this.random.nextDouble() * (1.7 - at)), this.getEyeY() + dy * dist, this.getZ() + dz * dist, 0.0, 0.0, 0.0);
                        }
                    }
                }
            }
            if (this.isInWater()) {
                this.setAirSupply(300);
            } else if (this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0f - 1.0f) * 0.4f, 0.5, (this.random.nextFloat() * 2.0f - 1.0f) * 0.4f));
                this.setYRot(this.random.nextFloat() * 360.0f);
                this.setOnGround(false);
                this.needsSync = true;
            }
            if (this.hasActiveAttackTarget()) {
                this.setYRot(this.yHeadRot);
            }
        }
        super.aiStep();
    }

    protected SoundEvent getFlopSound() {
        return SoundEvents.GUARDIAN_FLOP;
    }

    public float getTailAnimation(float a) {
        return Mth.lerp(a, this.clientSideTailAnimationO, this.clientSideTailAnimation);
    }

    public float getSpikesAnimation(float a) {
        return Mth.lerp(a, this.clientSideSpikesAnimationO, this.clientSideSpikesAnimation);
    }

    public float getAttackAnimationScale(float a) {
        return ((float)this.clientSideAttackTime + a) / (float)this.getAttackDuration();
    }

    public float getClientSideAttackTime() {
        return this.clientSideAttackTime;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    public static boolean checkGuardianSpawnRules(EntityType<? extends Guardian> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return !(random.nextInt(20) != 0 && level.canSeeSkyFromBelowWater(pos) || level.getDifficulty() == Difficulty.PEACEFUL || !EntitySpawnReason.isSpawner(spawnReason) && !level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos.below()).is(FluidTags.WATER));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity entity;
        if (!this.isMoving() && !source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS) && !source.is(DamageTypes.THORNS) && (entity = source.getDirectEntity()) instanceof LivingEntity) {
            LivingEntity cause = (LivingEntity)entity;
            cause.hurtServer(level, this.damageSources().thorns(this), 2.0f);
        }
        if (this.randomStrollGoal != null) {
            this.randomStrollGoal.trigger();
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public int getMaxHeadXRot() {
        return 180;
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(0.1f, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (!this.isMoving() && this.getTarget() == null) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
        }
    }

    private static class GuardianMoveControl<T extends Guardian>
    extends MoveControl<T> {
        public GuardianMoveControl(T guardian) {
            super(guardian);
        }

        @Override
        public void tick() {
            if (this.operation != MoveControl.Operation.MOVE_TO || ((Guardian)this.mob).getNavigation().isDone()) {
                ((Guardian)this.mob).setSpeed(0.0f);
                ((Guardian)this.mob).setMoving(false);
                return;
            }
            Vec3 delta = new Vec3(this.wantedX - ((Guardian)this.mob).getX(), this.wantedY - ((Guardian)this.mob).getY(), this.wantedZ - ((Guardian)this.mob).getZ());
            double length = delta.length();
            double xd = delta.x / length;
            double yd = delta.y / length;
            double zd = delta.z / length;
            float yRotD = (float)(Mth.atan2(delta.z, delta.x) * 57.2957763671875) - 90.0f;
            ((Guardian)this.mob).setYRot(this.rotlerp(((Guardian)this.mob).getYRot(), yRotD, 90.0f));
            ((Guardian)this.mob).yBodyRot = ((Guardian)this.mob).getYRot();
            float targetSpeed = (float)(this.speedModifier * ((Guardian)this.mob).getAttributeValue(Attributes.MOVEMENT_SPEED));
            float newSpeed = Mth.lerp(0.125f, ((Guardian)this.mob).getSpeed(), targetSpeed);
            ((Guardian)this.mob).setSpeed(newSpeed);
            double push = Math.sin((double)(((Guardian)this.mob).tickCount + ((Guardian)this.mob).getId()) * 0.5) * 0.05;
            double cos = Math.cos(((Guardian)this.mob).getYRot() * ((float)Math.PI / 180));
            double sin = Math.sin(((Guardian)this.mob).getYRot() * ((float)Math.PI / 180));
            double yPush = Math.sin((double)(((Guardian)this.mob).tickCount + ((Guardian)this.mob).getId()) * 0.75) * 0.05;
            ((Guardian)this.mob).setDeltaMovement(((Guardian)this.mob).getDeltaMovement().add(push * cos, yPush * (sin + cos) * 0.25 + (double)newSpeed * yd * 0.1, push * sin));
            LookControl control = ((Guardian)this.mob).getLookControl();
            double newLookX = ((Guardian)this.mob).getX() + xd * 2.0;
            double newLookY = ((Guardian)this.mob).getEyeY() + yd / length;
            double newLookZ = ((Guardian)this.mob).getZ() + zd * 2.0;
            double oldLookX = control.getWantedX();
            double oldLookY = control.getWantedY();
            double oldLookZ = control.getWantedZ();
            if (!control.isLookingAtTarget()) {
                oldLookX = newLookX;
                oldLookY = newLookY;
                oldLookZ = newLookZ;
            }
            ((Guardian)this.mob).getLookControl().setLookAt(Mth.lerp(0.125, oldLookX, newLookX), Mth.lerp(0.125, oldLookY, newLookY), Mth.lerp(0.125, oldLookZ, newLookZ), 10.0f, 40.0f);
            ((Guardian)this.mob).setMoving(true);
        }
    }

    private static class GuardianAttackGoal
    extends Goal {
        private final Guardian guardian;
        private int attackTime;
        private final boolean elder;

        public GuardianAttackGoal(Guardian guardian) {
            this.guardian = guardian;
            this.elder = guardian instanceof ElderGuardian;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.guardian.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && (this.elder || this.guardian.getTarget() != null && this.guardian.distanceToSqr(this.guardian.getTarget()) > 9.0);
        }

        @Override
        public void start() {
            this.attackTime = -10;
            this.guardian.getNavigation().stop();
            LivingEntity target = this.guardian.getTarget();
            if (target != null) {
                this.guardian.getLookControl().setLookAt(target, 90.0f, 90.0f);
            }
            this.guardian.needsSync = true;
        }

        @Override
        public void stop() {
            this.guardian.setActiveAttackTarget(0);
            this.guardian.setTarget(null);
            this.guardian.randomStrollGoal.trigger();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.guardian.getTarget();
            if (target == null) {
                return;
            }
            this.guardian.getNavigation().stop();
            this.guardian.getLookControl().setLookAt(target, 90.0f, 90.0f);
            if (!this.guardian.hasLineOfSight(target)) {
                this.guardian.setTarget(null);
                return;
            }
            ++this.attackTime;
            if (this.attackTime == 0) {
                this.guardian.setActiveAttackTarget(target.getId());
                if (!this.guardian.isSilent()) {
                    this.guardian.level().broadcastEntityEvent(this.guardian, (byte)21);
                }
            } else if (this.attackTime >= this.guardian.getAttackDuration()) {
                float magicDamage = 1.0f;
                if (this.guardian.level().getDifficulty() == Difficulty.HARD) {
                    magicDamage += 2.0f;
                }
                if (this.elder) {
                    magicDamage += 2.0f;
                }
                ServerLevel serverLevel = GuardianAttackGoal.getServerLevel(this.guardian);
                target.hurtServer(serverLevel, this.guardian.damageSources().indirectMagic(this.guardian, this.guardian), magicDamage);
                this.guardian.doHurtTarget(serverLevel, target);
                this.guardian.setTarget(null);
            }
            super.tick();
        }
    }

    private static class GuardianAttackSelector
    implements TargetingConditions.Selector {
        private final Guardian guardian;

        public GuardianAttackSelector(Guardian guardian) {
            this.guardian = guardian;
        }

        @Override
        public boolean test(@Nullable LivingEntity target, ServerLevel level) {
            return (target instanceof Player || target instanceof Squid || target instanceof Axolotl) && target.distanceToSqr(this.guardian) > 9.0;
        }
    }
}

