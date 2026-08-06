/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.fox;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.JumpGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.StrollThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Fox
extends Animal {
    private static final EntityDataAccessor<Integer> DATA_TYPE_ID = SynchedEntityData.defineId(Fox.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Fox.class, EntityDataSerializers.BYTE);
    private static final float BABY_SCALE = 0.6f;
    private static final int FLAG_SITTING = 1;
    public static final int FLAG_CROUCHING = 4;
    public static final int FLAG_INTERESTED = 8;
    public static final int FLAG_POUNCING = 16;
    private static final int FLAG_SLEEPING = 32;
    private static final int FLAG_FACEPLANTED = 64;
    private static final int FLAG_DEFENDING = 128;
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_TRUSTED_ID_0 = SynchedEntityData.defineId(Fox.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_TRUSTED_ID_1 = SynchedEntityData.defineId(Fox.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    private static final Predicate<ItemEntity> ALLOWED_ITEMS = e -> !e.hasPickUpDelay() && e.isAlive();
    private static final Predicate<Entity> TRUSTED_TARGET_SELECTOR = entity -> {
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            return livingEntity.getLastHurtMob() != null && livingEntity.getLastHurtMobTimestamp() < livingEntity.tickCount + 600;
        }
        return false;
    };
    private static final Predicate<Entity> STALKABLE_PREY = entity -> entity instanceof Chicken || entity instanceof Rabbit;
    private static final Predicate<Entity> AVOID_PLAYERS = entity -> !entity.isDiscrete() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test((Entity)entity);
    private static final int MIN_TICKS_BEFORE_EAT = 600;
    private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.FOX.getDimensions().scale(0.6f).withEyeHeight(0.34375f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.375f, 0.0f));
    private static final Codec<List<EntityReference<LivingEntity>>> TRUSTED_LIST_CODEC = EntityReference.codec().listOf();
    private static final boolean DEFAULT_SLEEPING = false;
    private static final boolean DEFAULT_SITTING = false;
    private static final boolean DEFAULT_CROUCHING = false;
    private Goal landTargetGoal;
    private Goal turtleEggTargetGoal;
    private Goal fishTargetGoal;
    private float interestedAngle;
    private float interestedAngleO;
    private float crouchAmount;
    private float crouchAmountO;
    private static final float MAX_CROUCH_AMOUNT = 5.0f;
    private int ticksSinceEaten;

    public Fox(EntityType<? extends Fox> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.lookControl = new FoxLookControl(this);
        this.moveControl = new FoxMoveControl<Fox>(this);
        this.setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 0.0f);
        this.setPathfindingMalus(PathType.DAMAGING, 0.0f);
        this.setCanPickUpLoot(true);
        this.getNavigation().setRequiredPathLength(32.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_TRUSTED_ID_0, Optional.empty());
        entityData.define(DATA_TRUSTED_ID_1, Optional.empty());
        entityData.define(DATA_TYPE_ID, Variant.DEFAULT.getId());
        entityData.define(DATA_FLAGS_ID, (byte)0);
    }

    @Override
    protected void registerGoals() {
        this.landTargetGoal = new NearestAttackableTargetGoal<Animal>(this, Animal.class, 10, false, false, (target, level) -> target instanceof Chicken || target instanceof Rabbit);
        this.turtleEggTargetGoal = new NearestAttackableTargetGoal<Turtle>(this, Turtle.class, 10, false, false, Turtle.BABY_ON_LAND_SELECTOR);
        this.fishTargetGoal = new NearestAttackableTargetGoal<AbstractFish>(this, AbstractFish.class, 20, false, false, (target, level) -> target instanceof AbstractSchoolingFish);
        this.goalSelector.addGoal(0, new FoxFloatGoal(this));
        this.goalSelector.addGoal(0, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(1, new FaceplantGoal(this));
        this.goalSelector.addGoal(2, new FoxPanicGoal(this, 2.2));
        this.goalSelector.addGoal(3, new FoxBreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<Player>(this, Player.class, 16.0f, 1.6, 1.4, entity -> AVOID_PLAYERS.test((Entity)entity) && !this.trusts((LivingEntity)entity) && !this.isDefending()));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<Wolf>(this, Wolf.class, 8.0f, 1.6, 1.4, entity -> !((Wolf)entity).isTame() && !this.isDefending()));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<PolarBear>(this, PolarBear.class, 8.0f, 1.6, 1.4, entity -> !this.isDefending()));
        this.goalSelector.addGoal(5, new StalkPreyGoal(this));
        this.goalSelector.addGoal(6, new FoxPounceGoal(this));
        this.goalSelector.addGoal(6, new SeekShelterGoal(this, 1.25));
        this.goalSelector.addGoal(7, new FoxMeleeAttackGoal(this, (double)1.2f, true));
        this.goalSelector.addGoal(7, new SleepGoal(this));
        this.goalSelector.addGoal(8, new FoxFollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(9, new FoxStrollThroughVillageGoal(this, 32, 200));
        this.goalSelector.addGoal(10, new FoxEatBerriesGoal(this, (double)1.2f, 12, 1));
        this.goalSelector.addGoal(10, new LeapAtTargetGoal(this, 0.4f));
        this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(11, new FoxSearchForItemsGoal(this));
        this.goalSelector.addGoal(12, new FoxLookAtPlayerGoal(this, this, Player.class, 24.0f));
        this.goalSelector.addGoal(13, new PerchAndSearchGoal(this));
        this.targetSelector.addGoal(3, new DefendTrustedTargetGoal(this, LivingEntity.class, false, false, (target, level) -> TRUSTED_TARGET_SELECTOR.test(target) && !this.trusts(target)));
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide() && this.isAlive() && this.isEffectiveAi()) {
            LivingEntity target;
            ++this.ticksSinceEaten;
            ItemStack itemInMouth = this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (this.canEat(itemInMouth)) {
                if (this.ticksSinceEaten > 600) {
                    ItemStack remainingFood = itemInMouth.finishUsingItem(this.level(), this);
                    if (!remainingFood.isEmpty()) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, remainingFood);
                    }
                    this.ticksSinceEaten = 0;
                } else if (this.ticksSinceEaten > 560 && this.random.nextFloat() < 0.1f) {
                    this.playEatingSound();
                    this.level().broadcastEntityEvent(this, (byte)45);
                }
            }
            if ((target = this.getTarget()) == null || !target.isAlive()) {
                this.setIsCrouching(false);
                this.setIsInterested(false);
            }
        }
        if (this.isSleeping() || this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0f;
            this.zza = 0.0f;
        }
        super.aiStep();
        if (this.isDefending() && this.random.nextFloat() < 0.05f) {
            this.playSound(SoundEvents.FOX_AGGRO, 1.0f, 1.0f);
        }
    }

    @Override
    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    private boolean canEat(ItemStack itemInMouth) {
        return this.isConsumableFood(itemInMouth) && this.getTarget() == null && this.onGround() && !this.isSleeping();
    }

    private boolean isConsumableFood(ItemStack itemStack) {
        return itemStack.has(DataComponents.FOOD) && itemStack.has(DataComponents.CONSUMABLE);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextFloat() < 0.2f) {
            float odds = random.nextFloat();
            ItemStack heldInMouth = odds < 0.05f ? new ItemStack(Items.EMERALD) : (odds < 0.2f ? new ItemStack(Items.EGG) : (odds < 0.4f ? (random.nextBoolean() ? new ItemStack(Items.RABBIT_FOOT) : new ItemStack(Items.RABBIT_HIDE)) : (odds < 0.6f ? new ItemStack(Items.WHEAT) : (odds < 0.8f ? new ItemStack(Items.LEATHER) : new ItemStack(Items.FEATHER)))));
            this.setItemSlot(EquipmentSlot.MAINHAND, heldInMouth);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 45) {
            ItemStack mouthItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!mouthItem.isEmpty()) {
                ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(mouthItem));
                for (int i = 0; i < 8; ++i) {
                    Vec3 direction = new Vec3(((double)this.random.nextFloat() - 0.5) * 0.1, (double)this.random.nextFloat() * 0.1 + 0.1, 0.0).xRot(-this.getXRot() * ((float)Math.PI / 180)).yRot(-this.getYRot() * ((float)Math.PI / 180));
                    this.level().addParticle(breakParticle, this.getX() + this.getLookAngle().x / 2.0, this.getY(), this.getZ() + this.getLookAngle().z / 2.0, direction.x, direction.y + 0.05, direction.z);
                }
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.3f).add(Attributes.MAX_HEALTH, 10.0).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.SAFE_FALL_DISTANCE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public @Nullable Fox getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Fox baby = EntityTypes.FOX.create((Level)level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setVariant(this.random.nextBoolean() ? this.getVariant() : ((Fox)partner).getVariant());
        }
        return baby;
    }

    public static boolean checkFoxSpawnRules(EntityType<Fox> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.FOXES_SPAWNABLE_ON) && Fox.isBrightEnoughToSpawn(level, pos);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        Holder<Biome> biome = level.getBiome(this.blockPosition());
        Variant variant = Variant.byBiome(biome);
        boolean isBaby = false;
        if (groupData instanceof FoxGroupData) {
            FoxGroupData foxGroupData = (FoxGroupData)groupData;
            variant = foxGroupData.variant;
            if (foxGroupData.getGroupSize() >= 2) {
                isBaby = true;
            }
        } else {
            groupData = new FoxGroupData(variant);
        }
        this.setVariant(variant);
        if (isBaby) {
            this.setAge(-24000);
        }
        if (level instanceof ServerLevel) {
            this.setTargetGoals();
        }
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    private void setTargetGoals() {
        if (this.getVariant() == Variant.RED) {
            this.targetSelector.addGoal(4, this.landTargetGoal);
            this.targetSelector.addGoal(4, this.turtleEggTargetGoal);
            this.targetSelector.addGoal(6, this.fishTargetGoal);
        } else {
            this.targetSelector.addGoal(4, this.fishTargetGoal);
            this.targetSelector.addGoal(6, this.landTargetGoal);
            this.targetSelector.addGoal(6, this.turtleEggTargetGoal);
        }
    }

    @Override
    protected void playEatingSound() {
        this.playSound(SoundEvents.FOX_EAT, 1.0f, 1.0f);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_TYPE_ID));
    }

    private void setVariant(Variant variant) {
        this.entityData.set(DATA_TYPE_ID, variant.getId());
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.FOX_VARIANT) {
            return Fox.castComponentValue(type, this.getVariant());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.FOX_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.FOX_VARIANT) {
            this.setVariant(Fox.castComponentValue(DataComponents.FOX_VARIANT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    private Stream<EntityReference<LivingEntity>> getTrustedEntities() {
        return Stream.concat(this.entityData.get(DATA_TRUSTED_ID_0).stream(), this.entityData.get(DATA_TRUSTED_ID_1).stream());
    }

    private void addTrustedEntity(LivingEntity entity) {
        this.addTrustedEntity(EntityReference.of(entity));
    }

    private void addTrustedEntity(EntityReference<LivingEntity> reference) {
        if (this.entityData.get(DATA_TRUSTED_ID_0).isPresent()) {
            this.entityData.set(DATA_TRUSTED_ID_1, Optional.of(reference));
        } else {
            this.entityData.set(DATA_TRUSTED_ID_0, Optional.of(reference));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Trusted", TRUSTED_LIST_CODEC, this.getTrustedEntities().toList());
        output.putBoolean("Sleeping", this.isSleeping());
        output.store("Type", Variant.CODEC, this.getVariant());
        output.putBoolean("Sitting", this.isSitting());
        output.putBoolean("Crouching", this.isCrouching());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.clearTrusted();
        input.read("Trusted", TRUSTED_LIST_CODEC).orElse(List.of()).forEach(this::addTrustedEntity);
        this.setSleeping(input.getBooleanOr("Sleeping", false));
        this.setVariant(input.read("Type", Variant.CODEC).orElse(Variant.DEFAULT));
        this.setSitting(input.getBooleanOr("Sitting", false));
        this.setIsCrouching(input.getBooleanOr("Crouching", false));
        if (this.level() instanceof ServerLevel) {
            this.setTargetGoals();
        }
    }

    private void clearTrusted() {
        this.entityData.set(DATA_TRUSTED_ID_0, Optional.empty());
        this.entityData.set(DATA_TRUSTED_ID_1, Optional.empty());
    }

    public boolean isSitting() {
        return this.getFlag(1);
    }

    public void setSitting(boolean value) {
        this.setFlag(1, value);
    }

    public boolean isFaceplanted() {
        return this.getFlag(64);
    }

    private void setFaceplanted(boolean faceplanted) {
        this.setFlag(64, faceplanted);
    }

    private boolean isDefending() {
        return this.getFlag(128);
    }

    private void setDefending(boolean defending) {
        this.setFlag(128, defending);
    }

    @Override
    public boolean isSleeping() {
        return this.getFlag(32);
    }

    private void setSleeping(boolean sleeping) {
        this.setFlag(32, sleeping);
    }

    private void setFlag(int flag, boolean value) {
        if (value) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | flag));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~flag));
        }
    }

    private boolean getFlag(int flag) {
        return (this.entityData.get(DATA_FLAGS_ID) & flag) != 0;
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND && this.canPickUpLoot();
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack) {
        ItemStack heldItemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        return heldItemStack.isEmpty() || this.ticksSinceEaten > 0 && this.isConsumableFood(itemStack) && !this.isConsumableFood(heldItemStack);
    }

    private void spitOutItem(ItemStack itemStack) {
        if (itemStack.isEmpty() || this.level().isClientSide()) {
            return;
        }
        ItemEntity thrownItem = new ItemEntity(this.level(), this.getX() + this.getLookAngle().x, this.getY() + 1.0, this.getZ() + this.getLookAngle().z, itemStack);
        thrownItem.setPickUpDelay(40);
        thrownItem.setThrower(this);
        this.playSound(SoundEvents.FOX_SPIT, 1.0f, 1.0f);
        this.level().addFreshEntity(thrownItem);
    }

    private void dropItemStack(ItemStack itemStack) {
        ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemStack);
        this.level().addFreshEntity(itemEntity);
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack itemStack = entity.getItem();
        if (this.canHoldItem(itemStack)) {
            int count = itemStack.getCount();
            if (count > 1) {
                this.dropItemStack(itemStack.split(count - 1));
            }
            this.spitOutItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            this.onItemPickup(entity);
            this.setItemSlot(EquipmentSlot.MAINHAND, itemStack.split(1));
            this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            this.take(entity, itemStack.getCount());
            entity.discard();
            this.ticksSinceEaten = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isEffectiveAi()) {
            boolean inWater = this.isInWater();
            if (inWater || this.getTarget() != null || this.level().isThundering()) {
                this.wakeUp();
            }
            if (inWater || this.isSleeping()) {
                this.setSitting(false);
            }
            if (this.isFaceplanted() && this.level().getRandom().nextFloat() < 0.2f) {
                BlockPos pos = this.blockPosition();
                BlockState state = this.level().getBlockState(pos);
                this.level().levelEvent(2001, pos, Block.getId(state));
            }
        }
        this.interestedAngleO = this.interestedAngle;
        this.interestedAngle = this.isInterested() ? (this.interestedAngle += (1.0f - this.interestedAngle) * 0.4f) : (this.interestedAngle += (0.0f - this.interestedAngle) * 0.4f);
        this.crouchAmountO = this.crouchAmount;
        if (this.isCrouching()) {
            this.crouchAmount += 0.2f;
            if (this.crouchAmount > 5.0f) {
                this.crouchAmount = 5.0f;
            }
        } else {
            this.crouchAmount = 0.0f;
        }
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.FOX_FOOD);
    }

    @Override
    protected void onOffspringSpawnedFromEgg(Player spawner, Mob offspring) {
        ((Fox)offspring).addTrustedEntity(spawner);
    }

    public boolean isPouncing() {
        return this.getFlag(16);
    }

    public void setIsPouncing(boolean pouncing) {
        this.setFlag(16, pouncing);
    }

    public boolean isFullyCrouched() {
        return this.crouchAmount == 5.0f;
    }

    public void setIsCrouching(boolean isCrouching) {
        this.setFlag(4, isCrouching);
    }

    @Override
    public boolean isCrouching() {
        return this.getFlag(4);
    }

    public void setIsInterested(boolean value) {
        this.setFlag(8, value);
    }

    public boolean isInterested() {
        return this.getFlag(8);
    }

    public float getHeadRollAngle(float a) {
        return Mth.lerp(a, this.interestedAngleO, this.interestedAngle) * 0.11f * (float)Math.PI;
    }

    public float getCrouchAmount(float a) {
        return Mth.lerp(a, this.crouchAmountO, this.crouchAmount);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.isDefending() && target == null) {
            this.setDefending(false);
        }
        super.setTarget(target);
    }

    private void wakeUp() {
        this.setSleeping(false);
    }

    private void clearStates() {
        this.setIsInterested(false);
        this.setIsCrouching(false);
        this.setSitting(false);
        this.setSleeping(false);
        this.setDefending(false);
        this.setFaceplanted(false);
    }

    public boolean canMove() {
        return !this.isSleeping() && !this.isSitting() && !this.isFaceplanted();
    }

    @Override
    public void playAmbientSound() {
        SoundEvent ambient = this.getAmbientSound();
        if (ambient == SoundEvents.FOX_SCREECH) {
            this.playSound(ambient, 2.0f, this.getVoicePitch());
        } else {
            super.playAmbientSound();
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        List<Entity> nearbyEntities;
        if (this.isSleeping()) {
            return SoundEvents.FOX_SLEEP;
        }
        if (!this.level().isBrightOutside() && this.random.nextFloat() < 0.1f && (nearbyEntities = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0, 16.0, 16.0), EntitySelector.NO_SPECTATORS)).isEmpty()) {
            return SoundEvents.FOX_SCREECH;
        }
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    private boolean trusts(LivingEntity entity) {
        return this.getTrustedEntities().anyMatch(trusted -> trusted.matches(entity));
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!itemStack.isEmpty()) {
            this.spawnAtLocation(level, itemStack);
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        super.dropAllDeathLoot(level, source);
    }

    public static boolean isPathClear(Fox fox, LivingEntity target) {
        double zdiff = target.getZ() - fox.getZ();
        double xdiff = target.getX() - fox.getX();
        double slope = zdiff / xdiff;
        int increments = 6;
        for (int i = 0; i < 6; ++i) {
            double z = slope == 0.0 ? 0.0 : zdiff * (double)((float)i / 6.0f);
            double x = slope == 0.0 ? xdiff * (double)((float)i / 6.0f) : z / slope;
            for (int j = 1; j < 4; ++j) {
                if (fox.level().getBlockState(BlockPos.containing(fox.getX() + x, fox.getY() + (double)j, fox.getZ() + z)).canBeReplaced()) continue;
                return false;
            }
        }
        return true;
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.55f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }

    public class FoxLookControl
    extends LookControl {
        final /* synthetic */ Fox this$0;

        public FoxLookControl(Fox this$0) {
            Fox fox = this$0;
            Objects.requireNonNull(fox);
            this.this$0 = fox;
            super(this$0);
        }

        @Override
        public void tick() {
            if (!this.this$0.isSleeping()) {
                super.tick();
            }
        }

        @Override
        protected boolean resetXRotOnTick() {
            return !this.this$0.isPouncing() && !this.this$0.isCrouching() && !this.this$0.isInterested() && !this.this$0.isFaceplanted();
        }
    }

    private static class FoxMoveControl<T extends Fox>
    extends MoveControl<T> {
        public FoxMoveControl(T fox) {
            super(fox);
        }

        @Override
        public void tick() {
            if (((Fox)this.mob).canMove()) {
                super.tick();
            }
        }
    }

    public static enum Variant implements StringRepresentable
    {
        RED(0, "red"),
        SNOW(1, "snow");

        public static final Variant DEFAULT;
        public static final Codec<Variant> CODEC;
        private static final IntFunction<Variant> BY_ID;
        public static final StreamCodec<ByteBuf, Variant> STREAM_CODEC;
        private final int id;
        private final String name;

        private Variant(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public int getId() {
            return this.id;
        }

        public static Variant byId(int id) {
            return BY_ID.apply(id);
        }

        public static Variant byBiome(Holder<Biome> biome) {
            return biome.is(BiomeTags.SPAWNS_SNOW_FOXES) ? SNOW : RED;
        }

        static {
            DEFAULT = RED;
            CODEC = StringRepresentable.fromEnum(Variant::values);
            BY_ID = ByIdMap.continuous(Variant::getId, Variant.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Variant::getId);
        }
    }

    private class FoxFloatGoal
    extends FloatGoal {
        final /* synthetic */ Fox this$0;

        public FoxFloatGoal(Fox fox) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox);
        }

        @Override
        public void start() {
            super.start();
            this.this$0.clearStates();
        }

        @Override
        public boolean canUse() {
            return this.this$0.isInWater() && this.this$0.getFluidHeight(FluidTags.WATER) > 0.25 || this.this$0.isInLava();
        }
    }

    private class FaceplantGoal
    extends Goal {
        private int countdown;
        final /* synthetic */ Fox this$0;

        public FaceplantGoal(Fox fox) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.this$0.isFaceplanted();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && this.countdown > 0;
        }

        @Override
        public void start() {
            this.countdown = this.adjustedTickDelay(40);
        }

        @Override
        public void stop() {
            this.this$0.setFaceplanted(false);
        }

        @Override
        public void tick() {
            --this.countdown;
        }
    }

    private class FoxPanicGoal
    extends PanicGoal {
        final /* synthetic */ Fox this$0;

        public FoxPanicGoal(Fox fox, double speedModifier) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox, speedModifier);
        }

        @Override
        public boolean shouldPanic() {
            return !this.this$0.isDefending() && super.shouldPanic();
        }
    }

    private class FoxBreedGoal
    extends BreedGoal {
        public FoxBreedGoal(Fox fox, double speedModifier) {
            Objects.requireNonNull(fox);
            super(fox, speedModifier);
        }

        @Override
        public void start() {
            ((Fox)this.animal).clearStates();
            ((Fox)this.partner).clearStates();
            super.start();
        }

        @Override
        protected void breed() {
            Fox offspring = (Fox)this.animal.getBreedOffspring(this.level, this.partner);
            if (offspring == null) {
                return;
            }
            ServerPlayer animalLoveCause = this.animal.getLoveCause();
            ServerPlayer partnerLoveCause = this.partner.getLoveCause();
            ServerPlayer loveCause = animalLoveCause;
            if (animalLoveCause != null) {
                offspring.addTrustedEntity(animalLoveCause);
            } else {
                loveCause = partnerLoveCause;
            }
            if (partnerLoveCause != null && animalLoveCause != partnerLoveCause) {
                offspring.addTrustedEntity(partnerLoveCause);
            }
            if (loveCause != null) {
                loveCause.awardStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(loveCause, this.animal, this.partner, offspring);
            }
            this.animal.setAge(6000);
            this.partner.setAge(6000);
            this.animal.resetLove();
            this.partner.resetLove();
            offspring.setAge(-24000);
            offspring.snapTo(this.animal.getX(), this.animal.getY(), this.animal.getZ(), 0.0f, 0.0f);
            this.level.addFreshEntityWithPassengers(offspring);
            this.level.broadcastEntityEvent(this.animal, (byte)18);
            if (this.level.getGameRules().get(GameRules.MOB_DROPS).booleanValue()) {
                this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), this.animal.getRandom().nextInt(7) + 1));
            }
        }
    }

    private class StalkPreyGoal
    extends Goal {
        final /* synthetic */ Fox this$0;

        public StalkPreyGoal(Fox fox) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.this$0.isSleeping()) {
                return false;
            }
            LivingEntity target = this.this$0.getTarget();
            return target != null && target.isAlive() && STALKABLE_PREY.test(target) && this.this$0.distanceToSqr(target) > 36.0 && !this.this$0.isCrouching() && !this.this$0.isInterested() && !this.this$0.jumping;
        }

        @Override
        public void start() {
            this.this$0.setSitting(false);
            this.this$0.setFaceplanted(false);
        }

        @Override
        public void stop() {
            LivingEntity target = this.this$0.getTarget();
            if (target != null && Fox.isPathClear(this.this$0, target)) {
                this.this$0.setIsInterested(true);
                this.this$0.setIsCrouching(true);
                this.this$0.getNavigation().stop();
                this.this$0.getLookControl().setLookAt(target, this.this$0.getMaxHeadYRot(), this.this$0.getMaxHeadXRot());
            } else {
                this.this$0.setIsInterested(false);
                this.this$0.setIsCrouching(false);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.this$0.getTarget();
            if (target == null) {
                return;
            }
            this.this$0.getLookControl().setLookAt(target, this.this$0.getMaxHeadYRot(), this.this$0.getMaxHeadXRot());
            if (this.this$0.distanceToSqr(target) <= 36.0) {
                this.this$0.setIsInterested(true);
                this.this$0.setIsCrouching(true);
                this.this$0.getNavigation().stop();
            } else {
                this.this$0.getNavigation().moveTo(target, 1.5);
            }
        }
    }

    public class FoxPounceGoal
    extends JumpGoal {
        final /* synthetic */ Fox this$0;

        public FoxPounceGoal(Fox this$0) {
            Fox fox = this$0;
            Objects.requireNonNull(fox);
            this.this$0 = fox;
        }

        @Override
        public boolean canUse() {
            if (!this.this$0.isFullyCrouched()) {
                return false;
            }
            LivingEntity target = this.this$0.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            if (target.getMotionDirection() != target.getDirection()) {
                return false;
            }
            boolean hasClearPath = Fox.isPathClear(this.this$0, target);
            if (!hasClearPath) {
                this.this$0.getNavigation().createPath(target, 0);
                this.this$0.setIsCrouching(false);
                this.this$0.setIsInterested(false);
            }
            return hasClearPath;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.this$0.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double yd = this.this$0.getDeltaMovement().y;
            return !(yd * yd < (double)0.05f && Math.abs(this.this$0.getXRot()) < 15.0f && this.this$0.onGround() || this.this$0.isFaceplanted());
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void start() {
            this.this$0.setJumping(true);
            this.this$0.setIsPouncing(true);
            this.this$0.setIsInterested(false);
            LivingEntity target = this.this$0.getTarget();
            if (target != null) {
                this.this$0.getLookControl().setLookAt(target, 60.0f, 30.0f);
                Vec3 uv = new Vec3(target.getX() - this.this$0.getX(), target.getY() - this.this$0.getY(), target.getZ() - this.this$0.getZ()).normalize();
                this.this$0.setDeltaMovement(this.this$0.getDeltaMovement().add(uv.x * 0.8, 0.9, uv.z * 0.8));
            }
            this.this$0.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.this$0.setIsCrouching(false);
            this.this$0.crouchAmount = 0.0f;
            this.this$0.crouchAmountO = 0.0f;
            this.this$0.setIsInterested(false);
            this.this$0.setIsPouncing(false);
        }

        @Override
        public void tick() {
            LivingEntity target = this.this$0.getTarget();
            if (target != null) {
                this.this$0.getLookControl().setLookAt(target, 60.0f, 30.0f);
            }
            if (!this.this$0.isFaceplanted()) {
                Vec3 movement = this.this$0.getDeltaMovement();
                if (movement.y * movement.y < (double)0.03f && this.this$0.getXRot() != 0.0f) {
                    this.this$0.setXRot(Mth.rotLerp(0.2f, this.this$0.getXRot(), 0.0f));
                } else {
                    float upwardsBias;
                    double biasedY;
                    double direction = movement.horizontalDistance();
                    double len = Math.sqrt(direction * direction + (biasedY = movement.y * (double)(upwardsBias = this.this$0.jumping && movement.y > 0.0 ? 6.5f : 1.0f)) * biasedY);
                    if (len > (double)1.0E-5f) {
                        double rotation = Math.signum(-biasedY) * Math.acos(direction / len) * 57.2957763671875;
                        this.this$0.setXRot((float)rotation);
                    }
                }
            }
            if (target != null && this.this$0.distanceTo(target) <= 2.0f) {
                this.this$0.doHurtTarget(FoxPounceGoal.getServerLevel(this.this$0.level()), target);
            } else if (this.this$0.getXRot() > 0.0f && this.this$0.onGround() && (float)this.this$0.getDeltaMovement().y != 0.0f && this.this$0.level().getBlockState(this.this$0.blockPosition()).is(Blocks.SNOW)) {
                this.this$0.setXRot(60.0f);
                this.this$0.setTarget(null);
                this.this$0.setFaceplanted(true);
            }
        }
    }

    private class SeekShelterGoal
    extends FleeSunGoal {
        private int interval;
        final /* synthetic */ Fox this$0;

        public SeekShelterGoal(Fox fox, double speedModifier) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox, speedModifier);
            this.interval = SeekShelterGoal.reducedTickDelay(100);
        }

        @Override
        public boolean canUse() {
            if (this.this$0.isSleeping() || this.mob.getTarget() != null) {
                return false;
            }
            if (this.this$0.level().isThundering() && this.this$0.level().canSeeSky(this.mob.blockPosition())) {
                return this.setWantedPos();
            }
            if (this.interval > 0) {
                --this.interval;
                return false;
            }
            this.interval = 100;
            BlockPos pos = this.mob.blockPosition();
            return this.this$0.level().isBrightOutside() && this.this$0.level().canSeeSky(pos) && !((ServerLevel)this.this$0.level()).isVillage(pos) && this.setWantedPos();
        }

        @Override
        public void start() {
            this.this$0.clearStates();
            super.start();
        }
    }

    private class FoxMeleeAttackGoal
    extends MeleeAttackGoal {
        final /* synthetic */ Fox this$0;

        public FoxMeleeAttackGoal(Fox fox, double speedModifier, boolean trackTarget) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox, speedModifier, trackTarget);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.mob.doHurtTarget(FoxMeleeAttackGoal.getServerLevel(this.mob), target);
                this.this$0.playSound(SoundEvents.FOX_BITE, 1.0f, 1.0f);
            }
        }

        @Override
        public void start() {
            this.this$0.setIsInterested(false);
            super.start();
        }

        @Override
        public boolean canUse() {
            return !this.this$0.isSitting() && !this.this$0.isSleeping() && !this.this$0.isCrouching() && !this.this$0.isFaceplanted() && super.canUse();
        }
    }

    private class SleepGoal
    extends FoxBehaviorGoal {
        private static final int WAIT_TIME_BEFORE_SLEEP = SleepGoal.reducedTickDelay(140);
        private int countdown;
        final /* synthetic */ Fox this$0;

        public SleepGoal(Fox fox) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox);
            this.countdown = fox.random.nextInt(WAIT_TIME_BEFORE_SLEEP);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (this.this$0.xxa != 0.0f || this.this$0.yya != 0.0f || this.this$0.zza != 0.0f) {
                return false;
            }
            return this.canSleep() || this.this$0.isSleeping();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            }
            return this.this$0.level().isBrightOutside() && this.hasShelter() && !this.alertable() && !this.this$0.isInPowderSnow;
        }

        @Override
        public void stop() {
            this.countdown = this.this$0.random.nextInt(WAIT_TIME_BEFORE_SLEEP);
            this.this$0.clearStates();
        }

        @Override
        public void start() {
            this.this$0.setSitting(false);
            this.this$0.setIsCrouching(false);
            this.this$0.setIsInterested(false);
            this.this$0.setJumping(false);
            this.this$0.setSleeping(true);
            this.this$0.getNavigation().stop();
            this.this$0.getMoveControl().setWantedPosition(this.this$0.getX(), this.this$0.getY(), this.this$0.getZ(), 0.0);
        }
    }

    private static class FoxFollowParentGoal
    extends FollowParentGoal {
        private final Fox fox;

        public FoxFollowParentGoal(Fox fox, double speedModifier) {
            super(fox, speedModifier);
            this.fox = fox;
        }

        @Override
        public boolean canUse() {
            return !this.fox.isDefending() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.fox.isDefending() && super.canContinueToUse();
        }

        @Override
        public void start() {
            this.fox.clearStates();
            super.start();
        }
    }

    private class FoxStrollThroughVillageGoal
    extends StrollThroughVillageGoal {
        final /* synthetic */ Fox this$0;

        public FoxStrollThroughVillageGoal(Fox fox, int searchRadius, int interval) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox, interval);
        }

        @Override
        public void start() {
            this.this$0.clearStates();
            super.start();
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.canFoxMove();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.canFoxMove();
        }

        private boolean canFoxMove() {
            return !this.this$0.isSleeping() && !this.this$0.isSitting() && !this.this$0.isDefending() && this.this$0.getTarget() == null;
        }
    }

    public class FoxEatBerriesGoal
    extends MoveToBlockGoal {
        private static final int WAIT_TICKS = 40;
        protected int ticksWaited;
        final /* synthetic */ Fox this$0;

        public FoxEatBerriesGoal(Fox this$0, double speedModifier, int searchRange, int verticalSearchRange) {
            Fox fox = this$0;
            Objects.requireNonNull(fox);
            this.this$0 = fox;
            super(this$0, speedModifier, searchRange, verticalSearchRange);
        }

        @Override
        public double acceptedDistance() {
            return 2.0;
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 100 == 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            BlockState blockState = level.getBlockState(pos);
            return blockState.is(Blocks.SWEET_BERRY_BUSH) && blockState.getValue(SweetBerryBushBlock.AGE) >= 2 || CaveVines.hasGlowBerries(blockState);
        }

        @Override
        public void tick() {
            if (this.isReachedTarget()) {
                if (this.ticksWaited >= 40) {
                    this.onReachedTarget();
                } else {
                    ++this.ticksWaited;
                }
            } else if (!this.isReachedTarget() && this.this$0.random.nextFloat() < 0.05f) {
                this.this$0.playSound(SoundEvents.FOX_SNIFF, 1.0f, 1.0f);
            }
            super.tick();
        }

        protected void onReachedTarget() {
            if (!FoxEatBerriesGoal.getServerLevel(this.this$0.level()).getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                return;
            }
            BlockState state = this.this$0.level().getBlockState(this.blockPos);
            if (state.is(Blocks.SWEET_BERRY_BUSH)) {
                this.pickSweetBerries(state);
            } else if (CaveVines.hasGlowBerries(state)) {
                this.pickGlowBerry(state);
            }
        }

        private void pickGlowBerry(BlockState state) {
            CaveVines.use(this.this$0, state, this.this$0.level(), this.blockPos);
        }

        private void pickSweetBerries(BlockState state) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            state.setValue(SweetBerryBushBlock.AGE, 1);
            int count = 1 + this.this$0.level().getRandom().nextInt(2) + (age == 3 ? 1 : 0);
            ItemStack heldItem = this.this$0.getItemBySlot(EquipmentSlot.MAINHAND);
            if (heldItem.isEmpty()) {
                this.this$0.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
                --count;
            }
            if (count > 0) {
                Block.popResource(this.this$0.level(), this.blockPos, new ItemStack(Items.SWEET_BERRIES, count));
            }
            this.this$0.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0f, 1.0f);
            this.this$0.level().setBlock(this.blockPos, (BlockState)state.setValue(SweetBerryBushBlock.AGE, 1), 2);
            this.this$0.level().gameEvent(GameEvent.BLOCK_CHANGE, this.blockPos, GameEvent.Context.of(this.this$0));
        }

        @Override
        public boolean canUse() {
            return !this.this$0.isSleeping() && super.canUse();
        }

        @Override
        public void start() {
            this.ticksWaited = 0;
            this.this$0.setSitting(false);
            super.start();
        }
    }

    private class FoxSearchForItemsGoal
    extends Goal {
        final /* synthetic */ Fox this$0;

        public FoxSearchForItemsGoal(Fox fox) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.this$0.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                return false;
            }
            if (this.this$0.getTarget() != null || this.this$0.getLastHurtByMob() != null) {
                return false;
            }
            if (!this.this$0.canMove()) {
                return false;
            }
            if (this.this$0.getRandom().nextInt(FoxSearchForItemsGoal.reducedTickDelay(10)) != 0) {
                return false;
            }
            List<ItemEntity> items = this.this$0.level().getEntitiesOfClass(ItemEntity.class, this.this$0.getBoundingBox().inflate(8.0, 8.0, 8.0), ALLOWED_ITEMS);
            return !items.isEmpty() && this.this$0.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
        }

        @Override
        public void tick() {
            List<ItemEntity> items = this.this$0.level().getEntitiesOfClass(ItemEntity.class, this.this$0.getBoundingBox().inflate(8.0, 8.0, 8.0), ALLOWED_ITEMS);
            ItemStack itemStack = this.this$0.getItemBySlot(EquipmentSlot.MAINHAND);
            if (itemStack.isEmpty() && !items.isEmpty()) {
                this.this$0.getNavigation().moveTo(items.get(0), (double)1.2f);
            }
        }

        @Override
        public void start() {
            List<ItemEntity> items = this.this$0.level().getEntitiesOfClass(ItemEntity.class, this.this$0.getBoundingBox().inflate(8.0, 8.0, 8.0), ALLOWED_ITEMS);
            if (!items.isEmpty()) {
                this.this$0.getNavigation().moveTo(items.get(0), (double)1.2f);
            }
        }
    }

    private class FoxLookAtPlayerGoal
    extends LookAtPlayerGoal {
        final /* synthetic */ Fox this$0;

        public FoxLookAtPlayerGoal(Fox fox, Mob mob, Class<? extends LivingEntity> lookAtType, float lookDistance) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(mob, lookAtType, lookDistance);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.this$0.isFaceplanted() && !this.this$0.isInterested();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && !this.this$0.isFaceplanted() && !this.this$0.isInterested();
        }
    }

    private class PerchAndSearchGoal
    extends FoxBehaviorGoal {
        private double relX;
        private double relZ;
        private int lookTime;
        private int looksRemaining;
        final /* synthetic */ Fox this$0;

        public PerchAndSearchGoal(Fox fox) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.this$0.getLastHurtByMob() == null && this.this$0.getRandom().nextFloat() < 0.02f && !this.this$0.isSleeping() && this.this$0.getTarget() == null && this.this$0.getNavigation().isDone() && !this.alertable() && !this.this$0.isPouncing() && !this.this$0.isCrouching();
        }

        @Override
        public boolean canContinueToUse() {
            return this.looksRemaining > 0;
        }

        @Override
        public void start() {
            this.resetLook();
            this.looksRemaining = 2 + this.this$0.getRandom().nextInt(3);
            this.this$0.setSitting(true);
            this.this$0.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.this$0.setSitting(false);
        }

        @Override
        public void tick() {
            --this.lookTime;
            if (this.lookTime <= 0) {
                --this.looksRemaining;
                this.resetLook();
            }
            this.this$0.getLookControl().setLookAt(this.this$0.getX() + this.relX, this.this$0.getEyeY(), this.this$0.getZ() + this.relZ, this.this$0.getMaxHeadYRot(), this.this$0.getMaxHeadXRot());
        }

        private void resetLook() {
            double rnd = Math.PI * 2 * this.this$0.getRandom().nextDouble();
            this.relX = Math.cos(rnd);
            this.relZ = Math.sin(rnd);
            this.lookTime = this.adjustedTickDelay(80 + this.this$0.getRandom().nextInt(20));
        }
    }

    private class DefendTrustedTargetGoal
    extends NearestAttackableTargetGoal<LivingEntity> {
        private @Nullable LivingEntity trustedLastHurtBy;
        private @Nullable LivingEntity trustedLastHurt;
        private int timestamp;
        final /* synthetic */ Fox this$0;

        public DefendTrustedTargetGoal(Fox fox, Class<LivingEntity> targetType, boolean mustSee, @Nullable boolean mustReach, TargetingConditions.Selector subselector) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            super(fox, targetType, 10, mustSee, mustReach, subselector);
        }

        @Override
        public boolean canUse() {
            if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
                return false;
            }
            ServerLevel level = DefendTrustedTargetGoal.getServerLevel(this.this$0.level());
            for (EntityReference<LivingEntity> trustedReference : this.this$0.getTrustedEntities().toList()) {
                LivingEntity trustedEntity = trustedReference.getEntity(level, LivingEntity.class);
                if (trustedEntity == null) continue;
                this.trustedLastHurt = trustedEntity;
                this.trustedLastHurtBy = trustedEntity.getLastHurtByMob();
                int timestamp = trustedEntity.getLastHurtByMobTimestamp();
                return timestamp != this.timestamp && this.canAttack(this.trustedLastHurtBy, this.targetConditions);
            }
            return false;
        }

        @Override
        public void start() {
            this.setTarget(this.trustedLastHurtBy);
            this.target = this.trustedLastHurtBy;
            if (this.trustedLastHurt != null) {
                this.timestamp = this.trustedLastHurt.getLastHurtByMobTimestamp();
            }
            this.this$0.playSound(SoundEvents.FOX_AGGRO, 1.0f, 1.0f);
            this.this$0.setDefending(true);
            this.this$0.wakeUp();
            super.start();
        }
    }

    public static class FoxGroupData
    extends AgeableMob.AgeableMobGroupData {
        public final Variant variant;

        public FoxGroupData(Variant variant) {
            super(false);
            this.variant = variant;
        }
    }

    private abstract class FoxBehaviorGoal
    extends Goal {
        private final TargetingConditions alertableTargeting;
        final /* synthetic */ Fox this$0;

        private FoxBehaviorGoal(Fox fox) {
            Fox fox2 = fox;
            Objects.requireNonNull(fox2);
            this.this$0 = fox2;
            this.alertableTargeting = TargetingConditions.forCombat().range(12.0).ignoreLineOfSight().selector(new FoxAlertableEntitiesSelector(this.this$0));
        }

        protected boolean hasShelter() {
            BlockPos foxPos = BlockPos.containing(this.this$0.getX(), this.this$0.getBoundingBox().maxY, this.this$0.getZ());
            return !this.this$0.level().canSeeSky(foxPos) && this.this$0.getWalkTargetValue(foxPos) >= 0.0f;
        }

        protected boolean alertable() {
            return !FoxBehaviorGoal.getServerLevel(this.this$0.level()).getNearbyEntities(LivingEntity.class, this.alertableTargeting, this.this$0, this.this$0.getBoundingBox().inflate(12.0, 6.0, 12.0)).isEmpty();
        }
    }

    public class FoxAlertableEntitiesSelector
    implements TargetingConditions.Selector {
        final /* synthetic */ Fox this$0;

        public FoxAlertableEntitiesSelector(Fox this$0) {
            Fox fox = this$0;
            Objects.requireNonNull(fox);
            this.this$0 = fox;
        }

        @Override
        public boolean test(LivingEntity target, ServerLevel level) {
            Player player;
            if (target instanceof Fox) {
                return false;
            }
            if (target instanceof Chicken || target instanceof Rabbit || target instanceof Monster) {
                return true;
            }
            if (target instanceof TamableAnimal) {
                TamableAnimal tamableAnimal = (TamableAnimal)target;
                return !tamableAnimal.isTame();
            }
            if (target instanceof Player && ((player = (Player)target).isSpectator() || player.isCreative())) {
                return false;
            }
            if (this.this$0.trusts(target)) {
                return false;
            }
            return !target.isSleeping() && !target.isDiscrete();
        }
    }
}

