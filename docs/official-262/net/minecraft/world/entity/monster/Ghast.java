/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Ghast
extends Mob
implements Enemy {
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(Ghast.class, EntityDataSerializers.BOOLEAN);
    private static final byte DEFAULT_EXPLOSION_POWER = 1;
    private int explosionPower = 1;

    public Ghast(EntityType<? extends Ghast> type, Level level) {
        super((EntityType<? extends Mob>)type, level);
        this.xpReward = 5;
        this.moveControl = new GhastMoveControl<Ghast>(this, false, () -> false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new RandomFloatAroundGoal(this));
        this.goalSelector.addGoal(7, new GhastLookGoal(this));
        this.goalSelector.addGoal(7, new GhastShootFireballGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false, (target, level) -> Math.abs(target.getY() - this.getY()) <= 4.0));
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_IS_CHARGING);
    }

    public void setCharging(boolean onOff) {
        this.entityData.set(DATA_IS_CHARGING, onOff);
    }

    public int getExplosionPower() {
        return this.explosionPower;
    }

    private static boolean isReflectedFireball(DamageSource source) {
        return source.getDirectEntity() instanceof LargeFireball && source.getEntity() instanceof Player;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return this.isInvulnerable() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !Ghast.isReflectedFireball(source) && super.isInvulnerableTo(level, source);
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
        this.travelFlying(input, 0.02f);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (Ghast.isReflectedFireball(source)) {
            super.hurtServer(level, source, 1000.0f);
            return true;
        }
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_IS_CHARGING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.FOLLOW_RANGE, 100.0).add(Attributes.CAMERA_DISTANCE, 8.0).add(Attributes.FLYING_SPEED, 0.06);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0f;
    }

    public static boolean checkGhastSpawnRules(EntityType<Ghast> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && random.nextInt(20) == 0 && Ghast.checkMobSpawnRules(type, level, spawnReason, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("ExplosionPower", (byte)this.explosionPower);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.explosionPower = input.getByteOr("ExplosionPower", (byte)1);
    }

    @Override
    public boolean supportQuadLeashAsHolder() {
        return true;
    }

    @Override
    public double leashElasticDistance() {
        return 10.0;
    }

    @Override
    public double leashSnapDistance() {
        return 16.0;
    }

    public static void faceMovementDirection(Mob ghast) {
        if (ghast.getTarget() == null) {
            Vec3 movement = ghast.getDeltaMovement();
            ghast.setYRot(-((float)Mth.atan2(movement.x, movement.z)) * 57.295776f);
            ghast.yBodyRot = ghast.getYRot();
        } else {
            LivingEntity target = ghast.getTarget();
            double maxDist = 64.0;
            if (target.distanceToSqr(ghast) < 4096.0) {
                double xdd = target.getX() - ghast.getX();
                double zdd = target.getZ() - ghast.getZ();
                ghast.setYRot(-((float)Mth.atan2(xdd, zdd)) * 57.295776f);
                ghast.yBodyRot = ghast.getYRot();
            }
        }
    }

    public static class GhastMoveControl<T extends Mob>
    extends MoveControl<T> {
        private int floatDuration;
        private final boolean careful;
        private final BooleanSupplier shouldBeStopped;

        public GhastMoveControl(T ghast, boolean careful, BooleanSupplier shouldBeStopped) {
            super(ghast);
            this.careful = careful;
            this.shouldBeStopped = shouldBeStopped;
        }

        @Override
        public void tick() {
            if (this.shouldBeStopped.getAsBoolean()) {
                this.operation = MoveControl.Operation.WAIT;
                this.mob.stopInPlace();
            }
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                return;
            }
            if (this.floatDuration-- <= 0) {
                this.floatDuration += this.mob.getRandom().nextInt(5) + 2;
                Vec3 travel = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
                if (this.canReach(travel)) {
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(travel.normalize().scale(this.mob.getAttributeValue(Attributes.FLYING_SPEED) * 5.0 / 3.0)));
                } else {
                    this.operation = MoveControl.Operation.WAIT;
                }
            }
        }

        private boolean canReach(Vec3 travel) {
            AABB aabb = this.mob.getBoundingBox();
            AABB aabbAtDestination = aabb.move(travel);
            if (this.careful) {
                for (BlockPos pos : BlockPos.betweenClosed(aabbAtDestination.inflate(1.0))) {
                    if (this.blockTraversalPossible(this.mob.level(), null, null, pos, false, false)) continue;
                    return false;
                }
            }
            boolean isInWater = this.mob.isInWater();
            boolean isInLava = this.mob.isInLava();
            Vec3 start = this.mob.position();
            Vec3 end = start.add(travel);
            return BlockGetter.forEachBlockIntersectedBetween(start, end, aabbAtDestination, (blockPos, i) -> {
                if (aabb.intersects(blockPos)) {
                    return true;
                }
                return this.blockTraversalPossible(this.mob.level(), start, end, blockPos, isInWater, isInLava);
            });
        }

        private boolean blockTraversalPossible(BlockGetter level, @Nullable Vec3 start, @Nullable Vec3 end, BlockPos pos, boolean canPathThroughWater, boolean canPathThroughLava) {
            boolean pathNoCollisions;
            boolean preciseBlockCollisions;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return true;
            }
            boolean bl = preciseBlockCollisions = start != null && end != null;
            boolean bl2 = preciseBlockCollisions ? !this.mob.collidedWithShapeMovingFrom(start, end, state.getCollisionShape(level, pos).move(new Vec3(pos)).toAabbs()) : (pathNoCollisions = state.getCollisionShape(level, pos).isEmpty());
            if (!this.careful) {
                return pathNoCollisions;
            }
            if (state.is(BlockTags.HAPPY_GHAST_AVOIDS)) {
                return false;
            }
            FluidState fluidState = level.getFluidState(pos);
            if (!(fluidState.isEmpty() || preciseBlockCollisions && !this.mob.collidedWithFluid(fluidState, pos, start, end))) {
                if (fluidState.is(FluidTags.WATER)) {
                    return canPathThroughWater;
                }
                if (fluidState.is(FluidTags.LAVA)) {
                    return canPathThroughLava;
                }
            }
            return pathNoCollisions;
        }
    }

    public static class RandomFloatAroundGoal
    extends Goal {
        private static final int MAX_ATTEMPTS = 64;
        private final Mob ghast;
        private final int distanceToBlocks;

        public RandomFloatAroundGoal(Mob ghast) {
            this(ghast, 0);
        }

        public RandomFloatAroundGoal(Mob ghast, int distanceToBlocks) {
            this.ghast = ghast;
            this.distanceToBlocks = distanceToBlocks;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            double zd;
            double yd;
            MoveControl moveControl = this.ghast.getMoveControl();
            if (!moveControl.hasWanted()) {
                return true;
            }
            double xd = moveControl.getWantedX() - this.ghast.getX();
            double dd = xd * xd + (yd = moveControl.getWantedY() - this.ghast.getY()) * yd + (zd = moveControl.getWantedZ() - this.ghast.getZ()) * zd;
            return dd < 1.0 || dd > 3600.0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            Vec3 result = RandomFloatAroundGoal.getSuitableFlyToPosition(this.ghast, this.distanceToBlocks);
            this.ghast.getMoveControl().setWantedPosition(result.x(), result.y(), result.z(), 1.0);
        }

        public static Vec3 getSuitableFlyToPosition(Mob mob, int distanceToBlocks) {
            BlockPos pos;
            int heightY;
            Level level = mob.level();
            RandomSource random = mob.getRandom();
            Vec3 center = mob.position();
            Vec3 result = null;
            for (int i = 0; i < 64; ++i) {
                result = RandomFloatAroundGoal.chooseRandomPositionWithRestriction(mob, center, random);
                if (result == null || !RandomFloatAroundGoal.isGoodTarget(level, result, distanceToBlocks)) continue;
                return result;
            }
            if (result == null) {
                result = RandomFloatAroundGoal.chooseRandomPosition(center, random);
            }
            if ((heightY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (pos = BlockPos.containing(result)).getX(), pos.getZ())) < pos.getY() && heightY > level.getMinY()) {
                result = new Vec3(result.x(), mob.getY() - Math.abs(mob.getY() - result.y()), result.z());
            }
            return result;
        }

        private static boolean isGoodTarget(Level level, Vec3 target, int distanceToBlocks) {
            if (distanceToBlocks <= 0) {
                return true;
            }
            BlockPos pos = BlockPos.containing(target);
            if (!level.getBlockState(pos).isAir()) {
                return false;
            }
            for (Direction dir : Direction.values()) {
                for (int i = 1; i < distanceToBlocks; ++i) {
                    BlockPos offset = pos.relative(dir, i);
                    if (level.getBlockState(offset).isAir()) continue;
                    return true;
                }
            }
            return false;
        }

        private static Vec3 chooseRandomPosition(Vec3 center, RandomSource random) {
            double xTarget = center.x() + (double)((random.nextFloat() * 2.0f - 1.0f) * 16.0f);
            double yTarget = center.y() + (double)((random.nextFloat() * 2.0f - 1.0f) * 16.0f);
            double zTarget = center.z() + (double)((random.nextFloat() * 2.0f - 1.0f) * 16.0f);
            return new Vec3(xTarget, yTarget, zTarget);
        }

        private static @Nullable Vec3 chooseRandomPositionWithRestriction(Mob mob, Vec3 center, RandomSource random) {
            Vec3 target = RandomFloatAroundGoal.chooseRandomPosition(center, random);
            if (mob.hasHome() && !mob.isWithinHome(target)) {
                return null;
            }
            return target;
        }
    }

    public static class GhastLookGoal
    extends Goal {
        private final Mob ghast;

        public GhastLookGoal(Mob ghast) {
            this.ghast = ghast;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Ghast.faceMovementDirection(this.ghast);
        }
    }

    private static class GhastShootFireballGoal
    extends Goal {
        private final Ghast ghast;
        public int chargeTime;

        public GhastShootFireballGoal(Ghast ghast) {
            this.ghast = ghast;
        }

        @Override
        public boolean canUse() {
            return this.ghast.getTarget() != null;
        }

        @Override
        public void start() {
            this.chargeTime = 0;
        }

        @Override
        public void stop() {
            this.ghast.setCharging(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.ghast.getTarget();
            if (target == null) {
                return;
            }
            double maxDist = 64.0;
            if (target.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight(target)) {
                Level level = this.ghast.level();
                ++this.chargeTime;
                if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                    level.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
                }
                if (this.chargeTime == 20) {
                    double d = 4.0;
                    Vec3 viewVector = this.ghast.getViewVector(1.0f);
                    double xdd = target.getX() - (this.ghast.getX() + viewVector.x * 4.0);
                    double ydd = target.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                    double zdd = target.getZ() - (this.ghast.getZ() + viewVector.z * 4.0);
                    Vec3 direction = new Vec3(xdd, ydd, zdd);
                    if (!this.ghast.isSilent()) {
                        level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                    }
                    LargeFireball entity = new LargeFireball(level, (LivingEntity)this.ghast, direction.normalize(), this.ghast.getExplosionPower());
                    entity.setPos(this.ghast.getX() + viewVector.x * 4.0, this.ghast.getY(0.5) + 0.5, entity.getZ() + viewVector.z * 4.0);
                    level.addFreshEntity(entity);
                    this.chargeTime = -40;
                }
            } else if (this.chargeTime > 0) {
                --this.chargeTime;
            }
            this.ghast.setCharging(this.chargeTime > 10);
        }
    }
}

