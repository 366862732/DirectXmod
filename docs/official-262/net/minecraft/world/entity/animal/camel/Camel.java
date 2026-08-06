/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.camel;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.CamelAi;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Camel
extends AbstractHorse {
    public static final float BABY_SCALE = 0.6f;
    public static final int DASH_COOLDOWN_TICKS = 55;
    public static final int MAX_HEAD_Y_ROT = 30;
    private static final float RUNNING_SPEED_BONUS = 0.1f;
    private static final float DASH_VERTICAL_MOMENTUM = 1.4285f;
    private static final float DASH_HORIZONTAL_MOMENTUM = 22.2222f;
    private static final int DASH_MINIMUM_DURATION_TICKS = 5;
    private static final int SITDOWN_DURATION_TICKS = 40;
    private static final int STANDUP_DURATION_TICKS = 52;
    private static final int IDLE_MINIMAL_DURATION_TICKS = 80;
    private static final float SITTING_HEIGHT_DIFFERENCE = 1.43f;
    private static final long DEFAULT_LAST_POSE_CHANGE_TICK = 0L;
    private static final Brain.Provider<Camel> BRAIN_PROVIDER = Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT), camel -> CamelAi.getActivities());
    public static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.LONG);
    public final AnimationState sitAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState sitUpAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState dashAnimationState = new AnimationState();
    private static final EntityDimensions BABY_STANDING_DIMENSIONS = EntityDimensions.scalable(0.95f, 1.4f).withEyeHeight(1.38f);
    private static final EntityDimensions BABY_SITTING_DIMENSIONS = EntityDimensions.scalable(0.95f, 0.425f).withEyeHeight(0.41f);
    private static final EntityDimensions ADULT_SITTING_DIMENSIONS = EntityDimensions.scalable(EntityTypes.CAMEL.getWidth(), EntityTypes.CAMEL.getHeight() - 1.43f).withEyeHeight(0.845f);
    private int dashCooldown = 0;
    private int idleAnimationTimeout = 0;

    public Camel(EntityType<? extends Camel> type, Level level) {
        super((EntityType<? extends AbstractHorse>)type, level);
        this.moveControl = new CamelMoveControl<Camel>(this);
        this.lookControl = new CamelLookControl(this);
        GroundPathNavigation navigation = (GroundPathNavigation)this.getNavigation();
        navigation.setCanFloat(true);
        navigation.setCanWalkOverFences(true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("LastPoseTick", this.entityData.get(LAST_POSE_CHANGE_TICK));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        long poseTick = input.getLongOr("LastPoseTick", 0L);
        if (poseTick < 0L) {
            this.setPose(Pose.SITTING);
        }
        this.resetLastPoseChangeTick(poseTick);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Camel.createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 32.0).add(Attributes.MOVEMENT_SPEED, 0.09f).add(Attributes.JUMP_STRENGTH, 0.42f).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DASH, false);
        entityData.define(LAST_POSE_CHANGE_TICK, 0L);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        CamelAi.initMemories(this, level.getRandom());
        this.resetLastPoseChangeTickToFullStand(level.getLevel().getGameTime());
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public static boolean checkCamelSpawnRules(EntityType<Camel> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.CAMELS_SPAWNABLE_ON) && Camel.isBrightEnoughToSpawn(level, pos);
    }

    protected Brain<Camel> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    public Brain<Camel> getBrain() {
        return super.getBrain();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return pose == Pose.SITTING ? (this.isBaby() ? BABY_SITTING_DIMENSIONS : ADULT_SITTING_DIMENSIONS) : (this.isBaby() ? BABY_STANDING_DIMENSIONS : super.getDefaultDimensions(pose));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("camelBrain");
        Brain<Camel> brain = this.getBrain();
        brain.tick(level, this);
        profiler.pop();
        profiler.push("camelActivityUpdate");
        CamelAi.updateActivity(this);
        profiler.pop();
        super.customServerAiStep(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDashing() && this.dashCooldown < 50 && (this.onGround() || this.isInLiquid() || this.isPassenger())) {
            this.setDashing(false);
        }
        if (this.dashCooldown > 0) {
            --this.dashCooldown;
            if (this.dashCooldown == 0) {
                this.level().playSound(null, this.blockPosition(), this.getDashReadySound(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
        }
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
        if (this.refuseToMove()) {
            this.clampHeadRotationToBody();
        }
        if (this.isCamelSitting() && this.isInWater()) {
            this.standUpInstantly();
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(40) + 80;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
        if (this.isCamelVisuallySitting()) {
            this.sitUpAnimationState.stop();
            this.dashAnimationState.stop();
            if (this.isVisuallySittingDown()) {
                this.sitAnimationState.startIfStopped(this.tickCount);
                this.sitPoseAnimationState.stop();
            } else {
                this.sitAnimationState.stop();
                this.sitPoseAnimationState.startIfStopped(this.tickCount);
            }
        } else {
            this.sitAnimationState.stop();
            this.sitPoseAnimationState.stop();
            this.dashAnimationState.animateWhen(this.isDashing(), this.tickCount);
            this.sitUpAnimationState.animateWhen(this.isInPoseTransition() && this.getPoseTime() >= 0L, this.tickCount);
        }
    }

    @Override
    protected void updateWalkAnimation(float distance) {
        float targetSpeed = this.getPose() == Pose.STANDING && !this.dashAnimationState.isStarted() ? Math.min(distance * 6.0f, 1.0f) : 0.0f;
        this.walkAnimation.update(targetSpeed, 0.2f, this.isBaby() ? 3.0f : 1.0f);
    }

    @Override
    public void travel(Vec3 input) {
        if (this.refuseToMove() && this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            input = input.multiply(0.0, 1.0, 0.0);
        }
        super.travel(input);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        if (controller.zza > 0.0f && this.isCamelSitting() && !this.isInPoseTransition()) {
            this.standUp();
        }
    }

    public boolean refuseToMove() {
        return this.isCamelSitting() || this.isInPoseTransition();
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        float movementBonus = controller.isSprinting() && this.getJumpCooldown() == 0 ? 0.1f : 0.0f;
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) + movementBonus;
    }

    @Override
    protected Vec2 getRiddenRotation(LivingEntity controller) {
        if (this.refuseToMove()) {
            return new Vec2(this.getXRot(), this.getYRot());
        }
        return super.getRiddenRotation(controller);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        if (this.refuseToMove()) {
            return Vec3.ZERO;
        }
        return super.getRiddenInput(controller, selfInput);
    }

    @Override
    public boolean canJump() {
        return !this.refuseToMove() && super.canJump();
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
        if (!this.isSaddled() || this.dashCooldown > 0 || !this.onGround()) {
            return;
        }
        super.onPlayerJump(jumpAmount);
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    protected void executeRidersJump(float amount, Vec3 input) {
        double jumpMomentum = this.getJumpPower();
        this.addDeltaMovement(this.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale((double)(22.2222f * amount) * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double)this.getBlockSpeedFactor()).add(0.0, (double)(1.4285f * amount) * jumpMomentum, 0.0));
        this.dashCooldown = 55;
        this.setDashing(true);
        this.needsSync = true;
    }

    public boolean isDashing() {
        return this.entityData.get(DASH);
    }

    public void setDashing(boolean isDashing) {
        this.entityData.set(DASH, isDashing);
    }

    @Override
    public void handleStartJump(int jumpScale) {
        this.makeSound(this.getDashingSound());
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.setDashing(true);
    }

    protected SoundEvent getDashingSound() {
        return SoundEvents.CAMEL_DASH;
    }

    protected SoundEvent getDashReadySound() {
        return SoundEvents.CAMEL_DASH_READY;
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public int getJumpCooldown() {
        return this.dashCooldown;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAMEL_HURT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        if (blockState.is(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS)) {
            this.playSound(SoundEvents.CAMEL_STEP_SAND, 1.0f, 1.0f);
        } else {
            this.playSound(SoundEvents.CAMEL_STEP, 1.0f, 1.0f);
        }
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.CAMEL_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && !this.isBaby()) {
            this.openCustomInventoryScreen(player);
            return InteractionResult.SUCCESS;
        }
        InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
        if (interactionResult.consumesAction()) {
            return interactionResult;
        }
        if (this.isFood(itemStack)) {
            return this.fedFood(player, itemStack);
        }
        if (this.getPassengers().size() < 2 && !this.isBaby()) {
            this.doPlayerRide(player);
        }
        if (this.isBaby() && player.isHolding(Items.GOLDEN_DANDELION)) {
            return super.mobInteract(player, hand);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onElasticLeashPull() {
        super.onElasticLeashPull();
        if (this.isCamelSitting() && !this.isInPoseTransition() && this.canCamelChangePose()) {
            this.standUp();
        }
    }

    @Override
    public Vec3[] getQuadLeashOffsets() {
        return Leashable.createQuadLeashOffsets(this, 0.02, 0.48, 0.25, 0.82);
    }

    public boolean canCamelChangePose() {
        return this.wouldNotSuffocateAtTargetPose(this.isCamelSitting() ? Pose.STANDING : Pose.SITTING);
    }

    @Override
    protected boolean handleEating(Player player, ItemStack itemStack) {
        boolean couldAgeUp;
        boolean couldSetInLove;
        boolean couldHeal;
        if (!this.isFood(itemStack)) {
            return false;
        }
        boolean bl = couldHeal = this.getHealth() < this.getMaxHealth();
        if (couldHeal) {
            this.heal(2.0f);
        }
        boolean bl2 = couldSetInLove = this.isTamed() && this.getAge() == 0 && this.canFallInLove();
        if (couldSetInLove) {
            this.setInLove(player);
        }
        if (couldAgeUp = this.canAgeUp()) {
            this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            if (!this.level().isClientSide()) {
                this.ageUp(10);
            }
        }
        if (couldHeal || couldSetInLove || couldAgeUp) {
            SoundEvent eatingSound;
            if (!this.isSilent() && (eatingSound = this.getEatingSound()) != null) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), eatingSound, this.getSoundSource(), 1.0f, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.2f);
            }
            this.gameEvent(GameEvent.EAT);
            return true;
        }
        return false;
    }

    @Override
    protected boolean canPerformRearing() {
        return false;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean canMate(Animal partner) {
        if (partner == this) return false;
        if (!(partner instanceof Camel)) return false;
        Camel camel = (Camel)partner;
        if (!this.canParent()) return false;
        if (!camel.canParent()) return false;
        return true;
    }

    @Override
    public @Nullable Camel getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.CAMEL.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.CAMEL_EAT;
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float dmg) {
        this.standUpInstantly();
        super.actuallyHurt(level, source, dmg);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = Math.max(this.getPassengers().indexOf(passenger), 0);
        boolean driver = index == 0;
        float offset = 0.5f;
        float height = (float)(this.isRemoved() ? (double)0.01f : this.getBodyAnchorAnimationYOffset(driver, 0.0f, dimensions, scale));
        if (this.getPassengers().size() > 1) {
            if (!driver) {
                offset = -0.7f;
            }
            if (passenger instanceof Animal) {
                offset += 0.2f;
            }
        }
        return new Vec3(0.0, height, offset * scale).yRot(-this.getYRot() * ((float)Math.PI / 180));
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.6f : 1.0f;
    }

    private double getBodyAnchorAnimationYOffset(boolean isFront, float partialTicks, EntityDimensions dimensions, float scale) {
        double ageSitYOffset = this.isBaby() ? 0.09375 : 0.375;
        double baseSitOffset = (double)dimensions.height() - ageSitYOffset;
        float sittingHeightDifference = scale * 1.43f;
        float verticalDrop = sittingHeightDifference - scale * 0.2f;
        float bottomPoint = sittingHeightDifference - verticalDrop;
        boolean isInTransition = this.isInPoseTransition();
        boolean isSitting = this.isCamelSitting();
        if (isInTransition) {
            float flexPointOffset;
            int halfPoint;
            int animationDuration;
            int n = animationDuration = isSitting ? 40 : 52;
            if (isSitting) {
                halfPoint = 28;
                flexPointOffset = isFront ? 0.5f : 0.1f;
            } else {
                halfPoint = isFront ? 24 : 32;
                flexPointOffset = isFront ? 0.6f : 0.35f;
            }
            float poseTime = Mth.clamp((float)this.getPoseTime() + partialTicks, 0.0f, (float)animationDuration);
            boolean isFirstPart = poseTime < (float)halfPoint;
            float part = isFirstPart ? poseTime / (float)halfPoint : (poseTime - (float)halfPoint) / (float)(animationDuration - halfPoint);
            float flexPoint = sittingHeightDifference - flexPointOffset * verticalDrop;
            baseSitOffset += isSitting ? (double)Mth.lerp(part, isFirstPart ? sittingHeightDifference : flexPoint, isFirstPart ? flexPoint : bottomPoint) : (double)Mth.lerp(part, isFirstPart ? bottomPoint - sittingHeightDifference : bottomPoint - flexPoint, isFirstPart ? bottomPoint - flexPoint : 0.0f);
        }
        if (isSitting && !isInTransition) {
            baseSitOffset += (double)bottomPoint;
        }
        return baseSitOffset;
    }

    @Override
    public Vec3 getLeashOffset(float partialTicks) {
        EntityDimensions dimensions = this.getDimensions(this.getPose());
        float scale = this.getAgeScale();
        return new Vec3(0.0, this.getBodyAnchorAnimationYOffset(true, partialTicks, dimensions, scale) - (double)(0.2f * scale), dimensions.width() * 0.56f);
    }

    @Override
    public int getMaxHeadYRot() {
        return 30;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() <= 2;
    }

    public boolean isCamelSitting() {
        return this.entityData.get(LAST_POSE_CHANGE_TICK) < 0L;
    }

    public boolean isCamelVisuallySitting() {
        return this.getPoseTime() < 0L != this.isCamelSitting();
    }

    public boolean isInPoseTransition() {
        long poseTime = this.getPoseTime();
        return poseTime < (long)(this.isCamelSitting() ? 40 : 52);
    }

    private boolean isVisuallySittingDown() {
        return this.isCamelSitting() && this.getPoseTime() < 40L && this.getPoseTime() >= 0L;
    }

    public void sitDown() {
        if (this.isCamelSitting()) {
            return;
        }
        this.makeSound(this.getSitDownSound());
        this.setPose(Pose.SITTING);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.resetLastPoseChangeTick(-this.level().getGameTime());
    }

    public void standUp() {
        if (!this.isCamelSitting()) {
            return;
        }
        this.makeSound(this.getStandUpSound());
        this.setPose(Pose.STANDING);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.resetLastPoseChangeTick(this.level().getGameTime());
    }

    protected SoundEvent getStandUpSound() {
        return SoundEvents.CAMEL_STAND;
    }

    protected SoundEvent getSitDownSound() {
        return SoundEvents.CAMEL_SIT;
    }

    public void standUpInstantly() {
        this.setPose(Pose.STANDING);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.resetLastPoseChangeTickToFullStand(this.level().getGameTime());
    }

    @VisibleForTesting
    public void resetLastPoseChangeTick(long syncedPoseTickTime) {
        this.entityData.set(LAST_POSE_CHANGE_TICK, syncedPoseTickTime);
    }

    private void resetLastPoseChangeTickToFullStand(long currentTime) {
        this.resetLastPoseChangeTick(Math.max(0L, currentTime - 52L - 1L));
    }

    public long getPoseTime() {
        return this.level().getGameTime() - Math.abs(this.entityData.get(LAST_POSE_CHANGE_TICK));
    }

    @Override
    protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
        if (slot == EquipmentSlot.SADDLE) {
            return this.getSaddleSound();
        }
        return super.getEquipSound(slot, stack, equippable);
    }

    protected Holder.Reference<SoundEvent> getSaddleSound() {
        return SoundEvents.CAMEL_SADDLE;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (!this.firstTick && DASH.equals(accessor)) {
            this.dashCooldown = this.dashCooldown == 0 ? 55 : this.dashCooldown;
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public boolean isTamed() {
        return true;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!this.level().isClientSide()) {
            player.openHorseInventory(this, this.inventory);
        }
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new CamelBodyRotationControl(this, this);
    }

    private static class CamelMoveControl<T extends Camel>
    extends MoveControl<T> {
        public CamelMoveControl(T camel) {
            super(camel);
        }

        @Override
        public void tick() {
            if (this.operation == MoveControl.Operation.MOVE_TO && !((Camel)this.mob).isLeashed() && ((Camel)this.mob).isCamelSitting() && !((Camel)this.mob).isInPoseTransition() && ((Camel)this.mob).canCamelChangePose()) {
                ((Camel)this.mob).standUp();
            }
            super.tick();
        }
    }

    private class CamelLookControl
    extends LookControl {
        final /* synthetic */ Camel this$0;

        private CamelLookControl(Camel camel) {
            Camel camel2 = camel;
            Objects.requireNonNull(camel2);
            this.this$0 = camel2;
            super(camel);
        }

        @Override
        public void tick() {
            if (!this.this$0.hasControllingPassenger()) {
                super.tick();
            }
        }
    }

    private class CamelBodyRotationControl
    extends BodyRotationControl {
        final /* synthetic */ Camel this$0;

        public CamelBodyRotationControl(Camel camel, Camel camel2) {
            Camel camel3 = camel;
            Objects.requireNonNull(camel3);
            this.this$0 = camel3;
            super(camel2);
        }

        @Override
        public void clientTick() {
            if (!this.this$0.refuseToMove()) {
                super.clientTick();
            }
        }
    }
}

