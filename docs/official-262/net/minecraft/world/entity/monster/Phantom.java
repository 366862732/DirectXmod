/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Phantom
extends Mob
implements Enemy {
    public static final float FLAP_DEGREES_PER_TICK = 7.448451f;
    public static final int TICKS_PER_FLAP = Mth.ceil(24.166098f);
    private static final EntityDataAccessor<Integer> ID_SIZE = SynchedEntityData.defineId(Phantom.class, EntityDataSerializers.INT);
    private Vec3 moveTargetPoint = Vec3.ZERO;
    private @Nullable BlockPos anchorPoint;
    private AttackPhase attackPhase = AttackPhase.CIRCLE;

    public Phantom(EntityType<? extends Phantom> type, Level level) {
        super((EntityType<? extends Mob>)type, level);
        this.xpReward = 5;
        this.moveControl = new PhantomMoveControl(this, (Mob)this);
        this.lookControl = new PhantomLookControl(this);
    }

    @Override
    public boolean isFlapping() {
        return (this.getUniqueFlapTickOffset() + this.tickCount) % TICKS_PER_FLAP == 0;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new PhantomBodyRotationControl(this, this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PhantomAttackStrategyGoal(this));
        this.goalSelector.addGoal(2, new PhantomSweepAttackGoal(this));
        this.goalSelector.addGoal(3, new PhantomCircleAroundAnchorGoal(this));
        this.targetSelector.addGoal(1, new PhantomAttackPlayerTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(ID_SIZE, 0);
    }

    public void setPhantomSize(int size) {
        this.entityData.set(ID_SIZE, Mth.clamp(size, 0, 64));
    }

    private void updatePhantomSizeInfo() {
        this.refreshDimensions();
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6 + this.getPhantomSize());
    }

    public int getPhantomSize() {
        return this.entityData.get(ID_SIZE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (ID_SIZE.equals(accessor)) {
            this.updatePhantomSizeInfo();
        }
        super.onSyncedDataUpdated(accessor);
    }

    public int getUniqueFlapTickOffset() {
        return this.getId() * 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            float anim = Mth.cos((float)(this.getUniqueFlapTickOffset() + this.tickCount) * 7.448451f * ((float)Math.PI / 180) + (float)Math.PI);
            float nextAnim = Mth.cos((float)(this.getUniqueFlapTickOffset() + this.tickCount + 1) * 7.448451f * ((float)Math.PI / 180) + (float)Math.PI);
            if (anim > 0.0f && nextAnim <= 0.0f) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.PHANTOM_FLAP, this.getSoundSource(), 0.95f + this.random.nextFloat() * 0.05f, 0.95f + this.random.nextFloat() * 0.05f, false);
            }
            float width = this.getBbWidth() * 1.48f;
            float c = Mth.cos(this.getYRot() * ((float)Math.PI / 180)) * width;
            float s = Mth.sin(this.getYRot() * ((float)Math.PI / 180)) * width;
            float h = (0.3f + anim * 0.45f) * this.getBbHeight() * 2.5f;
            this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() + (double)c, this.getY() + (double)h, this.getZ() + (double)s, 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() - (double)c, this.getY() + (double)h, this.getZ() - (double)s, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, 0.2f);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        this.anchorPoint = this.blockPosition().above(5);
        this.setPhantomSize(0);
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.anchorPoint = input.read("anchor_pos", BlockPos.CODEC).orElse(null);
        this.setPhantomSize(input.getIntOr("size", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("anchor_pos", BlockPos.CODEC, this.anchorPoint);
        output.putInt("size", this.getPhantomSize());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        int size = this.getPhantomSize();
        EntityDimensions originalDimensions = super.getDefaultDimensions(pose);
        return originalDimensions.scale(1.0f + 0.15f * (float)size);
    }

    private boolean canAttack(ServerLevel level, LivingEntity target, TargetingConditions targetConditions) {
        return targetConditions.test(level, this, target);
    }

    private static enum AttackPhase {
        CIRCLE,
        SWOOP;

    }

    private class PhantomMoveControl<T extends Mob>
    extends MoveControl<T> {
        private float speed;
        final /* synthetic */ Phantom this$0;

        public PhantomMoveControl(T mob) {
            reference v0 = var1_1;
            Objects.requireNonNull(v0);
            this.this$0 = v0;
            super(mob);
            this.speed = 0.1f;
        }

        @Override
        public void tick() {
            if (this.this$0.horizontalCollision) {
                this.this$0.setYRot(this.this$0.getYRot() + 180.0f);
                this.speed = 0.1f;
            }
            double tdx = this.this$0.moveTargetPoint.x - this.this$0.getX();
            double tdy = this.this$0.moveTargetPoint.y - this.this$0.getY();
            double tdz = this.this$0.moveTargetPoint.z - this.this$0.getZ();
            double sd = Math.sqrt(tdx * tdx + tdz * tdz);
            if (Math.abs(sd) > (double)1.0E-5f) {
                double yRelativeScale = 1.0 - Math.abs(tdy * (double)0.7f) / sd;
                sd = Math.sqrt((tdx *= yRelativeScale) * tdx + (tdz *= yRelativeScale) * tdz);
                double sd2 = Math.sqrt(tdx * tdx + tdz * tdz + tdy * tdy);
                float prev = this.this$0.getYRot();
                float angle = (float)Mth.atan2(tdz, tdx);
                float a = Mth.wrapDegrees(this.this$0.getYRot() + 90.0f);
                float b = Mth.wrapDegrees(angle * 57.295776f);
                this.this$0.setYRot(Mth.approachDegrees(a, b, 4.0f) - 90.0f);
                this.this$0.yBodyRot = this.this$0.getYRot();
                this.speed = Mth.degreesDifferenceAbs(prev, this.this$0.getYRot()) < 3.0f ? Mth.approach(this.speed, 1.8f, 0.005f * (1.8f / this.speed)) : Mth.approach(this.speed, 0.2f, 0.025f);
                float xRotD = (float)(-(Mth.atan2(-tdy, sd) * 57.2957763671875));
                this.this$0.setXRot(xRotD);
                float moveAngle = this.this$0.getYRot() + 90.0f;
                double txd = (double)(this.speed * Mth.cos(moveAngle * ((float)Math.PI / 180))) * Math.abs(tdx / sd2);
                double tzd = (double)(this.speed * Mth.sin(moveAngle * ((float)Math.PI / 180))) * Math.abs(tdz / sd2);
                double tyd = (double)(this.speed * Mth.sin(xRotD * ((float)Math.PI / 180))) * Math.abs(tdy / sd2);
                Vec3 movement = this.this$0.getDeltaMovement();
                this.this$0.setDeltaMovement(movement.add(new Vec3(txd, tyd, tzd).subtract(movement).scale(0.2)));
            }
        }
    }

    private static class PhantomLookControl
    extends LookControl {
        public PhantomLookControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
        }
    }

    private class PhantomBodyRotationControl
    extends BodyRotationControl {
        final /* synthetic */ Phantom this$0;

        public PhantomBodyRotationControl(Phantom phantom, Mob mob) {
            Phantom phantom2 = phantom;
            Objects.requireNonNull(phantom2);
            this.this$0 = phantom2;
            super(mob);
        }

        @Override
        public void clientTick() {
            this.this$0.yHeadRot = this.this$0.yBodyRot;
            this.this$0.yBodyRot = this.this$0.getYRot();
        }
    }

    private class PhantomAttackStrategyGoal
    extends Goal {
        private int nextSweepTick;
        final /* synthetic */ Phantom this$0;

        private PhantomAttackStrategyGoal(Phantom phantom) {
            Phantom phantom2 = phantom;
            Objects.requireNonNull(phantom2);
            this.this$0 = phantom2;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.this$0.getTarget();
            if (target != null) {
                return this.this$0.canAttack(PhantomAttackStrategyGoal.getServerLevel(this.this$0.level()), target, TargetingConditions.DEFAULT);
            }
            return false;
        }

        @Override
        public void start() {
            this.nextSweepTick = this.adjustedTickDelay(10);
            this.this$0.attackPhase = AttackPhase.CIRCLE;
            this.setAnchorAboveTarget();
        }

        @Override
        public void stop() {
            if (this.this$0.anchorPoint != null) {
                this.this$0.anchorPoint = this.this$0.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.this$0.anchorPoint).above(10 + this.this$0.random.nextInt(20));
            }
        }

        @Override
        public void tick() {
            if (this.this$0.attackPhase == AttackPhase.CIRCLE) {
                --this.nextSweepTick;
                if (this.nextSweepTick <= 0) {
                    this.this$0.attackPhase = AttackPhase.SWOOP;
                    this.setAnchorAboveTarget();
                    this.nextSweepTick = this.adjustedTickDelay((8 + this.this$0.random.nextInt(4)) * 20);
                    this.this$0.playSound(SoundEvents.PHANTOM_SWOOP, 10.0f, 0.95f + this.this$0.random.nextFloat() * 0.1f);
                }
            }
        }

        private void setAnchorAboveTarget() {
            if (this.this$0.anchorPoint == null) {
                return;
            }
            this.this$0.anchorPoint = this.this$0.getTarget().blockPosition().above(20 + this.this$0.random.nextInt(20));
            if (this.this$0.anchorPoint.getY() < this.this$0.level().getSeaLevel()) {
                this.this$0.anchorPoint = new BlockPos(this.this$0.anchorPoint.getX(), this.this$0.level().getSeaLevel() + 1, this.this$0.anchorPoint.getZ());
            }
        }
    }

    private class PhantomSweepAttackGoal
    extends PhantomMoveTargetGoal {
        private static final int CAT_SEARCH_TICK_DELAY = 20;
        private boolean isScaredOfCat;
        private int catSearchTick;
        final /* synthetic */ Phantom this$0;

        private PhantomSweepAttackGoal(Phantom phantom) {
            Phantom phantom2 = phantom;
            Objects.requireNonNull(phantom2);
            this.this$0 = phantom2;
            super(phantom);
        }

        @Override
        public boolean canUse() {
            return this.this$0.getTarget() != null && this.this$0.attackPhase == AttackPhase.SWOOP;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.this$0.getTarget();
            if (target == null) {
                return false;
            }
            if (!target.isAlive()) {
                return false;
            }
            if (target instanceof Player) {
                Player player = (Player)target;
                if (target.isSpectator() || player.isCreative()) {
                    return false;
                }
            }
            if (!this.canUse()) {
                return false;
            }
            if (this.this$0.tickCount > this.catSearchTick) {
                this.catSearchTick = this.this$0.tickCount + 20;
                List<Entity> cats = this.this$0.level().getEntitiesOfClass(Cat.class, this.this$0.getBoundingBox().inflate(16.0), EntitySelector.ENTITY_STILL_ALIVE);
                for (Cat cat : cats) {
                    cat.hiss();
                }
                this.isScaredOfCat = !cats.isEmpty();
            }
            return !this.isScaredOfCat;
        }

        @Override
        public void stop() {
            this.this$0.setTarget(null);
            this.this$0.attackPhase = AttackPhase.CIRCLE;
        }

        @Override
        public void tick() {
            LivingEntity target = this.this$0.getTarget();
            if (target == null) {
                return;
            }
            this.this$0.moveTargetPoint = new Vec3(target.getX(), target.getY(0.5), target.getZ());
            if (this.this$0.getBoundingBox().inflate(0.2f).intersects(target.getBoundingBox())) {
                this.this$0.doHurtTarget(PhantomSweepAttackGoal.getServerLevel(this.this$0.level()), target);
                this.this$0.attackPhase = AttackPhase.CIRCLE;
                if (!this.this$0.isSilent()) {
                    this.this$0.level().levelEvent(1039, this.this$0.blockPosition(), 0);
                }
            } else if (this.this$0.horizontalCollision || this.this$0.hurtTime > 0) {
                this.this$0.attackPhase = AttackPhase.CIRCLE;
            }
        }
    }

    private class PhantomCircleAroundAnchorGoal
    extends PhantomMoveTargetGoal {
        private float angle;
        private float distance;
        private float height;
        private float clockwise;
        final /* synthetic */ Phantom this$0;

        private PhantomCircleAroundAnchorGoal(Phantom phantom) {
            Phantom phantom2 = phantom;
            Objects.requireNonNull(phantom2);
            this.this$0 = phantom2;
            super(phantom);
        }

        @Override
        public boolean canUse() {
            return this.this$0.getTarget() == null || this.this$0.attackPhase == AttackPhase.CIRCLE;
        }

        @Override
        public void start() {
            this.distance = 5.0f + this.this$0.random.nextFloat() * 10.0f;
            this.height = -4.0f + this.this$0.random.nextFloat() * 9.0f;
            this.clockwise = this.this$0.random.nextBoolean() ? 1.0f : -1.0f;
            this.selectNext();
        }

        @Override
        public void tick() {
            if (this.this$0.random.nextInt(this.adjustedTickDelay(350)) == 0) {
                this.height = -4.0f + this.this$0.random.nextFloat() * 9.0f;
            }
            if (this.this$0.random.nextInt(this.adjustedTickDelay(250)) == 0) {
                this.distance += 1.0f;
                if (this.distance > 15.0f) {
                    this.distance = 5.0f;
                    this.clockwise = -this.clockwise;
                }
            }
            if (this.this$0.random.nextInt(this.adjustedTickDelay(450)) == 0) {
                this.angle = this.this$0.random.nextFloat() * 2.0f * (float)Math.PI;
                this.selectNext();
            }
            if (this.touchingTarget()) {
                this.selectNext();
            }
            if (this.this$0.moveTargetPoint.y < this.this$0.getY() && !this.this$0.level().isEmptyBlock(this.this$0.blockPosition().below(1))) {
                this.height = Math.max(1.0f, this.height);
                this.selectNext();
            }
            if (this.this$0.moveTargetPoint.y > this.this$0.getY() && !this.this$0.level().isEmptyBlock(this.this$0.blockPosition().above(1))) {
                this.height = Math.min(-1.0f, this.height);
                this.selectNext();
            }
        }

        private void selectNext() {
            if (this.this$0.anchorPoint == null) {
                this.this$0.anchorPoint = this.this$0.blockPosition();
            }
            this.angle += this.clockwise * 15.0f * ((float)Math.PI / 180);
            this.this$0.moveTargetPoint = Vec3.atLowerCornerOf(this.this$0.anchorPoint).add(this.distance * Mth.cos(this.angle), -4.0f + this.height, this.distance * Mth.sin(this.angle));
        }
    }

    private class PhantomAttackPlayerTargetGoal
    extends Goal {
        private final TargetingConditions attackTargeting;
        private int nextScanTick;
        final /* synthetic */ Phantom this$0;

        private PhantomAttackPlayerTargetGoal(Phantom phantom) {
            Phantom phantom2 = phantom;
            Objects.requireNonNull(phantom2);
            this.this$0 = phantom2;
            this.attackTargeting = TargetingConditions.forCombat().range(64.0);
            this.nextScanTick = PhantomAttackPlayerTargetGoal.reducedTickDelay(20);
        }

        @Override
        public boolean canUse() {
            if (this.nextScanTick > 0) {
                --this.nextScanTick;
                return false;
            }
            this.nextScanTick = PhantomAttackPlayerTargetGoal.reducedTickDelay(60);
            ServerLevel level = PhantomAttackPlayerTargetGoal.getServerLevel(this.this$0.level());
            List<Player> players = level.getNearbyPlayers(this.attackTargeting, this.this$0, this.this$0.getBoundingBox().inflate(16.0, 64.0, 16.0));
            if (!players.isEmpty()) {
                players.sort(Comparator.comparing(Entity::getY).reversed());
                for (Player player : players) {
                    if (!this.this$0.canAttack(level, player, TargetingConditions.DEFAULT)) continue;
                    this.this$0.setTarget(player);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.this$0.getTarget();
            if (target != null) {
                return this.this$0.canAttack(PhantomAttackPlayerTargetGoal.getServerLevel(this.this$0.level()), target, TargetingConditions.DEFAULT);
            }
            return false;
        }
    }

    private abstract class PhantomMoveTargetGoal
    extends Goal {
        final /* synthetic */ Phantom this$0;

        public PhantomMoveTargetGoal(Phantom phantom) {
            Phantom phantom2 = phantom;
            Objects.requireNonNull(phantom2);
            this.this$0 = phantom2;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        protected boolean touchingTarget() {
            return this.this$0.moveTargetPoint.distanceToSqr(this.this$0.getX(), this.this$0.getY(), this.this$0.getZ()) < 4.0;
        }
    }
}

