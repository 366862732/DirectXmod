/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.turtle;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Turtle
extends Animal {
    private static final EntityDataAccessor<Boolean> HAS_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LAYING_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
    private static final float BABY_SCALE = 0.3f;
    private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.TURTLE.getDimensions().withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, EntityTypes.TURTLE.getHeight(), -0.25f)).scale(0.3f);
    private static final boolean DEFAULT_HAS_EGG = false;
    private int layEggCounter;
    public static final TargetingConditions.Selector BABY_ON_LAND_SELECTOR = (target, level) -> target.isBaby() && !target.isInWater();
    private BlockPos homePos = BlockPos.ZERO;
    private @Nullable BlockPos travelPos;
    private boolean goingHome;

    public Turtle(EntityType<? extends Turtle> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0f);
        this.setPathfindingMalus(PathType.DOOR_IRON_CLOSED, -1.0f);
        this.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, -1.0f);
        this.setPathfindingMalus(PathType.DOOR_OPEN, -1.0f);
        this.moveControl = new TurtleMoveControl<Turtle>(this);
    }

    public void setHomePos(BlockPos pos) {
        this.homePos = pos;
    }

    public BlockPos getHomePos() {
        return this.homePos;
    }

    public boolean hasEgg() {
        return this.entityData.get(HAS_EGG);
    }

    private void setHasEgg(boolean onOff) {
        this.entityData.set(HAS_EGG, onOff);
    }

    public boolean isLayingEgg() {
        return this.entityData.get(LAYING_EGG);
    }

    private void setLayingEgg(boolean on) {
        this.layEggCounter = on ? 1 : 0;
        this.entityData.set(LAYING_EGG, on);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(HAS_EGG, false);
        entityData.define(LAYING_EGG, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("home_pos", BlockPos.CODEC, this.homePos);
        output.putBoolean("has_egg", this.hasEgg());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setHomePos(input.read("home_pos", BlockPos.CODEC).orElse(this.blockPosition()));
        super.readAdditionalSaveData(input);
        this.setHasEgg(input.getBooleanOr("has_egg", false));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        this.setHomePos(this.blockPosition());
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public static boolean checkTurtleSpawnRules(EntityType<Turtle> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return pos.getY() < level.getSeaLevel() + 4 && TurtleEggBlock.onSand(level, pos) && Turtle.isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TurtlePanicGoal(this, 1.2));
        this.goalSelector.addGoal(1, new TurtleBreedGoal(this, 1.0));
        this.goalSelector.addGoal(1, new TurtleLayEggGoal(this, 1.0));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.1, i -> i.is(ItemTags.TURTLE_FOOD), false));
        this.goalSelector.addGoal(3, new TurtleGoToWaterGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TurtleGoHomeGoal(this, 1.0));
        this.goalSelector.addGoal(7, new TurtleTravelGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new TurtleRandomStrollGoal(this, 1.0, 100));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (!this.isInWater() && this.onGround() && !this.isBaby()) {
            return SoundEvents.TURTLE_AMBIENT_LAND;
        }
        return super.getAmbientSound();
    }

    @Override
    protected void playSwimSound(float volume) {
        super.playSwimSound(volume * 1.5f);
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.TURTLE_SWIM;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        if (this.isBaby()) {
            return SoundEvents.TURTLE_HURT_BABY;
        }
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        if (this.isBaby()) {
            return SoundEvents.TURTLE_DEATH_BABY;
        }
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        SoundEvent sound = this.isBaby() ? SoundEvents.TURTLE_SHAMBLE_BABY : SoundEvents.TURTLE_SHAMBLE;
        this.playSound(sound, 0.15f, 1.0f);
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && !this.hasEgg();
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.15f;
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.3f : 1.0f;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new TurtlePathNavigation(this, level);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.TURTLE.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.TURTLE_FOOD);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (!this.goingHome && level.getFluidState(pos).is(FluidTags.WATER)) {
            return 10.0f;
        }
        if (TurtleEggBlock.onSand(level, pos)) {
            return 10.0f;
        }
        return level.getPathfindingCostFromLightLevels(pos);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isAlive() && this.isLayingEgg() && this.layEggCounter >= 1 && this.layEggCounter % 5 == 0) {
            BlockPos pos = this.blockPosition();
            if (TurtleEggBlock.onSand(this.level(), pos)) {
                this.level().levelEvent(2001, pos, Block.getId(this.level().getBlockState(pos.below())));
                this.gameEvent(GameEvent.ENTITY_ACTION);
            }
        }
    }

    @Override
    protected void ageBoundaryReached() {
        ServerLevel level;
        Level level2;
        super.ageBoundaryReached();
        if (!this.isBaby() && (level2 = this.level()) instanceof ServerLevel && (level = (ServerLevel)level2).getGameRules().get(GameRules.MOB_DROPS).booleanValue()) {
            this.dropFromGiftLootTable(level, BuiltInLootTables.TURTLE_GROW, this::spawnAtLocation);
        }
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(0.1f, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (!(this.getTarget() != null || this.goingHome && this.homePos.closerToCenterThan(this.position(), 20.0))) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
        }
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightningBolt) {
        this.hurtServer(level, this.damageSources().lightningBolt(), Float.MAX_VALUE);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    private static class TurtleMoveControl<T extends Turtle>
    extends MoveControl<T> {
        public TurtleMoveControl(T turtle) {
            super(turtle);
        }

        private void updateSpeed() {
            if (((Turtle)this.mob).isInWater()) {
                ((Turtle)this.mob).setDeltaMovement(((Turtle)this.mob).getDeltaMovement().add(0.0, 0.005, 0.0));
                if (!((Turtle)this.mob).getHomePos().closerToCenterThan(((Turtle)this.mob).position(), 16.0)) {
                    ((Turtle)this.mob).setSpeed(Math.max(((Turtle)this.mob).getSpeed() / 2.0f, 0.08f));
                }
                if (((Turtle)this.mob).isBaby()) {
                    ((Turtle)this.mob).setSpeed(Math.max(((Turtle)this.mob).getSpeed() / 3.0f, 0.06f));
                }
            } else if (((Turtle)this.mob).onGround()) {
                ((Turtle)this.mob).setSpeed(Math.max(((Turtle)this.mob).getSpeed() / 2.0f, 0.06f));
            }
        }

        @Override
        public void tick() {
            double zd;
            double yd;
            this.updateSpeed();
            if (this.operation != MoveControl.Operation.MOVE_TO || ((Turtle)this.mob).getNavigation().isDone()) {
                ((Turtle)this.mob).setSpeed(0.0f);
                return;
            }
            double xd = this.wantedX - ((Turtle)this.mob).getX();
            double dd = Math.sqrt(xd * xd + (yd = this.wantedY - ((Turtle)this.mob).getY()) * yd + (zd = this.wantedZ - ((Turtle)this.mob).getZ()) * zd);
            if (dd < (double)1.0E-5f) {
                ((Turtle)this.mob).setSpeed(0.0f);
                return;
            }
            yd /= dd;
            float yRotD = (float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f;
            ((Turtle)this.mob).setYRot(this.rotlerp(((Turtle)this.mob).getYRot(), yRotD, 90.0f));
            ((Turtle)this.mob).yBodyRot = ((Turtle)this.mob).getYRot();
            float targetSpeed = (float)(this.speedModifier * ((Turtle)this.mob).getAttributeValue(Attributes.MOVEMENT_SPEED));
            ((Turtle)this.mob).setSpeed(Mth.lerp(0.125f, ((Turtle)this.mob).getSpeed(), targetSpeed));
            ((Turtle)this.mob).setDeltaMovement(((Turtle)this.mob).getDeltaMovement().add(0.0, (double)((Turtle)this.mob).getSpeed() * yd * 0.1, 0.0));
        }
    }

    private static class TurtlePanicGoal
    extends PanicGoal {
        public TurtlePanicGoal(Turtle turtle, double speedModifier) {
            super(turtle, speedModifier);
        }

        @Override
        public boolean canUse() {
            if (!this.shouldPanic()) {
                return false;
            }
            BlockPos blockPos = this.lookForWater(this.mob.level(), this.mob, 7);
            if (blockPos != null) {
                this.posX = blockPos.getX();
                this.posY = blockPos.getY();
                this.posZ = blockPos.getZ();
                return true;
            }
            return this.findRandomPosition();
        }
    }

    private static class TurtleBreedGoal
    extends BreedGoal {
        private final Turtle turtle;

        public TurtleBreedGoal(Turtle turtle, double speedModifier) {
            super(turtle, speedModifier);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.turtle.hasEgg();
        }

        @Override
        protected void breed() {
            ServerPlayer loveCause = this.animal.getLoveCause();
            if (loveCause == null && this.partner.getLoveCause() != null) {
                loveCause = this.partner.getLoveCause();
            }
            if (loveCause != null) {
                loveCause.awardStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(loveCause, this.animal, this.partner, null);
            }
            this.turtle.setHasEgg(true);
            this.animal.setAge(6000);
            this.partner.setAge(6000);
            this.animal.resetLove();
            this.partner.resetLove();
            RandomSource random = this.animal.getRandom();
            if (TurtleBreedGoal.getServerLevel(this.level).getGameRules().get(GameRules.MOB_DROPS).booleanValue()) {
                this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
            }
        }
    }

    private static class TurtleLayEggGoal
    extends MoveToBlockGoal {
        private final Turtle turtle;

        public TurtleLayEggGoal(Turtle turtle, double speedModifier) {
            super(turtle, speedModifier, 16);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            if (this.turtle.hasEgg() && this.turtle.homePos.closerToCenterThan(this.turtle.position(), 9.0)) {
                return super.canUse();
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.turtle.hasEgg() && this.turtle.homePos.closerToCenterThan(this.turtle.position(), 9.0);
        }

        @Override
        public void tick() {
            super.tick();
            BlockPos turtlePos = this.turtle.blockPosition();
            if (!this.turtle.isInWater() && this.isReachedTarget()) {
                if (this.turtle.layEggCounter < 1) {
                    this.turtle.setLayingEgg(true);
                } else if (this.turtle.layEggCounter > this.adjustedTickDelay(200)) {
                    Level level = this.turtle.level();
                    level.playSound(null, turtlePos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.3f, 0.9f + level.getRandom().nextFloat() * 0.2f);
                    BlockPos eggPos = this.blockPos.above();
                    BlockState eggState = (BlockState)Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, this.turtle.random.nextInt(4) + 1);
                    level.setBlock(eggPos, eggState, 3);
                    level.gameEvent(GameEvent.BLOCK_PLACE, eggPos, GameEvent.Context.of(this.turtle, eggState));
                    this.turtle.setHasEgg(false);
                    this.turtle.setLayingEgg(false);
                    this.turtle.setInLoveTime(600);
                }
                if (this.turtle.isLayingEgg()) {
                    ++this.turtle.layEggCounter;
                }
            }
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            if (!level.isEmptyBlock(pos.above())) {
                return false;
            }
            return TurtleEggBlock.isSand(level, pos);
        }
    }

    private static class TurtleGoToWaterGoal
    extends MoveToBlockGoal {
        private static final int GIVE_UP_TICKS = 1200;
        private final Turtle turtle;

        private TurtleGoToWaterGoal(Turtle turtle, double speedModifier) {
            super(turtle, turtle.isBaby() ? 2.0 : speedModifier, 24);
            this.turtle = turtle;
            this.verticalSearchStart = -1;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.isInWater() && this.tryTicks <= 1200 && this.isValidTarget(this.turtle.level(), this.blockPos);
        }

        @Override
        public boolean canUse() {
            if (this.turtle.isBaby() && !this.turtle.isInWater()) {
                return super.canUse();
            }
            if (!(this.turtle.goingHome || this.turtle.isInWater() || this.turtle.hasEgg())) {
                return super.canUse();
            }
            return false;
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 160 == 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getBlockState(pos).is(Blocks.WATER);
        }
    }

    private static class TurtleGoHomeGoal
    extends Goal {
        private final Turtle turtle;
        private final double speedModifier;
        private boolean stuck;
        private int closeToHomeTryTicks;
        private static final int GIVE_UP_TICKS = 600;

        public TurtleGoHomeGoal(Turtle turtle, double speedModifier) {
            this.turtle = turtle;
            this.speedModifier = speedModifier;
        }

        @Override
        public boolean canUse() {
            if (this.turtle.isBaby()) {
                return false;
            }
            if (this.turtle.hasEgg()) {
                return true;
            }
            if (this.turtle.getRandom().nextInt(TurtleGoHomeGoal.reducedTickDelay(700)) != 0) {
                return false;
            }
            return !this.turtle.homePos.closerToCenterThan(this.turtle.position(), 64.0);
        }

        @Override
        public void start() {
            this.turtle.goingHome = true;
            this.stuck = false;
            this.closeToHomeTryTicks = 0;
        }

        @Override
        public void stop() {
            this.turtle.goingHome = false;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.homePos.closerToCenterThan(this.turtle.position(), 7.0) && !this.stuck && this.closeToHomeTryTicks <= this.adjustedTickDelay(600);
        }

        @Override
        public void tick() {
            BlockPos homePos = this.turtle.homePos;
            boolean closeToHome = homePos.closerToCenterThan(this.turtle.position(), 16.0);
            if (closeToHome) {
                ++this.closeToHomeTryTicks;
            }
            if (this.turtle.getNavigation().isDone()) {
                Vec3 homePosVec = Vec3.atBottomCenterOf(homePos);
                Vec3 nextPos = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, homePosVec, 0.3141592741012573);
                if (nextPos == null) {
                    nextPos = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, homePosVec, 1.5707963705062866);
                }
                if (nextPos != null && !closeToHome && !this.turtle.level().getBlockState(BlockPos.containing(nextPos)).is(Blocks.WATER)) {
                    nextPos = DefaultRandomPos.getPosTowards(this.turtle, 16, 5, homePosVec, 1.5707963705062866);
                }
                if (nextPos == null) {
                    this.stuck = true;
                    return;
                }
                this.turtle.getNavigation().moveTo(nextPos.x, nextPos.y, nextPos.z, this.speedModifier);
            }
        }
    }

    private static class TurtleTravelGoal
    extends Goal {
        private final Turtle turtle;
        private final double speedModifier;
        private boolean stuck;

        public TurtleTravelGoal(Turtle turtle, double speedModifier) {
            this.turtle = turtle;
            this.speedModifier = speedModifier;
        }

        @Override
        public boolean canUse() {
            return !this.turtle.goingHome && !this.turtle.hasEgg() && this.turtle.isInWater();
        }

        @Override
        public void start() {
            int xzDist = 512;
            int yDist = 4;
            RandomSource random = this.turtle.random;
            int xt = random.nextInt(1025) - 512;
            int yt = random.nextInt(9) - 4;
            int zt = random.nextInt(1025) - 512;
            if ((double)yt + this.turtle.getY() > (double)(this.turtle.level().getSeaLevel() - 1)) {
                yt = 0;
            }
            this.turtle.travelPos = BlockPos.containing((double)xt + this.turtle.getX(), (double)yt + this.turtle.getY(), (double)zt + this.turtle.getZ());
            this.stuck = false;
        }

        @Override
        public void tick() {
            if (this.turtle.travelPos == null) {
                this.stuck = true;
                return;
            }
            if (this.turtle.getNavigation().isDone()) {
                Vec3 targetPos = Vec3.atBottomCenterOf(this.turtle.travelPos);
                Vec3 nextPos = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, targetPos, 0.3141592741012573);
                if (nextPos == null) {
                    nextPos = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, targetPos, 1.5707963705062866);
                }
                if (nextPos != null) {
                    int xc = Mth.floor(nextPos.x);
                    int zc = Mth.floor(nextPos.z);
                    int r = 34;
                    if (!this.turtle.level().hasChunksAt(xc - 34, zc - 34, xc + 34, zc + 34)) {
                        nextPos = null;
                    }
                }
                if (nextPos == null) {
                    this.stuck = true;
                    return;
                }
                this.turtle.getNavigation().moveTo(nextPos.x, nextPos.y, nextPos.z, this.speedModifier);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.getNavigation().isDone() && !this.stuck && !this.turtle.goingHome && !this.turtle.isInLove() && !this.turtle.hasEgg();
        }

        @Override
        public void stop() {
            this.turtle.travelPos = null;
            super.stop();
        }
    }

    private static class TurtleRandomStrollGoal
    extends RandomStrollGoal {
        private final Turtle turtle;

        private TurtleRandomStrollGoal(Turtle turtle, double speedModifier, int interval) {
            super(turtle, speedModifier, interval);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            if (!(this.mob.isInWater() || this.turtle.goingHome || this.turtle.hasEgg())) {
                return super.canUse();
            }
            return false;
        }
    }

    private static class TurtlePathNavigation
    extends AmphibiousPathNavigation {
        public TurtlePathNavigation(Turtle mob, Level level) {
            super(mob, level);
        }

        @Override
        public boolean isStableDestination(BlockPos pos) {
            Mob mob = this.mob;
            if (mob instanceof Turtle) {
                Turtle turtle = (Turtle)mob;
                if (turtle.travelPos != null) {
                    return this.level.getBlockState(pos).is(Blocks.WATER);
                }
            }
            return !this.level.getBlockState(pos.below()).isAir();
        }
    }
}

