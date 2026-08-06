/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.apache.commons.lang3.mutable.MutableFloat
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.enchantment;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class EnchantmentHelper {
    public static int getItemEnchantmentLevel(Holder<Enchantment> enchantment, ItemInstance piece) {
        ItemEnchantments enchantments = piece.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return enchantments.getLevel(enchantment);
    }

    public static ItemEnchantments updateEnchantments(ItemStack itemStack, Consumer<ItemEnchantments.Mutable> consumer) {
        DataComponentType<ItemEnchantments> componentType = EnchantmentHelper.getComponentType(itemStack);
        ItemEnchantments oldEnchantments = itemStack.get(componentType);
        if (oldEnchantments == null) {
            return ItemEnchantments.EMPTY;
        }
        ItemEnchantments.Mutable mutableEnchantments = new ItemEnchantments.Mutable(oldEnchantments);
        consumer.accept(mutableEnchantments);
        ItemEnchantments newEnchantments = mutableEnchantments.toImmutable();
        itemStack.set(componentType, newEnchantments);
        return newEnchantments;
    }

    public static boolean canStoreEnchantments(ItemStack itemStack) {
        return itemStack.has(EnchantmentHelper.getComponentType(itemStack));
    }

    public static void setEnchantments(ItemStack itemStack, ItemEnchantments enchantments) {
        itemStack.set(EnchantmentHelper.getComponentType(itemStack), enchantments);
    }

    public static ItemEnchantments getEnchantmentsForCrafting(ItemStack itemStack) {
        return itemStack.getOrDefault(EnchantmentHelper.getComponentType(itemStack), ItemEnchantments.EMPTY);
    }

    private static DataComponentType<ItemEnchantments> getComponentType(ItemStack itemStack) {
        return itemStack.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }

    public static boolean hasAnyEnchantments(ItemStack itemStack) {
        return !itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty() || !itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }

    public static int processDurabilityChange(ServerLevel serverLevel, ItemStack itemStack, int amount) {
        MutableFloat modifiedAmount = new MutableFloat((float)amount);
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyDurabilityChange(serverLevel, level, itemStack, modifiedAmount));
        return modifiedAmount.intValue();
    }

    public static int processAmmoUse(ServerLevel serverLevel, ItemStack weapon, ItemStack ammo, int amount) {
        MutableFloat modifiedAmount = new MutableFloat((float)amount);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyAmmoCount(serverLevel, level, ammo, modifiedAmount));
        return modifiedAmount.intValue();
    }

    public static int processBlockExperience(ServerLevel serverLevel, ItemStack itemStack, int amount) {
        MutableFloat modifiedAmount = new MutableFloat((float)amount);
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyBlockExperience(serverLevel, level, itemStack, modifiedAmount));
        return modifiedAmount.intValue();
    }

    public static int processMobExperience(ServerLevel serverLevel, @Nullable Entity killer, Entity killed, int amount) {
        if (killer instanceof LivingEntity) {
            LivingEntity livingKiller = (LivingEntity)killer;
            MutableFloat modifiedAmount = new MutableFloat((float)amount);
            EnchantmentHelper.runIterationOnEquipment(livingKiller, (enchantment, level, item) -> ((Enchantment)enchantment.value()).modifyMobExperience(serverLevel, level, item.itemStack(), killed, modifiedAmount));
            return modifiedAmount.intValue();
        }
        return amount;
    }

    public static ItemStack createBook(EnchantmentInstance enchant) {
        ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
        itemStack.enchant(enchant.enchantment(), enchant.level());
        return itemStack;
    }

    private static void runIterationOnItem(ItemStack piece, EnchantmentVisitor method) {
        ItemEnchantments enchantments = piece.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            method.accept((Holder)entry.getKey(), entry.getIntValue());
        }
    }

    private static void runIterationOnItem(ItemStack piece, EquipmentSlot slot, LivingEntity owner, EnchantmentInSlotVisitor method) {
        if (piece.isEmpty()) {
            return;
        }
        ItemEnchantments itemEnchantments = piece.get(DataComponents.ENCHANTMENTS);
        if (itemEnchantments == null || itemEnchantments.isEmpty()) {
            return;
        }
        EnchantedItemInUse itemInUse = new EnchantedItemInUse(piece, slot, owner);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantments.entrySet()) {
            Holder enchantment = (Holder)entry.getKey();
            if (!((Enchantment)enchantment.value()).matchingSlot(slot)) continue;
            method.accept(enchantment, entry.getIntValue(), itemInUse);
        }
    }

    private static void runIterationOnEquipment(LivingEntity owner, EnchantmentInSlotVisitor method) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            EnchantmentHelper.runIterationOnItem(owner.getItemBySlot(slot), slot, owner, method);
        }
    }

    public static boolean isImmuneToDamage(ServerLevel serverLevel, LivingEntity victim, DamageSource source) {
        MutableBoolean result = new MutableBoolean();
        EnchantmentHelper.runIterationOnEquipment(victim, (enchantment, level, item) -> result.setValue(result.isTrue() || ((Enchantment)enchantment.value()).isImmuneToDamage(serverLevel, level, victim, source)));
        return result.isTrue();
    }

    public static float getDamageProtection(ServerLevel serverLevel, LivingEntity victim, DamageSource source) {
        MutableFloat result = new MutableFloat(0.0f);
        EnchantmentHelper.runIterationOnEquipment(victim, (enchantment, level, item) -> ((Enchantment)enchantment.value()).modifyDamageProtection(serverLevel, level, item.itemStack(), victim, source, result));
        return result.floatValue();
    }

    public static float modifyDamage(ServerLevel serverLevel, ItemStack itemStack, Entity victim, DamageSource damageSource, float damage) {
        MutableFloat result = new MutableFloat(damage);
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyDamage(serverLevel, level, itemStack, victim, damageSource, result));
        return result.floatValue();
    }

    public static float modifyFallBasedDamage(ServerLevel serverLevel, ItemStack itemStack, Entity victim, DamageSource damageSource, float damage) {
        MutableFloat result = new MutableFloat(damage);
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyFallBasedDamage(serverLevel, level, itemStack, victim, damageSource, result));
        return result.floatValue();
    }

    public static float modifyArmorEffectiveness(ServerLevel serverLevel, ItemStack itemStack, Entity victim, DamageSource damageSource, float armorFraction) {
        MutableFloat result = new MutableFloat(armorFraction);
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyArmorEffectivness(serverLevel, level, itemStack, victim, damageSource, result));
        return result.floatValue();
    }

    public static float modifyKnockback(ServerLevel serverLevel, ItemStack itemStack, Entity victim, DamageSource damageSource, float knockback) {
        MutableFloat result = new MutableFloat(knockback);
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyKnockback(serverLevel, level, itemStack, victim, damageSource, result));
        return result.floatValue();
    }

    public static void doPostAttackEffects(ServerLevel serverLevel, Entity victim, DamageSource damageSource) {
        Entity entity = damageSource.getEntity();
        if (entity instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity)entity;
            EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, victim, damageSource, attacker.getWeaponItem());
        } else {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, victim, damageSource, null);
        }
    }

    public static void doPostPiercingAttackEffects(ServerLevel serverLevel, LivingEntity user) {
        EnchantmentHelper.runIterationOnItem(user.getWeaponItem(), EquipmentSlot.MAINHAND, user, (enchantment, level, item) -> ((Enchantment)enchantment.value()).doPostPiercingAttack(serverLevel, level, item, user));
    }

    public static void doPostAttackEffectsWithItemSource(ServerLevel serverLevel, Entity victim, DamageSource damageSource, @Nullable ItemStack source) {
        EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(serverLevel, victim, damageSource, source, null);
    }

    public static void doPostAttackEffectsWithItemSourceOnBreak(ServerLevel serverLevel, Entity victim, DamageSource damageSource, @Nullable ItemStack source, @Nullable Consumer<Item> attackerlessOnBreak) {
        if (victim instanceof LivingEntity) {
            LivingEntity livingVictim = (LivingEntity)victim;
            EnchantmentHelper.runIterationOnEquipment(livingVictim, (enchantment, level, item) -> ((Enchantment)enchantment.value()).doPostAttack(serverLevel, level, item, EnchantmentTarget.VICTIM, victim, damageSource));
        }
        if (source != null) {
            Entity entity = damageSource.getEntity();
            if (entity instanceof LivingEntity) {
                LivingEntity attacker = (LivingEntity)entity;
                EnchantmentHelper.runIterationOnItem(source, EquipmentSlot.MAINHAND, attacker, (enchantment, level, item) -> ((Enchantment)enchantment.value()).doPostAttack(serverLevel, level, item, EnchantmentTarget.ATTACKER, victim, damageSource));
            } else if (attackerlessOnBreak != null) {
                EnchantedItemInUse item2 = new EnchantedItemInUse(source, null, null, attackerlessOnBreak);
                EnchantmentHelper.runIterationOnItem(source, (enchantment, level) -> ((Enchantment)enchantment.value()).doPostAttack(serverLevel, level, item2, EnchantmentTarget.ATTACKER, victim, damageSource));
            }
        }
    }

    public static void runLocationChangedEffects(ServerLevel serverLevel, LivingEntity entity) {
        EnchantmentHelper.runIterationOnEquipment(entity, (enchantment, level, item) -> ((Enchantment)enchantment.value()).runLocationChangedEffects(serverLevel, level, item, entity));
    }

    public static void runLocationChangedEffects(ServerLevel serverLevel, ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        EnchantmentHelper.runIterationOnItem(stack, slot, entity, (enchantment, level, item) -> ((Enchantment)enchantment.value()).runLocationChangedEffects(serverLevel, level, item, entity));
    }

    public static void stopLocationBasedEffects(LivingEntity entity) {
        EnchantmentHelper.runIterationOnEquipment(entity, (enchantment, level, item) -> ((Enchantment)enchantment.value()).stopLocationBasedEffects(level, item, entity));
    }

    public static void stopLocationBasedEffects(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        EnchantmentHelper.runIterationOnItem(stack, slot, entity, (enchantment, level, item) -> ((Enchantment)enchantment.value()).stopLocationBasedEffects(level, item, entity));
    }

    public static void tickEffects(ServerLevel serverLevel, LivingEntity entity) {
        EnchantmentHelper.runIterationOnEquipment(entity, (enchantment, level, item) -> ((Enchantment)enchantment.value()).tick(serverLevel, level, item, entity));
    }

    public static int getEnchantmentLevel(Holder<Enchantment> enchantment, LivingEntity entity) {
        Collection<ItemStack> allowedSlots = enchantment.value().getSlotItems(entity).values();
        int bestLevel = 0;
        for (ItemStack piece : allowedSlots) {
            int newLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment, piece);
            if (newLevel <= bestLevel) continue;
            bestLevel = newLevel;
        }
        return bestLevel;
    }

    public static int processProjectileCount(ServerLevel serverLevel, ItemStack weapon, Entity shooter, int count) {
        MutableFloat modifiedCount = new MutableFloat((float)count);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyProjectileCount(serverLevel, level, weapon, shooter, modifiedCount));
        return Math.max(0, modifiedCount.intValue());
    }

    public static float processProjectileSpread(ServerLevel serverLevel, ItemStack weapon, Entity shooter, float angle) {
        MutableFloat modifiedAngle = new MutableFloat(angle);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyProjectileSpread(serverLevel, level, weapon, shooter, modifiedAngle));
        return Math.max(0.0f, modifiedAngle.floatValue());
    }

    public static int getPiercingCount(ServerLevel serverLevel, ItemStack weapon, ItemStack ammo) {
        MutableFloat modifiedAmount = new MutableFloat(0.0f);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyPiercingCount(serverLevel, level, ammo, modifiedAmount));
        return Math.max(0, modifiedAmount.intValue());
    }

    public static void onProjectileSpawned(ServerLevel serverLevel, ItemStack weapon, Projectile projectileEntity, Consumer<Item> onBreak) {
        LivingEntity le;
        Entity entity = projectileEntity.getOwner();
        LivingEntity owner = entity instanceof LivingEntity ? (le = (LivingEntity)entity) : null;
        EnchantedItemInUse item = new EnchantedItemInUse(weapon, null, owner, onBreak);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, level) -> ((Enchantment)enchantment.value()).onProjectileSpawned(serverLevel, level, item, projectileEntity));
    }

    public static void onHitBlock(ServerLevel serverLevel, ItemStack weapon, @Nullable LivingEntity owner, Entity entity, @Nullable EquipmentSlot slot, Vec3 hitLocation, BlockState hitBlock, Consumer<Item> onBreak) {
        EnchantedItemInUse item = new EnchantedItemInUse(weapon, slot, owner, onBreak);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, level) -> ((Enchantment)enchantment.value()).onHitBlock(serverLevel, level, item, entity, hitLocation, hitBlock));
    }

    public static int modifyDurabilityToRepairFromXp(ServerLevel serverLevel, ItemStack item, int durability) {
        MutableFloat modifiedDurability = new MutableFloat((float)durability);
        EnchantmentHelper.runIterationOnItem(item, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyDurabilityToRepairFromXp(serverLevel, level, item, modifiedDurability));
        return Math.max(0, modifiedDurability.intValue());
    }

    public static float processEquipmentDropChance(ServerLevel serverLevel, LivingEntity entity, DamageSource killingBlow, float chance) {
        MutableFloat modifiedChance = new MutableFloat(chance);
        RandomSource random = entity.getRandom();
        EnchantmentHelper.runIterationOnEquipment(entity, (enchantment, level, item) -> {
            LootContext context = Enchantment.damageContext(serverLevel, level, entity, killingBlow);
            ((Enchantment)enchantment.value()).getEffects(EnchantmentEffectComponents.EQUIPMENT_DROPS).forEach(filteredEffect -> {
                if (filteredEffect.enchanted() == EnchantmentTarget.VICTIM && filteredEffect.affected() == EnchantmentTarget.VICTIM && filteredEffect.matches(context)) {
                    modifiedChance.setValue(((EnchantmentValueEffect)filteredEffect.effect()).process(level, random, modifiedChance.floatValue()));
                }
            });
        });
        Entity attacker = killingBlow.getEntity();
        if (attacker instanceof LivingEntity) {
            LivingEntity livingAttacker = (LivingEntity)attacker;
            EnchantmentHelper.runIterationOnEquipment(livingAttacker, (enchantment, level, item) -> {
                LootContext context = Enchantment.damageContext(serverLevel, level, entity, killingBlow);
                ((Enchantment)enchantment.value()).getEffects(EnchantmentEffectComponents.EQUIPMENT_DROPS).forEach(filteredEffect -> {
                    if (filteredEffect.enchanted() == EnchantmentTarget.ATTACKER && filteredEffect.affected() == EnchantmentTarget.VICTIM && filteredEffect.matches(context)) {
                        modifiedChance.setValue(((EnchantmentValueEffect)filteredEffect.effect()).process(level, random, modifiedChance.floatValue()));
                    }
                });
            });
        }
        return modifiedChance.floatValue();
    }

    public static void forEachModifier(ItemStack itemStack, EquipmentSlotGroup slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach(effect -> {
            if (((Enchantment)enchantment.value()).definition().slots().contains(slot)) {
                consumer.accept(effect.attribute(), effect.getModifier(level, slot));
            }
        }));
    }

    public static void forEachModifier(ItemStack itemStack, EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        EnchantmentHelper.runIterationOnItem(itemStack, (enchantment, level) -> ((Enchantment)enchantment.value()).getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach(effect -> {
            if (((Enchantment)enchantment.value()).matchingSlot(slot)) {
                consumer.accept(effect.attribute(), effect.getModifier(level, slot));
            }
        }));
    }

    public static int getFishingLuckBonus(ServerLevel serverLevel, ItemStack rod, Entity fisher) {
        MutableFloat modifiedSpeed = new MutableFloat(0.0f);
        EnchantmentHelper.runIterationOnItem(rod, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyFishingLuckBonus(serverLevel, level, rod, fisher, modifiedSpeed));
        return Math.max(0, modifiedSpeed.intValue());
    }

    public static float getFishingTimeReduction(ServerLevel serverLevel, ItemStack rod, Entity fisher) {
        MutableFloat modifiedSpeed = new MutableFloat(0.0f);
        EnchantmentHelper.runIterationOnItem(rod, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyFishingTimeReduction(serverLevel, level, rod, fisher, modifiedSpeed));
        return Math.max(0.0f, modifiedSpeed.floatValue());
    }

    public static int getTridentReturnToOwnerAcceleration(ServerLevel serverLevel, ItemStack weapon, Entity trident) {
        MutableFloat modifiedAcceleration = new MutableFloat(0.0f);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyTridentReturnToOwnerAcceleration(serverLevel, level, weapon, trident, modifiedAcceleration));
        return Math.max(0, modifiedAcceleration.intValue());
    }

    public static float modifyCrossbowChargingTime(ItemStack crossbow, LivingEntity holder, float time) {
        MutableFloat modifiedTime = new MutableFloat(time);
        EnchantmentHelper.runIterationOnItem(crossbow, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyCrossbowChargeTime(holder.getRandom(), level, modifiedTime));
        return Math.max(0.0f, modifiedTime.floatValue());
    }

    public static float getTridentSpinAttackStrength(ItemStack trident, LivingEntity holder) {
        MutableFloat strength = new MutableFloat(0.0f);
        EnchantmentHelper.runIterationOnItem(trident, (enchantment, level) -> ((Enchantment)enchantment.value()).modifyTridentSpinAttackStrength(holder.getRandom(), level, strength));
        return strength.floatValue();
    }

    public static boolean hasTag(ItemStack item, TagKey<Enchantment> tag) {
        ItemEnchantments enchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder enchantment = (Holder)entry.getKey();
            if (!enchantment.is(tag)) continue;
            return true;
        }
        return false;
    }

    public static boolean has(ItemStack item, DataComponentType<?> effectType) {
        MutableBoolean found = new MutableBoolean(false);
        EnchantmentHelper.runIterationOnItem(item, (enchantment, level) -> {
            if (((Enchantment)enchantment.value()).effects().has(effectType)) {
                found.setTrue();
            }
        });
        return found.booleanValue();
    }

    public static <T> Optional<T> pickHighestLevel(ItemStack itemStack, DataComponentType<List<T>> componentType) {
        Pair<List<T>, Integer> picked = EnchantmentHelper.getHighestLevel(itemStack, componentType);
        if (picked != null) {
            List list = (List)picked.getFirst();
            int enchantmentLevel = (Integer)picked.getSecond();
            return Optional.of(list.get(Math.min(enchantmentLevel, list.size()) - 1));
        }
        return Optional.empty();
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public static <T> @Nullable Pair<T, Integer> getHighestLevel(ItemStack item, DataComponentType<T> effectType) {
        @Nullable MutableObject found = new MutableObject();
        EnchantmentHelper.runIterationOnItem(item, (enchantment, level) -> {
            Object effect;
            if ((found.get() == null || (Integer)((Pair)found.get()).getSecond() < level) && (effect = ((Enchantment)enchantment.value()).effects().get(effectType)) != null) {
                found.setValue((Object)Pair.of(effect, (Object)level));
            }
        });
        return (Pair)found.get();
    }

    public static Optional<EnchantedItemInUse> getRandomItemWith(DataComponentType<?> componentType, LivingEntity source, Predicate<ItemStack> predicate) {
        ArrayList<EnchantedItemInUse> items = new ArrayList<EnchantedItemInUse>();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack item = source.getItemBySlot(slot);
            if (!predicate.test(item)) continue;
            ItemEnchantments enchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Holder enchantment = (Holder)entry.getKey();
                if (!((Enchantment)enchantment.value()).effects().has(componentType) || !((Enchantment)enchantment.value()).matchingSlot(slot)) continue;
                items.add(new EnchantedItemInUse(item, slot, source));
            }
        }
        return Util.getRandomSafe(items, source.getRandom());
    }

    public static int getEnchantmentCost(RandomSource random, int slot, int bookcases, ItemStack itemStack) {
        Enchantable enchantable = itemStack.get(DataComponents.ENCHANTABLE);
        if (enchantable == null) {
            return 0;
        }
        if (bookcases > 15) {
            bookcases = 15;
        }
        int selected = random.nextInt(8) + 1 + (bookcases >> 1) + random.nextInt(bookcases + 1);
        if (slot == 0) {
            return Math.max(selected / 3, 1);
        }
        if (slot == 1) {
            return selected * 2 / 3 + 1;
        }
        return Math.max(selected, bookcases * 2);
    }

    public static ItemStack enchantItem(RandomSource random, ItemStack itemStack, int enchantmentCost, RegistryAccess registryAccess, Optional<? extends HolderSet<Enchantment>> set) {
        return EnchantmentHelper.enchantItem(random, itemStack, enchantmentCost, set.map(HolderSet::stream).orElseGet(() -> registryAccess.lookupOrThrow(Registries.ENCHANTMENT).listElements().map(h -> h)));
    }

    public static ItemStack enchantItem(RandomSource random, ItemStack itemStack, int enchantmentCost, Stream<Holder<Enchantment>> source) {
        List<EnchantmentInstance> enchants = EnchantmentHelper.selectEnchantment(random, itemStack, enchantmentCost, source);
        if (itemStack.is(Items.BOOK)) {
            itemStack = new ItemStack(Items.ENCHANTED_BOOK);
        }
        for (EnchantmentInstance enchant : enchants) {
            itemStack.enchant(enchant.enchantment(), enchant.level());
        }
        return itemStack;
    }

    public static List<EnchantmentInstance> selectEnchantment(RandomSource random, ItemStack itemStack, int enchantmentCost, Stream<Holder<Enchantment>> source) {
        ArrayList results = Lists.newArrayList();
        Enchantable enchantable = itemStack.get(DataComponents.ENCHANTABLE);
        if (enchantable == null) {
            return results;
        }
        enchantmentCost += 1 + random.nextInt(enchantable.value() / 4 + 1) + random.nextInt(enchantable.value() / 4 + 1);
        float randomSpan = (random.nextFloat() + random.nextFloat() - 1.0f) * 0.15f;
        List<EnchantmentInstance> enchantments = EnchantmentHelper.getAvailableEnchantmentResults(enchantmentCost = Mth.clamp(Math.round((float)enchantmentCost + (float)enchantmentCost * randomSpan), 1, Integer.MAX_VALUE), itemStack, source);
        if (!enchantments.isEmpty()) {
            WeightedRandom.getRandomItem(random, enchantments, EnchantmentInstance::weight).ifPresent(results::add);
            while (random.nextInt(50) <= enchantmentCost) {
                if (!results.isEmpty()) {
                    EnchantmentHelper.filterCompatibleEnchantments(enchantments, (EnchantmentInstance)results.getLast());
                }
                if (enchantments.isEmpty()) break;
                WeightedRandom.getRandomItem(random, enchantments, EnchantmentInstance::weight).ifPresent(results::add);
                enchantmentCost /= 2;
            }
        }
        return results;
    }

    public static void filterCompatibleEnchantments(List<EnchantmentInstance> enchants, EnchantmentInstance target) {
        enchants.removeIf(e -> !Enchantment.areCompatible(target.enchantment(), e.enchantment()));
    }

    public static boolean isEnchantmentCompatible(Collection<Holder<Enchantment>> enchants, Holder<Enchantment> target) {
        for (Holder<Enchantment> existing : enchants) {
            if (Enchantment.areCompatible(existing, target)) continue;
            return false;
        }
        return true;
    }

    public static List<EnchantmentInstance> getAvailableEnchantmentResults(int value, ItemStack itemStack, Stream<Holder<Enchantment>> source) {
        ArrayList results = Lists.newArrayList();
        boolean isBook = itemStack.is(Items.BOOK);
        source.filter(enchantment -> ((Enchantment)enchantment.value()).isPrimaryItem(itemStack) || isBook).forEach(holder -> {
            Enchantment enchantment = (Enchantment)holder.value();
            for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); --level) {
                if (value < enchantment.getMinCost(level) || value > enchantment.getMaxCost(level)) continue;
                results.add(new EnchantmentInstance((Holder<Enchantment>)holder, level));
                break;
            }
        });
        return results;
    }

    public static void enchantItemFromProvider(ItemStack itemStack, RegistryAccess registryAccess, ResourceKey<EnchantmentProvider> providerKey, DifficultyInstance difficulty, RandomSource random) {
        EnchantmentProvider provider = registryAccess.lookupOrThrow(Registries.ENCHANTMENT_PROVIDER).getValue(providerKey);
        if (provider != null) {
            EnchantmentHelper.updateEnchantments(itemStack, enchantments -> provider.enchant(itemStack, (ItemEnchantments.Mutable)enchantments, random, difficulty));
        }
    }

    @FunctionalInterface
    private static interface EnchantmentVisitor {
        public void accept(Holder<Enchantment> var1, int var2);
    }

    @FunctionalInterface
    private static interface EnchantmentInSlotVisitor {
        public void accept(Holder<Enchantment> var1, int var2, EnchantedItemInUse var3);
    }
}

