/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityTypeIds {
    public static final ResourceKey<BlockEntityType<?>> FURNACE = BlockEntityTypeIds.create("furnace");
    public static final ResourceKey<BlockEntityType<?>> CHEST = BlockEntityTypeIds.create("chest");
    public static final ResourceKey<BlockEntityType<?>> TRAPPED_CHEST = BlockEntityTypeIds.create("trapped_chest");
    public static final ResourceKey<BlockEntityType<?>> ENDER_CHEST = BlockEntityTypeIds.create("ender_chest");
    public static final ResourceKey<BlockEntityType<?>> JUKEBOX = BlockEntityTypeIds.create("jukebox");
    public static final ResourceKey<BlockEntityType<?>> DISPENSER = BlockEntityTypeIds.create("dispenser");
    public static final ResourceKey<BlockEntityType<?>> DROPPER = BlockEntityTypeIds.create("dropper");
    public static final ResourceKey<BlockEntityType<?>> SIGN = BlockEntityTypeIds.create("sign");
    public static final ResourceKey<BlockEntityType<?>> HANGING_SIGN = BlockEntityTypeIds.create("hanging_sign");
    public static final ResourceKey<BlockEntityType<?>> MOB_SPAWNER = BlockEntityTypeIds.create("mob_spawner");
    public static final ResourceKey<BlockEntityType<?>> CREAKING_HEART = BlockEntityTypeIds.create("creaking_heart");
    public static final ResourceKey<BlockEntityType<?>> PISTON = BlockEntityTypeIds.create("piston");
    public static final ResourceKey<BlockEntityType<?>> BREWING_STAND = BlockEntityTypeIds.create("brewing_stand");
    public static final ResourceKey<BlockEntityType<?>> ENCHANTING_TABLE = BlockEntityTypeIds.create("enchanting_table");
    public static final ResourceKey<BlockEntityType<?>> END_PORTAL = BlockEntityTypeIds.create("end_portal");
    public static final ResourceKey<BlockEntityType<?>> BEACON = BlockEntityTypeIds.create("beacon");
    public static final ResourceKey<BlockEntityType<?>> SKULL = BlockEntityTypeIds.create("skull");
    public static final ResourceKey<BlockEntityType<?>> DAYLIGHT_DETECTOR = BlockEntityTypeIds.create("daylight_detector");
    public static final ResourceKey<BlockEntityType<?>> HOPPER = BlockEntityTypeIds.create("hopper");
    public static final ResourceKey<BlockEntityType<?>> COMPARATOR = BlockEntityTypeIds.create("comparator");
    public static final ResourceKey<BlockEntityType<?>> BANNER = BlockEntityTypeIds.create("banner");
    public static final ResourceKey<BlockEntityType<?>> STRUCTURE_BLOCK = BlockEntityTypeIds.create("structure_block");
    public static final ResourceKey<BlockEntityType<?>> END_GATEWAY = BlockEntityTypeIds.create("end_gateway");
    public static final ResourceKey<BlockEntityType<?>> COMMAND_BLOCK = BlockEntityTypeIds.create("command_block");
    public static final ResourceKey<BlockEntityType<?>> SHULKER_BOX = BlockEntityTypeIds.create("shulker_box");
    public static final ResourceKey<BlockEntityType<?>> CONDUIT = BlockEntityTypeIds.create("conduit");
    public static final ResourceKey<BlockEntityType<?>> BARREL = BlockEntityTypeIds.create("barrel");
    public static final ResourceKey<BlockEntityType<?>> SMOKER = BlockEntityTypeIds.create("smoker");
    public static final ResourceKey<BlockEntityType<?>> BLAST_FURNACE = BlockEntityTypeIds.create("blast_furnace");
    public static final ResourceKey<BlockEntityType<?>> LECTERN = BlockEntityTypeIds.create("lectern");
    public static final ResourceKey<BlockEntityType<?>> BELL = BlockEntityTypeIds.create("bell");
    public static final ResourceKey<BlockEntityType<?>> JIGSAW = BlockEntityTypeIds.create("jigsaw");
    public static final ResourceKey<BlockEntityType<?>> CAMPFIRE = BlockEntityTypeIds.create("campfire");
    public static final ResourceKey<BlockEntityType<?>> BEEHIVE = BlockEntityTypeIds.create("beehive");
    public static final ResourceKey<BlockEntityType<?>> SCULK_SENSOR = BlockEntityTypeIds.create("sculk_sensor");
    public static final ResourceKey<BlockEntityType<?>> CALIBRATED_SCULK_SENSOR = BlockEntityTypeIds.create("calibrated_sculk_sensor");
    public static final ResourceKey<BlockEntityType<?>> SCULK_CATALYST = BlockEntityTypeIds.create("sculk_catalyst");
    public static final ResourceKey<BlockEntityType<?>> SCULK_SHRIEKER = BlockEntityTypeIds.create("sculk_shrieker");
    public static final ResourceKey<BlockEntityType<?>> CHISELED_BOOKSHELF = BlockEntityTypeIds.create("chiseled_bookshelf");
    public static final ResourceKey<BlockEntityType<?>> SHELF = BlockEntityTypeIds.create("shelf");
    public static final ResourceKey<BlockEntityType<?>> BRUSHABLE_BLOCK = BlockEntityTypeIds.create("brushable_block");
    public static final ResourceKey<BlockEntityType<?>> DECORATED_POT = BlockEntityTypeIds.create("decorated_pot");
    public static final ResourceKey<BlockEntityType<?>> CRAFTER = BlockEntityTypeIds.create("crafter");
    public static final ResourceKey<BlockEntityType<?>> TRIAL_SPAWNER = BlockEntityTypeIds.create("trial_spawner");
    public static final ResourceKey<BlockEntityType<?>> VAULT = BlockEntityTypeIds.create("vault");
    public static final ResourceKey<BlockEntityType<?>> TEST_BLOCK = BlockEntityTypeIds.create("test_block");
    public static final ResourceKey<BlockEntityType<?>> TEST_INSTANCE_BLOCK = BlockEntityTypeIds.create("test_instance_block");
    public static final ResourceKey<BlockEntityType<?>> COPPER_GOLEM_STATUE = BlockEntityTypeIds.create("copper_golem_statue");
    public static final ResourceKey<BlockEntityType<?>> POTENT_SULFUR = BlockEntityTypeIds.create("potent_sulfur");

    private static ResourceKey<BlockEntityType<?>> create(String name) {
        return ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.withDefaultNamespace(name));
    }
}

