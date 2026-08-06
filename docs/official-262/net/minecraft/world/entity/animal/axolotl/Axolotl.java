/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.BinaryAnimator;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.EasingType;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.axolotl.AxolotlAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Axolotl
extends Animal
implements Bucketable {
    public static final int TOTAL_PLAYDEAD_TIME = 200;
    private static final int POSE_ANIMATION_TICKS = 10;
    private static final Brain.Provider<Axolotl> BRAIN_PROVIDER = Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorType.AXOLOTL_ATTACKABLES, SensorType.FOOD_TEMPTATIONS), axolotl -> AxolotlAi.getActivities());
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_DEAD = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(Axolotl.class, EntityDataSerializers.BOOLEAN);
    public static final double PLAYER_REGEN_DETECTION_RANGE = 20.0;
    public static final int RARE_VARIANT_CHANCE = 1200;
    private static final int AXOLOTL_TOTAL_AIR_SUPPLY = 6000;
    public static final String VARIANT_TAG = "Variant";
    private static final int REHYDRATE_AIR_SUPPLY = 1800;
    private static final int REGEN_BUFF_MAX_DURATION = 2400;
    private static final boolean DEFAULT_FROM_BUCKET = false;
    public final BinaryAnimator playingDeadAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
    public final BinaryAnimator inWaterAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
    public final BinaryAnimator onGroundAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
    public final BinaryAnimator movingAnimator = new BinaryAnimator(10, EasingType.IN_OUT_SINE);
    public final AnimationState swimAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState walkUnderWaterAnimationState = new AnimationState();
    public final AnimationState idleUnderWaterAnimationState = new AnimationState();
    public final AnimationState idleUnderWaterOnGroundAnimationState = new AnimationState();
    public final AnimationState idleOnGroundAnimationState = new AnimationState();
    public final AnimationState playDeadAnimationState = new AnimationState();
    private final ImmutableList<AnimationState> ALL_ANIMATIONS = ImmutableList.of((Object)this.swimAnimationState, (Object)this.walkAnimationState, (Object)this.walkUnderWaterAnimationState, (Object)this.idleUnderWaterAnimationState, (Object)this.idleUnderWaterOnGroundAnimationState, (Object)this.idleOnGroundAnimationState, (Object)this.playDeadAnimationState);
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.375f, 0.21f).withEyeHeight(0.09375f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.1875f, 0.0f));
    private static final int REGEN_BUFF_BASE_DURATION = 100;

    public Axolotl(EntityType<? extends Axolotl> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0f);
        this.moveControl = new AxolotlMoveControl<Axolotl>(this);
        this.lookControl = new AxolotlLookControl(this, this, 20);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_VARIANT, 0);
        entityData.define(DATA_PLAYING_DEAD, false);
        entityData.define(FROM_BUCKET, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store(VARIANT_TAG, Variant.LEGACY_CODEC, this.getVariant());
        output.putBoolean("FromBucket", this.fromBucket());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.read(VARIANT_TAG, Variant.LEGACY_CODEC).orElse(Variant.DEFAULT));
        this.setFromBucket(input.getBooleanOr("FromBucket", false));
    }

    @Override
    public void playAmbientSound() {
        if (this.isPlayingDead()) {
            return;
        }
        super.playAmbientSound();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        boolean isBaby = false;
        if (spawnReason == EntitySpawnReason.BUCKET) {
            return groupData;
        }
        RandomSource random = level.getRandom();
        if (groupData instanceof AxolotlGroupData) {
            if (((AxolotlGroupData)groupData).getGroupSize() >= 2) {
                isBaby = true;
            }
        } else {
            groupData = new AxolotlGroupData(Variant.getCommonSpawnVariant(random), Variant.getCommonSpawnVariant(random));
        }
        this.setVariant(((AxolotlGroupData)groupData).getVariant(random));
        if (isBaby) {
            this.setAge(-24000);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    public void baseTick() {
        Level level;
        int airSupply = this.getAirSupply();
        super.baseTick();
        if (!this.isNoAi() && (level = this.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.handleAirSupply(serverLevel, airSupply);
        }
        if (this.level().isClientSide()) {
            if (this.isBaby()) {
                this.tickBabyAnimations();
            } else {
                this.tickAdultAnimations();
            }
        }
    }

    private void tickBabyAnimations() {
        boolean isPlayingDead = this.isPlayingDead();
        boolean isInWater = this.isInWater();
        boolean onGround = this.onGround();
        boolean isMoving = this.walkAnimation.isMoving() || this.getXRot() != this.xRotO || this.getYRot() != this.yRotO;
        this.movingAnimator.tick(isMoving);
        if (isPlayingDead) {
            this.soloAnimation(this.playDeadAnimationState);
            return;
        }
        if (isMoving) {
            if (isInWater && !onGround) {
                this.soloAnimation(this.swimAnimationState);
            } else if (!isInWater && onGround) {
                this.soloAnimation(this.walkAnimationState);
            } else {
                this.soloAnimation(this.walkUnderWaterAnimationState);
            }
        } else if (isInWater && !onGround) {
            this.soloAnimation(this.idleUnderWaterAnimationState);
        } else if (isInWater && onGround) {
            this.soloAnimation(this.idleUnderWaterOnGroundAnimationState);
        } else {
            this.soloAnimation(this.idleOnGroundAnimationState);
        }
    }

    private void soloAnimation(AnimationState toStart) {
        for (AnimationState animation : this.ALL_ANIMATIONS) {
            if (animation == toStart) {
                animation.startIfStopped(this.tickCount);
                continue;
            }
            animation.stop();
        }
    }

    private void tickAdultAnimations() {
        AxolotlAnimationState animationState = this.isPlayingDead() ? AxolotlAnimationState.PLAYING_DEAD : (this.isInWater() ? AxolotlAnimationState.IN_WATER : (this.onGround() ? AxolotlAnimationState.ON_GROUND : AxolotlAnimationState.IN_AIR));
        this.playingDeadAnimator.tick(animationState == AxolotlAnimationState.PLAYING_DEAD);
        this.inWaterAnimator.tick(animationState == AxolotlAnimationState.IN_WATER);
        this.onGroundAnimator.tick(animationState == AxolotlAnimationState.ON_GROUND);
        boolean isMoving = this.walkAnimation.isMoving() || this.getXRot() != this.xRotO || this.getYRot() != this.yRotO;
        this.movingAnimator.tick(isMoving);
    }

    protected void handleAirSupply(ServerLevel level, int preTickAirSupply) {
        if (this.isAlive() && !this.isInWaterOrRain()) {
            this.setAirSupply(preTickAirSupply - 1);
            if (this.shouldTakeDrowningDamage()) {
                this.setAirSupply(0);
                this.hurtServer(level, this.damageSources().dryOut(), 2.0f);
            }
        } else {
            this.setAirSupply(this.getMaxAirSupply());
        }
    }

    public void rehydrate() {
        int newAirSupply = this.getAirSupply() + 1800;
        this.setAirSupply(Math.min(newAirSupply, this.getMaxAirSupply()));
    }

    @Override
    public int getMaxAirSupply() {
        return 6000;
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    private void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.getId());
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.AXOLOTL_VARIANT) {
            return Axolotl.castComponentValue(type, this.getVariant());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.AXOLOTL_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.AXOLOTL_VARIANT) {
            this.setVariant(Axolotl.castComponentValue(DataComponents.AXOLOTL_VARIANT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    private static boolean useRareVariant(RandomSource random) {
        return random.nextInt(1200) == 0;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public void setPlayingDead(boolean playingDead) {
        this.entityData.set(DATA_PLAYING_DEAD, playingDead);
    }

    public boolean isPlayingDead() {
        return this.entityData.get(DATA_PLAYING_DEAD);
    }

    @Override
    public boolean fromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Axolotl baby = EntityTypes.AXOLOTL.create((Level)level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            Variant variant = Axolotl.useRareVariant(this.random) ? Variant.getRareSpawnVariant(this.random) : (this.random.nextBoolean() ? this.getVariant() : ((Axolotl)partner).getVariant());
            baby.setVariant(variant);
            baby.setPersistenceRequired();
        }
        return baby;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.AXOLOTL_FOOD);
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("axolotlBrain");
        this.getBrain().tick(level, this);
        profiler.pop();
        profiler.push("axolotlActivityUpdate");
        AxolotlAi.updateActivity(this);
        profiler.pop();
        if (!this.isNoAi()) {
            Optional<Integer> playDeadTicks = this.getBrain().getMemory(MemoryModuleType.PLAY_DEAD_TICKS);
            this.setPlayingDead(playDeadTicks.isPresent() && playDeadTicks.get() > 0);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 1.0).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public void playAttackSound() {
        this.playSound(SoundEvents.AXOLOTL_ATTACK, 1.0f, 1.0f);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        float currentHealth = this.getHealth();
        if (!this.isNoAi() && this.random.nextInt(3) == 0 && ((float)this.random.nextInt(3) < damage || currentHealth / this.getMaxHealth() < 0.5f) && damage < currentHealth && this.isInWater() && (source.getEntity() != null || source.getDirectEntity() != null) && !this.isPlayingDead()) {
            this.brain.setMemory(MemoryModuleType.PLAY_DEAD_TICKS, 200);
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);
        bucket.copyFrom(DataComponents.AXOLOTL_VARIANT, this);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            tag.putInt("Age", this.getAge());
            tag.putBoolean("AgeLocked", this.isAgeLocked());
            Brain<Axolotl> brain = this.getBrain();
            if (brain.hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN)) {
                tag.putLong("HuntingCooldown", brain.getTimeUntilExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN));
            }
        });
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        this.setAge(tag.getIntOr("Age", 0));
        this.setAgeLocked(tag.getBooleanOr("AgeLocked", false));
        tag.getLong("HuntingCooldown").ifPresentOrElse(huntingCooldown -> this.getBrain().setMemoryWithExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN, true, tag.getLongOr("HuntingCooldown", 0L)), () -> this.getBrain().setMemory(MemoryModuleType.HAS_HUNTING_COOLDOWN, Optional.empty()));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.AXOLOTL_BUCKET);
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_AXOLOTL;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.isPlayingDead() && super.canBeSeenAsEnemy();
    }

    public static void onStopAttacking(ServerLevel level, Axolotl body, LivingEntity target) {
        Entity entity;
        DamageSource lastDamageSource;
        if (target.isDeadOrDying() && (lastDamageSource = target.getLastDamageSource()) != null && (entity = lastDamageSource.getEntity()) instanceof Player) {
            Player player = (Player)entity;
            List<Player> playersInRange = level.getEntitiesOfClass(Player.class, body.getBoundingBox().inflate(20.0));
            if (playersInRange.contains(player)) {
                body.applySupportingEffects(player);
            }
        }
    }

    public void applySupportingEffects(Player player) {
        MobEffectInstance regenEffect = player.getEffect(MobEffects.REGENERATION);
        if (regenEffect == null || regenEffect.endsWithin(2399)) {
            int previousDuration = regenEffect != null ? regenEffect.getDuration() : 0;
            int regenDuration = Math.min(2400, 100 + previousDuration);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenDuration, 0), this);
        }
        player.removeEffect(MobEffects.MINING_FATIGUE);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.fromBucket();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.AXOLOTL_IDLE_WATER : SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.AXOLOTL_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.AXOLOTL_SWIM;
    }

    protected Brain<Axolotl> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    public Brain<Axolotl> getBrain() {
        return super.getBrain();
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    @Override
    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack itemStack) {
        if (itemStack.is(Items.TROPICAL_FISH_BUCKET)) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.WATER_BUCKET)));
        } else {
            super.usePlayerItem(player, hand, itemStack);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.fromBucket() && !this.hasCustomName();
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return this.getTargetFromBrain();
    }

    public static boolean checkAxolotlSpawnRules(EntityType<? extends LivingEntity> type, ServerLevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.AXOLOTLS_SPAWNABLE_ON);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    private static class AxolotlMoveControl<T extends Axolotl>
    extends SmoothSwimmingMoveControl<T> {
        public AxolotlMoveControl(T axolotl) {
            super(axolotl, 85, 10, 0.1f, 0.5f, false);
        }

        @Override
        public void tick() {
            if (!((Axolotl)this.mob).isPlayingDead()) {
                super.tick();
            }
        }
    }

    private class AxolotlLookControl
    extends SmoothSwimmingLookControl {
        final /* synthetic */ Axolotl this$0;

        public AxolotlLookControl(Axolotl axolotl, Axolotl axolotl2, int maxYRotFromCenter) {
            Axolotl axolotl3 = axolotl;
            Objects.requireNonNull(axolotl3);
            this.this$0 = axolotl3;
            super(axolotl2, maxYRotFromCenter);
        }

        @Override
        public void tick() {
            if (!this.this$0.isPlayingDead()) {
                super.tick();
            }
        }
    }

    public static enum Variant implements StringRepresentable
    {
        LUCY(0, "lucy", true),
        WILD(1, "wild", true),
        GOLD(2, "gold", true),
        CYAN(3, "cyan", true),
        BLUE(4, "blue", false);

        public static final Variant DEFAULT;
        private static final IntFunction<Variant> BY_ID;
        public static final StreamCodec<ByteBuf, Variant> STREAM_CODEC;
        public static final Codec<Variant> CODEC;
        @Deprecated
        public static final Codec<Variant> LEGACY_CODEC;
        private final int id;
        private final String name;
        private final boolean common;

        private Variant(int id, String name, boolean common) {
            this.id = id;
            this.name = name;
            this.common = common;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static Variant byId(int id) {
            return BY_ID.apply(id);
        }

        public static Variant getCommonSpawnVariant(RandomSource random) {
            return Variant.getSpawnVariant(random, true);
        }

        public static Variant getRareSpawnVariant(RandomSource random) {
            return Variant.getSpawnVariant(random, false);
        }

        private static Variant getSpawnVariant(RandomSource random, boolean common) {
            Variant[] validVariants = (Variant[])Arrays.stream(Variant.values()).filter(v -> v.common == common).toArray(Variant[]::new);
            return Util.getRandom(validVariants, random);
        }

        static {
            DEFAULT = LUCY;
            BY_ID = ByIdMap.continuous(Variant::getId, Variant.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Variant::getId);
            CODEC = StringRepresentable.fromEnum(Variant::values);
            LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Variant::getId);
        }
    }

    public static class AxolotlGroupData
    extends AgeableMob.AgeableMobGroupData {
        public final Variant[] types;

        public AxolotlGroupData(Variant ... types) {
            super(false);
            this.types = types;
        }

        public Variant getVariant(RandomSource random) {
            return this.types[random.nextInt(this.types.length)];
        }
    }

    public static enum AxolotlAnimationState {
        PLAYING_DEAD,
        IN_WATER,
        ON_GROUND,
        IN_AIR;

    }
}

