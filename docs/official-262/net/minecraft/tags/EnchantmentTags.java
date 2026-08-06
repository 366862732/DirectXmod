/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public interface EnchantmentTags {
    public static final TagKey<Enchantment> TOOLTIP_ORDER = EnchantmentTags.create("tooltip_order");
    public static final TagKey<Enchantment> ARMOR_EXCLUSIVE = EnchantmentTags.create("exclusive_set/armor");
    public static final TagKey<Enchantment> BOOTS_EXCLUSIVE = EnchantmentTags.create("exclusive_set/boots");
    public static final TagKey<Enchantment> BOW_EXCLUSIVE = EnchantmentTags.create("exclusive_set/bow");
    public static final TagKey<Enchantment> CROSSBOW_EXCLUSIVE = EnchantmentTags.create("exclusive_set/crossbow");
    public static final TagKey<Enchantment> DAMAGE_EXCLUSIVE = EnchantmentTags.create("exclusive_set/damage");
    public static final TagKey<Enchantment> MINING_EXCLUSIVE = EnchantmentTags.create("exclusive_set/mining");
    public static final TagKey<Enchantment> RIPTIDE_EXCLUSIVE = EnchantmentTags.create("exclusive_set/riptide");
    public static final TagKey<Enchantment> TRADEABLE = EnchantmentTags.create("tradeable");
    public static final TagKey<Enchantment> DOUBLE_TRADE_PRICE = EnchantmentTags.create("double_trade_price");
    public static final TagKey<Enchantment> IN_ENCHANTING_TABLE = EnchantmentTags.create("in_enchanting_table");
    public static final TagKey<Enchantment> ON_MOB_SPAWN_EQUIPMENT = EnchantmentTags.create("on_mob_spawn_equipment");
    public static final TagKey<Enchantment> ON_TRADED_EQUIPMENT = EnchantmentTags.create("on_traded_equipment");
    public static final TagKey<Enchantment> ON_RANDOM_LOOT = EnchantmentTags.create("on_random_loot");
    public static final TagKey<Enchantment> CURSE = EnchantmentTags.create("curse");
    public static final TagKey<Enchantment> SMELTS_LOOT = EnchantmentTags.create("smelts_loot");
    public static final TagKey<Enchantment> PREVENTS_BEE_SPAWNS_WHEN_MINING = EnchantmentTags.create("prevents_bee_spawns_when_mining");
    public static final TagKey<Enchantment> PREVENTS_DECORATED_POT_SHATTERING = EnchantmentTags.create("prevents_decorated_pot_shattering");
    public static final TagKey<Enchantment> PREVENTS_ICE_MELTING = EnchantmentTags.create("prevents_ice_melting");
    public static final TagKey<Enchantment> PREVENTS_INFESTED_SPAWNS = EnchantmentTags.create("prevents_infested_spawns");
    public static final TagKey<Enchantment> TREASURE = EnchantmentTags.create("treasure");
    public static final TagKey<Enchantment> NON_TREASURE = EnchantmentTags.create("non_treasure");
    public static final TagKey<Enchantment> TRADES_DESERT_COMMON = EnchantmentTags.create("trades/desert_common");
    public static final TagKey<Enchantment> TRADES_JUNGLE_COMMON = EnchantmentTags.create("trades/jungle_common");
    public static final TagKey<Enchantment> TRADES_PLAINS_COMMON = EnchantmentTags.create("trades/plains_common");
    public static final TagKey<Enchantment> TRADES_SAVANNA_COMMON = EnchantmentTags.create("trades/savanna_common");
    public static final TagKey<Enchantment> TRADES_SNOW_COMMON = EnchantmentTags.create("trades/snow_common");
    public static final TagKey<Enchantment> TRADES_SWAMP_COMMON = EnchantmentTags.create("trades/swamp_common");
    public static final TagKey<Enchantment> TRADES_TAIGA_COMMON = EnchantmentTags.create("trades/taiga_common");

    private static TagKey<Enchantment> create(String name) {
        return TagKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(name));
    }
}

