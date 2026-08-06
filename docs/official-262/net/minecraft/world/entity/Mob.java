/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Maps
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugPathInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.entity.EquipmentUser;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jspecify.annotations.Nullable;

public abstract class Mob
extends LivingEntity
implements Targeting,
EquipmentUser,
Leashable {
    private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
    private static final int MOB_FLAG_NO_AI = 1;
    private static final int MOB_FLAG_LEFTHANDED = 2;
    private static final int MOB_FLAG_AGGRESSIVE = 4;
    protected static final int PICKUP_REACH = 1;
    private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);
    private static final List<EquipmentSlot> EQUIPMENT_POPULATION_ORDER = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static final float MAX_WEARING_ARMOR_CHANCE = 0.15f;
    public static final float WEARING_ARMOR_UPGRADE_MATERIAL_CHANCE = 0.1087f;
    public static final float WEARING_ARMOR_UPGRADE_MATERIAL_ATTEMPTS = 3.0f;
    public static final float MAX_PICKUP_LOOT_CHANCE = 0.55f;
    public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5f;
    public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25f;
    public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
    private static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04f) - (double)0.6f;
    private static final boolean DEFAULT_CAN_PICK_UP_LOOT = false;
    private static final boolean DEFAULT_PERSISTENCE_REQUIRED = false;
    private static final boolean DEFAULT_LEFT_HANDED = false;
    private static final boolean DEFAULT_NO_AI = false;
    protected static final Identifier RANDOM_SPAWN_BONUS_ID = Identifier.withDefaultNamespace("random_spawn_bonus");
    public static final String TAG_DROP_CHANCES = "drop_chances";
    public static final String TAG_LEFT_HANDED = "LeftHanded";
    public static final String TAG_CAN_PICK_UP_LOOT = "CanPickUpLoot";
    public static final String TAG_NO_AI = "NoAI";
    public static final String TAG_PERSISTENCE_REQUIRED = "PersistenceRequired";
    public int ambientSoundTime;
    protected int xpReward;
    protected LookControl lookControl;
    protected MoveControl moveControl;
    protected JumpControl jumpControl;
    private final BodyRotationControl bodyRotationControl;
    protected PathNavigation navigation;
    protected final GoalSelector goalSelector;
    protected final GoalSelector targetSelector;
    private @Nullable LivingEntity target;
    private final Sensing sensing;
    private DropChances dropChances = DropChances.DEFAULT;
    private boolean canPickUpLoot = false;
    private boolean persistenceRequired = false;
    private final Map<PathType, Float> pathfindingMalus = Maps.newEnumMap(PathType.class);
    private Optional<ResourceKey<LootTable>> lootTable = Optional.empty();
    private long lootTableSeed;
    private @Nullable Leashable.LeashData leashData;
    private BlockPos homePosition = BlockPos.ZERO;
    private int homeRadius = -1;

    protected Mob(EntityType<? extends Mob> type, Level level) {
        super((EntityType<? extends LivingEntity>)type, level);
        this.goalSelector = new GoalSelector();
        this.targetSelector = new GoalSelector();
        this.lookControl = new LookControl(this);
        this.moveControl = new MoveControl<Mob>(this);
        this.jumpControl = new JumpControl(this);
        this.bodyRotationControl = this.createBodyControl();
        this.navigation = this.createNavigation(level);
        this.sensing = new Sensing(this);
        if (level instanceof ServerLevel) {
            this.registerGoals();
        }
    }

    protected void registerGoals() {
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0);
    }

    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    protected boolean shouldPassengersInheritMalus() {
        return false;
    }

    public float getPathfindingMalus(PathType pathType) {
        Mob riding;
        Entity entity = this.getControlledVehicle();
        Mob inheritFrom = entity instanceof Mob && (riding = (Mob)entity).shouldPassengersInheritMalus() ? riding : this;
        Float malus = inheritFrom.pathfindingMalus.get((Object)pathType);
        return malus == null ? pathType.getMalus() : malus.floatValue();
    }

    public void setPathfindingMalus(PathType pathType, float cost) {
        this.pathfindingMalus.put(pathType, Float.valueOf(cost));
    }

    public void onPathfindingStart() {
    }

    public void onPathfindingDone() {
    }

    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this);
    }

    public LookControl getLookControl() {
        return this.lookControl;
    }

    public MoveControl getMoveControl() {
        Entity entity = this.getControlledVehicle();
        if (entity instanceof Mob) {
            Mob riding = (Mob)entity;
            return riding.getMoveControl();
        }
        return this.moveControl;
    }

    public JumpControl getJumpControl() {
        return this.jumpControl;
    }

    public PathNavigation getNavigation() {
        Entity entity = this.getControlledVehicle();
        if (entity instanceof Mob) {
            Mob riding = (Mob)entity;
            return riding.getNavigation();
        }
        return this.navigation;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Entity firstPassenger = this.getFirstPassenger();
        if (this.isNoAi()) return null;
        if (!(firstPassenger instanceof Mob)) return null;
        Mob passenger = (Mob)firstPassenger;
        if (!firstPassenger.canControlVehicle()) return null;
        Mob mob = passenger;
        return mob;
    }

    public Sensing getSensing() {
        return this.sensing;
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return this.asValidTarget(this.target);
    }

    public @Nullable LivingEntity getTargetUnchecked() {
        return this.target;
    }

    protected @Nullable LivingEntity asValidTarget(@Nullable LivingEntity target) {
        Player player;
        if (target instanceof Player && ((player = (Player)target).isCreative() || player.isSpectator())) {
            return null;
        }
        if (target != null && !this.canAttack(target)) {
            return null;
        }
        return target;
    }

    protected final @Nullable LivingEntity getTargetFromBrain() {
        return this.asValidTarget(this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null));
    }

    public void setTarget(@Nullable LivingEntity target) {
        this.target = this.asValidTarget(target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !target.is(EntityTypes.GHAST) && super.canAttack(target);
    }

    public boolean canUseNonMeleeWeapon(ItemStack item) {
        return false;
    }

    public void ate() {
        this.gameEvent(GameEvent.EAT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_MOB_FLAGS_ID, (byte)0);
    }

    public int getAmbientSoundInterval() {
        return 80;
    }

    public void playAmbientSound() {
        this.makeSound(this.getAmbientSound());
    }

    @Override
    public void baseTick() {
        super.baseTick();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("mobBaseTick");
        if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
            this.resetAmbientSoundTime();
            this.playAmbientSound();
        }
        profiler.pop();
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.resetAmbientSoundTime();
        super.playHurtSound(source);
    }

    private void resetAmbientSoundTime() {
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        if (this.xpReward > 0) {
            int result = this.xpReward;
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                ItemStack item;
                if (!slot.canIncreaseExperience() || (item = this.getItemBySlot(slot)).isEmpty() || !(this.dropChances.byEquipment(slot) <= 1.0f)) continue;
                result += 1 + this.random.nextInt(3);
            }
            return result;
        }
        return this.xpReward;
    }

    public void spawnAnim() {
        if (this.level().isClientSide()) {
            this.makePoofParticles();
        } else {
            this.level().broadcastEntityEvent(this, (byte)20);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 20) {
            this.spawnAnim();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 5 == 0) {
            this.updateControlFlags();
        }
    }

    protected void updateControlFlags() {
        boolean noController = !(this.getControllingPassenger() instanceof Mob);
        boolean notInBoat = !(this.getVehicle() instanceof AbstractBoat);
        this.goalSelector.setControlFlag(Goal.Flag.MOVE, noController);
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, noController && notInBoat);
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, noController);
    }

    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        this.bodyRotationControl.clientTick();
    }

    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean(TAG_CAN_PICK_UP_LOOT, this.canPickUpLoot());
        output.putBoolean(TAG_PERSISTENCE_REQUIRED, this.persistenceRequired);
        if (!this.dropChances.equals(DropChances.DEFAULT)) {
            output.store(TAG_DROP_CHANCES, DropChances.CODEC, this.dropChances);
        }
        this.writeLeashData(output, this.leashData);
        if (this.hasHome()) {
            output.putInt("home_radius", this.homeRadius);
            output.store("home_pos", BlockPos.CODEC, this.homePosition);
        }
        output.putBoolean(TAG_LEFT_HANDED, this.isLeftHanded());
        this.lootTable.ifPresent(lootTable -> output.store("DeathLootTable", LootTable.KEY_CODEC, lootTable));
        if (this.lootTableSeed != 0L) {
            output.putLong("DeathLootTableSeed", this.lootTableSeed);
        }
        if (this.isNoAi()) {
            output.putBoolean(TAG_NO_AI, this.isNoAi());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setCanPickUpLoot(input.getBooleanOr(TAG_CAN_PICK_UP_LOOT, false));
        this.persistenceRequired = input.getBooleanOr(TAG_PERSISTENCE_REQUIRED, false);
        this.dropChances = input.read(TAG_DROP_CHANCES, DropChances.CODEC).orElse(DropChances.DEFAULT);
        this.readLeashData(input);
        this.homeRadius = input.getIntOr("home_radius", -1);
        if (this.homeRadius >= 0) {
            this.homePosition = input.read("home_pos", BlockPos.CODEC).orElse(BlockPos.ZERO);
        }
        this.setLeftHanded(input.getBooleanOr(TAG_LEFT_HANDED, false));
        this.lootTable = input.read("DeathLootTable", LootTable.KEY_CODEC);
        this.lootTableSeed = input.getLongOr("DeathLootTableSeed", 0L);
        this.setNoAi(input.getBooleanOr(TAG_NO_AI, false));
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        super.dropFromLootTable(level, source, playerKilled);
        this.lootTable = Optional.empty();
    }

    @Override
    public final Optional<ResourceKey<LootTable>> getLootTable() {
        if (this.lootTable.isPresent()) {
            return this.lootTable;
        }
        return super.getLootTable();
    }

    @Override
    public long getLootTableSeed() {
        return this.lootTableSeed;
    }

    public void setZza(float zza) {
        this.zza = zza;
    }

    public void setYya(float yya) {
        this.yya = yya;
    }

    public void setXxa(float xxa) {
        this.xxa = xxa;
    }

    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed);
        this.setZza(speed);
    }

    public void stopInPlace() {
        this.getNavigation().stop();
        this.setXxa(0.0f);
        this.setYya(0.0f);
        this.setSpeed(0.0f);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.resetAngularLeashMomentum();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.is(EntityTypeTags.BURN_IN_DAYLIGHT)) {
            this.burnUndead();
        }
        ProfilerFiller profiler = Profiler.get();
        profiler.push("looting");
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.canPickUpLoot() && this.isAlive() && !this.dead && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                Vec3i pickupReach = this.getPickupReach();
                List<ItemEntity> entities = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(pickupReach.getX(), pickupReach.getY(), pickupReach.getZ()));
                for (ItemEntity entity : entities) {
                    if (entity.isRemoved() || entity.getItem().isEmpty() || entity.hasPickUpDelay() || !this.wantsToPickUp(serverLevel, entity.getItem())) continue;
                    this.pickUpItem(serverLevel, entity);
                }
            }
        }
        profiler.pop();
    }

    protected EquipmentSlot sunProtectionSlot() {
        return EquipmentSlot.HEAD;
    }

    private void burnUndead() {
        if (!this.isAlive() || !this.isSunBurnTick()) {
            return;
        }
        EquipmentSlot slot = this.sunProtectionSlot();
        ItemStack sunBlocker = this.getItemBySlot(slot);
        if (!sunBlocker.isEmpty()) {
            if (sunBlocker.isDamageableItem()) {
                Item sunBlockerItem = sunBlocker.getItem();
                sunBlocker.setDamageValue(sunBlocker.getDamageValue() + this.random.nextInt(2));
                if (sunBlocker.getDamageValue() >= sunBlocker.getMaxDamage()) {
                    this.onEquippedItemBroken(sunBlockerItem, slot);
                    this.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
            return;
        }
        this.igniteForSeconds(8.0f);
    }

    private boolean isSunBurnTick() {
        if (!this.level().isClientSide() && this.level().environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, this.position()).booleanValue()) {
            boolean isInNonBurnableBlock;
            float br = this.getLightLevelDependentMagicValue();
            BlockPos roundedPos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
            boolean bl = isInNonBurnableBlock = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
            if (br > 0.5f && this.random.nextFloat() * 30.0f < (br - 0.4f) * 2.0f && !isInNonBurnableBlock && this.level().canSeeSky(roundedPos)) {
                return true;
            }
        }
        return false;
    }

    protected Vec3i getPickupReach() {
        return ITEM_PICKUP_REACH;
    }

    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack itemStack = entity.getItem();
        ItemStack equippedWithStack = this.equipItemIfPossible(level, itemStack.copy());
        if (!equippedWithStack.isEmpty()) {
            this.onItemPickup(entity);
            this.take(entity, equippedWithStack.getCount());
            itemStack.shrink(equippedWithStack.getCount());
            if (itemStack.isEmpty()) {
                entity.discard();
            }
        }
    }

    public ItemStack equipItemIfPossible(ServerLevel level, ItemStack itemStack) {
        EquipmentSlot slot = this.getEquipmentSlotForItem(itemStack);
        if (!this.isEquippableInSlot(itemStack, slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack current = this.getItemBySlot(slot);
        boolean canReplace = this.canReplaceCurrentItem(itemStack, current, slot);
        if (slot.isArmor() && !canReplace) {
            slot = EquipmentSlot.MAINHAND;
            current = this.getItemBySlot(slot);
            canReplace = current.isEmpty();
        }
        if (canReplace && this.canHoldItem(itemStack)) {
            double dropChance = this.dropChances.byEquipment(slot);
            if (!current.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1f, 0.0f) < dropChance) {
                this.spawnAtLocation(level, current);
            }
            ItemStack toEquip = slot.limit(itemStack);
            this.setItemSlotAndDropWhenKilled(slot, toEquip);
            this.persistenceRequired = true;
            return toEquip;
        }
        return ItemStack.EMPTY;
    }

    protected void setItemSlotAndDropWhenKilled(EquipmentSlot slot, ItemStack itemStack) {
        this.setItemSlot(slot, itemStack);
        this.setGuaranteedDrop(slot);
    }

    protected boolean canShearEquipment(Player player) {
        return !this.isVehicle();
    }

    protected boolean attemptToShearEquipment(Player player, InteractionHand hand, ItemStack heldItem) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = this.getItemBySlot(slot);
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            if (equippable == null || !equippable.canBeSheared() || EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) && !player.isCreative()) continue;
            this.shearItem(player, hand, heldItem, slot, itemStack);
            this.playSound(equippable.shearingSound().value());
            return true;
        }
        return false;
    }

    protected void shearItem(Player player, InteractionHand hand, ItemStack heldItem, EquipmentSlot slot, ItemStack itemStackToShear) {
        heldItem.hurtAndBreak(1, (LivingEntity)player, hand.asEquipmentSlot());
        Vec3 equipmentSpawnOffset = this.getAttachments().getAverage(EntityAttachment.PASSENGER);
        this.setItemSlot(slot, ItemStack.EMPTY);
        this.gameEvent(GameEvent.SHEAR, player);
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.spawnAtLocation(serverLevel, itemStackToShear, equipmentSpawnOffset);
            CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.trigger((ServerPlayer)player, itemStackToShear, this);
        }
    }

    public void setGuaranteedDrop(EquipmentSlot slot) {
        this.dropChances = this.dropChances.withGuaranteedDrop(slot);
    }

    protected boolean canReplaceCurrentItem(ItemStack newItemStack, ItemStack currentItemStack, EquipmentSlot slot) {
        if (currentItemStack.isEmpty()) {
            return true;
        }
        if (slot.isArmor()) {
            return this.compareArmor(newItemStack, currentItemStack, slot);
        }
        if (slot == EquipmentSlot.MAINHAND) {
            return this.compareWeapons(newItemStack, currentItemStack, slot);
        }
        return false;
    }

    private boolean compareArmor(ItemStack newItemStack, ItemStack currentItemStack, EquipmentSlot slot) {
        if (EnchantmentHelper.has(currentItemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        }
        double newDefense = this.getApproximateAttributeWith(newItemStack, Attributes.ARMOR, slot);
        double oldDefense = this.getApproximateAttributeWith(currentItemStack, Attributes.ARMOR, slot);
        double newToughness = this.getApproximateAttributeWith(newItemStack, Attributes.ARMOR_TOUGHNESS, slot);
        double oldToughness = this.getApproximateAttributeWith(currentItemStack, Attributes.ARMOR_TOUGHNESS, slot);
        if (newDefense != oldDefense) {
            return newDefense > oldDefense;
        }
        if (newToughness != oldToughness) {
            return newToughness > oldToughness;
        }
        return this.canReplaceEqualItem(newItemStack, currentItemStack);
    }

    private boolean compareWeapons(ItemStack newItemStack, ItemStack currentItemStack, EquipmentSlot slot) {
        double oldAttackDamage;
        double newAttackDamage;
        TagKey<Item> preferredWeaponType = this.getPreferredWeaponType();
        if (preferredWeaponType != null) {
            if (currentItemStack.is(preferredWeaponType) && !newItemStack.is(preferredWeaponType)) {
                return false;
            }
            if (!currentItemStack.is(preferredWeaponType) && newItemStack.is(preferredWeaponType)) {
                return true;
            }
        }
        if ((newAttackDamage = this.getApproximateAttributeWith(newItemStack, Attributes.ATTACK_DAMAGE, slot)) != (oldAttackDamage = this.getApproximateAttributeWith(currentItemStack, Attributes.ATTACK_DAMAGE, slot))) {
            return newAttackDamage > oldAttackDamage;
        }
        return this.canReplaceEqualItem(newItemStack, currentItemStack);
    }

    private double getApproximateAttributeWith(ItemStack itemStack, Holder<Attribute> attribute, EquipmentSlot slot) {
        double baseValue = this.getAttributes().hasAttribute(attribute) ? this.getAttributeBaseValue(attribute) : 0.0;
        ItemAttributeModifiers attributeModifiers = itemStack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return attributeModifiers.compute(attribute, baseValue, slot);
    }

    public boolean canReplaceEqualItem(ItemStack newItemStack, ItemStack currentItemStack) {
        int currentDamageValue;
        Set<Object2IntMap.Entry<Holder<Enchantment>>> currentEnchantments = currentItemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        Set<Object2IntMap.Entry<Holder<Enchantment>>> newEnchantments = newItemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        if (newEnchantments.size() != currentEnchantments.size()) {
            return newEnchantments.size() > currentEnchantments.size();
        }
        int newDamageValue = newItemStack.getDamageValue();
        if (newDamageValue != (currentDamageValue = currentItemStack.getDamageValue())) {
            return newDamageValue < currentDamageValue;
        }
        return newItemStack.has(DataComponents.CUSTOM_NAME) && !currentItemStack.has(DataComponents.CUSTOM_NAME);
    }

    public boolean canHoldItem(ItemStack itemStack) {
        return true;
    }

    public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
        return this.canHoldItem(itemStack);
    }

    public @Nullable TagKey<Item> getPreferredWeaponType() {
        return null;
    }

    public boolean removeWhenFarAway(double distSqr) {
        return true;
    }

    public boolean requiresCustomPersistence() {
        return this.isPassenger() || this.isLeashed();
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
            this.discard();
            return;
        }
        if (this.isPersistenceRequired() || this.requiresCustomPersistence()) {
            this.noActionTime = 0;
            return;
        }
        Player player = this.level().getNearestPlayer(this, -1.0);
        if (player != null) {
            int instantDespawnDistance;
            int despawnDistanceSqr;
            double distSqr = player.distanceToSqr(this);
            if (distSqr > (double)(despawnDistanceSqr = (instantDespawnDistance = this.getType().getCategory().getDespawnDistance()) * instantDespawnDistance) && this.removeWhenFarAway(distSqr)) {
                this.discard();
            }
            int noDespawnDistance = this.getType().getCategory().getNoDespawnDistance();
            int noDespawnDistanceSqr = noDespawnDistance * noDespawnDistance;
            if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && distSqr > (double)noDespawnDistanceSqr && this.removeWhenFarAway(distSqr)) {
                this.discard();
            } else if (distSqr < (double)noDespawnDistanceSqr) {
                this.noActionTime = 0;
            }
        }
    }

    @Override
    protected final void serverAiStep() {
        ++this.noActionTime;
        ProfilerFiller profiler = Profiler.get();
        profiler.push("sensing");
        this.sensing.tick();
        profiler.pop();
        int idBasedTickCount = this.tickCount + this.getId();
        if (idBasedTickCount % 2 == 0 || this.tickCount <= 1) {
            profiler.push("targetSelector");
            this.targetSelector.tick();
            profiler.pop();
            profiler.push("goalSelector");
            this.goalSelector.tick();
            profiler.pop();
        } else {
            profiler.push("targetSelector");
            this.targetSelector.tickRunningGoals(false);
            profiler.pop();
            profiler.push("goalSelector");
            this.goalSelector.tickRunningGoals(false);
            profiler.pop();
        }
        profiler.push("navigation");
        this.navigation.tick();
        profiler.pop();
        profiler.push("mob tick");
        this.customServerAiStep((ServerLevel)this.level());
        profiler.pop();
        profiler.push("controls");
        profiler.push("move");
        this.moveControl.tick();
        profiler.popPush("look");
        this.lookControl.tick();
        profiler.popPush("jump");
        this.jumpControl.tick();
        profiler.pop();
        profiler.pop();
    }

    protected void customServerAiStep(ServerLevel level) {
    }

    public int getMaxHeadXRot() {
        return 40;
    }

    public int getMaxHeadYRot() {
        return 75;
    }

    protected void clampHeadRotationToBody() {
        float limit = this.getMaxHeadYRot();
        float headYRot = this.getYHeadRot();
        float delta = Mth.wrapDegrees(this.yBodyRot - headYRot);
        float targetDelta = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - headYRot), -limit, limit);
        float newHeadYRot = headYRot + delta - targetDelta;
        this.setYHeadRot(newHeadYRot);
    }

    public int getHeadRotSpeed() {
        return 10;
    }

    public void lookAt(Entity entity, float yMax, float xMax) {
        double yd;
        double xd = entity.getX() - this.getX();
        double zd = entity.getZ() - this.getZ();
        if (entity instanceof LivingEntity) {
            LivingEntity mob = (LivingEntity)entity;
            yd = mob.getEyeY() - this.getEyeY();
        } else {
            yd = (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0 - this.getEyeY();
        }
        double sd = Math.sqrt(xd * xd + zd * zd);
        float yRotD = (float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f;
        float xRotD = (float)(-(Mth.atan2(yd, sd) * 57.2957763671875));
        this.setXRot(this.rotlerp(this.getXRot(), xRotD, xMax));
        this.setYRot(this.rotlerp(this.getYRot(), yRotD, yMax));
    }

    private float rotlerp(float a, float b, float max) {
        float diff = Mth.wrapDegrees(b - a);
        if (diff > max) {
            diff = max;
        }
        if (diff < -max) {
            diff = -max;
        }
        return a + diff;
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        return EntitySpawnReason.isSpawner(spawnReason) || level.getBlockState(below).isValidSpawn(level, below, type);
    }

    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
        return true;
    }

    public boolean checkSpawnObstruction(LevelReader level) {
        return !level.containsAnyLiquid(this.getBoundingBox()) && level.isUnobstructed(this);
    }

    public int getMaxSpawnClusterSize() {
        return 4;
    }

    public boolean isMaxGroupSizeReached(int groupSize) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        if (this.getTarget() == null) {
            return this.getComfortableFallDistance(0.0f);
        }
        int sacrifice = (int)(this.getHealth() - this.getMaxHealth() * 0.33f);
        if ((sacrifice -= (3 - this.level().getDifficulty().getId()) * 4) < 0) {
            sacrifice = 0;
        }
        return this.getComfortableFallDistance(sacrifice);
    }

    public ItemStack getBodyArmorItem() {
        return this.getItemBySlot(EquipmentSlot.BODY);
    }

    public boolean isSaddled() {
        return this.hasValidEquippableItemForSlot(EquipmentSlot.SADDLE);
    }

    public boolean isWearingBodyArmor() {
        return this.hasValidEquippableItemForSlot(EquipmentSlot.BODY);
    }

    private boolean hasValidEquippableItemForSlot(EquipmentSlot slot) {
        return this.hasItemInSlot(slot) && this.isEquippableInSlot(this.getItemBySlot(slot), slot);
    }

    public Container createEquipmentSlotContainer(final EquipmentSlot slot) {
        return new ContainerSingleItem(){
            final /* synthetic */ Mob this$0;
            {
                Mob mob = this$0;
                Objects.requireNonNull(mob);
                this.this$0 = mob;
            }

            @Override
            public ItemStack getTheItem() {
                return this.this$0.getItemBySlot(slot);
            }

            @Override
            public void setTheItem(ItemStack itemStack) {
                this.this$0.setItemSlot(slot, itemStack);
                if (!itemStack.isEmpty()) {
                    this.this$0.setGuaranteedDrop(slot);
                    this.this$0.setPersistenceRequired();
                }
            }

            @Override
            public void setChanged() {
            }

            @Override
            public boolean stillValid(Player player) {
                return player.getVehicle() == this.this$0 || player.isWithinEntityInteractionRange(this.this$0, 4.0);
            }
        };
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = this.getItemBySlot(slot);
            float dropChance = this.dropChances.byEquipment(slot);
            if (dropChance == 0.0f) continue;
            boolean preserve = this.dropChances.isPreserved(slot);
            Object object = source.getEntity();
            if (object instanceof LivingEntity) {
                LivingEntity livingSource = (LivingEntity)object;
                object = this.level();
                if (object instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel)object;
                    dropChance = EnchantmentHelper.processEquipmentDropChance(serverLevel, livingSource, source, dropChance);
                }
            }
            if (itemStack.isEmpty() || EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) || !killedByPlayer && !preserve || !(this.random.nextFloat() < dropChance)) continue;
            if (!preserve && itemStack.isDamageableItem()) {
                itemStack.setDamageValue(itemStack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemStack.getMaxDamage() - 3, 1))));
            }
            this.spawnAtLocation(level, itemStack);
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    public DropChances getDropChances() {
        return this.dropChances;
    }

    public void dropPreservedEquipment(ServerLevel level) {
        this.dropPreservedEquipment(level, stack -> true);
    }

    public Set<EquipmentSlot> dropPreservedEquipment(ServerLevel level, Predicate<ItemStack> shouldDrop) {
        HashSet<EquipmentSlot> slotsPreventedFromDropping = new HashSet<EquipmentSlot>();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = this.getItemBySlot(slot);
            if (itemStack.isEmpty()) continue;
            if (!shouldDrop.test(itemStack)) {
                slotsPreventedFromDropping.add(slot);
                continue;
            }
            if (!this.dropChances.isPreserved(slot)) continue;
            this.setItemSlot(slot, ItemStack.EMPTY);
            this.spawnAtLocation(level, itemStack);
        }
        return slotsPreventedFromDropping;
    }

    private LootParams createEquipmentParams(ServerLevel serverLevel) {
        return new LootParams.Builder(serverLevel).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.EQUIPMENT);
    }

    public void equip(EquipmentTable equipment) {
        this.equip(equipment.lootTable(), equipment.slotDropChances());
    }

    public void equip(ResourceKey<LootTable> lootTable, Map<EquipmentSlot, Float> dropChances) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.equip(lootTable, this.createEquipmentParams(serverLevel), dropChances);
        }
    }

    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextFloat() < 0.15f * difficulty.getSpecialMultiplier()) {
            int armorType = random.nextInt(3);
            int i = 1;
            while ((float)i <= 3.0f) {
                if (random.nextFloat() < 0.1087f) {
                    ++armorType;
                }
                ++i;
            }
            float partialChance = this.level().getDifficulty() == Difficulty.HARD ? 0.1f : 0.25f;
            boolean first = true;
            for (EquipmentSlot slot : EQUIPMENT_POPULATION_ORDER) {
                Item equip;
                ItemStack itemStack = this.getItemBySlot(slot);
                if (!first && random.nextFloat() < partialChance) break;
                first = false;
                if (!itemStack.isEmpty() || (equip = Mob.getEquipmentForSlot(slot, armorType)) == null) continue;
                this.setItemSlot(slot, new ItemStack(equip));
            }
        }
    }

    public static @Nullable Item getEquipmentForSlot(EquipmentSlot slot, int type) {
        switch (slot) {
            case HEAD: {
                if (type == 0) {
                    return Items.LEATHER_HELMET;
                }
                if (type == 1) {
                    return Items.COPPER_HELMET;
                }
                if (type == 2) {
                    return Items.GOLDEN_HELMET;
                }
                if (type == 3) {
                    return Items.CHAINMAIL_HELMET;
                }
                if (type == 4) {
                    return Items.IRON_HELMET;
                }
                if (type == 5) {
                    return Items.DIAMOND_HELMET;
                }
            }
            case CHEST: {
                if (type == 0) {
                    return Items.LEATHER_CHESTPLATE;
                }
                if (type == 1) {
                    return Items.COPPER_CHESTPLATE;
                }
                if (type == 2) {
                    return Items.GOLDEN_CHESTPLATE;
                }
                if (type == 3) {
                    return Items.CHAINMAIL_CHESTPLATE;
                }
                if (type == 4) {
                    return Items.IRON_CHESTPLATE;
                }
                if (type == 5) {
                    return Items.DIAMOND_CHESTPLATE;
                }
            }
            case LEGS: {
                if (type == 0) {
                    return Items.LEATHER_LEGGINGS;
                }
                if (type == 1) {
                    return Items.COPPER_LEGGINGS;
                }
                if (type == 2) {
                    return Items.GOLDEN_LEGGINGS;
                }
                if (type == 3) {
                    return Items.CHAINMAIL_LEGGINGS;
                }
                if (type == 4) {
                    return Items.IRON_LEGGINGS;
                }
                if (type == 5) {
                    return Items.DIAMOND_LEGGINGS;
                }
            }
            case FEET: {
                if (type == 0) {
                    return Items.LEATHER_BOOTS;
                }
                if (type == 1) {
                    return Items.COPPER_BOOTS;
                }
                if (type == 2) {
                    return Items.GOLDEN_BOOTS;
                }
                if (type == 3) {
                    return Items.CHAINMAIL_BOOTS;
                }
                if (type == 4) {
                    return Items.IRON_BOOTS;
                }
                if (type != 5) break;
                return Items.DIAMOND_BOOTS;
            }
        }
        return null;
    }

    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance localDifficulty) {
        this.enchantSpawnedWeapon(level, random, localDifficulty);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            this.enchantSpawnedArmor(level, random, slot, localDifficulty);
        }
    }

    protected void enchantSpawnedWeapon(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        this.enchantSpawnedEquipment(level, EquipmentSlot.MAINHAND, random, 0.25f, difficulty);
    }

    protected void enchantSpawnedArmor(ServerLevelAccessor level, RandomSource random, EquipmentSlot slot, DifficultyInstance difficulty) {
        this.enchantSpawnedEquipment(level, slot, random, 0.5f, difficulty);
    }

    private void enchantSpawnedEquipment(ServerLevelAccessor level, EquipmentSlot slot, RandomSource random, float chance, DifficultyInstance difficulty) {
        ItemStack itemStack = this.getItemBySlot(slot);
        if (!itemStack.isEmpty() && random.nextFloat() < chance * difficulty.getSpecialMultiplier()) {
            EnchantmentHelper.enchantItemFromProvider(itemStack, level.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, random);
            this.setItemSlot(slot, itemStack);
        }
    }

    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        RandomSource random = level.getRandom();
        AttributeInstance followRange = Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE));
        if (!followRange.hasModifier(RANDOM_SPAWN_BONUS_ID)) {
            followRange.addPermanentModifier(new AttributeModifier(RANDOM_SPAWN_BONUS_ID, random.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        this.setLeftHanded(random.nextFloat() < 0.05f);
        return groupData;
    }

    public void setPersistenceRequired() {
        this.persistenceRequired = true;
    }

    @Override
    public void setDropChance(EquipmentSlot slot, float percent) {
        this.dropChances = this.dropChances.withEquipmentChance(slot, percent);
    }

    @Override
    public boolean canPickUpLoot() {
        return this.canPickUpLoot;
    }

    public void setCanPickUpLoot(boolean canPickUpLoot) {
        this.canPickUpLoot = canPickUpLoot;
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return this.canPickUpLoot();
    }

    public boolean isPersistenceRequired() {
        return this.persistenceRequired;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!this.isAlive()) {
            return InteractionResult.PASS;
        }
        InteractionResult interactionResult = this.checkAndHandleImportantInteractions(player, hand);
        if (interactionResult.consumesAction()) {
            this.gameEvent(GameEvent.ENTITY_INTERACT, player);
            return interactionResult;
        }
        InteractionResult superReaction = super.interact(player, hand, location);
        if (superReaction != InteractionResult.PASS) {
            return superReaction;
        }
        interactionResult = this.mobInteract(player, hand);
        if (interactionResult.consumesAction()) {
            this.gameEvent(GameEvent.ENTITY_INTERACT, player);
            return interactionResult;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult checkAndHandleImportantInteractions(Player player, InteractionHand hand) {
        InteractionResult nameTagInteractionResult;
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.NAME_TAG) && (nameTagInteractionResult = itemStack.interactLivingEntity(player, this, hand)).consumesAction()) {
            return nameTagInteractionResult;
        }
        if (itemStack.getItem() instanceof SpawnEggItem) {
            Level level = this.level();
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                Optional<Mob> offspring = SpawnEggItem.spawnOffspringFromSpawnEgg(player, this, this.getType(), serverLevel, this.position(), itemStack);
                offspring.ifPresent(mob -> this.onOffspringSpawnedFromEgg(player, (Mob)mob));
                if (offspring.isEmpty()) {
                    return InteractionResult.PASS;
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    protected void onOffspringSpawnedFromEgg(Player spawner, Mob offspring) {
    }

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack itemStack) {
        int beforeUseCount = itemStack.getCount();
        UseRemainder useRemainder = itemStack.get(DataComponents.USE_REMAINDER);
        itemStack.consume(1, player);
        if (useRemainder != null) {
            ItemStack newHandStack = useRemainder.convertIntoRemainder(itemStack, beforeUseCount, player.hasInfiniteMaterials(), player::handleExtraItemsCreatedOnUse);
            player.setItemInHand(hand, newHandStack);
        }
    }

    public boolean isWithinHome() {
        return this.isWithinHome(this.blockPosition());
    }

    public boolean isWithinHome(BlockPos pos) {
        if (this.homeRadius == -1) {
            return true;
        }
        return this.homePosition.distSqr(pos) < (double)(this.homeRadius * this.homeRadius);
    }

    public boolean isWithinHome(Vec3 pos) {
        if (this.homeRadius == -1) {
            return true;
        }
        return this.homePosition.distToCenterSqr(pos) < (double)(this.homeRadius * this.homeRadius);
    }

    public void setHomeTo(BlockPos newCenter, int radius) {
        this.homePosition = newCenter;
        this.homeRadius = radius;
    }

    public BlockPos getHomePosition() {
        return this.homePosition;
    }

    public int getHomeRadius() {
        return this.homeRadius;
    }

    public void clearHome() {
        this.homeRadius = -1;
    }

    public boolean hasHome() {
        return this.homeRadius != -1;
    }

    public <T extends Mob> @Nullable T convertTo(EntityType<T> entityType, ConversionParams params, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion) {
        if (this.isRemoved()) {
            return null;
        }
        Mob newMob = (Mob)entityType.create(this.level(), spawnReason);
        if (newMob == null) {
            return null;
        }
        params.type().convert(this, newMob, params);
        afterConversion.finalizeConversion(newMob);
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.addFreshEntity(newMob);
        }
        if (params.type().shouldDiscardAfterConversion()) {
            this.discard();
        }
        return (T)newMob;
    }

    public <T extends Mob> @Nullable T convertTo(EntityType<T> entityType, ConversionParams params, ConversionParams.AfterConversion<T> afterConversion) {
        return this.convertTo(entityType, params, EntitySpawnReason.CONVERSION, afterConversion);
    }

    @Override
    public @Nullable Leashable.LeashData getLeashData() {
        return this.leashData;
    }

    private void resetAngularLeashMomentum() {
        if (this.leashData != null) {
            this.leashData.angularMomentum = 0.0;
        }
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData) {
        this.leashData = leashData;
    }

    @Override
    public void onLeashRemoved() {
        if (this.getLeashData() == null) {
            this.clearHome();
        }
    }

    @Override
    public void leashTooFarBehaviour() {
        Leashable.super.leashTooFarBehaviour();
        this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
    }

    @Override
    public boolean canBeLeashed() {
        return !(this instanceof Enemy);
    }

    @Override
    public boolean startRiding(Entity entity, boolean force, boolean sendEventAndTriggers) {
        boolean result = super.startRiding(entity, force, sendEventAndTriggers);
        if (result && this.isLeashed()) {
            this.dropLeash();
        }
        return result;
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && !this.isNoAi();
    }

    public void setNoAi(boolean flag) {
        byte val = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, flag ? (byte)(val | 1) : (byte)(val & 0xFFFFFFFE));
    }

    public void setLeftHanded(boolean flag) {
        byte val = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, flag ? (byte)(val | 2) : (byte)(val & 0xFFFFFFFD));
    }

    public void setAggressive(boolean flag) {
        byte val = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, flag ? (byte)(val | 4) : (byte)(val & 0xFFFFFFFB));
    }

    public boolean isNoAi() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
    }

    public boolean isLeftHanded() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
    }

    public boolean isAggressive() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
    }

    public void setBaby(boolean baby) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public boolean isWithinMeleeAttackRange(LivingEntity target) {
        double minRange;
        double maxRange;
        AttackRange attackRange = this.getActiveItem().get(DataComponents.ATTACK_RANGE);
        if (attackRange == null) {
            maxRange = DEFAULT_ATTACK_REACH;
            minRange = 0.0;
        } else {
            maxRange = attackRange.effectiveMaxRange(this);
            minRange = attackRange.effectiveMinRange(this);
        }
        AABB hitbox = target.getHitbox();
        return this.getAttackBoundingBox(maxRange).intersects(hitbox) && (minRange <= 0.0 || !this.getAttackBoundingBox(minRange).intersects(hitbox));
    }

    protected AABB getAttackBoundingBox(double horizontalExpansion) {
        AABB aabb;
        Entity vehicle = this.getVehicle();
        if (vehicle != null) {
            AABB mountAabb = vehicle.getBoundingBox();
            AABB ownAabb = this.getBoundingBox();
            aabb = new AABB(Math.min(ownAabb.minX, mountAabb.minX), ownAabb.minY, Math.min(ownAabb.minZ, mountAabb.minZ), Math.max(ownAabb.maxX, mountAabb.maxX), ownAabb.maxY, Math.max(ownAabb.maxZ, mountAabb.maxZ));
        } else {
            aabb = this.getBoundingBox();
        }
        return aabb.inflate(horizontalExpansion, 0.0, horizontalExpansion);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float dmg = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack weaponItem = this.getWeaponItem();
        DamageSource damageSource = weaponItem.getDamageSource(this);
        dmg = EnchantmentHelper.modifyDamage(level, weaponItem, target, damageSource, dmg);
        dmg += weaponItem.getItem().getAttackDamageBonus(target, dmg, damageSource);
        Vec3 oldMovement = target.getDeltaMovement();
        boolean wasHurt = target.hurtServer(level, damageSource, dmg);
        if (wasHurt) {
            this.causeExtraKnockback(target, this.getKnockback(target, damageSource), oldMovement, damageSource, dmg, true);
            if (target instanceof LivingEntity) {
                LivingEntity livingTarget = (LivingEntity)target;
                weaponItem.hurtEnemy(livingTarget, this);
            }
            EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
            this.setLastHurtMob(target);
            this.playAttackSound();
        }
        this.postPiercingAttack();
        return wasHurt;
    }

    @Override
    protected void jumpInLiquid(TagKey<Fluid> type) {
        if (this.getNavigation().canFloat()) {
            super.jumpInLiquid(type);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
        }
    }

    @VisibleForTesting
    public void removeFreeWill() {
        this.removeAllGoals(goal -> true);
        this.getBrain().removeAllBehaviors();
    }

    @VisibleForDebug
    @VisibleForTesting
    public GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    public void removeAllGoals(Predicate<Goal> predicate) {
        this.goalSelector.removeAllGoals(predicate);
    }

    @Override
    protected void removeAfterChangingDimensions() {
        super.removeAfterChangingDimensions();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = this.getItemBySlot(slot);
            if (itemStack.isEmpty()) continue;
            itemStack.setCount(0);
        }
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return SpawnEggItem.byId(this.getType()).map(ItemStack::new).orElse(null);
    }

    @Override
    protected void onAttributeUpdated(Holder<Attribute> attribute) {
        super.onAttributeUpdated(attribute);
        if (attribute.is(Attributes.FOLLOW_RANGE) || attribute.is(Attributes.TEMPT_RANGE)) {
            this.getNavigation().updatePathfinderMaxVisitedNodes();
        }
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
        registration.register(DebugSubscriptions.ENTITY_PATHS, () -> {
            Path path = this.getNavigation().getPath();
            if (path != null && path.debugData() != null) {
                return new DebugPathInfo(path.copy(), this.getNavigation().getMaxDistanceToWaypoint());
            }
            return null;
        });
        registration.register(DebugSubscriptions.GOAL_SELECTORS, () -> {
            Set<WrappedGoal> availableGoals = this.goalSelector.getAvailableGoals();
            ArrayList<DebugGoalInfo.DebugGoal> goalInfo = new ArrayList<DebugGoalInfo.DebugGoal>(availableGoals.size());
            availableGoals.forEach(goal -> goalInfo.add(new DebugGoalInfo.DebugGoal(goal.getPriority(), goal.isRunning(), goal.getGoal().getClass().getSimpleName())));
            return new DebugGoalInfo(goalInfo);
        });
        if (!this.brain.isBrainDead()) {
            registration.register(DebugSubscriptions.BRAINS, () -> DebugBrainDump.takeBrainDump(level, this));
        }
    }

    public float chargeSpeedModifier() {
        return 1.0f;
    }
}

