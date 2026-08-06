/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.pig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.PigSoundVariant;
import net.minecraft.world.entity.animal.pig.PigSoundVariants;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.pig.PigVariants;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Pig
extends Animal
implements ItemSteerable {
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Holder<PigVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.PIG_VARIANT);
    private static final EntityDataAccessor<Holder<PigSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.PIG_SOUND_VARIANT);
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45f, 0.45f).withEyeHeight(0.40625f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.5f, 0.0f));
    private final ItemBasedSteering steering;

    public Pig(EntityType<? extends Pig> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, i -> i.is(Items.CARROT_ON_A_STICK), false));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, i -> i.is(ItemTags.PIG_FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Player player;
        Entity entity;
        if (this.isSaddled() && (entity = this.getFirstPassenger()) instanceof Player && (player = (Player)entity).isHolding(Items.CARROT_ON_A_STICK)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_BOOST_TIME.equals(accessor) && this.level().isClientSide()) {
            this.steering.onSynced();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        HolderLookup.RegistryLookup pigSoundVariants = this.registryAccess().lookupOrThrow(Registries.PIG_SOUND_VARIANT);
        entityData.define(DATA_BOOST_TIME, 0);
        entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), PigVariants.DEFAULT));
        entityData.define(DATA_SOUND_VARIANT_ID, (Holder)pigSoundVariants.get(PigSoundVariants.CLASSIC).or(((Registry)pigSoundVariants)::getAny).orElseThrow());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        VariantUtils.writeVariant(output, this.getVariant());
        this.getSoundVariant().unwrapKey().ifPresent(soundVariant -> output.store("sound_variant", ResourceKey.codec(Registries.PIG_SOUND_VARIANT), soundVariant));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        VariantUtils.readVariant(input, Registries.PIG_VARIANT).ifPresent(this::setVariant);
        input.read("sound_variant", ResourceKey.codec(Registries.PIG_SOUND_VARIANT)).flatMap(soundVariant -> this.registryAccess().lookupOrThrow(Registries.PIG_SOUND_VARIANT).get((ResourceKey)soundVariant)).ifPresent(this::setSoundVariant);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getSoundSet().ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.getSoundSet().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getSoundSet().deathSound().value();
    }

    @Override
    protected void playEatingSound() {
        this.makeSound(this.getSoundSet().eatSound().value());
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(this.getSoundSet().stepSound().value(), 0.15f, 1.0f);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean hasFood = this.isFood(player.getItemInHand(hand));
        if (!hasFood && this.isSaddled() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        InteractionResult interactionResult = super.mobInteract(player, hand);
        if (!interactionResult.consumesAction()) {
            ItemStack itemStack = player.getItemInHand(hand);
            if (this.isEquippableInSlot(itemStack, EquipmentSlot.SADDLE)) {
                return itemStack.interactLivingEntity(player, this, hand);
            }
            return InteractionResult.PASS;
        }
        return interactionResult;
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.SADDLE) {
            return this.isAlive() && !this.isBaby();
        }
        return super.canUseSlot(slot);
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);
    }

    @Override
    protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
        if (slot == EquipmentSlot.SADDLE) {
            return SoundEvents.PIG_SADDLE;
        }
        return super.getEquipSound(slot, stack, equippable);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightningBolt) {
        if (level.getDifficulty() != Difficulty.PEACEFUL) {
            ZombifiedPiglin zombifiedPiglin = this.convertTo(EntityTypes.ZOMBIFIED_PIGLIN, ConversionParams.single(this, false, true), zp -> {
                zp.populateDefaultEquipmentSlots(this.getRandom(), level.getCurrentDifficultyAt(this.blockPosition()));
                zp.setPersistenceRequired();
            });
            if (zombifiedPiglin == null) {
                super.thunderHit(level, lightningBolt);
            }
        } else {
            super.thunderHit(level, lightningBolt);
        }
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5f);
        this.yBodyRot = this.yHeadRot = this.getYRot();
        this.yRotO = this.yHeadRot;
        this.steering.tickBoost();
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225 * (double)this.steering.boostFactor());
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public @Nullable Pig getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Pig baby = EntityTypes.PIG.create((Level)level, EntitySpawnReason.BREEDING);
        if (baby != null && partner instanceof Pig) {
            Pig partnerPig = (Pig)partner;
            baby.setVariant(this.random.nextBoolean() ? this.getVariant() : partnerPig.getVariant());
        }
        return baby;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.PIG_FOOD);
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }

    private void setVariant(Holder<PigVariant> variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    public Holder<PigVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    private Holder<PigSoundVariant> getSoundVariant() {
        return this.entityData.get(DATA_SOUND_VARIANT_ID);
    }

    private void setSoundVariant(Holder<PigSoundVariant> soundVariant) {
        this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
    }

    private PigSoundVariant.PigSoundSet getSoundSet() {
        return this.isBaby() ? this.getSoundVariant().value().babySounds() : this.getSoundVariant().value().adultSounds();
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.PIG_VARIANT) {
            return Pig.castComponentValue(type, this.getVariant());
        }
        if (type == DataComponents.PIG_SOUND_VARIANT) {
            return Pig.castComponentValue(type, this.getSoundVariant());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.PIG_VARIANT);
        this.applyImplicitComponentIfPresent(components, DataComponents.PIG_SOUND_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.PIG_VARIANT) {
            this.setVariant(Pig.castComponentValue(DataComponents.PIG_VARIANT, value));
            return true;
        }
        if (type == DataComponents.PIG_SOUND_VARIANT) {
            this.setSoundVariant(Pig.castComponentValue(DataComponents.PIG_SOUND_VARIANT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.PIG_VARIANT).ifPresent(this::setVariant);
        this.setSoundVariant(PigSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()));
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }
}

