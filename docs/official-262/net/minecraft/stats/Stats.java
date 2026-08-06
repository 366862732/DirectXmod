/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.stats;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class Stats {
    public static final StatType<Block> BLOCK_MINED = Stats.makeRegistryStatType("mined", BuiltInRegistries.BLOCK);
    public static final StatType<Item> ITEM_CRAFTED = Stats.makeRegistryStatType("crafted", BuiltInRegistries.ITEM);
    public static final StatType<Item> ITEM_USED = Stats.makeRegistryStatType("used", BuiltInRegistries.ITEM);
    public static final StatType<Item> ITEM_BROKEN = Stats.makeRegistryStatType("broken", BuiltInRegistries.ITEM);
    public static final StatType<Item> ITEM_PICKED_UP = Stats.makeRegistryStatType("picked_up", BuiltInRegistries.ITEM);
    public static final StatType<Item> ITEM_DROPPED = Stats.makeRegistryStatType("dropped", BuiltInRegistries.ITEM);
    public static final StatType<EntityType<?>> ENTITY_KILLED = Stats.makeRegistryStatType("killed", BuiltInRegistries.ENTITY_TYPE);
    public static final StatType<EntityType<?>> ENTITY_KILLED_BY = Stats.makeRegistryStatType("killed_by", BuiltInRegistries.ENTITY_TYPE);
    public static final StatType<Identifier> CUSTOM = Stats.makeRegistryStatType("custom", BuiltInRegistries.CUSTOM_STAT);
    public static final Identifier LEAVE_GAME = Stats.makeCustomStat("leave_game", StatFormatter.DEFAULT);
    public static final Identifier PLAY_TIME = Stats.makeCustomStat("play_time", StatFormatter.TIME);
    public static final Identifier TOTAL_WORLD_TIME = Stats.makeCustomStat("total_world_time", StatFormatter.TIME);
    public static final Identifier TIME_SINCE_DEATH = Stats.makeCustomStat("time_since_death", StatFormatter.TIME);
    public static final Identifier TIME_SINCE_REST = Stats.makeCustomStat("time_since_rest", StatFormatter.TIME);
    public static final Identifier CROUCH_TIME = Stats.makeCustomStat("sneak_time", StatFormatter.TIME);
    public static final Identifier WALK_ONE_CM = Stats.makeCustomStat("walk_one_cm", StatFormatter.DISTANCE);
    public static final Identifier CROUCH_ONE_CM = Stats.makeCustomStat("crouch_one_cm", StatFormatter.DISTANCE);
    public static final Identifier SPRINT_ONE_CM = Stats.makeCustomStat("sprint_one_cm", StatFormatter.DISTANCE);
    public static final Identifier WALK_ON_WATER_ONE_CM = Stats.makeCustomStat("walk_on_water_one_cm", StatFormatter.DISTANCE);
    public static final Identifier FALL_ONE_CM = Stats.makeCustomStat("fall_one_cm", StatFormatter.DISTANCE);
    public static final Identifier CLIMB_ONE_CM = Stats.makeCustomStat("climb_one_cm", StatFormatter.DISTANCE);
    public static final Identifier FLY_ONE_CM = Stats.makeCustomStat("fly_one_cm", StatFormatter.DISTANCE);
    public static final Identifier WALK_UNDER_WATER_ONE_CM = Stats.makeCustomStat("walk_under_water_one_cm", StatFormatter.DISTANCE);
    public static final Identifier MINECART_ONE_CM = Stats.makeCustomStat("minecart_one_cm", StatFormatter.DISTANCE);
    public static final Identifier BOAT_ONE_CM = Stats.makeCustomStat("boat_one_cm", StatFormatter.DISTANCE);
    public static final Identifier PIG_ONE_CM = Stats.makeCustomStat("pig_one_cm", StatFormatter.DISTANCE);
    public static final Identifier HAPPY_GHAST_ONE_CM = Stats.makeCustomStat("happy_ghast_one_cm", StatFormatter.DISTANCE);
    public static final Identifier HORSE_ONE_CM = Stats.makeCustomStat("horse_one_cm", StatFormatter.DISTANCE);
    public static final Identifier AVIATE_ONE_CM = Stats.makeCustomStat("aviate_one_cm", StatFormatter.DISTANCE);
    public static final Identifier SWIM_ONE_CM = Stats.makeCustomStat("swim_one_cm", StatFormatter.DISTANCE);
    public static final Identifier STRIDER_ONE_CM = Stats.makeCustomStat("strider_one_cm", StatFormatter.DISTANCE);
    public static final Identifier NAUTILUS_ONE_CM = Stats.makeCustomStat("nautilus_one_cm", StatFormatter.DISTANCE);
    public static final Identifier JUMP = Stats.makeCustomStat("jump", StatFormatter.DEFAULT);
    public static final Identifier DROP = Stats.makeCustomStat("drop", StatFormatter.DEFAULT);
    public static final Identifier DAMAGE_DEALT = Stats.makeCustomStat("damage_dealt", StatFormatter.DIVIDE_BY_TEN);
    public static final Identifier DAMAGE_DEALT_ABSORBED = Stats.makeCustomStat("damage_dealt_absorbed", StatFormatter.DIVIDE_BY_TEN);
    public static final Identifier DAMAGE_DEALT_RESISTED = Stats.makeCustomStat("damage_dealt_resisted", StatFormatter.DIVIDE_BY_TEN);
    public static final Identifier DAMAGE_TAKEN = Stats.makeCustomStat("damage_taken", StatFormatter.DIVIDE_BY_TEN);
    public static final Identifier DAMAGE_BLOCKED_BY_SHIELD = Stats.makeCustomStat("damage_blocked_by_shield", StatFormatter.DIVIDE_BY_TEN);
    public static final Identifier DAMAGE_ABSORBED = Stats.makeCustomStat("damage_absorbed", StatFormatter.DIVIDE_BY_TEN);
    public static final Identifier DAMAGE_RESISTED = Stats.makeCustomStat("damage_resisted", StatFormatter.DIVIDE_BY_TEN);
    public static final Identifier DEATHS = Stats.makeCustomStat("deaths", StatFormatter.DEFAULT);
    public static final Identifier MOB_KILLS = Stats.makeCustomStat("mob_kills", StatFormatter.DEFAULT);
    public static final Identifier ANIMALS_BRED = Stats.makeCustomStat("animals_bred", StatFormatter.DEFAULT);
    public static final Identifier PLAYER_KILLS = Stats.makeCustomStat("player_kills", StatFormatter.DEFAULT);
    public static final Identifier FISH_CAUGHT = Stats.makeCustomStat("fish_caught", StatFormatter.DEFAULT);
    public static final Identifier TALKED_TO_VILLAGER = Stats.makeCustomStat("talked_to_villager", StatFormatter.DEFAULT);
    public static final Identifier TRADED_WITH_VILLAGER = Stats.makeCustomStat("traded_with_villager", StatFormatter.DEFAULT);
    public static final Identifier EAT_CAKE_SLICE = Stats.makeCustomStat("eat_cake_slice", StatFormatter.DEFAULT);
    public static final Identifier FILL_CAULDRON = Stats.makeCustomStat("fill_cauldron", StatFormatter.DEFAULT);
    public static final Identifier USE_CAULDRON = Stats.makeCustomStat("use_cauldron", StatFormatter.DEFAULT);
    public static final Identifier CLEAN_ARMOR = Stats.makeCustomStat("clean_armor", StatFormatter.DEFAULT);
    public static final Identifier CLEAN_BANNER = Stats.makeCustomStat("clean_banner", StatFormatter.DEFAULT);
    public static final Identifier CLEAN_SHULKER_BOX = Stats.makeCustomStat("clean_shulker_box", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_BREWINGSTAND = Stats.makeCustomStat("interact_with_brewingstand", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_BEACON = Stats.makeCustomStat("interact_with_beacon", StatFormatter.DEFAULT);
    public static final Identifier INSPECT_DROPPER = Stats.makeCustomStat("inspect_dropper", StatFormatter.DEFAULT);
    public static final Identifier INSPECT_HOPPER = Stats.makeCustomStat("inspect_hopper", StatFormatter.DEFAULT);
    public static final Identifier INSPECT_DISPENSER = Stats.makeCustomStat("inspect_dispenser", StatFormatter.DEFAULT);
    public static final Identifier PLAY_NOTEBLOCK = Stats.makeCustomStat("play_noteblock", StatFormatter.DEFAULT);
    public static final Identifier TUNE_NOTEBLOCK = Stats.makeCustomStat("tune_noteblock", StatFormatter.DEFAULT);
    public static final Identifier POT_FLOWER = Stats.makeCustomStat("pot_flower", StatFormatter.DEFAULT);
    public static final Identifier TRIGGER_TRAPPED_CHEST = Stats.makeCustomStat("trigger_trapped_chest", StatFormatter.DEFAULT);
    public static final Identifier OPEN_ENDERCHEST = Stats.makeCustomStat("open_enderchest", StatFormatter.DEFAULT);
    public static final Identifier ENCHANT_ITEM = Stats.makeCustomStat("enchant_item", StatFormatter.DEFAULT);
    public static final Identifier PLAY_RECORD = Stats.makeCustomStat("play_record", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_FURNACE = Stats.makeCustomStat("interact_with_furnace", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_CRAFTING_TABLE = Stats.makeCustomStat("interact_with_crafting_table", StatFormatter.DEFAULT);
    public static final Identifier OPEN_CHEST = Stats.makeCustomStat("open_chest", StatFormatter.DEFAULT);
    public static final Identifier SLEEP_IN_BED = Stats.makeCustomStat("sleep_in_bed", StatFormatter.DEFAULT);
    public static final Identifier OPEN_SHULKER_BOX = Stats.makeCustomStat("open_shulker_box", StatFormatter.DEFAULT);
    public static final Identifier OPEN_BARREL = Stats.makeCustomStat("open_barrel", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_BLAST_FURNACE = Stats.makeCustomStat("interact_with_blast_furnace", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_SMOKER = Stats.makeCustomStat("interact_with_smoker", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_LECTERN = Stats.makeCustomStat("interact_with_lectern", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_CAMPFIRE = Stats.makeCustomStat("interact_with_campfire", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_CARTOGRAPHY_TABLE = Stats.makeCustomStat("interact_with_cartography_table", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_LOOM = Stats.makeCustomStat("interact_with_loom", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_STONECUTTER = Stats.makeCustomStat("interact_with_stonecutter", StatFormatter.DEFAULT);
    public static final Identifier BELL_RING = Stats.makeCustomStat("bell_ring", StatFormatter.DEFAULT);
    public static final Identifier RAID_TRIGGER = Stats.makeCustomStat("raid_trigger", StatFormatter.DEFAULT);
    public static final Identifier RAID_WIN = Stats.makeCustomStat("raid_win", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_ANVIL = Stats.makeCustomStat("interact_with_anvil", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_GRINDSTONE = Stats.makeCustomStat("interact_with_grindstone", StatFormatter.DEFAULT);
    public static final Identifier TARGET_HIT = Stats.makeCustomStat("target_hit", StatFormatter.DEFAULT);
    public static final Identifier INTERACT_WITH_SMITHING_TABLE = Stats.makeCustomStat("interact_with_smithing_table", StatFormatter.DEFAULT);

    private static Identifier makeCustomStat(String id, StatFormatter formatter) {
        Identifier location = Identifier.withDefaultNamespace(id);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, id, location);
        CUSTOM.get(location, formatter);
        return location;
    }

    private static <T> StatType<T> makeRegistryStatType(String name, Registry<T> registry) {
        MutableComponent displayName = Component.translatable("stat_type.minecraft." + name);
        return Registry.register(BuiltInRegistries.STAT_TYPE, name, new StatType<T>(registry, displayName));
    }
}

