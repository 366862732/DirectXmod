/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.goat;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.goat.GoatAi;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Goat
extends Animal {
    public static final float LONG_JUMPING_DIMENSION_SCALE_FACTOR = 0.7f;
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45f, 0.65f).withEyeHeight(0.59375f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.53125f, 0.0f));
    public static final float BABY_DEFAULT_X_HEAD_ROT = 22.5f;
    public static final float MAX_ADDED_RAMMING_X_HEAD_ROT = 30.0f;
    private static final float BABY_SCALE = 0.55f;
    private static final int ADULT_ATTACK_DAMAGE = 2;
    private static final int BABY_ATTACK_DAMAGE = 1;
    private static final Brain.Provider<Goat> BRAIN_PROVIDER = Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS), goat -> GoatAi.getActivities());
    public static final int GOAT_FALL_DAMAGE_REDUCTION = 10;
    public static final double GOAT_SCREAMING_CHANCE = 0.02;
    public static final double UNIHORN_CHANCE = (double)0.1f;
    private static final EntityDataAccessor<Boolean> DATA_IS_SCREAMING_GOAT = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_LEFT_HORN = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_RIGHT_HORN = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.BOOLEAN);
    private static final boolean DEFAULT_IS_SCREAMING = false;
    private static final boolean DEFAULT_HAS_LEFT_HORN = true;
    private static final boolean DEFAULT_HAS_RIGHT_HORN = true;
    private boolean isLoweringHead;
    private int lowerHeadTick;

    public Goat(EntityType<? extends Goat> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.getNavigation().setCanFloat(true);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0f);
        this.setPathfindingMalus(PathType.ON_TOP_OF_POWDER_SNOW, -1.0f);
    }

    public ItemStack createHorn() {
        RandomSource random = RandomSource.createThreadLocalInstance(this.getUUID().hashCode());
        TagKey<Instrument> key = this.isScreamingGoat() ? InstrumentTags.SCREAMING_GOAT_HORNS : InstrumentTags.REGULAR_GOAT_HORNS;
        return this.level().registryAccess().lookupOrThrow(Registries.INSTRUMENT).getRandomElementOf(key, random).map(instrument -> InstrumentItem.create(Items.GOAT_HORN, instrument)).orElseGet(() -> new ItemStack(Items.GOAT_HORN));
    }

    protected Brain<Goat> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2f).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void ageBoundaryReached() {
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.isBaby() ? 1.0 : 2.0);
    }

    @Override
    protected int calculateFallDamage(double fallDistance, float damageModifier) {
        return super.calculateFallDamage(fallDistance, damageModifier) - 10;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isScreamingGoat()) {
            return SoundEvents.GOAT_SCREAMING_AMBIENT;
        }
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.isScreamingGoat()) {
            return SoundEvents.GOAT_SCREAMING_HURT;
        }
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        if (this.isScreamingGoat()) {
            return SoundEvents.GOAT_SCREAMING_DEATH;
        }
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.GOAT_STEP, 0.15f, 1.0f);
    }

    protected SoundEvent getMilkingSound() {
        if (this.isScreamingGoat()) {
            return SoundEvents.GOAT_SCREAMING_MILK;
        }
        return SoundEvents.GOAT_MILK;
    }

    @Override
    public @Nullable Goat getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Goat newGoat = EntityTypes.GOAT.create((Level)level, EntitySpawnReason.BREEDING);
        if (newGoat != null) {
            AgeableMob goat;
            GoatAi.initMemories(newGoat, level.getRandom());
            AgeableMob selectedParent = level.getRandom().nextBoolean() ? this : partner;
            boolean babyIsScreaming = selectedParent instanceof Goat && ((Goat)(goat = selectedParent)).isScreamingGoat() || level.getRandom().nextDouble() < 0.02;
            newGoat.setScreamingGoat(babyIsScreaming);
        }
        return newGoat;
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.55f : 1.0f;
    }

    public Brain<Goat> getBrain() {
        return super.getBrain();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("goatBrain");
        this.getBrain().tick(level, this);
        profiler.pop();
        profiler.push("goatActivityUpdate");
        GoatAi.updateActivity(this);
        profiler.pop();
        super.customServerAiStep(level);
    }

    @Override
    public int getMaxHeadYRot() {
        return 15;
    }

    @Override
    public void setYHeadRot(float yHeadRot) {
        int maxHeadYRot = this.getMaxHeadYRot();
        float deltaFromBody = Mth.degreesDifference(this.yBodyRot, yHeadRot);
        float deltaFromBodyClamped = Mth.clamp(deltaFromBody, (float)(-maxHeadYRot), (float)maxHeadYRot);
        super.setYHeadRot(this.yBodyRot + deltaFromBodyClamped);
    }

    @Override
    protected void playEatingSound() {
        this.level().playSound(null, this, this.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_EAT : SoundEvents.GOAT_EAT, SoundSource.NEUTRAL, 1.0f, Mth.randomBetween(this.level().getRandom(), 0.8f, 1.2f));
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.GOAT_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.is(Items.BUCKET) && !this.isBaby()) {
            player.playSound(this.getMilkingSound(), 1.0f, 1.0f);
            ItemStack bucketOrMilkBucket = ItemUtils.createFilledResult(heldItem, player, Items.MILK_BUCKET.getDefaultInstance());
            player.setItemInHand(hand, bucketOrMilkBucket);
            return InteractionResult.SUCCESS;
        }
        InteractionResult interactionResult = super.mobInteract(player, hand);
        if (interactionResult.consumesAction() && this.isFood(heldItem)) {
            this.playEatingSound();
        }
        return interactionResult;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        RandomSource random = level.getRandom();
        GoatAi.initMemories(this, random);
        this.setScreamingGoat(random.nextDouble() < 0.02);
        this.ageBoundaryReached();
        if (!this.isBaby() && (double)random.nextFloat() < (double)0.1f) {
            EntityDataAccessor<Boolean> hornToRemove = random.nextBoolean() ? DATA_HAS_LEFT_HORN : DATA_HAS_RIGHT_HORN;
            this.entityData.set(hornToRemove, false);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions entityDimensions = this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
        return pose == Pose.LONG_JUMPING ? entityDimensions.scale(0.7f) : entityDimensions;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsScreamingGoat", this.isScreamingGoat());
        output.putBoolean("HasLeftHorn", this.hasLeftHorn());
        output.putBoolean("HasRightHorn", this.hasRightHorn());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setScreamingGoat(input.getBooleanOr("IsScreamingGoat", false));
        this.entityData.set(DATA_HAS_LEFT_HORN, input.getBooleanOr("HasLeftHorn", true));
        this.entityData.set(DATA_HAS_RIGHT_HORN, input.getBooleanOr("HasRightHorn", true));
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 58) {
            this.isLoweringHead = true;
        } else if (id == 59) {
            this.isLoweringHead = false;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        this.lowerHeadTick = this.isLoweringHead ? ++this.lowerHeadTick : (this.lowerHeadTick -= 2);
        this.lowerHeadTick = Mth.clamp(this.lowerHeadTick, 0, 20);
        super.aiStep();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_IS_SCREAMING_GOAT, false);
        entityData.define(DATA_HAS_LEFT_HORN, true);
        entityData.define(DATA_HAS_RIGHT_HORN, true);
    }

    public boolean hasLeftHorn() {
        return this.entityData.get(DATA_HAS_LEFT_HORN);
    }

    public boolean hasRightHorn() {
        return this.entityData.get(DATA_HAS_RIGHT_HORN);
    }

    public boolean dropHorn() {
        if (this.isBaby()) {
            return false;
        }
        boolean hasLeft = this.hasLeftHorn();
        boolean hasRight = this.hasRightHorn();
        if (!hasLeft && !hasRight) {
            return false;
        }
        EntityDataAccessor<Boolean> hornToDrop = !hasLeft ? DATA_HAS_RIGHT_HORN : (!hasRight ? DATA_HAS_LEFT_HORN : (this.random.nextBoolean() ? DATA_HAS_LEFT_HORN : DATA_HAS_RIGHT_HORN));
        this.entityData.set(hornToDrop, false);
        Vec3 bodyPosition = this.position();
        ItemStack item = this.createHorn();
        double deltaX = Mth.randomBetween(this.random, -0.2f, 0.2f);
        double deltaY = Mth.randomBetween(this.random, 0.3f, 0.7f);
        double deltaZ = Mth.randomBetween(this.random, -0.2f, 0.2f);
        ItemEntity itemEntity = new ItemEntity(this.level(), bodyPosition.x(), bodyPosition.y(), bodyPosition.z(), item, deltaX, deltaY, deltaZ);
        this.level().addFreshEntity(itemEntity);
        return true;
    }

    public boolean isScreamingGoat() {
        return this.entityData.get(DATA_IS_SCREAMING_GOAT);
    }

    public void setScreamingGoat(boolean isScreamingGoat) {
        this.entityData.set(DATA_IS_SCREAMING_GOAT, isScreamingGoat);
    }

    public float getRammingXHeadRot() {
        float maxRammingXHeadRot = this.isBaby() ? 52.5f : 30.0f;
        return (float)this.lowerHeadTick / 20.0f * maxRammingXHeadRot * ((float)Math.PI / 180);
    }

    public static boolean checkGoatSpawnRules(EntityType<? extends Animal> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.GOATS_SPAWNABLE_ON) && Goat.isBrightEnoughToSpawn(level, pos);
    }
}

