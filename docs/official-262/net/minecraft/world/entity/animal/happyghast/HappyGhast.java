/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.happyghast;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.happyghast.HappyGhastAi;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HappyGhast
extends Animal {
    public static final float BABY_SCALE = 0.2375f;
    public static final int WANDER_GROUND_DISTANCE = 16;
    public static final int SMALL_RESTRICTION_RADIUS = 32;
    public static final int LARGE_RESTRICTION_RADIUS = 64;
    public static final int RESTRICTION_RADIUS_BUFFER = 16;
    public static final int FAST_HEALING_TICKS = 20;
    public static final int SLOW_HEALING_TICKS = 600;
    public static final int MAX_PASSANGERS = 4;
    private static final int STILL_TIMEOUT_ON_LOAD_GRACE_PERIOD = 60;
    private static final int MAX_STILL_TIMEOUT = 10;
    private static final Brain.Provider<HappyGhast> BRAIN_PROVIDER = Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT_ANY_TYPE, SensorType.NEAREST_PLAYERS), happyGhast -> HappyGhastAi.getActivities());
    public static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0f;
    private int leashHolderTime = 0;
    private int serverStillTimeout;
    private static final EntityDataAccessor<Boolean> IS_LEASH_HOLDER = SynchedEntityData.defineId(HappyGhast.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STAYS_STILL = SynchedEntityData.defineId(HappyGhast.class, EntityDataSerializers.BOOLEAN);
    private static final float MAX_SCALE = 1.0f;
    private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.HAPPY_GHAST.getDimensions().scale(0.2375f).withEyeHeight(0.46875f);

    public HappyGhast(EntityType<? extends HappyGhast> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.moveControl = new Ghast.GhastMoveControl<HappyGhast>(this, true, this::isOnStillTimeout);
        this.lookControl = new HappyGhastLookControl(this);
    }

    private void setServerStillTimeout(int serverStillTimeout) {
        Level level;
        if (this.serverStillTimeout <= 0 && serverStillTimeout > 0 && (level = this.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
            serverLevel.getChunkSource().chunkMap.sendToTrackingPlayers(this, ClientboundEntityPositionSyncPacket.of(this));
        }
        this.serverStillTimeout = serverStillTimeout;
        this.syncStayStillFlag();
    }

    private PathNavigation createBabyNavigation(Level level) {
        return new BabyFlyingPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new HappyGhastFloatGoal(this));
        this.goalSelector.addGoal(4, new TemptGoal.ForNonPathfinders((Mob)this, 1.0, itemStack -> this.isWearingBodyArmor() || this.isBaby() ? itemStack.is(ItemTags.HAPPY_GHAST_FOOD) : itemStack.is(ItemTags.HAPPY_GHAST_TEMPT_ITEMS), false, 7.0));
        this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this, 16));
    }

    private void adultGhastSetup() {
        this.moveControl = new Ghast.GhastMoveControl<HappyGhast>(this, true, this::isOnStillTimeout);
        this.lookControl = new HappyGhastLookControl(this);
        this.navigation = this.createNavigation(this.level());
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.removeAllGoals(goal -> true);
            this.registerGoals();
            this.getBrain().stopAll(serverLevel, this);
            this.brain.clearMemories();
        }
    }

    private void babyGhastSetup() {
        this.moveControl = new FlyingMoveControl<HappyGhast>(this, 180, true);
        this.lookControl = new LookControl(this);
        this.navigation = this.createBabyNavigation(this.level());
        this.setServerStillTimeout(0);
        this.removeAllGoals(goal -> true);
    }

    @Override
    protected void ageBoundaryReached() {
        if (this.isBaby()) {
            this.babyGhastSetup();
        } else {
            this.adultGhastSetup();
        }
        super.ageBoundaryReached();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.TEMPT_RANGE, 16.0).add(Attributes.FLYING_SPEED, 0.05).add(Attributes.MOVEMENT_SPEED, 0.05).add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.CAMERA_DISTANCE, 8.0);
    }

    @Override
    protected float sanitizeScale(float scale) {
        return Math.min(scale, 1.0f);
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
        float speed = (float)this.getAttributeValue(Attributes.FLYING_SPEED) * 5.0f / 3.0f;
        this.travelFlying(input, speed, speed, speed);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (!level.isEmptyBlock(pos)) {
            return 0.0f;
        }
        if (level.isEmptyBlock(pos.below()) && !level.isEmptyBlock(pos.below(2))) {
            return 10.0f;
        }
        return 5.0f;
    }

    @Override
    public boolean canBreatheUnderwater() {
        if (this.isBaby()) {
            return true;
        }
        return super.canBreatheUnderwater();
    }

    @Override
    protected boolean shouldStayCloseToLeashHolder() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
    }

    @Override
    public float getVoicePitch() {
        return 1.0f;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public int getAmbientSoundInterval() {
        int interval = super.getAmbientSoundInterval();
        if (this.isVehicle()) {
            return interval * 6;
        }
        return interval;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.GHASTLING_AMBIENT : SoundEvents.HAPPY_GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isBaby() ? SoundEvents.GHASTLING_HURT : SoundEvents.HAPPY_GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isBaby() ? SoundEvents.GHASTLING_DEATH : SoundEvents.HAPPY_GHAST_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return this.isBaby() ? 1.0f : 4.0f;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.HAPPY_GHAST.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.2375f : 1.0f;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.HAPPY_GHAST_FOOD);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.BODY) {
            return this.isAlive() && !this.isBaby();
        }
        return super.canUseSlot(slot);
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.BODY;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult interactionResult;
        if (this.isBaby()) {
            return super.mobInteract(player, hand);
        }
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.isEmpty() && (interactionResult = itemStack.interactLivingEntity(player, this, hand)).consumesAction()) {
            return interactionResult;
        }
        if (this.isWearingBodyArmor() && !player.isSecondaryUseActive()) {
            this.doPlayerRide(player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void doPlayerRide(Player player) {
        if (!this.level().isClientSide()) {
            player.startRiding(this);
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        if (!this.isVehicle()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HARNESS_GOGGLES_DOWN, this.getSoundSource(), 1.0f, 1.0f);
        }
        super.addPassenger(passenger);
        if (!this.level().isClientSide()) {
            if (!this.scanPlayerAboveGhast()) {
                this.setServerStillTimeout(0);
            } else if (this.serverStillTimeout > 10) {
                this.setServerStillTimeout(10);
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide()) {
            this.setServerStillTimeout(10);
        }
        if (!this.isVehicle()) {
            this.clearHome();
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HARNESS_GOGGLES_UP, this.getSoundSource(), 1.0f, 1.0f);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 4;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Entity firstPassenger = this.getFirstPassenger();
        if (this.isWearingBodyArmor() && !this.isOnStillTimeout() && firstPassenger instanceof Player) {
            Player player = (Player)firstPassenger;
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa;
        float forward = 0.0f;
        float up = 0.0f;
        if (controller.zza != 0.0f) {
            float forwardLook = Mth.cos(controller.getXRot() * ((float)Math.PI / 180));
            float upLook = -Mth.sin(controller.getXRot() * ((float)Math.PI / 180));
            if (controller.zza < 0.0f) {
                forwardLook *= -0.5f;
                upLook *= -0.5f;
            }
            up = upLook;
            forward = forwardLook;
        }
        if (controller.isJumping()) {
            up += 0.5f;
        }
        return new Vec3(strafe, up, forward).scale((double)3.9f * this.getAttributeValue(Attributes.FLYING_SPEED));
    }

    protected Vec2 getRiddenRotation(LivingEntity controller) {
        return new Vec2(controller.getXRot() * 0.5f, controller.getYRot());
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        Vec2 rotation = this.getRiddenRotation(controller);
        float yRot = this.getYRot();
        float diff = Mth.wrapDegrees(rotation.y - yRot);
        float turnSpeed = 0.08f;
        this.setRot(yRot += diff * 0.08f, rotation.x);
        this.yBodyRot = this.yHeadRot = yRot;
        this.yRotO = this.yHeadRot;
    }

    protected Brain<HappyGhast> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    public Brain<HappyGhast> getBrain() {
        return super.getBrain();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (this.isBaby()) {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("happyGhastBrain");
            this.getBrain().tick(level, this);
            profiler.pop();
            profiler.push("happyGhastActivityUpdate");
            HappyGhastAi.updateActivity(this);
            profiler.pop();
        }
        this.checkRestriction();
        super.customServerAiStep(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.leashHolderTime > 0) {
            --this.leashHolderTime;
        }
        this.setLeashHolder(this.leashHolderTime > 0);
        if (this.serverStillTimeout > 0) {
            if (this.tickCount > 60) {
                --this.serverStillTimeout;
            }
            this.setServerStillTimeout(this.serverStillTimeout);
        }
        if (this.scanPlayerAboveGhast()) {
            this.setServerStillTimeout(10);
        }
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide()) {
            this.setRequiresPrecisePosition(this.isOnStillTimeout());
        }
        super.aiStep();
        this.continuousHeal();
    }

    private int getHappyGhastRestrictionRadius() {
        if (!this.isBaby() && this.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            return 64;
        }
        return 32;
    }

    private void checkRestriction() {
        if (this.isLeashed() || this.isVehicle()) {
            return;
        }
        int radius = this.getHappyGhastRestrictionRadius();
        if (this.hasHome() && this.getHomePosition().closerThan(this.blockPosition(), radius + 16) && radius == this.getHomeRadius()) {
            return;
        }
        this.setHomeTo(this.blockPosition(), radius);
    }

    private void continuousHeal() {
        ServerLevel level;
        block5: {
            block4: {
                Level level2 = this.level();
                if (!(level2 instanceof ServerLevel)) break block4;
                level = (ServerLevel)level2;
                if (this.isAlive() && this.deathTime == 0 && this.getMaxHealth() != this.getHealth()) break block5;
            }
            return;
        }
        boolean isFastHealing = this.isInClouds() || level.precipitationAt(this.blockPosition()) != Biome.Precipitation.NONE;
        if (this.tickCount % (isFastHealing ? 20 : 600) == 0) {
            this.heal(1.0f);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(IS_LEASH_HOLDER, false);
        entityData.define(STAYS_STILL, false);
    }

    private void setLeashHolder(boolean isLeashHolder) {
        this.entityData.set(IS_LEASH_HOLDER, isLeashHolder);
    }

    public boolean isLeashHolder() {
        return this.entityData.get(IS_LEASH_HOLDER);
    }

    private void syncStayStillFlag() {
        this.entityData.set(STAYS_STILL, this.serverStillTimeout > 0);
    }

    public boolean staysStill() {
        return this.entityData.get(STAYS_STILL);
    }

    @Override
    public boolean supportQuadLeashAsHolder() {
        return true;
    }

    @Override
    public Vec3[] getQuadLeashHolderOffsets() {
        return Leashable.createQuadLeashOffsets(this, -0.03125, 0.4375, 0.46875, 0.03125);
    }

    @Override
    public Vec3 getLeashOffset() {
        return Vec3.ZERO;
    }

    @Override
    public double leashElasticDistance() {
        return 10.0;
    }

    @Override
    public double leashSnapDistance() {
        return 16.0;
    }

    @Override
    public void onElasticLeashPull() {
        super.onElasticLeashPull();
        this.getMoveControl().setWait();
    }

    @Override
    public void notifyLeashHolder(Leashable entity) {
        if (entity.supportQuadLeash()) {
            this.leashHolderTime = 5;
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("still_timeout", this.serverStillTimeout);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.setServerStillTimeout(tag.getIntOr("still_timeout", 0));
    }

    public boolean isOnStillTimeout() {
        return this.staysStill() || this.serverStillTimeout > 0;
    }

    private boolean scanPlayerAboveGhast() {
        AABB happyGhastBb = this.getBoundingBox();
        AABB ghastDetectionBox = new AABB(happyGhastBb.minX - 1.0, happyGhastBb.maxY - (double)1.0E-5f, happyGhastBb.minZ - 1.0, happyGhastBb.maxX + 1.0, happyGhastBb.maxY + happyGhastBb.getYsize() / 2.0, happyGhastBb.maxZ + 1.0);
        for (Player player : this.level().players()) {
            Entity rootVehicle;
            if (player.isSpectator() || (rootVehicle = player.getRootVehicle()) instanceof HappyGhast || !ghastDetectionBox.contains(rootVehicle.position())) continue;
            return true;
        }
        return false;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new HappyGhastBodyRotationControl(this);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        if (this.isBaby() || !this.isAlive()) {
            return false;
        }
        if (this.level().isClientSide() && other instanceof Player && other.position().y >= this.getBoundingBox().maxY) {
            return true;
        }
        if (this.isVehicle() && other instanceof HappyGhast) {
            return true;
        }
        return this.isOnStillTimeout();
    }

    @Override
    public boolean isFlyingVehicle() {
        return !this.isBaby();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    private class HappyGhastLookControl
    extends LookControl {
        final /* synthetic */ HappyGhast this$0;

        private HappyGhastLookControl(HappyGhast happyGhast) {
            HappyGhast happyGhast2 = happyGhast;
            Objects.requireNonNull(happyGhast2);
            this.this$0 = happyGhast2;
            super(happyGhast);
        }

        @Override
        public void tick() {
            if (this.this$0.isOnStillTimeout()) {
                float closeAngle = Mth.wrapDegrees90(this.this$0.getYRot());
                this.this$0.setYRot(this.this$0.getYRot() - closeAngle);
                this.this$0.setYHeadRot(this.this$0.getYRot());
                return;
            }
            if (this.lookAtCooldown > 0) {
                --this.lookAtCooldown;
                double xdd = this.wantedX - this.this$0.getX();
                double zdd = this.wantedZ - this.this$0.getZ();
                this.this$0.setYRot(-((float)Mth.atan2(xdd, zdd)) * 57.295776f);
                this.this$0.yHeadRot = this.this$0.yBodyRot = this.this$0.getYRot();
                return;
            }
            Ghast.faceMovementDirection(this.mob);
        }
    }

    private static class BabyFlyingPathNavigation
    extends FlyingPathNavigation {
        public BabyFlyingPathNavigation(HappyGhast mob, Level level) {
            super(mob, level);
            this.setCanOpenDoors(false);
            this.setCanFloat(true);
            this.setRequiredPathLength(48.0f);
        }

        @Override
        protected boolean canMoveDirectly(Vec3 startPos, Vec3 stopPos) {
            return BabyFlyingPathNavigation.isClearForMovementBetween(this.mob, startPos, stopPos, false);
        }
    }

    private class HappyGhastFloatGoal
    extends FloatGoal {
        final /* synthetic */ HappyGhast this$0;

        public HappyGhastFloatGoal(HappyGhast happyGhast) {
            HappyGhast happyGhast2 = happyGhast;
            Objects.requireNonNull(happyGhast2);
            this.this$0 = happyGhast2;
            super(happyGhast);
        }

        @Override
        public boolean canUse() {
            return !this.this$0.isOnStillTimeout() && super.canUse();
        }
    }

    private class HappyGhastBodyRotationControl
    extends BodyRotationControl {
        final /* synthetic */ HappyGhast this$0;

        public HappyGhastBodyRotationControl(HappyGhast happyGhast) {
            HappyGhast happyGhast2 = happyGhast;
            Objects.requireNonNull(happyGhast2);
            this.this$0 = happyGhast2;
            super(happyGhast);
        }

        @Override
        public void clientTick() {
            if (this.this$0.isVehicle()) {
                this.this$0.yBodyRot = this.this$0.yHeadRot = this.this$0.getYRot();
            }
            super.clientTick();
        }
    }
}

