/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.storage.loot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.storage.loot.LootTable;

public class BuiltInLootTables {
    private static final Set<ResourceKey<LootTable>> LOCATIONS = new HashSet<ResourceKey<LootTable>>();
    private static final Set<ResourceKey<LootTable>> IMMUTABLE_LOCATIONS = Collections.unmodifiableSet(LOCATIONS);
    public static final ResourceKey<LootTable> SPAWN_BONUS_CHEST = BuiltInLootTables.register("chests/spawn_bonus_chest");
    public static final ResourceKey<LootTable> END_CITY_TREASURE = BuiltInLootTables.register("chests/end_city_treasure");
    public static final ResourceKey<LootTable> SIMPLE_DUNGEON = BuiltInLootTables.register("chests/simple_dungeon");
    public static final ResourceKey<LootTable> VILLAGE_WEAPONSMITH = BuiltInLootTables.register("chests/village/village_weaponsmith");
    public static final ResourceKey<LootTable> VILLAGE_TOOLSMITH = BuiltInLootTables.register("chests/village/village_toolsmith");
    public static final ResourceKey<LootTable> VILLAGE_ARMORER = BuiltInLootTables.register("chests/village/village_armorer");
    public static final ResourceKey<LootTable> VILLAGE_CARTOGRAPHER = BuiltInLootTables.register("chests/village/village_cartographer");
    public static final ResourceKey<LootTable> VILLAGE_MASON = BuiltInLootTables.register("chests/village/village_mason");
    public static final ResourceKey<LootTable> VILLAGE_SHEPHERD = BuiltInLootTables.register("chests/village/village_shepherd");
    public static final ResourceKey<LootTable> VILLAGE_BUTCHER = BuiltInLootTables.register("chests/village/village_butcher");
    public static final ResourceKey<LootTable> VILLAGE_FLETCHER = BuiltInLootTables.register("chests/village/village_fletcher");
    public static final ResourceKey<LootTable> VILLAGE_FISHER = BuiltInLootTables.register("chests/village/village_fisher");
    public static final ResourceKey<LootTable> VILLAGE_TANNERY = BuiltInLootTables.register("chests/village/village_tannery");
    public static final ResourceKey<LootTable> VILLAGE_TEMPLE = BuiltInLootTables.register("chests/village/village_temple");
    public static final ResourceKey<LootTable> VILLAGE_DESERT_HOUSE = BuiltInLootTables.register("chests/village/village_desert_house");
    public static final ResourceKey<LootTable> VILLAGE_PLAINS_HOUSE = BuiltInLootTables.register("chests/village/village_plains_house");
    public static final ResourceKey<LootTable> VILLAGE_TAIGA_HOUSE = BuiltInLootTables.register("chests/village/village_taiga_house");
    public static final ResourceKey<LootTable> VILLAGE_SNOWY_HOUSE = BuiltInLootTables.register("chests/village/village_snowy_house");
    public static final ResourceKey<LootTable> VILLAGE_SAVANNA_HOUSE = BuiltInLootTables.register("chests/village/village_savanna_house");
    public static final ResourceKey<LootTable> ABANDONED_MINESHAFT = BuiltInLootTables.register("chests/abandoned_mineshaft");
    public static final ResourceKey<LootTable> NETHER_BRIDGE = BuiltInLootTables.register("chests/nether_bridge");
    public static final ResourceKey<LootTable> STRONGHOLD_LIBRARY = BuiltInLootTables.register("chests/stronghold_library");
    public static final ResourceKey<LootTable> STRONGHOLD_CROSSING = BuiltInLootTables.register("chests/stronghold_crossing");
    public static final ResourceKey<LootTable> STRONGHOLD_CORRIDOR = BuiltInLootTables.register("chests/stronghold_corridor");
    public static final ResourceKey<LootTable> DESERT_PYRAMID = BuiltInLootTables.register("chests/desert_pyramid");
    public static final ResourceKey<LootTable> JUNGLE_TEMPLE = BuiltInLootTables.register("chests/jungle_temple");
    public static final ResourceKey<LootTable> JUNGLE_TEMPLE_DISPENSER = BuiltInLootTables.register("chests/jungle_temple_dispenser");
    public static final ResourceKey<LootTable> IGLOO_CHEST = BuiltInLootTables.register("chests/igloo_chest");
    public static final ResourceKey<LootTable> WOODLAND_MANSION = BuiltInLootTables.register("chests/woodland_mansion");
    public static final ResourceKey<LootTable> UNDERWATER_RUIN_SMALL = BuiltInLootTables.register("chests/underwater_ruin_small");
    public static final ResourceKey<LootTable> UNDERWATER_RUIN_BIG = BuiltInLootTables.register("chests/underwater_ruin_big");
    public static final ResourceKey<LootTable> BURIED_TREASURE = BuiltInLootTables.register("chests/buried_treasure");
    public static final ResourceKey<LootTable> SHIPWRECK_MAP = BuiltInLootTables.register("chests/shipwreck_map");
    public static final ResourceKey<LootTable> SHIPWRECK_SUPPLY = BuiltInLootTables.register("chests/shipwreck_supply");
    public static final ResourceKey<LootTable> SHIPWRECK_TREASURE = BuiltInLootTables.register("chests/shipwreck_treasure");
    public static final ResourceKey<LootTable> PILLAGER_OUTPOST = BuiltInLootTables.register("chests/pillager_outpost");
    public static final ResourceKey<LootTable> BASTION_TREASURE = BuiltInLootTables.register("chests/bastion_treasure");
    public static final ResourceKey<LootTable> BASTION_OTHER = BuiltInLootTables.register("chests/bastion_other");
    public static final ResourceKey<LootTable> BASTION_BRIDGE = BuiltInLootTables.register("chests/bastion_bridge");
    public static final ResourceKey<LootTable> BASTION_HOGLIN_STABLE = BuiltInLootTables.register("chests/bastion_hoglin_stable");
    public static final ResourceKey<LootTable> ANCIENT_CITY = BuiltInLootTables.register("chests/ancient_city");
    public static final ResourceKey<LootTable> ANCIENT_CITY_ICE_BOX = BuiltInLootTables.register("chests/ancient_city_ice_box");
    public static final ResourceKey<LootTable> RUINED_PORTAL = BuiltInLootTables.register("chests/ruined_portal");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD = BuiltInLootTables.register("chests/trial_chambers/reward");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD_COMMON = BuiltInLootTables.register("chests/trial_chambers/reward_common");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD_RARE = BuiltInLootTables.register("chests/trial_chambers/reward_rare");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD_UNIQUE = BuiltInLootTables.register("chests/trial_chambers/reward_unique");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD_OMINOUS = BuiltInLootTables.register("chests/trial_chambers/reward_ominous");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD_OMINOUS_COMMON = BuiltInLootTables.register("chests/trial_chambers/reward_ominous_common");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD_OMINOUS_RARE = BuiltInLootTables.register("chests/trial_chambers/reward_ominous_rare");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE = BuiltInLootTables.register("chests/trial_chambers/reward_ominous_unique");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_SUPPLY = BuiltInLootTables.register("chests/trial_chambers/supply");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_CORRIDOR = BuiltInLootTables.register("chests/trial_chambers/corridor");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_INTERSECTION = BuiltInLootTables.register("chests/trial_chambers/intersection");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_INTERSECTION_BARREL = BuiltInLootTables.register("chests/trial_chambers/intersection_barrel");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_ENTRANCE = BuiltInLootTables.register("chests/trial_chambers/entrance");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_CORRIDOR_DISPENSER = BuiltInLootTables.register("dispensers/trial_chambers/corridor");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_CHAMBER_DISPENSER = BuiltInLootTables.register("dispensers/trial_chambers/chamber");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_WATER_DISPENSER = BuiltInLootTables.register("dispensers/trial_chambers/water");
    public static final ResourceKey<LootTable> TRIAL_CHAMBERS_CORRIDOR_POT = BuiltInLootTables.register("pots/trial_chambers/corridor");
    public static final ResourceKey<LootTable> EQUIPMENT_TRIAL_CHAMBER = BuiltInLootTables.register("equipment/trial_chamber");
    public static final ResourceKey<LootTable> EQUIPMENT_TRIAL_CHAMBER_RANGED = BuiltInLootTables.register("equipment/trial_chamber_ranged");
    public static final ResourceKey<LootTable> EQUIPMENT_TRIAL_CHAMBER_MELEE = BuiltInLootTables.register("equipment/trial_chamber_melee");
    public static final ColorCollection<ResourceKey<LootTable>> SHEEP = ColorCollection.NAMES.map(color -> BuiltInLootTables.register("entities/sheep/" + color));
    public static final ResourceKey<LootTable> FISHING = BuiltInLootTables.register("gameplay/fishing");
    public static final ResourceKey<LootTable> FISHING_JUNK = BuiltInLootTables.register("gameplay/fishing/junk");
    public static final ResourceKey<LootTable> FISHING_TREASURE = BuiltInLootTables.register("gameplay/fishing/treasure");
    public static final ResourceKey<LootTable> FISHING_FISH = BuiltInLootTables.register("gameplay/fishing/fish");
    public static final ResourceKey<LootTable> CAT_MORNING_GIFT = BuiltInLootTables.register("gameplay/cat_morning_gift");
    public static final ResourceKey<LootTable> ARMORER_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/armorer_gift");
    public static final ResourceKey<LootTable> BUTCHER_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/butcher_gift");
    public static final ResourceKey<LootTable> CARTOGRAPHER_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/cartographer_gift");
    public static final ResourceKey<LootTable> CLERIC_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/cleric_gift");
    public static final ResourceKey<LootTable> FARMER_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/farmer_gift");
    public static final ResourceKey<LootTable> FISHERMAN_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/fisherman_gift");
    public static final ResourceKey<LootTable> FLETCHER_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/fletcher_gift");
    public static final ResourceKey<LootTable> LEATHERWORKER_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/leatherworker_gift");
    public static final ResourceKey<LootTable> LIBRARIAN_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/librarian_gift");
    public static final ResourceKey<LootTable> MASON_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/mason_gift");
    public static final ResourceKey<LootTable> SHEPHERD_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/shepherd_gift");
    public static final ResourceKey<LootTable> TOOLSMITH_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/toolsmith_gift");
    public static final ResourceKey<LootTable> WEAPONSMITH_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/weaponsmith_gift");
    public static final ResourceKey<LootTable> UNEMPLOYED_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/unemployed_gift");
    public static final ResourceKey<LootTable> BABY_VILLAGER_GIFT = BuiltInLootTables.register("gameplay/hero_of_the_village/baby_gift");
    public static final ResourceKey<LootTable> SNIFFER_DIGGING = BuiltInLootTables.register("gameplay/sniffer_digging");
    public static final ResourceKey<LootTable> PANDA_SNEEZE = BuiltInLootTables.register("gameplay/panda_sneeze");
    public static final ResourceKey<LootTable> CHICKEN_LAY = BuiltInLootTables.register("gameplay/chicken_lay");
    public static final ResourceKey<LootTable> ARMADILLO_SHED = BuiltInLootTables.register("gameplay/armadillo_shed");
    public static final ResourceKey<LootTable> TURTLE_GROW = BuiltInLootTables.register("gameplay/turtle_grow");
    public static final ResourceKey<LootTable> HARVEST_CAVE_VINE = BuiltInLootTables.register("harvest/cave_vine");
    public static final ResourceKey<LootTable> HARVEST_SWEET_BERRY_BUSH = BuiltInLootTables.register("harvest/sweet_berry_bush");
    public static final ResourceKey<LootTable> HARVEST_BEEHIVE = BuiltInLootTables.register("harvest/beehive");
    public static final ResourceKey<LootTable> CARVE_PUMPKIN = BuiltInLootTables.register("carve/pumpkin");
    public static final ResourceKey<LootTable> PIGLIN_BARTERING = BuiltInLootTables.register("gameplay/piglin_bartering");
    public static final ResourceKey<LootTable> SPAWNER_TRIAL_CHAMBER_KEY = BuiltInLootTables.register("spawners/trial_chamber/key");
    public static final ResourceKey<LootTable> SPAWNER_TRIAL_CHAMBER_CONSUMABLES = BuiltInLootTables.register("spawners/trial_chamber/consumables");
    public static final ResourceKey<LootTable> SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY = BuiltInLootTables.register("spawners/ominous/trial_chamber/key");
    public static final ResourceKey<LootTable> SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES = BuiltInLootTables.register("spawners/ominous/trial_chamber/consumables");
    public static final ResourceKey<LootTable> SPAWNER_TRIAL_ITEMS_TO_DROP_WHEN_OMINOUS = BuiltInLootTables.register("spawners/trial_chamber/items_to_drop_when_ominous");
    public static final ResourceKey<LootTable> ARMADILLO_BRUSH = BuiltInLootTables.register("brush/armadillo");
    public static final ResourceKey<LootTable> BOGGED_SHEAR = BuiltInLootTables.register("shearing/bogged");
    public static final ResourceKey<LootTable> SHEAR_MOOSHROOM = BuiltInLootTables.register("shearing/mooshroom");
    public static final ResourceKey<LootTable> SHEAR_RED_MOOSHROOM = BuiltInLootTables.register("shearing/mooshroom/red");
    public static final ResourceKey<LootTable> SHEAR_BROWN_MOOSHROOM = BuiltInLootTables.register("shearing/mooshroom/brown");
    public static final ResourceKey<LootTable> SHEAR_SNOW_GOLEM = BuiltInLootTables.register("shearing/snow_golem");
    public static final ResourceKey<LootTable> SHEAR_SHEEP = BuiltInLootTables.register("shearing/sheep");
    public static final ColorCollection<ResourceKey<LootTable>> SHEAR_DYED_SHEEP = ColorCollection.NAMES.map(color -> BuiltInLootTables.register("shearing/sheep/" + color));
    public static final ResourceKey<LootTable> CHARGED_CREEPER = BuiltInLootTables.register("charged_creeper/root");
    public static final ResourceKey<LootTable> CHARGED_CREEPER_PIGLIN = BuiltInLootTables.register("charged_creeper/piglin");
    public static final ResourceKey<LootTable> CHARGED_CREEPER_CREEPER = BuiltInLootTables.register("charged_creeper/creeper");
    public static final ResourceKey<LootTable> CHARGED_CREEPER_SKELETON = BuiltInLootTables.register("charged_creeper/skeleton");
    public static final ResourceKey<LootTable> CHARGED_CREEPER_WITHER_SKELETON = BuiltInLootTables.register("charged_creeper/wither_skeleton");
    public static final ResourceKey<LootTable> CHARGED_CREEPER_ZOMBIE = BuiltInLootTables.register("charged_creeper/zombie");
    public static final ResourceKey<LootTable> DESERT_WELL_ARCHAEOLOGY = BuiltInLootTables.register("archaeology/desert_well");
    public static final ResourceKey<LootTable> DESERT_PYRAMID_ARCHAEOLOGY = BuiltInLootTables.register("archaeology/desert_pyramid");
    public static final ResourceKey<LootTable> TRAIL_RUINS_ARCHAEOLOGY_COMMON = BuiltInLootTables.register("archaeology/trail_ruins_common");
    public static final ResourceKey<LootTable> TRAIL_RUINS_ARCHAEOLOGY_RARE = BuiltInLootTables.register("archaeology/trail_ruins_rare");
    public static final ResourceKey<LootTable> OCEAN_RUIN_WARM_ARCHAEOLOGY = BuiltInLootTables.register("archaeology/ocean_ruin_warm");
    public static final ResourceKey<LootTable> OCEAN_RUIN_COLD_ARCHAEOLOGY = BuiltInLootTables.register("archaeology/ocean_ruin_cold");

    private static ResourceKey<LootTable> register(String location) {
        return BuiltInLootTables.register(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(location)));
    }

    private static ResourceKey<LootTable> register(ResourceKey<LootTable> location) {
        if (LOCATIONS.add(location)) {
            return location;
        }
        throw new IllegalArgumentException(String.valueOf(location.identifier()) + " is already a registered built-in loot table");
    }

    public static Set<ResourceKey<LootTable>> all() {
        return IMMUTABLE_LOCATIONS;
    }
}

