/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.cubemob;

import com.google.common.annotations.VisibleForTesting;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public abstract class AbstractCubeMob
extends AgeableMob {
    protected static final EntityDataAccessor<Integer> ID_SIZE = SynchedEntityData.defineId(AbstractCubeMob.class, EntityDataSerializers.INT);
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 127;
    public static final int MAX_NATURAL_SIZE = 4;
    private static final boolean DEFAULT_WAS_ON_GROUND = false;
    public float targetSquish;
    public float squish;
    public float oSquish;
    private boolean wasOnGround = false;

    protected AbstractCubeMob(EntityType<? extends AbstractCubeMob> type, Level level) {
        super((EntityType<? extends AgeableMob>)type, level);
        this.fixupDimensions();
        this.moveControl = new CubeMobMoveControl<AbstractCubeMob>(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CubeMobFloatGoal(this));
        this.goalSelector.addGoal(4, new CubeMobRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new CubeMobKeepOnJumpingGoal(this));
        this.addBehaviourGoals();
        this.addTargetingGoals();
    }

    protected abstract void addBehaviourGoals();

    protected abstract void addTargetingGoals();

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(ID_SIZE, 1);
    }

    @VisibleForTesting
    public void setSize(int size, boolean updateHealth) {
        int actualSize = Mth.clamp(size, 1, 127);
        this.entityData.set(ID_SIZE, actualSize);
        this.reapplyPosition();
        this.refreshDimensions();
        this.setcubeMobHealth(actualSize);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2f + 0.1f * (float)actualSize);
        if (updateHealth) {
            this.setHealth(this.getMaxHealth());
        }
    }

    protected void setcubeMobHealth(int actualSize) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(actualSize * actualSize);
    }

    public int getSize() {
        return this.entityData.get(ID_SIZE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Size", this.getSize() - 1);
        output.putBoolean("wasOnGround", this.wasOnGround);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setSize(input.getIntOr("Size", 0) + 1, false);
        super.readAdditionalSaveData(input);
        this.wasOnGround = input.getBooleanOr("wasOnGround", false);
    }

    public boolean isTiny() {
        return this.getSize() <= 1;
    }

    protected abstract @Nullable ParticleOptions getParticleType();

    @Override
    public void tick() {
        this.oSquish = this.squish;
        this.squish += (this.targetSquish - this.squish) * 0.5f;
        super.tick();
        if (this.onGround() && !this.wasOnGround) {
            float size = this.getDimensions(this.getPose()).width() * 2.0f;
            float radius = size / 2.0f;
            int i = 0;
            while ((float)i < size * 16.0f) {
                float dir = this.random.nextFloat() * ((float)Math.PI * 2);
                float d = this.random.nextFloat() * 0.5f + 0.5f;
                float xd = Mth.sin(dir) * radius * d;
                float zd = Mth.cos(dir) * radius * d;
                ParticleOptions particleType = this.getParticleType();
                if (particleType != null) {
                    this.level().addParticle(particleType, this.getX() + (double)xd, this.getY(), this.getZ() + (double)zd, 0.0, 0.0, 0.0);
                }
                ++i;
            }
            this.playSound(this.getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) / 0.8f);
            this.targetSquish = -0.5f;
        } else if (!this.onGround() && this.wasOnGround) {
            this.targetSquish = 1.0f;
        }
        this.wasOnGround = this.onGround();
        this.decreaseSquish();
    }

    protected void decreaseSquish() {
        this.targetSquish *= 0.6f;
    }

    protected int getJumpDelay() {
        return this.random.nextInt(20) + 10;
    }

    @Override
    public void refreshDimensions() {
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();
        super.refreshDimensions();
        this.setPos(oldX, oldY, oldZ);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (ID_SIZE.equals(accessor)) {
            this.refreshDimensions();
            this.setYRot(this.yHeadRot);
            this.yBodyRot = this.yHeadRot;
            if (this.isInWater() && this.random.nextInt(20) == 0) {
                this.doWaterSplashEffect();
            }
        }
        super.onSyncedDataUpdated(accessor);
    }

    public EntityType<? extends AbstractCubeMob> getType() {
        return super.getType();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        int size = this.getSize();
        if (!this.level().isClientSide() && size > 1 && this.isDeadOrDying()) {
            float width = this.getDimensions(this.getPose()).width();
            float xzCubeSpawnOffset = width / 2.0f;
            int halfSize = size / 2;
            int count = this.getSplitCount();
            PlayerTeam team = this.getTeam();
            for (int i = 0; i < count; ++i) {
                float xd = ((float)(i % 2) - 0.5f) * xzCubeSpawnOffset;
                float zd = ((float)(i / 2) - 0.5f) * xzCubeSpawnOffset;
                this.convertTo(this.getType(), new ConversionParams(ConversionType.SPLIT_ON_DEATH, false, false, team), EntitySpawnReason.TRIGGERED, cubeMob -> this.setUpSplitCube((AbstractCubeMob)cubeMob, halfSize, xd, zd));
            }
        }
        super.remove(reason);
    }

    protected void setUpSplitCube(AbstractCubeMob cubeMob, int halfSize, float xd, float zd) {
        cubeMob.setSize(halfSize, true);
        cubeMob.snapTo(this.getX() + (double)xd, this.getY() + 0.5, this.getZ() + (double)zd, this.random.nextFloat() * 360.0f, 0.0f);
    }

    protected int getSplitCount() {
        return 2 + this.random.nextInt(3);
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        if (entity instanceof IronGolem && this.isDealsDamage()) {
            this.dealDamage((LivingEntity)entity);
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (this.isDealsDamage()) {
            this.dealDamage(player);
        }
    }

    protected void dealDamage(LivingEntity target) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            DamageSource damageSource;
            ServerLevel level2 = (ServerLevel)level;
            if (this.isAlive() && this.isWithinMeleeAttackRange(target) && this.hasLineOfSight(target) && target.hurtServer(level2, damageSource = this.damageSources().mobAttack(this), this.getAttackDamage())) {
                this.playSound(SoundEvents.SLIME_ATTACK, 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
                EnchantmentHelper.doPostAttackEffects(level2, target, damageSource);
            }
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, (double)dimensions.height() - 0.015625 * (double)this.getSize() * (double)scale, 0.0);
    }

    protected boolean isDealsDamage() {
        return !this.isTiny() && this.isEffectiveAi();
    }

    protected float getAttackDamage() {
        return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.getType().getDimensions().scale(this.getSize());
    }

    @Override
    public void jumpFromGround() {
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x, this.getJumpPower(), movement.z);
        this.needsSync = true;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f * (float)this.getSize();
    }

    @Override
    public int getMaxHeadXRot() {
        return 0;
    }

    protected boolean doPlayJumpSound() {
        return this.getSize() > 0;
    }

    public float getSoundPitch() {
        float pitchAdjuster = this.isTiny() ? 1.4f : 0.8f;
        return ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) * pitchAdjuster;
    }

    protected abstract SoundEvent getJumpSound();

    @Override
    protected abstract SoundEvent getHurtSound(DamageSource var1);

    @Override
    protected abstract SoundEvent getDeathSound();

    protected abstract SoundEvent getSquishSound();

    @Override
    public @Nullable AbstractCubeMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        this.setSpawnSize(level, difficulty);
        return data;
    }

    protected void setSpawnSize(ServerLevelAccessor level, DifficultyInstance difficulty) {
        RandomSource random = level.getRandom();
        int sizeScale = random.nextInt(3);
        if (sizeScale < 2 && random.nextFloat() < 0.5f * difficulty.getSpecialMultiplier()) {
            ++sizeScale;
        }
        int size = 1 << sizeScale;
        this.setSize(size, true);
    }

    protected static class CubeMobMoveControl<T extends AbstractCubeMob>
    extends MoveControl<T> {
        private float yRot;
        private int jumpDelay;
        private boolean isAggressive;

        public CubeMobMoveControl(T cubeMob) {
            super(cubeMob);
            this.yRot = 180.0f * ((Entity)cubeMob).getYRot() / (float)Math.PI;
        }

        public void setDirection(float yRot, boolean isAggressive) {
            this.yRot = yRot;
            this.isAggressive = isAggressive;
        }

        public void setWantedMovement(double speedModifier) {
            this.speedModifier = speedModifier;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            ((AbstractCubeMob)this.mob).setYRot(this.rotlerp(((AbstractCubeMob)this.mob).getYRot(), this.yRot, 90.0f));
            ((AbstractCubeMob)this.mob).yHeadRot = ((AbstractCubeMob)this.mob).getYRot();
            ((AbstractCubeMob)this.mob).yBodyRot = ((AbstractCubeMob)this.mob).getYRot();
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                ((AbstractCubeMob)this.mob).setZza(0.0f);
                return;
            }
            this.operation = MoveControl.Operation.WAIT;
            if (((AbstractCubeMob)this.mob).onGround()) {
                ((AbstractCubeMob)this.mob).setSpeed((float)(this.speedModifier * ((AbstractCubeMob)this.mob).getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = ((AbstractCubeMob)this.mob).getJumpDelay();
                    if (this.isAggressive) {
                        this.jumpDelay /= 3;
                    }
                    ((AbstractCubeMob)this.mob).getJumpControl().jump();
                    if (((AbstractCubeMob)this.mob).doPlayJumpSound()) {
                        ((AbstractCubeMob)this.mob).playSound(((AbstractCubeMob)this.mob).getJumpSound(), ((AbstractCubeMob)this.mob).getSoundVolume(), ((AbstractCubeMob)this.mob).getSoundPitch());
                    }
                } else {
                    ((AbstractCubeMob)this.mob).xxa = 0.0f;
                    ((AbstractCubeMob)this.mob).zza = 0.0f;
                    ((AbstractCubeMob)this.mob).setSpeed(0.0f);
                }
            } else {
                ((AbstractCubeMob)this.mob).setSpeed((float)(this.speedModifier * ((AbstractCubeMob)this.mob).getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }
    }

    private static class CubeMobFloatGoal
    extends Goal {
        private final AbstractCubeMob cubeMob;

        public CubeMobFloatGoal(AbstractCubeMob mob) {
            this.cubeMob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
            mob.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return (this.cubeMob.isInWater() || this.cubeMob.isInLava()) && this.cubeMob.getMoveControl() instanceof CubeMobMoveControl;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MoveControl moveControl;
            if (this.cubeMob.getRandom().nextFloat() < 0.8f) {
                this.cubeMob.getJumpControl().jump();
            }
            if ((moveControl = this.cubeMob.getMoveControl()) instanceof CubeMobMoveControl) {
                CubeMobMoveControl cubeMobMoveControl = (CubeMobMoveControl)moveControl;
                cubeMobMoveControl.setWantedMovement(1.2);
            }
        }
    }

    private static class CubeMobRandomDirectionGoal
    extends Goal {
        private final AbstractCubeMob cubeMob;
        private float chosenDegrees;
        private int nextRandomizeTime;

        public CubeMobRandomDirectionGoal(AbstractCubeMob cubeMob) {
            this.cubeMob = cubeMob;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.cubeMob.getTarget() == null && (this.cubeMob.onGround() || this.cubeMob.isInWater() || this.cubeMob.isInLava() || this.cubeMob.hasEffect(MobEffects.LEVITATION)) && this.cubeMob.getMoveControl() instanceof CubeMobMoveControl;
        }

        @Override
        public void tick() {
            MoveControl moveControl;
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + this.cubeMob.getRandom().nextInt(60));
                this.chosenDegrees = this.cubeMob.getRandom().nextInt(360);
            }
            if ((moveControl = this.cubeMob.getMoveControl()) instanceof CubeMobMoveControl) {
                CubeMobMoveControl cubeMobMoveControl = (CubeMobMoveControl)moveControl;
                cubeMobMoveControl.setDirection(this.chosenDegrees, false);
            }
        }
    }

    private static class CubeMobKeepOnJumpingGoal
    extends Goal {
        private final AbstractCubeMob cubeMob;

        public CubeMobKeepOnJumpingGoal(AbstractCubeMob mob) {
            this.cubeMob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !this.cubeMob.isPassenger();
        }

        @Override
        public void tick() {
            MoveControl moveControl = this.cubeMob.getMoveControl();
            if (moveControl instanceof CubeMobMoveControl) {
                CubeMobMoveControl cubeMobMoveControl = (CubeMobMoveControl)moveControl;
                cubeMobMoveControl.setWantedMovement(1.0);
            }
        }
    }

    protected static class CubeMobAttackGoal
    extends Goal {
        private final AbstractCubeMob cubeMob;
        private int growTiredTimer;

        public CubeMobAttackGoal(AbstractCubeMob cubeMob) {
            this.cubeMob = cubeMob;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.cubeMob.getTarget();
            if (target == null) {
                return false;
            }
            if (!this.cubeMob.canAttack(target)) {
                return false;
            }
            return this.cubeMob.getMoveControl() instanceof CubeMobMoveControl;
        }

        @Override
        public void start() {
            this.growTiredTimer = CubeMobAttackGoal.reducedTickDelay(300);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.cubeMob.getTarget();
            if (target == null) {
                return false;
            }
            if (!this.cubeMob.canAttack(target)) {
                return false;
            }
            return --this.growTiredTimer > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            MoveControl moveControl;
            LivingEntity target = this.cubeMob.getTarget();
            if (target != null) {
                this.cubeMob.lookAt(target, 10.0f, 10.0f);
            }
            if ((moveControl = this.cubeMob.getMoveControl()) instanceof CubeMobMoveControl) {
                CubeMobMoveControl cubeMobMoveControl = (CubeMobMoveControl)moveControl;
                cubeMobMoveControl.setDirection(this.cubeMob.getYRot(), this.cubeMob.isDealsDamage());
            }
        }
    }
}

