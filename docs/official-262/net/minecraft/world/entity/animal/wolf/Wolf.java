/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.wolf;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariants;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Wolf
extends TamableAnimal
implements NeutralMob {
    private static final EntityDataAccessor<Boolean> DATA_INTERESTED_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Holder<WolfVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.WOLF_VARIANT);
    private static final EntityDataAccessor<Holder<WolfSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.WOLF_SOUND_VARIANT);
    public static final TargetingConditions.Selector PREY_SELECTOR = (target, level) -> target.is(EntityTypes.SHEEP) || target.is(EntityTypes.RABBIT) || target.is(EntityTypes.FOX);
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.3f, 0.425f).withEyeHeight(0.34375f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.4375f, 0.0f));
    private static final float START_HEALTH = 8.0f;
    private static final float TAME_HEALTH = 40.0f;
    private static final float ARMOR_REPAIR_UNIT = 0.125f;
    public static final float DEFAULT_TAIL_ANGLE = 0.62831855f;
    private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;
    private float interestedAngle;
    private float interestedAngleO;
    private boolean isWet;
    private boolean isShaking;
    private float shakeAnim;
    private float shakeAnimO;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

    public Wolf(EntityType<? extends Wolf> type, Level level) {
        super((EntityType<? extends TamableAnimal>)type, level);
        this.setTame(false, false);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0f);
        this.setPathfindingMalus(PathType.ON_TOP_OF_POWDER_SNOW, -1.0f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(this, 1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new WolfAvoidEntityGoal<Llama>(this, this, Llama.class, 24.0f, 1.5, 1.5));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4f));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new BegGoal(this, 8.0f));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new NonTameRandomTargetGoal<Animal>(this, Animal.class, false, PREY_SELECTOR));
        this.targetSelector.addGoal(6, new NonTameRandomTargetGoal<Turtle>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<AbstractSkeleton>((Mob)this, AbstractSkeleton.class, false));
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<Wolf>(this, true));
    }

    public Identifier getTexture() {
        WolfVariant.AssetInfo assetInfo;
        WolfVariant variant = this.getVariant().value();
        WolfVariant.AssetInfo assetInfo2 = assetInfo = this.isBaby() ? variant.babyInfo() : variant.adultInfo();
        if (this.isTame()) {
            return assetInfo.tame().texturePath();
        }
        if (this.isAngry()) {
            return assetInfo.angry().texturePath();
        }
        return assetInfo.wild().texturePath();
    }

    private Holder<WolfVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    private void setVariant(Holder<WolfVariant> variant) {
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    private Holder<WolfSoundVariant> getSoundVariant() {
        return this.entityData.get(DATA_SOUND_VARIANT_ID);
    }

    private WolfSoundVariant.WolfSoundSet getSoundSet() {
        return this.isBaby() ? this.getSoundVariant().value().babySounds() : this.getSoundVariant().value().adultSounds();
    }

    private void setSoundVariant(Holder<WolfSoundVariant> soundVariant) {
        this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.WOLF_VARIANT) {
            return Wolf.castComponentValue(type, this.getVariant());
        }
        if (type == DataComponents.WOLF_SOUND_VARIANT) {
            return Wolf.castComponentValue(type, this.getSoundVariant());
        }
        if (type == DataComponents.WOLF_COLLAR) {
            return Wolf.castComponentValue(type, this.getCollarColor());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.WOLF_VARIANT);
        this.applyImplicitComponentIfPresent(components, DataComponents.WOLF_SOUND_VARIANT);
        this.applyImplicitComponentIfPresent(components, DataComponents.WOLF_COLLAR);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.WOLF_VARIANT) {
            this.setVariant(Wolf.castComponentValue(DataComponents.WOLF_VARIANT, value));
            return true;
        }
        if (type == DataComponents.WOLF_SOUND_VARIANT) {
            this.setSoundVariant(Wolf.castComponentValue(DataComponents.WOLF_SOUND_VARIANT, value));
            return true;
        }
        if (type == DataComponents.WOLF_COLLAR) {
            this.setCollarColor(Wolf.castComponentValue(DataComponents.WOLF_COLLAR, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.3f).add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        HolderLookup.RegistryLookup wolfSoundVariants = this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT);
        entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), WolfVariants.DEFAULT));
        entityData.define(DATA_SOUND_VARIANT_ID, (Holder)wolfSoundVariants.get(WolfSoundVariants.CLASSIC).or(((Registry)wolfSoundVariants)::getAny).orElseThrow());
        entityData.define(DATA_INTERESTED_ID, false);
        entityData.define(DATA_COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getId());
        entityData.define(DATA_ANGER_END_TIME, -1L);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(this.getSoundSet().stepSound().value(), 0.15f, 1.0f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("CollarColor", DyeColor.LEGACY_ID_CODEC, this.getCollarColor());
        VariantUtils.writeVariant(output, this.getVariant());
        this.addPersistentAngerSaveData(output);
        this.getSoundVariant().unwrapKey().ifPresent(soundVariant -> output.store("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT), soundVariant));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        VariantUtils.readVariant(input, Registries.WOLF_VARIANT).ifPresent(this::setVariant);
        this.setCollarColor(input.read("CollarColor", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLLAR_COLOR));
        this.readPersistentAngerSaveData(this.level(), input);
        input.read("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT)).flatMap(soundVariant -> this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT).get((ResourceKey)soundVariant)).ifPresent(this::setSoundVariant);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (groupData instanceof WolfPackData) {
            WolfPackData wolfGroupData = (WolfPackData)groupData;
            this.setVariant(wolfGroupData.type);
        } else {
            Optional selectedVariant = VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.WOLF_VARIANT);
            if (selectedVariant.isPresent()) {
                this.setVariant(selectedVariant.get());
                groupData = new WolfPackData(selectedVariant.get());
            }
        }
        this.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()));
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isAngry()) {
            return this.getSoundSet().growlSound().value();
        }
        if (this.random.nextInt(3) == 0) {
            if (this.isTame() && this.getHealth() < 20.0f) {
                return this.getSoundSet().whineSound().value();
            }
            return this.getSoundSet().pantSound().value();
        }
        return this.getSoundSet().ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.canArmorAbsorb(source)) {
            return SoundEvents.WOLF_ARMOR_DAMAGE;
        }
        return this.getSoundSet().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getSoundSet().deathSound().value();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.isWet && !this.isShaking && !this.isPathFinding() && this.onGround()) {
            this.isShaking = true;
            this.shakeAnim = 0.0f;
            this.shakeAnimO = 0.0f;
            this.level().broadcastEntityEvent(this, (byte)8);
        }
        if (!this.level().isClientSide()) {
            this.updatePersistentAnger((ServerLevel)this.level(), true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }
        this.interestedAngleO = this.interestedAngle;
        this.interestedAngle = this.isInterested() ? (this.interestedAngle += (1.0f - this.interestedAngle) * 0.4f) : (this.interestedAngle += (0.0f - this.interestedAngle) * 0.4f);
        if (this.isInWaterOrRain()) {
            this.isWet = true;
            if (this.isShaking && !this.level().isClientSide()) {
                this.level().broadcastEntityEvent(this, (byte)56);
                this.cancelShake();
            }
        } else if ((this.isWet || this.isShaking) && this.isShaking) {
            if (this.shakeAnim == 0.0f) {
                this.playSound(SoundEvents.WOLF_SHAKE, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
                this.gameEvent(GameEvent.ENTITY_ACTION);
            }
            this.shakeAnimO = this.shakeAnim;
            this.shakeAnim += 0.05f;
            if (this.shakeAnimO >= 2.0f) {
                this.isWet = false;
                this.isShaking = false;
                this.shakeAnimO = 0.0f;
                this.shakeAnim = 0.0f;
            }
            if (this.shakeAnim > 0.4f) {
                float yt = (float)this.getY();
                int shakeCount = (int)(Mth.sin((this.shakeAnim - 0.4f) * (float)Math.PI) * 7.0f);
                Vec3 movement = this.getDeltaMovement();
                for (int i = 0; i < shakeCount; ++i) {
                    float xo = (this.random.nextFloat() * 2.0f - 1.0f) * this.getBbWidth() * 0.5f;
                    float zo = (this.random.nextFloat() * 2.0f - 1.0f) * this.getBbWidth() * 0.5f;
                    this.level().addParticle(ParticleTypes.SPLASH, this.getX() + (double)xo, yt + 0.8f, this.getZ() + (double)zo, movement.x, movement.y, movement.z);
                }
            }
        }
    }

    private void cancelShake() {
        this.isShaking = false;
        this.shakeAnim = 0.0f;
        this.shakeAnimO = 0.0f;
    }

    @Override
    public void die(DamageSource source) {
        this.isWet = false;
        this.isShaking = false;
        this.shakeAnimO = 0.0f;
        this.shakeAnim = 0.0f;
        super.die(source);
    }

    public float getWetShade(float a) {
        if (!this.isWet) {
            return 1.0f;
        }
        return Math.min(0.75f + Mth.lerp(a, this.shakeAnimO, this.shakeAnim) / 2.0f * 0.25f, 1.0f);
    }

    public float getShakeAnim(float a) {
        return Mth.lerp(a, this.shakeAnimO, this.shakeAnim);
    }

    public float getHeadRollAngle(float a) {
        return Mth.lerp(a, this.interestedAngleO, this.interestedAngle) * 0.15f * (float)Math.PI;
    }

    @Override
    public int getMaxHeadXRot() {
        if (this.isInSittingPose()) {
            return 20;
        }
        return super.getMaxHeadXRot();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        this.setOrderedToSit(false);
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
        if (!this.canArmorAbsorb(source)) {
            super.actuallyHurt(level, source, damage);
            return;
        }
        ItemStack armorBefore = this.getBodyArmorItem();
        int damageBefore = armorBefore.getDamageValue();
        int maxDamage = armorBefore.getMaxDamage();
        armorBefore.hurtAndBreak(Mth.ceil(damage), (LivingEntity)this, EquipmentSlot.BODY);
        if (Crackiness.WOLF_ARMOR.byDamage(damageBefore, maxDamage) != Crackiness.WOLF_ARMOR.byDamage(this.getBodyArmorItem())) {
            this.playSound(SoundEvents.WOLF_ARMOR_CRACK);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.ARMADILLO_SCUTE), this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.2, 0.1, 0.2, 0.1);
        }
    }

    private boolean canArmorAbsorb(DamageSource source) {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR) && !source.is(DamageTypeTags.BYPASSES_WOLF_ARMOR);
    }

    @Override
    protected void applyTamingSideEffects() {
        if (this.isTame()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
            this.setHealth(40.0f);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
        }
    }

    @Override
    protected void hurtArmor(DamageSource damageSource, float damage) {
        this.doHurtEquipment(damageSource, damage, EquipmentSlot.BODY);
    }

    @Override
    protected boolean canShearEquipment(Player player) {
        return this.isOwnedBy(player);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.isTame()) {
            if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                this.feed(player, hand, itemStack, 2.0f, 2.0f);
                return InteractionResult.SUCCESS;
            }
            if (itemStack.is(ItemTags.WOLF_COLLAR_DYES) && this.isOwnedBy(player)) {
                DyeColor color = itemStack.get(DataComponents.DYE);
                if (color == null || color == this.getCollarColor()) return super.mobInteract(player, hand);
                this.setCollarColor(color);
                itemStack.consume(1, player);
                return InteractionResult.SUCCESS;
            }
            if (this.isEquippableInSlot(itemStack, EquipmentSlot.BODY) && !this.isWearingBodyArmor() && this.isOwnedBy(player) && !this.isBaby()) {
                this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, itemStack.copyWithCount(1));
                itemStack.consume(1, player);
                return InteractionResult.SUCCESS;
            }
            if (this.isInSittingPose() && this.isWearingBodyArmor() && this.isOwnedBy(player) && this.getBodyArmorItem().isDamaged() && this.getBodyArmorItem().isValidRepairItem(itemStack)) {
                itemStack.shrink(1);
                this.playSound(SoundEvents.WOLF_ARMOR_REPAIR);
                ItemStack armor = this.getBodyArmorItem();
                int repairUnit = (int)((float)armor.getMaxDamage() * 0.125f);
                armor.setDamageValue(Math.max(0, armor.getDamageValue() - repairUnit));
                return InteractionResult.SUCCESS;
            }
            InteractionResult interactionResult = super.mobInteract(player, hand);
            if (interactionResult.consumesAction() || !this.isOwnedBy(player)) return interactionResult;
            this.setOrderedToSit(!this.isOrderedToSit());
            this.jumping = false;
            this.navigation.stop();
            this.setTarget(null);
            return InteractionResult.SUCCESS.withoutItem();
        }
        if (this.level().isClientSide() || !itemStack.is(Items.BONE) || this.isAngry()) return super.mobInteract(player, hand);
        itemStack.consume(1, player);
        this.tryToTame(player);
        return InteractionResult.SUCCESS_SERVER;
    }

    private void tryToTame(Player player) {
        if (this.random.nextInt(3) == 0) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 8) {
            this.isShaking = true;
            this.shakeAnim = 0.0f;
            this.shakeAnimO = 0.0f;
        } else if (id == 56) {
            this.cancelShake();
        } else {
            super.handleEntityEvent(id);
        }
    }

    public float getTailAngle() {
        if (this.isAngry()) {
            return 1.5393804f;
        }
        if (this.isTame()) {
            float maxHealth = this.getMaxHealth();
            float damageRatio = (maxHealth - this.getHealth()) / maxHealth;
            return (0.55f - damageRatio * 0.4f) * (float)Math.PI;
        }
        return 0.62831855f;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.WOLF_FOOD);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 8;
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.entityData.get(DATA_ANGER_END_TIME);
    }

    @Override
    public void setPersistentAngerEndTime(long endTime) {
        this.entityData.set(DATA_ANGER_END_TIME, endTime);
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> persistentAngerTarget) {
        this.persistentAngerTarget = persistentAngerTarget;
    }

    public DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    private void setCollarColor(DyeColor color) {
        this.entityData.set(DATA_COLLAR_COLOR, color.getId());
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public @Nullable Wolf getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Wolf baby = EntityTypes.WOLF.create((Level)level, EntitySpawnReason.BREEDING);
        if (baby != null && partner instanceof Wolf) {
            Wolf partnerWolf = (Wolf)partner;
            if (this.random.nextBoolean()) {
                baby.setVariant(this.getVariant());
            } else {
                baby.setVariant(partnerWolf.getVariant());
            }
            if (this.isTame()) {
                baby.setOwnerReference(this.getOwnerReference());
                baby.setTame(true, true);
                DyeColor parent1CollarColor = this.getCollarColor();
                DyeColor parent2CollarColor = partnerWolf.getCollarColor();
                baby.setCollarColor(DyeColor.getMixedColor(level, parent1CollarColor, parent2CollarColor));
            }
            baby.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), this.random));
        }
        return baby;
    }

    public void setIsInterested(boolean value) {
        this.entityData.set(DATA_INTERESTED_ID, value);
    }

    @Override
    public boolean canMate(Animal partner) {
        if (partner == this) {
            return false;
        }
        if (!this.isTame()) {
            return false;
        }
        if (!(partner instanceof Wolf)) {
            return false;
        }
        Wolf wolf = (Wolf)partner;
        if (!wolf.isTame()) {
            return false;
        }
        if (wolf.isInSittingPose()) {
            return false;
        }
        return this.isInLove() && wolf.isInLove();
    }

    public boolean isInterested() {
        return this.entityData.get(DATA_INTERESTED_ID);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        TamableAnimal animal;
        AbstractHorse horse;
        if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Wolf) {
            Wolf wolfTarget = (Wolf)target;
            return !wolfTarget.isTame() || wolfTarget.getOwner() != owner;
        }
        if (target instanceof Player) {
            Player playerOwner;
            Player playerTarget = (Player)target;
            if (owner instanceof Player && !(playerOwner = (Player)owner).canHarmPlayer(playerTarget)) {
                return false;
            }
        }
        if (target instanceof AbstractHorse && (horse = (AbstractHorse)target).isTamed()) {
            return false;
        }
        return !(target instanceof TamableAnimal) || !(animal = (TamableAnimal)target).isTame();
    }

    @Override
    public boolean canBeLeashed() {
        return !this.isAngry();
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }

    public static boolean checkWolfSpawnRules(EntityType<Wolf> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.WOLVES_SPAWNABLE_ON) && Wolf.isBrightEnoughToSpawn(level, pos);
    }

    private class WolfAvoidEntityGoal<T extends LivingEntity>
    extends AvoidEntityGoal<T> {
        private final Wolf wolf;
        final /* synthetic */ Wolf this$0;

        public WolfAvoidEntityGoal(Wolf wolf, Wolf wolf2, Class<T> avoidClass, float maxDist, double walkSpeedModifier, double sprintSpeedModifier) {
            Wolf wolf3 = wolf;
            Objects.requireNonNull(wolf3);
            this.this$0 = wolf3;
            super(wolf2, avoidClass, maxDist, walkSpeedModifier, sprintSpeedModifier);
            this.wolf = wolf2;
        }

        @Override
        public boolean canUse() {
            if (super.canUse() && this.toAvoid instanceof Llama) {
                return !this.wolf.isTame() && this.avoidLlama((Llama)this.toAvoid);
            }
            return false;
        }

        private boolean avoidLlama(Llama llama) {
            return llama.getStrength() >= this.this$0.random.nextInt(5);
        }

        @Override
        public void start() {
            this.this$0.setTarget(null);
            super.start();
        }

        @Override
        public void tick() {
            this.this$0.setTarget(null);
            super.tick();
        }
    }

    public static class WolfPackData
    extends AgeableMob.AgeableMobGroupData {
        public final Holder<WolfVariant> type;

        public WolfPackData(Holder<WolfVariant> type) {
            super(false);
            this.type = type;
        }
    }
}

