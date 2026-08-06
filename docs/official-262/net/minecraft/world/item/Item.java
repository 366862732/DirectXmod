/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.item;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Item
implements ItemLike,
FeatureElement {
    public static final Codec<Holder<Item>> CODEC = BuiltInRegistries.ITEM.holderByNameCodec().validate(item -> item.is(Items.AIR.builtInRegistryHolder()) ? DataResult.error(() -> "Item must not be minecraft:air") : DataResult.success((Object)item));
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);
    public static final Codec<Holder<Item>> CODEC_WITH_BOUND_COMPONENTS = CODEC.validate(item -> {
        if (!item.areComponentsBound()) {
            return DataResult.error(() -> "Item " + item.getRegisteredName() + " does not have components yet");
        }
        return DataResult.success((Object)item);
    });
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<Block, Item> BY_BLOCK = Maps.newHashMap();
    public static final Identifier BASE_ATTACK_DAMAGE_ID = Identifier.withDefaultNamespace("base_attack_damage");
    public static final Identifier BASE_ATTACK_SPEED_ID = Identifier.withDefaultNamespace("base_attack_speed");
    public static final int DEFAULT_MAX_STACK_SIZE = 64;
    public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
    public static final int MAX_BAR_WIDTH = 13;
    protected static final int APPROXIMATELY_INFINITE_USE_DURATION = 72000;
    private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
    private final @Nullable ItemStackTemplate craftingRemainingItem;
    protected final String descriptionId;
    private final FeatureFlagSet requiredFeatures;

    public static int getId(Item item) {
        return item == null ? 0 : BuiltInRegistries.ITEM.getId(item);
    }

    public static Item byId(int id) {
        return BuiltInRegistries.ITEM.byId(id);
    }

    @Deprecated
    public static Item byBlock(Block block) {
        return BY_BLOCK.getOrDefault(block, Items.AIR);
    }

    public Item(Properties properties) {
        String className;
        this.descriptionId = properties.effectiveDescriptionId();
        DataComponentInitializers.Initializer<Item> componentInitializer = properties.finalizeInitializer(Component.translatable(this.descriptionId), properties.effectiveModel());
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.add(properties.itemIdOrThrow(), componentInitializer);
        this.craftingRemainingItem = properties.craftingRemainingItem;
        this.requiredFeatures = properties.requiredFeatures;
        if (SharedConstants.IS_RUNNING_IN_IDE && !(className = this.getClass().getSimpleName()).endsWith("Item")) {
            LOGGER.error("Item classes should end with Item and {} doesn't.", (Object)className);
        }
    }

    @Deprecated
    public Holder.Reference<Item> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public DataComponentMap components() {
        return this.builtInRegistryHolder.components();
    }

    public int getDefaultMaxStackSize() {
        return this.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack itemStack, int ticksRemaining) {
    }

    public void onDestroyed(ItemEntity itemEntity) {
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        Tool tool = itemStack.get(DataComponents.TOOL);
        if (tool == null) return true;
        if (tool.canDestroyBlocksInCreative()) return true;
        if (!(user instanceof Player)) return true;
        Player player = (Player)user;
        if (player.getAbilities().instabuild) return false;
        return true;
    }

    @Override
    public Item asItem() {
        return this;
    }

    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public float getDestroySpeed(ItemStack itemStack, BlockState state) {
        Tool tool = itemStack.get(DataComponents.TOOL);
        return tool != null ? tool.getMiningSpeed(state) : 1.0f;
    }

    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.startConsuming(player, stack, hand);
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.swappable()) {
            return equippable.swapWithEquipmentSlot(stack, player);
        }
        if (stack.has(DataComponents.BLOCKS_ATTACKS)) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon != null) {
            player.startUsingItem(hand);
            kineticWeapon.makeSound(player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity) {
        Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.onConsume(level, entity, itemStack);
        }
        return itemStack;
    }

    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0f - (float)stack.getDamageValue() * 13.0f / (float)stack.getMaxDamage()), 0, 13);
    }

    public int getBarColor(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        float healthPercentage = Math.max(0.0f, ((float)maxDamage - (float)stack.getDamageValue()) / (float)maxDamage);
        return Mth.hsvToRgb(healthPercentage / 3.0f, 1.0f, 1.0f);
    }

    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction clickAction, Player player) {
        return false;
    }

    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
        return false;
    }

    public float getAttackDamageBonus(Entity victim, float damage, DamageSource damageSource) {
        return 0.0f;
    }

    @Deprecated
    public @Nullable DamageSource getItemDamageSource(LivingEntity attacker) {
        return null;
    }

    public void hurtEnemy(ItemStack itemStack, LivingEntity mob, LivingEntity attacker) {
    }

    public void postHurtEnemy(ItemStack itemStack, LivingEntity mob, LivingEntity attacker) {
    }

    public boolean mineBlock(ItemStack itemStack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        Tool tool = itemStack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        }
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0f && tool.damagePerBlock() > 0) {
            itemStack.hurtAndBreak(tool.damagePerBlock(), owner, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    public boolean isCorrectToolForDrops(ItemStack itemStack, BlockState state) {
        Tool tool = itemStack.get(DataComponents.TOOL);
        return tool != null && tool.isCorrectForDrops(state);
    }

    public InteractionResult interactLivingEntity(ItemStack itemStack, Player player, LivingEntity target, InteractionHand type) {
        return InteractionResult.PASS;
    }

    public String toString() {
        return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
    }

    public final @Nullable ItemStackTemplate getCraftingRemainder() {
        return this.craftingRemainingItem;
    }

    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
    }

    public void onCraftedBy(ItemStack itemStack, Player player) {
        this.onCraftedPostProcess(itemStack, player.level());
    }

    public void onCraftedPostProcess(ItemStack itemStack, Level level) {
    }

    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.animation();
        }
        if (itemStack.has(DataComponents.BLOCKS_ATTACKS)) {
            return ItemUseAnimation.BLOCK;
        }
        if (itemStack.has(DataComponents.KINETIC_WEAPON)) {
            return ItemUseAnimation.SPEAR;
        }
        return ItemUseAnimation.NONE;
    }

    public int getUseDuration(ItemStack itemStack, LivingEntity user) {
        Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.consumeTicks();
        }
        if (itemStack.has(DataComponents.BLOCKS_ATTACKS) || itemStack.has(DataComponents.KINETIC_WEAPON)) {
            return 72000;
        }
        return 0;
    }

    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        return false;
    }

    @Deprecated
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
    }

    public Optional<TooltipComponent> getTooltipImage(ItemStack itemStack) {
        return Optional.empty();
    }

    @VisibleForTesting
    public final String getDescriptionId() {
        return this.descriptionId;
    }

    public Component getName(ItemStack itemStack) {
        return itemStack.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    public boolean isFoil(ItemStack itemStack) {
        return itemStack.isEnchanted();
    }

    protected static BlockHitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluid) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        return level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, fluid, player));
    }

    public boolean useOnRelease(ItemStack itemStack) {
        return false;
    }

    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    public boolean canFitInsideContainerItems() {
        return true;
    }

    @Override
    public final FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    public boolean shouldPrintOpWarning(ItemStack stack, @Nullable Player player) {
        return false;
    }

    public static class Properties {
        private static final DependantName<Item, String> BLOCK_DESCRIPTION_ID = id -> Util.makeDescriptionId("block", id.identifier());
        private static final DependantName<Item, String> ITEM_DESCRIPTION_ID = id -> Util.makeDescriptionId("item", id.identifier());
        private DataComponentInitializers.Initializer<Item> componentInitializer = (builder, context, id) -> builder.addAll(DataComponents.COMMON_ITEM_COMPONENTS);
        private @Nullable ItemStackTemplate craftingRemainingItem;
        private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        private @Nullable ResourceKey<Item> id;
        private DependantName<Item, String> descriptionId = ITEM_DESCRIPTION_ID;
        private final DependantName<Item, Identifier> model = ResourceKey::identifier;

        public Properties food(FoodProperties foodProperties) {
            return this.food(foodProperties, Consumables.DEFAULT_FOOD);
        }

        public Properties food(FoodProperties foodProperties, Consumable consumable) {
            return this.component(DataComponents.FOOD, foodProperties).component(DataComponents.CONSUMABLE, consumable);
        }

        public Properties usingConvertsTo(Item item) {
            return this.component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(item)));
        }

        public Properties useCooldown(float seconds) {
            return this.component(DataComponents.USE_COOLDOWN, new UseCooldown(seconds));
        }

        public Properties stacksTo(int max) {
            return this.component(DataComponents.MAX_STACK_SIZE, max);
        }

        public Properties durability(int maxDamage) {
            this.component(DataComponents.MAX_DAMAGE, maxDamage);
            this.component(DataComponents.MAX_STACK_SIZE, 1);
            this.component(DataComponents.DAMAGE, 0);
            return this;
        }

        public Properties craftRemainder(Item craftingRemainingItem) {
            return this.craftRemainder(new ItemStackTemplate(craftingRemainingItem));
        }

        public Properties craftRemainder(ItemStackTemplate craftingRemainingItem) {
            this.craftingRemainingItem = craftingRemainingItem;
            return this;
        }

        public Properties rarity(Rarity rarity) {
            return this.component(DataComponents.RARITY, rarity);
        }

        public Properties fireResistant() {
            return this.delayedComponent(DataComponents.DAMAGE_RESISTANT, context -> new DamageResistant(context.getOrThrow(DamageTypeTags.IS_FIRE)));
        }

        public Properties jukeboxPlayable(ResourceKey<JukeboxSong> song) {
            return this.delayedComponent(DataComponents.JUKEBOX_PLAYABLE, context -> new JukeboxPlayable(context.getOrThrow(song)));
        }

        public Properties enchantable(int value) {
            return this.component(DataComponents.ENCHANTABLE, new Enchantable(value));
        }

        public Properties repairable(Item repairItem) {
            return this.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(repairItem.builtInRegistryHolder())));
        }

        public Properties repairable(TagKey<Item> repairItems) {
            HolderGetter<Item> registrationLookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM);
            return this.component(DataComponents.REPAIRABLE, new Repairable(registrationLookup.getOrThrow(repairItems)));
        }

        public Properties equippable(EquipmentSlot slot) {
            return this.component(DataComponents.EQUIPPABLE, Equippable.builder(slot).build());
        }

        public Properties equippableUnswappable(EquipmentSlot slot) {
            return this.component(DataComponents.EQUIPPABLE, Equippable.builder(slot).setSwappable(false).build());
        }

        public Properties tool(ToolMaterial material, TagKey<Block> minesEfficiently, float attackDamageBaseline, float attackSpeedBaseline, float disableBlockingSeconds) {
            return material.applyToolProperties(this, minesEfficiently, attackDamageBaseline, attackSpeedBaseline, disableBlockingSeconds);
        }

        public Properties pickaxe(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline) {
            return this.tool(material, BlockTags.MINEABLE_WITH_PICKAXE, attackDamageBaseline, attackSpeedBaseline, 0.0f);
        }

        public Properties axe(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline) {
            return this.tool(material, BlockTags.MINEABLE_WITH_AXE, attackDamageBaseline, attackSpeedBaseline, 5.0f);
        }

        public Properties hoe(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline) {
            return this.tool(material, BlockTags.MINEABLE_WITH_HOE, attackDamageBaseline, attackSpeedBaseline, 0.0f);
        }

        public Properties shovel(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline) {
            return this.tool(material, BlockTags.MINEABLE_WITH_SHOVEL, attackDamageBaseline, attackSpeedBaseline, 0.0f);
        }

        public Properties sword(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline) {
            return material.applySwordProperties(this, attackDamageBaseline, attackSpeedBaseline);
        }

        public Properties spear(ToolMaterial material, float attackDuration, float damageMultiplier, float delay, float dismountTime, float dismountThreshold, float knockbackTime, float knockbackThreshold, float damageTime, float damageThreshold) {
            return this.durability(material.durability()).repairable(material.repairItems()).enchantable(material.enchantmentValue()).delayedHolderComponent(DataComponents.DAMAGE_TYPE, DamageTypes.SPEAR).component(DataComponents.KINETIC_WEAPON, new KineticWeapon(10, (int)(delay * 20.0f), KineticWeapon.Condition.ofAttackerSpeed((int)(dismountTime * 20.0f), dismountThreshold), KineticWeapon.Condition.ofAttackerSpeed((int)(knockbackTime * 20.0f), knockbackThreshold), KineticWeapon.Condition.ofRelativeSpeed((int)(damageTime * 20.0f), damageThreshold), 0.38f, damageMultiplier, Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE), Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT))).component(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false, Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_ATTACK : SoundEvents.SPEAR_ATTACK), Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT))).component(DataComponents.ATTACK_RANGE, new AttackRange(2.0f, 4.5f, 2.0f, 6.5f, 0.125f, 0.5f)).component(DataComponents.MINIMUM_ATTACK_CHARGE, Float.valueOf(1.0f)).component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, (int)(attackDuration * 20.0f))).attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 0.0f + material.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, (double)(1.0f / attackDuration) - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()).component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0f)).component(DataComponents.WEAPON, new Weapon(1));
        }

        public Properties spawnEgg(EntityType<?> type) {
            return this.component(DataComponents.ENTITY_DATA, TypedEntityData.of(type, new CompoundTag())).requiredFeatures(type.requiredFeatures());
        }

        public Properties humanoidArmor(ArmorMaterial material, ArmorType type) {
            return this.durability(type.getDurability(material.durability())).attributes(material.createAttributes(type)).enchantable(material.enchantmentValue()).component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot()).setEquipSound(material.equipSound()).setAsset(material.assetId()).build()).repairable(material.repairIngredient());
        }

        public Properties wolfArmor(ArmorMaterial material) {
            return this.durability(ArmorType.BODY.getDurability(material.durability())).attributes(material.createAttributes(ArmorType.BODY)).repairable(material.repairIngredient()).component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.BODY).setEquipSound(material.equipSound()).setAsset(material.assetId()).setAllowedEntities(HolderSet.direct(EntityTypes.WOLF.builtInRegistryHolder())).setCanBeSheared(true).setShearingSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARMOR_UNEQUIP_WOLF)).build()).component(DataComponents.BREAK_SOUND, SoundEvents.WOLF_ARMOR_BREAK).stacksTo(1);
        }

        public Properties horseArmor(ArmorMaterial material) {
            HolderGetter<EntityType<?>> entityGetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
            return this.attributes(material.createAttributes(ArmorType.BODY)).component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.BODY).setEquipSound(SoundEvents.HORSE_ARMOR).setAsset(material.assetId()).setAllowedEntities(entityGetter.getOrThrow(EntityTypeTags.CAN_WEAR_HORSE_ARMOR)).setDamageOnHurt(false).setCanBeSheared(true).setShearingSound(SoundEvents.HORSE_ARMOR_UNEQUIP).build()).stacksTo(1);
        }

        public Properties nautilusArmor(ArmorMaterial material) {
            HolderGetter<EntityType<?>> entityGetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
            return this.attributes(material.createAttributes(ArmorType.BODY)).component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.BODY).setEquipSound(SoundEvents.ARMOR_EQUIP_NAUTILUS).setAsset(material.assetId()).setAllowedEntities(entityGetter.getOrThrow(EntityTypeTags.CAN_WEAR_NAUTILUS_ARMOR)).setDamageOnHurt(false).setEquipOnInteract(true).setCanBeSheared(true).setShearingSound(SoundEvents.ARMOR_UNEQUIP_NAUTILUS).build()).stacksTo(1);
        }

        public Properties trimMaterial(ResourceKey<TrimMaterial> material) {
            return this.delayedHolderComponent(DataComponents.PROVIDES_TRIM_MATERIAL, material);
        }

        public Properties requiredFeatures(FeatureFlag ... flags) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(flags);
            return this;
        }

        public Properties requiredFeatures(FeatureFlagSet flags) {
            if (!FeatureFlags.REGISTRY.isSubset(flags)) {
                throw new IllegalArgumentException("Mismatched flag sets");
            }
            this.requiredFeatures = flags;
            return this;
        }

        public Properties setId(ResourceKey<Item> id) {
            this.id = id;
            return this;
        }

        public Properties overrideDescription(String descriptionId) {
            this.descriptionId = DependantName.fixed(descriptionId);
            return this;
        }

        public Properties useBlockDescriptionPrefix() {
            this.descriptionId = BLOCK_DESCRIPTION_ID;
            return this;
        }

        public Properties useItemDescriptionPrefix() {
            this.descriptionId = ITEM_DESCRIPTION_ID;
            return this;
        }

        private ResourceKey<Item> itemIdOrThrow() {
            return Objects.requireNonNull(this.id, "Item id not set");
        }

        protected String effectiveDescriptionId() {
            return this.descriptionId.get(this.itemIdOrThrow());
        }

        public Identifier effectiveModel() {
            return this.model.get(this.itemIdOrThrow());
        }

        public <T> Properties component(DataComponentType<T> type, T value) {
            this.componentInitializer = this.componentInitializer.add(type, value);
            return this;
        }

        public <T> Properties delayedComponent(DataComponentType<T> type, DataComponentInitializers.SingleComponentInitializer<T> initializer) {
            this.componentInitializer = this.componentInitializer.andThen(initializer.asInitializer(type));
            return this;
        }

        public <T> Properties delayedHolderComponent(DataComponentType<Holder<T>> type, ResourceKey<T> valueKey) {
            this.componentInitializer = this.componentInitializer.andThen((components, context, key) -> components.set(type, context.getOrThrow(valueKey)));
            return this;
        }

        public Properties attributes(ItemAttributeModifiers attributes) {
            return this.component(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
        }

        private DataComponentInitializers.Initializer<Item> finalizeInitializer(Component name, Identifier model) {
            return this.componentInitializer.andThen((components, context, key) -> components.set(DataComponents.ITEM_NAME, name).set(DataComponents.ITEM_MODEL, model).addValidator(c -> {
                if (c.has(DataComponents.DAMAGE) && c.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
                    throw new IllegalStateException("Item cannot have both durability and be stackable");
                }
            }));
        }
    }

    public static interface TooltipContext {
        public static final TooltipContext EMPTY = new TooltipContext(){

            @Override
            public @Nullable HolderLookup.Provider registries() {
                return null;
            }

            @Override
            public float tickRate() {
                return 20.0f;
            }

            @Override
            public @Nullable MapItemSavedData mapData(MapId id) {
                return null;
            }

            @Override
            public boolean isPeaceful() {
                return false;
            }
        };

        public @Nullable HolderLookup.Provider registries();

        public float tickRate();

        public @Nullable MapItemSavedData mapData(MapId var1);

        public boolean isPeaceful();

        public static TooltipContext of(final @Nullable Level level) {
            if (level == null) {
                return EMPTY;
            }
            return new TooltipContext(){

                @Override
                public HolderLookup.Provider registries() {
                    return level.registryAccess();
                }

                @Override
                public float tickRate() {
                    return level.tickRateManager().tickrate();
                }

                @Override
                public MapItemSavedData mapData(MapId id) {
                    return level.getMapData(id);
                }

                @Override
                public boolean isPeaceful() {
                    return level.getDifficulty() == Difficulty.PEACEFUL;
                }
            };
        }

        public static TooltipContext of(final HolderLookup.Provider registries) {
            return new TooltipContext(){

                @Override
                public HolderLookup.Provider registries() {
                    return registries;
                }

                @Override
                public float tickRate() {
                    return 20.0f;
                }

                @Override
                public @Nullable MapItemSavedData mapData(MapId id) {
                    return null;
                }

                @Override
                public boolean isPeaceful() {
                    return false;
                }
            };
        }
    }
}

