/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.cubemob;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SulfurCubeArchetype;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.SulfurCubeContent;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SulfurCube
extends AbstractCubeMob
implements Bucketable,
Shearable {
    public static final int SPLIT_COUNT = 2;
    public static final int MAX_SIZE = 2;
    public static final int MIN_SIZE = 1;
    public static final int PICKUP_TIMER_DURATION = 100;
    public static final double PUSH_DISTANCE_THRESHOLD = (double)1.3f;
    private int pickupTimer = 0;
    private int pushSoundCooldown = 0;
    private boolean floatsInLiquids = false;
    private static final double MAX_PLAYER_PUSH_SPEED = 0.5;
    private static final float PLAYER_PUSH_SPEED_SCALE_MULTIPLIER = 0.3f;
    private static final float VEHICLE_PUSH_SPEED_SCALE_MULTIPLIER = 0.16f;
    private static final float VERTICAL_PUSH_MULTIPLIER = 0.3f;
    private Optional<SulfurCubeArchetype.ExplosionData> explosionData = Optional.empty();
    private SulfurCubeArchetype.KnockbackModifiers knockbackModifier = SulfurCubeArchetype.DEFAULT_KNOCKBACK_MODIFIERS;
    private SulfurCubeArchetype.SoundSettings soundSettings = SulfurCubeArchetype.DEFAULT_SOUND_SETTINGS;
    private int fuse = -1;
    private List<SulfurCubeArchetype.ContactDamage> contactDamages = new ArrayList<SulfurCubeArchetype.ContactDamage>();
    private static final EntityDataAccessor<Integer> MAX_FUSE = SynchedEntityData.defineId(SulfurCube.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(SulfurCube.class, EntityDataSerializers.BOOLEAN);
    private static final boolean DEFAULT_FROM_BUCKET = false;
    private static final float HORIZONTAL_HIT_ANGLE_SCALE = 1.6f;
    private static final float VERTICAL_HIT_ANGLE_SCALE = 0.5f;
    private static final float VERTICAL_POSITION_ANGLE_SCALE = 0.8f;
    private static final float EXTRA_KNOCKBACK_DAMPENING = 0.25f;
    private static final Predicate<ItemEntity> ALLOWED_ITEMS = e -> !e.hasPickUpDelay() && e.isAlive() && SulfurCube.isSwallowableItem(e.getItem());

    public SulfurCube(EntityType<? extends SulfurCube> type, Level level) {
        super((EntityType<? extends AbstractCubeMob>)type, level);
        this.lookControl = new SulfurCubeLookControl(this);
        this.moveControl = new SulfurCubeMobMoveControl<SulfurCube>(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(FROM_BUCKET, false);
        entityData.define(MAX_FUSE, -1);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new SulfurCubeTemptGoal((Mob)this, 1.0, itemStack -> this.isBaby() ? itemStack.is(ItemTags.SULFUR_CUBE_FOOD) : SulfurCube.isSwallowableItem(itemStack), false, 1.0));
        this.goalSelector.addGoal(3, new SulfurCubeSearchForItemsGoal(this, this));
    }

    @Override
    public boolean fromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.getFuse() >= 0;
    }

    private void setFuse(int fuse) {
        this.fuse = fuse;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (MAX_FUSE.equals(accessor)) {
            this.setFuse(this.entityData.get(MAX_FUSE));
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_SULFUR_CUBE;
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);
        bucket.copyFrom(DataComponents.SULFUR_CUBE_CONTENT, this);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            tag.putInt("age", this.getAge());
            tag.putBoolean("age_locked", this.isAgeLocked());
        });
    }

    @Override
    public boolean canBreatheUnderwater() {
        return this.hasBodyItem() || super.canBreatheUnderwater();
    }

    @Override
    protected void travelInFluid(Vec3 input) {
        super.travelInFluid(input);
        if (!this.hasBodyItem() || !this.floatsInLiquids) {
            return;
        }
        float vibeAmount = 0.2f * Mth.sin((float)this.tickCount * 0.4f);
        double immersion = this.getFluidHeight(this.isInWater() ? FluidTags.WATER : FluidTags.LAVA) - this.getFluidJumpThreshold() + (double)vibeAmount;
        if (immersion > 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, Math.min(1.0, immersion) * (double)0.04f, 0.0));
        }
    }

    @Override
    public double getFluidJumpThreshold() {
        return (double)this.getBbHeight() * 0.2;
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        this.setAge(tag.getIntOr("age", 0));
        this.setAgeLocked(tag.getBooleanOr("age_locked", false));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.SULFUR_CUBE_BUCKET);
    }

    @Override
    protected void addTargetingGoals() {
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f;
    }

    @Override
    protected boolean isDealsDamage() {
        return false;
    }

    public static boolean checkSulfurCubeSpawnRules(EntityType<SulfurCube> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return true;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.hasBodyItem() || this.fromBucket();
    }

    @Override
    public boolean canBeLeashed() {
        return this.hasBodyItem();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.hasBodyItem()) {
            if (this.canExplode() && !this.isPrimed()) {
                AbstractArrow projectile;
                Entity sourceEntity = source.getDirectEntity();
                if (source.is(DamageTypeTags.IS_FIRE) || sourceEntity instanceof AbstractArrow && (projectile = (AbstractArrow)sourceEntity).isOnFire()) {
                    this.primeTime(false);
                } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                    this.primeTime(true);
                }
            }
            if (source.is(DamageTypeTags.SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO)) {
                if (!source.is(DamageTypeTags.NO_KNOCKBACK)) {
                    this.dealDefaultKnockback(source, damage, true);
                }
                return true;
            }
        }
        return super.hurtServer(level, source, damage);
    }

    public boolean hasBodyItem() {
        return !this.getItemBySlot(EquipmentSlot.BODY).isEmpty();
    }

    public boolean canExplode() {
        return this.explosionData.isPresent() && this.isAlive() && !this.isPrimed();
    }

    @VisibleForTesting
    public List<SulfurCubeArchetype> matchingArchetypes(ItemStack stack) {
        return this.level().registryAccess().lookupOrThrow(Registries.SULFUR_CUBE_ARCHETYPE).stream().filter(arch -> stack.is(arch.items())).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void tick() {
        this.tickFuse();
        this.primeWhenOnPoweredPosition();
        super.tick();
    }

    private void tickFuse() {
        if (this.fuse > 0) {
            --this.fuse;
        }
        if (this.explosionData.isEmpty()) {
            return;
        }
        if (this.fuse == 0) {
            this.dropLeash();
            this.dead = true;
            Level level = this.level();
            if (level instanceof ServerLevel) {
                ServerLevel level2 = (ServerLevel)level;
                if (level2.getGameRules().get(GameRules.TNT_EXPLODES).booleanValue()) {
                    Level.ExplosionInteraction explosionInteraction = level2.getGameRules().get(GameRules.MOB_GRIEFING) != false ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                    level2.explode(this, Explosion.getDefaultDamageSource(this.level(), this), this.getPortalCooldown() > 0 ? PrimedTnt.USED_PORTAL_DAMAGE_CALCULATOR : null, this.getX(), this.getY(0.0625), this.getZ(), this.explosionData.get().power(), this.explosionData.get().causesFire(), explosionInteraction);
                }
                this.triggerOnDeathMobEffects(level2, Entity.RemovalReason.KILLED);
            }
            this.discard();
        }
    }

    private void primeWhenOnPoweredPosition() {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            BlockPos here;
            ServerLevel level2 = (ServerLevel)level;
            if (this.canExplode() && level2.getBestOwnOrNeighbourSignal(here = BlockPos.containing(this.position())) != 0) {
                this.primeTime(false);
            }
        }
    }

    public boolean primeTime(boolean imminent) {
        ServerLevel serverLevel;
        Level level;
        if (this.explosionData.isEmpty() || !this.isAlive() || !((level = this.level()) instanceof ServerLevel) || !(serverLevel = (ServerLevel)level).getGameRules().get(GameRules.TNT_EXPLODES).booleanValue() || this.isPrimed()) {
            return false;
        }
        int fuse = this.explosionData.get().fuse();
        int fuseTime = imminent ? PrimedTnt.getRandomShortFuse(fuse, this.getRandom()) : fuse;
        this.setInvulnerable(true);
        this.setFuse(fuseTime);
        this.entityData.set(MAX_FUSE, fuseTime);
        this.makeSound(SoundEvents.TNT_PRIMED);
        this.gameEvent(GameEvent.PRIME_FUSE);
        return true;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.pickupTimer > 0) {
            --this.pickupTimer;
        }
        if (this.pushSoundCooldown > 0) {
            --this.pushSoundCooldown;
        }
    }

    @Override
    protected @Nullable Map<EquipmentSlot, ItemStack> collectEquipmentChanges(Map<EquipmentSlot, ItemStack> lastEquipmentItems) {
        ItemStack current;
        ItemStack previous = lastEquipmentItems.get(EquipmentSlot.BODY);
        if (this.equipmentHasChanged(previous, current = this.getItemBySlot(EquipmentSlot.BODY))) {
            AttributeInstance attr;
            if (!current.isEmpty()) {
                this.removeAllGoals(g -> true);
                this.setSpeed(0.0f);
            } else {
                this.registerGoals();
            }
            for (SulfurCubeArchetype archetype : this.matchingArchetypes(previous)) {
                for (SulfurCubeArchetype.AttributeEntry mod : archetype.attributeModifiers()) {
                    attr = this.getAttribute(mod.attribute());
                    if (attr == null) continue;
                    attr.removeModifier(mod.modifier());
                }
            }
            this.floatsInLiquids = false;
            this.explosionData = Optional.empty();
            this.contactDamages.clear();
            this.knockbackModifier = SulfurCubeArchetype.DEFAULT_KNOCKBACK_MODIFIERS;
            this.soundSettings = SulfurCubeArchetype.DEFAULT_SOUND_SETTINGS;
            for (SulfurCubeArchetype archetype : this.matchingArchetypes(current)) {
                if (archetype.buoyant()) {
                    this.floatsInLiquids = true;
                }
                if (archetype.explosion().isPresent()) {
                    this.explosionData = archetype.explosion();
                }
                if (archetype.contactDamage().isPresent()) {
                    this.contactDamages.add(archetype.contactDamage().get());
                }
                this.knockbackModifier = archetype.knockbackModifiers();
                this.soundSettings = archetype.soundSettings();
                for (SulfurCubeArchetype.AttributeEntry mod : archetype.attributeModifiers()) {
                    attr = this.getAttribute(mod.attribute());
                    if (attr == null) continue;
                    attr.addOrUpdateTransientModifier(mod.modifier());
                }
            }
        }
        return super.collectEquipmentChanges(lastEquipmentItems);
    }

    @Override
    public float maxUpStep() {
        if (this.hasBodyItem()) {
            return 0.0f;
        }
        return super.maxUpStep();
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return this.hasBodyItem();
    }

    @Override
    public boolean canFreeze() {
        if (this.hasBodyItem()) {
            return false;
        }
        return super.canFreeze();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (this.isBaby()) {
            if (this.isFood(heldItem) && this.canAgeUp()) {
                int age = this.getAge();
                this.usePlayerItem(player, hand, heldItem);
                this.ageUp(SulfurCube.getSpeedUpSecondsWhenFeeding(-age), true);
                this.playEatingSound();
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }
        if (this.isPrimed()) {
            return InteractionResult.PASS;
        }
        if (this.canExplode() && (heldItem.is(Items.FLINT_AND_STEEL) || heldItem.is(Items.FIRE_CHARGE))) {
            ServerLevel serverLevel;
            Level level = this.level();
            if (level instanceof ServerLevel && !(serverLevel = (ServerLevel)level).getGameRules().get(GameRules.TNT_EXPLODES).booleanValue()) {
                player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
                return InteractionResult.PASS;
            }
            this.primeTime(false);
            if (heldItem.is(Items.FLINT_AND_STEEL)) {
                heldItem.hurtAndBreak(1, (LivingEntity)player, hand.asEquipmentSlot());
            } else {
                heldItem.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(heldItem.getItem()));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (heldItem.is(Items.SHEARS) && this.readyForShearing()) {
            Level level = this.level();
            if (level instanceof ServerLevel) {
                ServerLevel level2 = (ServerLevel)level;
                ItemStack itemStackToShear = this.getItemBySlot(EquipmentSlot.BODY);
                this.shear(level2, SoundSource.PLAYERS, heldItem);
                this.gameEvent(GameEvent.SHEAR, player);
                heldItem.hurtAndBreak(1, (LivingEntity)player, hand.asEquipmentSlot());
                CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.trigger((ServerPlayer)player, itemStackToShear, this);
            }
            return InteractionResult.SUCCESS;
        }
        if (SulfurCube.isSwallowableItem(heldItem)) {
            boolean itWorked = this.equipItem(heldItem);
            if (itWorked) {
                heldItem.consume(1, player);
                this.gameEvent(GameEvent.ENTITY_INTERACT);
            }
            return itWorked ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
        }
        return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    public boolean equipItem(ItemStack heldItem) {
        Object swallowedItem;
        if (this.isBaby()) {
            return false;
        }
        if (this.hasBodyItem()) {
            swallowedItem = this.getItemBySlot(EquipmentSlot.BODY).getItem();
            if (heldItem.is(swallowedItem)) {
                return false;
            }
            Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER);
            Level level = this.level();
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                this.spawnAtLocation(serverLevel, this.getItemBySlot(EquipmentSlot.BODY), equipmentSpawnOffset);
            }
        }
        if (!this.level().isClientSide()) {
            swallowedItem = this.getItemBySlot(EquipmentSlot.BODY);
            this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, heldItem.copyWithCount(1));
            if (!((ItemStack)swallowedItem).isEmpty()) {
                Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, slot -> ItemStack.EMPTY);
                lastEquipmentItems.put(EquipmentSlot.BODY, (ItemStack)swallowedItem);
                this.collectEquipmentChanges(lastEquipmentItems);
            }
        }
        this.playSound(this.getAbsorbSound());
        return true;
    }

    private void applyContactDamage(Entity entity) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            for (SulfurCubeArchetype.ContactDamage damage : this.contactDamages) {
                entity.hurtServer(serverLevel, new DamageSource(damage.damageType(), damage.attributeToSource() ? this : null), damage.amount().sample(this.getRandom()));
            }
        }
    }

    protected void playEatingSound() {
        this.makeSound(SoundEvents.SULFUR_CUBE_SMALL_EAT);
    }

    @Override
    public boolean canBePickedUpWithBucket(ItemStack itemStack) {
        return itemStack.getItem() == Items.BUCKET;
    }

    @Override
    public EquipmentSlot getEquipmentSlotForItem(ItemStack itemStack) {
        if (SulfurCube.isSwallowableItem(itemStack)) {
            return EquipmentSlot.BODY;
        }
        return super.getEquipmentSlotForItem(itemStack);
    }

    @Override
    public boolean isEquippableInSlot(ItemStack itemStack, EquipmentSlot slot) {
        if (slot == EquipmentSlot.BODY) {
            return SulfurCube.isSwallowableItem(itemStack);
        }
        return super.isEquippableInSlot(itemStack, slot);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.isTiny()) {
            return SoundEvents.SULFUR_CUBE_SMALL_HURT;
        }
        return SoundEvents.SULFUR_CUBE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        if (this.isTiny()) {
            return SoundEvents.SULFUR_CUBE_SMALL_DEATH;
        }
        return SoundEvents.SULFUR_CUBE_DEATH;
    }

    @Override
    protected SoundEvent getSquishSound() {
        if (this.isTiny()) {
            return SoundEvents.SULFUR_CUBE_SMALL_SQUISH;
        }
        if (this.hasBodyItem()) {
            return SoundEvents.SULFUR_CUBE_BOUNCE;
        }
        return SoundEvents.SULFUR_CUBE_SQUISH;
    }

    @Override
    protected SoundEvent getJumpSound() {
        return this.isTiny() ? SoundEvents.SULFUR_CUBE_SMALL_JUMP : SoundEvents.SULFUR_CUBE_JUMP;
    }

    private SoundEvent getAbsorbSound() {
        return SoundEvents.SULFUR_CUBE_ABSORB;
    }

    private SoundEvent getEjectSound() {
        return SoundEvents.SULFUR_CUBE_EJECT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        if (!this.hasBodyItem()) {
            super.playStepSound(pos, blockState);
        }
    }

    @Override
    protected @Nullable ParticleOptions getParticleType() {
        return ParticleTypes.SULFUR_CUBE_GOO;
    }

    public static AttributeSupplier.Builder createSulfurCubeAttributes() {
        return Mob.createMobAttributes().add(Attributes.TEMPT_RANGE, 8.0);
    }

    @Override
    public void shear(ServerLevel level, SoundSource soundSource, ItemStack tool) {
        Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER);
        ItemStack itemStackToShear = this.getItemBySlot(EquipmentSlot.BODY);
        this.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        this.spawnAtLocation(level, itemStackToShear, equipmentSpawnOffset);
        this.playSound(this.getEjectSound());
        this.pickupTimer = 100;
    }

    @Override
    public boolean readyForShearing() {
        return this.hasBodyItem();
    }

    @Override
    public boolean canPickUpLoot() {
        return !this.hasBodyItem();
    }

    private static boolean isSwallowableItem(ItemStack itemStack) {
        return itemStack.is(ItemTags.SULFUR_CUBE_SWALLOWABLE);
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
    public boolean canHoldItem(ItemStack itemStack) {
        ItemStack heldItemStack = this.getItemBySlot(EquipmentSlot.BODY);
        return heldItemStack.isEmpty() && SulfurCube.isSwallowableItem(itemStack) && !this.isBaby();
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack itemStack = entity.getItem();
        if (this.canHoldItem(itemStack) && this.pickupTimer <= 0) {
            this.onItemPickup(entity);
            this.setItemSlot(EquipmentSlot.BODY, itemStack.split(1));
            this.playSound(this.getAbsorbSound());
            this.setGuaranteedDrop(EquipmentSlot.BODY);
            this.take(entity, 1);
        }
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return this.isBaby() ? 0 : 1 + this.random.nextInt(2);
    }

    @Override
    protected int getSplitCount() {
        if (this.isPrimed()) {
            return 0;
        }
        return 2;
    }

    @Override
    protected void setSpawnSize(ServerLevelAccessor level, DifficultyInstance difficulty) {
        if (this.isBaby()) {
            this.setSize(1, true);
        } else {
            this.setSize(2, true);
        }
    }

    @Override
    public void setSize(int size, boolean updateHealth) {
        super.setSize(size, updateHealth);
        if (updateHealth && size == 1 && !this.isBaby()) {
            this.setBaby(true);
        }
    }

    @Override
    protected void setUpSplitCube(AbstractCubeMob cubeMob, int halfSize, float xd, float zd) {
        super.setUpSplitCube(cubeMob, halfSize, xd, zd);
        cubeMob.setBaby(true);
    }

    @Override
    public @Nullable AbstractCubeMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        SulfurCube sulfurCube = EntityTypes.SULFUR_CUBE.create((Level)level, EntitySpawnReason.BREEDING);
        if (sulfurCube != null) {
            sulfurCube.setSize(1, true);
        }
        return sulfurCube;
    }

    private boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.SULFUR_CUBE_FOOD);
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby()) {
            this.setSize(2, true);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("pickup_timer", this.pickupTimer);
        output.putBoolean("from_bucket", this.fromBucket());
        output.putInt("fuse", this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.pickupTimer = input.getIntOr("pickup_timer", 0);
        this.setFromBucket(input.getBooleanOr("from_bucket", false));
        this.setFuse(input.getIntOr("fuse", -1));
        this.entityData.set(MAX_FUSE, this.getFuse());
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void doPush(Entity entity) {
        super.doPush(entity);
        this.applyContactDamage(entity);
    }

    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        this.playerPush(player);
    }

    private void playerPush(Player player) {
        if (!this.hasBodyItem()) {
            return;
        }
        Entity pusher = player.isPassenger() ? player.getRootVehicle() : player;
        Vec3 cubeToPusher = this.position().subtract(pusher.position());
        double pusherFeetPosition = pusher.getY();
        double sulfurCubeBottomPosition = this.getY();
        double sulfurCubeTopPosition = sulfurCubeBottomPosition + (double)this.getBbHeight();
        double pusherTopPosition = pusherFeetPosition + (double)pusher.getBbHeight();
        if (cubeToPusher.horizontalDistance() < (double)1.3f && pusherFeetPosition <= sulfurCubeTopPosition && pusherTopPosition > sulfurCubeBottomPosition) {
            double knockback = Math.max(0.0, 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 pushDirection = cubeToPusher.horizontal().normalize().scale(knockback);
            float pushSpeedScale = player.isPassenger() ? 0.16f : 0.3f;
            double playerSpeed = player.getKnownSpeed().length() * 2.0 * (double)pushSpeedScale;
            playerSpeed = Mth.clamp(playerSpeed, 0.0, 0.5);
            Vec3 pushVelocity = new Vec3(pushDirection.x, this.onGround() ? knockback * (double)0.3f : 0.0, pushDirection.z).scale(playerSpeed);
            this.needsSync = true;
            float push_sound_threshold = this.soundSettings.pushSoundImpulseThreshold();
            if (pushVelocity.lengthSqr() > (double)(push_sound_threshold * push_sound_threshold) && this.pushSoundCooldown <= 0) {
                this.pushSoundCooldown = (int)(this.soundSettings.pushSoundCooldown() * 20.0f);
                this.playSound(this.soundSettings.pushSound().value());
            }
            this.addDeltaMovement(pushVelocity);
            this.applyContactDamage(player);
        }
    }

    private Vec2 applyHorizontalHitAngleScale(float horizontalAngleScale, Vec2 originalAngle, Vec3 attackerPosition, Vec3 attackerAimDirection, Vec3 targetCenter) {
        Vec3 attackerToTarget = targetCenter.subtract(attackerPosition).normalize();
        float angleDiff = (float)Math.atan2(attackerAimDirection.x * attackerToTarget.z - attackerAimDirection.z * attackerToTarget.x, attackerAimDirection.x * attackerToTarget.x + attackerAimDirection.z * attackerToTarget.z);
        return originalAngle.rotate(angleDiff * horizontalAngleScale);
    }

    private Vec2 applyVerticalHitAnglePowerTransfer(float verticalHitAngleScale, float horizontalPower, float verticalPower, Vec3 attackerPosition, Vec3 attackerAimDirection, Vec3 targetCenteredPosition, float targetHeight) {
        float targetHalfHeight = 0.5f * targetHeight;
        Vec3 targetTopPos = targetCenteredPosition.add(0.0, targetHalfHeight, 0.0);
        Vec3 tagetBottomPos = targetCenteredPosition.add(0.0, -targetHalfHeight, 0.0);
        Vec3 attackerToTargetTop = targetTopPos.subtract(attackerPosition).normalize();
        Vec3 attackerToTargetBottom = tagetBottomPos.subtract(attackerPosition).normalize();
        float verticalHitAngleFactor = (float)Mth.clampedMap(attackerAimDirection.y, attackerToTargetTop.y, attackerToTargetBottom.y, -1.0, 1.0);
        float transferredPowerRatio = Math.abs(verticalHitAngleFactor * verticalHitAngleScale);
        if (verticalHitAngleFactor < 0.0f) {
            transferredPowerRatio = -transferredPowerRatio;
        }
        float px = horizontalPower * (1.0f - transferredPowerRatio);
        float py = verticalPower * (1.0f + transferredPowerRatio);
        return new Vec2(px, py);
    }

    private Vec2 applyVerticalPositionAnglePowerRotation(float verticalPositionAngleScale, float horizontalPower, float verticalPower, float originalHorizontalPower, float originalVerticalPower, Vec3 attackerFeetPosition, Vec3 targetFeetPosition) {
        float verticalRatio;
        Vec3 attackerFeetToTargetFeet = targetFeetPosition.subtract(attackerFeetPosition);
        float verticalPositionAngle = (float)Math.atan2(-attackerFeetToTargetFeet.y, attackerFeetToTargetFeet.horizontalDistance());
        Vec2 powerBeforeRotation = new Vec2(horizontalPower, verticalPower);
        Vec2 rotatedPower = powerBeforeRotation.rotate(-verticalPositionAngle * verticalPositionAngleScale);
        float horizontalRatio = originalHorizontalPower > 0.0f ? Mth.abs(rotatedPower.x) / originalHorizontalPower : 0.0f;
        float maxRatio = Math.max(horizontalRatio, verticalRatio = originalVerticalPower > 0.0f ? Mth.abs(rotatedPower.y) / originalVerticalPower : 0.0f);
        if (maxRatio > 1.0f) {
            rotatedPower = rotatedPower.scale(1.0f / maxRatio);
        }
        return rotatedPower;
    }

    @Override
    public void knockback(double power, double xd, double zd, DamageSource source, float damage, boolean comesFromEffect) {
        if (source.getEntity() == null || !this.hasBodyItem()) {
            super.knockback(power, xd, zd, source, damage, comesFromEffect);
            return;
        }
        float horizontalHitAngleScale = 1.6f;
        float verticalHitAngleScale = 0.5f;
        float verticalPositionAngleScale = 0.8f;
        float horizontalPower = this.knockbackModifier.horizontalPower();
        float verticalPower = this.knockbackModifier.verticalPower();
        float originalHorizontalPower = horizontalPower;
        float originalVerticalPower = verticalPower;
        Holder<SoundEvent> hitSound = this.soundSettings.hitSound();
        Vec2 originalAngle = new Vec2((float)xd, (float)zd);
        Vec2 newAngle = this.applyHorizontalHitAngleScale(1.6f, originalAngle, source.getEntity().getEyePosition(), source.getEntity().getLookAngle().normalize(), this.getBoundingBox().getCenter());
        Vec2 newPower = this.applyVerticalHitAnglePowerTransfer(0.5f, horizontalPower, verticalPower, source.getEntity().getEyePosition(), source.getEntity().getLookAngle().normalize(), this.getBoundingBox().getCenter(), this.getBbHeight());
        horizontalPower = newPower.x;
        verticalPower = newPower.y;
        newPower = this.applyVerticalPositionAnglePowerRotation(0.8f, horizontalPower, verticalPower, originalHorizontalPower, originalVerticalPower, source.getEntity().position(), this.position());
        horizontalPower = newPower.x;
        verticalPower = newPower.y;
        xd = newAngle.x;
        zd = newAngle.y;
        float powerMultiplier = Mth.sqrt(damage) * (comesFromEffect ? (float)power * 0.25f : 1.0f);
        horizontalPower *= powerMultiplier;
        verticalPower *= powerMultiplier;
        double knockBackResistance = this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        horizontalPower *= (float)(1.0 - knockBackResistance);
        verticalPower *= (float)(1.0 - knockBackResistance);
        this.needsSync = true;
        Vec3 deltaMovement = this.getDeltaMovement();
        horizontalPower *= 0.4f;
        horizontalPower = Mth.clamp(horizontalPower, -128.0f, 128.0f);
        verticalPower = Mth.clamp(verticalPower, -128.0f, 128.0f);
        Vec3 horizontalKnockback = new Vec3(xd, 0.0, zd).normalize().scale(horizontalPower);
        this.setDeltaMovement(deltaMovement.x - horizontalKnockback.x, deltaMovement.y + (double)verticalPower * 1.2, deltaMovement.z - horizontalKnockback.z);
        this.playSound(hitSound.value());
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.SULFUR_CUBE_CONTENT) {
            return SulfurCube.castComponentValue(type, SulfurCube.getSulfurCubeContent(this.getBodyArmorItem()));
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.SULFUR_CUBE_CONTENT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.SULFUR_CUBE_CONTENT) {
            this.setSulfurCubeContent(SulfurCube.castComponentValue(DataComponents.SULFUR_CUBE_CONTENT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    private static @Nullable SulfurCubeContent getSulfurCubeContent(ItemStack itemStack) {
        return itemStack.isEmpty() ? null : SulfurCubeContent.ofNonEmpty(itemStack);
    }

    private void setSulfurCubeContent(SulfurCubeContent sulfurCubeContent) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, sulfurCubeContent.absorbedBlockItemStack().create());
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, this.getBbHeight() / 2.0f, 0.0);
    }

    @Override
    protected void setcubeMobHealth(int actualSize) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4 * actualSize);
    }

    @Override
    public boolean isInvulnerableToPiercingWeapon() {
        return this.isInvulnerable() && !this.isPrimed();
    }

    @Override
    public boolean canBePickedFromInside() {
        return !this.hasBodyItem();
    }

    private class SulfurCubeLookControl
    extends LookControl {
        final /* synthetic */ SulfurCube this$0;

        private SulfurCubeLookControl(SulfurCube sulfurCube) {
            SulfurCube sulfurCube2 = sulfurCube;
            Objects.requireNonNull(sulfurCube2);
            this.this$0 = sulfurCube2;
            super(sulfurCube);
        }

        @Override
        public void tick() {
            if (!this.this$0.hasBodyItem()) {
                super.tick();
                return;
            }
            float closeAngle = Mth.wrapDegrees90(this.this$0.getYRot());
            this.this$0.setYRot(this.this$0.getYRot() - closeAngle);
            this.this$0.setYHeadRot(this.this$0.getYRot());
        }
    }

    protected static class SulfurCubeMobMoveControl<T extends SulfurCube>
    extends AbstractCubeMob.CubeMobMoveControl<T> {
        public SulfurCubeMobMoveControl(T cubeMob) {
            super(cubeMob);
        }

        @Override
        public void tick() {
            if (!((SulfurCube)this.mob).hasBodyItem()) {
                super.tick();
            }
        }
    }

    private static class SulfurCubeTemptGoal
    extends TemptGoal.ForNonPathfinders {
        public SulfurCubeTemptGoal(Mob mob, double speedModifier, Predicate<ItemStack> items, boolean canScare, double stopDistance) {
            super(mob, speedModifier, items, canScare, stopDistance);
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        protected void stopNavigation() {
            MoveControl moveControl = this.mob.getMoveControl();
            if (moveControl instanceof AbstractCubeMob.CubeMobMoveControl) {
                AbstractCubeMob.CubeMobMoveControl cubeMobMoveControl = (AbstractCubeMob.CubeMobMoveControl)moveControl;
                cubeMobMoveControl.setWantedMovement(0.0);
            }
        }

        @Override
        protected void navigateTowards(Player player) {
            this.mob.lookAt(player, 10.0f, 10.0f);
            MoveControl moveControl = this.mob.getMoveControl();
            if (moveControl instanceof AbstractCubeMob.CubeMobMoveControl) {
                AbstractCubeMob.CubeMobMoveControl cubeMobMoveControl = (AbstractCubeMob.CubeMobMoveControl)moveControl;
                cubeMobMoveControl.setDirection(this.mob.getYRot(), true);
            }
        }
    }

    private class SulfurCubeSearchForItemsGoal
    extends Goal {
        private final SulfurCube sulfurCube;
        private @Nullable ItemEntity targetItem;
        final /* synthetic */ SulfurCube this$0;

        public SulfurCubeSearchForItemsGoal(SulfurCube sulfurCube, SulfurCube sulfurCube2) {
            SulfurCube sulfurCube3 = sulfurCube;
            Objects.requireNonNull(sulfurCube3);
            this.this$0 = sulfurCube3;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
            this.sulfurCube = sulfurCube2;
        }

        @Override
        public boolean canUse() {
            if (this.sulfurCube.isBaby() || this.sulfurCube.pickupTimer > 0) {
                return false;
            }
            this.targetItem = SulfurCubeSearchForItemsGoal.getServerLevel(this.sulfurCube).getNearestEntity(this.sulfurCube.level().getEntitiesOfClass(ItemEntity.class, this.sulfurCube.getBoundingBox().inflate(8.0, 8.0, 8.0), ALLOWED_ITEMS), this.sulfurCube.getX(), this.sulfurCube.getY(), this.sulfurCube.getZ());
            return this.targetItem != null;
        }

        @Override
        public void tick() {
            this.this$0.lookAt(this.targetItem, 10.0f, 10.0f);
            MoveControl moveControl = this.this$0.getMoveControl();
            if (moveControl instanceof AbstractCubeMob.CubeMobMoveControl) {
                AbstractCubeMob.CubeMobMoveControl cubeMobMoveControl = (AbstractCubeMob.CubeMobMoveControl)moveControl;
                cubeMobMoveControl.setDirection(this.this$0.getYRot(), true);
            }
        }
    }
}

