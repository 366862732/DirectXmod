/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.enchantment.providers;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.enchantment.providers.EnchantmentsByCostWithDifficulty;
import net.minecraft.world.item.enchantment.providers.SingleEnchantment;

public interface VanillaEnchantmentProviders {
    public static final ResourceKey<EnchantmentProvider> MOB_SPAWN_EQUIPMENT = VanillaEnchantmentProviders.create("mob_spawn_equipment");
    public static final ResourceKey<EnchantmentProvider> PILLAGER_SPAWN_CROSSBOW = VanillaEnchantmentProviders.create("pillager_spawn_crossbow");
    public static final ResourceKey<EnchantmentProvider> RAID_PILLAGER_POST_WAVE_3 = VanillaEnchantmentProviders.create("raid/pillager_post_wave_3");
    public static final ResourceKey<EnchantmentProvider> RAID_PILLAGER_POST_WAVE_5 = VanillaEnchantmentProviders.create("raid/pillager_post_wave_5");
    public static final ResourceKey<EnchantmentProvider> RAID_VINDICATOR = VanillaEnchantmentProviders.create("raid/vindicator");
    public static final ResourceKey<EnchantmentProvider> RAID_VINDICATOR_POST_WAVE_5 = VanillaEnchantmentProviders.create("raid/vindicator_post_wave_5");
    public static final ResourceKey<EnchantmentProvider> ENDERMAN_LOOT_DROP = VanillaEnchantmentProviders.create("enderman_loot_drop");

    public static void bootstrap(BootstrapContext<EnchantmentProvider> context) {
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
        context.register(MOB_SPAWN_EQUIPMENT, new EnchantmentsByCostWithDifficulty(enchantments.getOrThrow(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT), 5, 17));
        context.register(PILLAGER_SPAWN_CROSSBOW, new SingleEnchantment(enchantments.getOrThrow(Enchantments.PIERCING), ConstantInt.of(1)));
        context.register(RAID_PILLAGER_POST_WAVE_3, new SingleEnchantment(enchantments.getOrThrow(Enchantments.QUICK_CHARGE), ConstantInt.of(1)));
        context.register(RAID_PILLAGER_POST_WAVE_5, new SingleEnchantment(enchantments.getOrThrow(Enchantments.QUICK_CHARGE), ConstantInt.of(2)));
        context.register(RAID_VINDICATOR, new SingleEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS), ConstantInt.of(1)));
        context.register(RAID_VINDICATOR_POST_WAVE_5, new SingleEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS), ConstantInt.of(2)));
        context.register(ENDERMAN_LOOT_DROP, new SingleEnchantment(enchantments.getOrThrow(Enchantments.SILK_TOUCH), ConstantInt.of(1)));
    }

    public static ResourceKey<EnchantmentProvider> create(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT_PROVIDER, Identifier.withDefaultNamespace(id));
    }
}

