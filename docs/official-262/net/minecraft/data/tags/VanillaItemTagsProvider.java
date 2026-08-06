/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.tags;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BlockItemTagAppender;
import net.minecraft.data.tags.BlockItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.tags.VanillaBlockItemTagsProvider;
import net.minecraft.references.BlockItemId;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.WeatheringCopperCollection;

public class VanillaItemTagsProvider
extends TagsProvider<Item> {
    public VanillaItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider);
    }

    @Override
    protected BlockItemTagAppender<Item> tag(TagKey<Item> tag) {
        return new BlockItemTagAppender<Item>(this, super.tag(tag)){
            {
                Objects.requireNonNull(this$0);
                super(original);
            }

            @Override
            protected ResourceKey<Item> convertElement(BlockItemId element) {
                return element.item();
            }
        };
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        new VanillaBlockItemTagsProvider(tagId -> BlockItemTagsProvider.wrapForItems(this.tag((TagKey)tagId.item()))).run();
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.BANNERS)).addAll(VanillaItemTagsProvider.toIds(BlockItemIds.BANNER));
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.BOATS)).add(new ResourceKey[]{ItemIds.OAK_BOAT, ItemIds.SPRUCE_BOAT, ItemIds.BIRCH_BOAT, ItemIds.JUNGLE_BOAT, ItemIds.ACACIA_BOAT, ItemIds.DARK_OAK_BOAT, ItemIds.PALE_OAK_BOAT, ItemIds.MANGROVE_BOAT, ItemIds.BAMBOO_RAFT, ItemIds.CHERRY_BOAT})).addTag((TagKey)ItemTags.CHEST_BOATS);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.BUNDLES)).add((ResourceKey)ItemIds.BUNDLE)).addAll(ItemIds.DYED_BUNDLE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CHEST_BOATS)).add(new ResourceKey[]{ItemIds.OAK_CHEST_BOAT, ItemIds.SPRUCE_CHEST_BOAT, ItemIds.BIRCH_CHEST_BOAT, ItemIds.JUNGLE_CHEST_BOAT, ItemIds.ACACIA_CHEST_BOAT, ItemIds.DARK_OAK_CHEST_BOAT, ItemIds.PALE_OAK_CHEST_BOAT, ItemIds.MANGROVE_CHEST_BOAT, ItemIds.BAMBOO_CHEST_RAFT, ItemIds.CHERRY_CHEST_BOAT});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.EGGS)).add(new ResourceKey[]{ItemIds.EGG, ItemIds.BLUE_EGG, ItemIds.BROWN_EGG});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FISHES)).add(new ResourceKey[]{ItemIds.COD, ItemIds.COOKED_COD, ItemIds.SALMON, ItemIds.COOKED_SALMON, ItemIds.PUFFERFISH, ItemIds.TROPICAL_FISH});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CREEPER_DROP_MUSIC_DISCS)).add(new ResourceKey[]{ItemIds.MUSIC_DISC_13, ItemIds.MUSIC_DISC_CAT, ItemIds.MUSIC_DISC_BLOCKS, ItemIds.MUSIC_DISC_CHIRP, ItemIds.MUSIC_DISC_FAR, ItemIds.MUSIC_DISC_MALL, ItemIds.MUSIC_DISC_MELLOHI, ItemIds.MUSIC_DISC_STAL, ItemIds.MUSIC_DISC_STRAD, ItemIds.MUSIC_DISC_WARD, ItemIds.MUSIC_DISC_11, ItemIds.MUSIC_DISC_WAIT});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.COALS)).add(new ResourceKey[]{ItemIds.COAL, ItemIds.CHARCOAL});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.ARROWS)).add(new ResourceKey[]{ItemIds.ARROW, ItemIds.TIPPED_ARROW, ItemIds.SPECTRAL_ARROW});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.LECTERN_BOOKS)).add(new ResourceKey[]{ItemIds.WRITTEN_BOOK, ItemIds.WRITABLE_BOOK});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.BEACON_PAYMENT_ITEMS)).add(new ResourceKey[]{ItemIds.NETHERITE_INGOT, ItemIds.EMERALD, ItemIds.DIAMOND, ItemIds.GOLD_INGOT, ItemIds.IRON_INGOT});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PIGLIN_REPELLENTS)).add(BlockItemIds.SOUL_TORCH).add(BlockItemIds.SOUL_LANTERN).add(BlockItemIds.SOUL_CAMPFIRE);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.PIGLIN_LOVED)).addTag((TagKey)ItemTags.GOLD_ORES)).add(BlockItemIds.GOLD_BLOCK, BlockItemIds.GILDED_BLACKSTONE, BlockItemIds.LIGHT_WEIGHTED_PRESSURE_PLATE).add((ResourceKey)ItemIds.GOLD_INGOT)).add(BlockItemIds.BELL).add(new ResourceKey[]{ItemIds.CLOCK, ItemIds.GOLDEN_CARROT, ItemIds.GLISTERING_MELON_SLICE, ItemIds.GOLDEN_APPLE, ItemIds.ENCHANTED_GOLDEN_APPLE, ItemIds.GOLDEN_HELMET, ItemIds.GOLDEN_CHESTPLATE, ItemIds.GOLDEN_LEGGINGS, ItemIds.GOLDEN_BOOTS, ItemIds.GOLDEN_HORSE_ARMOR, ItemIds.GOLDEN_NAUTILUS_ARMOR, ItemIds.GOLDEN_SWORD, ItemIds.GOLDEN_SPEAR, ItemIds.GOLDEN_PICKAXE, ItemIds.GOLDEN_SHOVEL, ItemIds.GOLDEN_AXE, ItemIds.GOLDEN_HOE, ItemIds.RAW_GOLD})).add(BlockItemIds.RAW_GOLD_BLOCK, BlockItemIds.GOLDEN_DANDELION);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.IGNORED_BY_PIGLIN_BABIES)).add((ResourceKey)ItemIds.LEATHER);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PIGLIN_FOOD)).add(new ResourceKey[]{ItemIds.PORKCHOP, ItemIds.COOKED_PORKCHOP});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PIGLIN_SAFE_ARMOR)).add(new ResourceKey[]{ItemIds.GOLDEN_HELMET, ItemIds.GOLDEN_CHESTPLATE, ItemIds.GOLDEN_LEGGINGS, ItemIds.GOLDEN_BOOTS});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FOX_FOOD)).add(BlockItemIds.SWEET_BERRY_CROP, BlockItemIds.GLOW_BERRY_CROP);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.DUPLICATES_ALLAYS)).add((ResourceKey)ItemIds.AMETHYST_SHARD);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.BREWING_FUEL)).add((ResourceKey)ItemIds.BLAZE_POWDER);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.NON_FLAMMABLE_WOOD)).add(BlockItemIds.WARPED_STEM, BlockItemIds.STRIPPED_WARPED_STEM, BlockItemIds.WARPED_HYPHAE, BlockItemIds.STRIPPED_WARPED_HYPHAE, BlockItemIds.CRIMSON_STEM, BlockItemIds.STRIPPED_CRIMSON_STEM, BlockItemIds.CRIMSON_HYPHAE, BlockItemIds.STRIPPED_CRIMSON_HYPHAE, BlockItemIds.CRIMSON_PLANKS, BlockItemIds.WARPED_PLANKS, BlockItemIds.CRIMSON_SLAB, BlockItemIds.WARPED_SLAB, BlockItemIds.CRIMSON_PRESSURE_PLATE, BlockItemIds.WARPED_PRESSURE_PLATE, BlockItemIds.CRIMSON_FENCE, BlockItemIds.WARPED_FENCE, BlockItemIds.CRIMSON_TRAPDOOR, BlockItemIds.WARPED_TRAPDOOR, BlockItemIds.CRIMSON_FENCE_GATE, BlockItemIds.WARPED_FENCE_GATE, BlockItemIds.CRIMSON_STAIRS, BlockItemIds.WARPED_STAIRS, BlockItemIds.CRIMSON_BUTTON, BlockItemIds.WARPED_BUTTON, BlockItemIds.CRIMSON_DOOR, BlockItemIds.WARPED_DOOR, BlockItemIds.CRIMSON_SIGN, BlockItemIds.WARPED_SIGN, BlockItemIds.WARPED_HANGING_SIGN, BlockItemIds.CRIMSON_HANGING_SIGN, BlockItemIds.WARPED_SHELF, BlockItemIds.CRIMSON_SHELF);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.WOODEN_TOOL_MATERIALS)).addTag((TagKey)ItemTags.PLANKS);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.STONE_TOOL_MATERIALS)).add(BlockItemIds.COBBLESTONE, BlockItemIds.BLACKSTONE, BlockItemIds.COBBLED_DEEPSLATE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.COPPER_TOOL_MATERIALS)).add((ResourceKey)ItemIds.COPPER_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.IRON_TOOL_MATERIALS)).add((ResourceKey)ItemIds.IRON_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.GOLD_TOOL_MATERIALS)).add((ResourceKey)ItemIds.GOLD_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.DIAMOND_TOOL_MATERIALS)).add((ResourceKey)ItemIds.DIAMOND);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.NETHERITE_TOOL_MATERIALS)).add((ResourceKey)ItemIds.NETHERITE_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_LEATHER_ARMOR)).add((ResourceKey)ItemIds.LEATHER);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_COPPER_ARMOR)).add((ResourceKey)ItemIds.COPPER_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_CHAIN_ARMOR)).add((ResourceKey)ItemIds.IRON_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_IRON_ARMOR)).add((ResourceKey)ItemIds.IRON_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_GOLD_ARMOR)).add((ResourceKey)ItemIds.GOLD_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_DIAMOND_ARMOR)).add((ResourceKey)ItemIds.DIAMOND);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_NETHERITE_ARMOR)).add((ResourceKey)ItemIds.NETHERITE_INGOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_TURTLE_HELMET)).add((ResourceKey)ItemIds.TURTLE_SCUTE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.REPAIRS_WOLF_ARMOR)).add((ResourceKey)ItemIds.ARMADILLO_SCUTE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.STONE_CRAFTING_MATERIALS)).add(BlockItemIds.COBBLESTONE, BlockItemIds.BLACKSTONE, BlockItemIds.COBBLED_DEEPSLATE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FREEZE_IMMUNE_WEARABLES)).add(new ResourceKey[]{ItemIds.LEATHER_BOOTS, ItemIds.LEATHER_LEGGINGS, ItemIds.LEATHER_CHESTPLATE, ItemIds.LEATHER_HELMET, ItemIds.LEATHER_HORSE_ARMOR});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.AXOLOTL_FOOD)).add((ResourceKey)ItemIds.TROPICAL_FISH_BUCKET);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CLUSTER_MAX_HARVESTABLES)).add(new ResourceKey[]{ItemIds.DIAMOND_PICKAXE, ItemIds.GOLDEN_PICKAXE, ItemIds.IRON_PICKAXE, ItemIds.NETHERITE_PICKAXE, ItemIds.STONE_PICKAXE, ItemIds.WOODEN_PICKAXE, ItemIds.COPPER_PICKAXE});
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.COMPASSES)).add((ResourceKey)ItemIds.COMPASS)).add((ResourceKey)ItemIds.RECOVERY_COMPASS);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.CREEPER_IGNITERS)).add((ResourceKey)ItemIds.FLINT_AND_STEEL)).add((ResourceKey)ItemIds.FIRE_CHARGE);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SWORDS)).add((ResourceKey)ItemIds.DIAMOND_SWORD)).add((ResourceKey)ItemIds.STONE_SWORD)).add((ResourceKey)ItemIds.GOLDEN_SWORD)).add((ResourceKey)ItemIds.NETHERITE_SWORD)).add((ResourceKey)ItemIds.WOODEN_SWORD)).add((ResourceKey)ItemIds.IRON_SWORD)).add((ResourceKey)ItemIds.COPPER_SWORD);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.AXES)).add((ResourceKey)ItemIds.DIAMOND_AXE)).add((ResourceKey)ItemIds.STONE_AXE)).add((ResourceKey)ItemIds.GOLDEN_AXE)).add((ResourceKey)ItemIds.NETHERITE_AXE)).add((ResourceKey)ItemIds.WOODEN_AXE)).add((ResourceKey)ItemIds.IRON_AXE)).add((ResourceKey)ItemIds.COPPER_AXE);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.PICKAXES)).add((ResourceKey)ItemIds.DIAMOND_PICKAXE)).add((ResourceKey)ItemIds.STONE_PICKAXE)).add((ResourceKey)ItemIds.GOLDEN_PICKAXE)).add((ResourceKey)ItemIds.NETHERITE_PICKAXE)).add((ResourceKey)ItemIds.WOODEN_PICKAXE)).add((ResourceKey)ItemIds.IRON_PICKAXE)).add((ResourceKey)ItemIds.COPPER_PICKAXE);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SHOVELS)).add((ResourceKey)ItemIds.DIAMOND_SHOVEL)).add((ResourceKey)ItemIds.STONE_SHOVEL)).add((ResourceKey)ItemIds.GOLDEN_SHOVEL)).add((ResourceKey)ItemIds.NETHERITE_SHOVEL)).add((ResourceKey)ItemIds.WOODEN_SHOVEL)).add((ResourceKey)ItemIds.IRON_SHOVEL)).add((ResourceKey)ItemIds.COPPER_SHOVEL);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.HOES)).add((ResourceKey)ItemIds.DIAMOND_HOE)).add((ResourceKey)ItemIds.STONE_HOE)).add((ResourceKey)ItemIds.GOLDEN_HOE)).add((ResourceKey)ItemIds.NETHERITE_HOE)).add((ResourceKey)ItemIds.WOODEN_HOE)).add((ResourceKey)ItemIds.IRON_HOE)).add((ResourceKey)ItemIds.COPPER_HOE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SPEARS)).add(new ResourceKey[]{ItemIds.DIAMOND_SPEAR, ItemIds.STONE_SPEAR, ItemIds.GOLDEN_SPEAR, ItemIds.NETHERITE_SPEAR, ItemIds.WOODEN_SPEAR, ItemIds.IRON_SPEAR, ItemIds.COPPER_SPEAR});
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.BREAKS_DECORATED_POTS)).addTag((TagKey)ItemTags.SWORDS)).addTag((TagKey)ItemTags.AXES)).addTag((TagKey)ItemTags.PICKAXES)).addTag((TagKey)ItemTags.SHOVELS)).addTag((TagKey)ItemTags.HOES)).add((ResourceKey)ItemIds.TRIDENT)).add((ResourceKey)ItemIds.MACE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SKELETON_PREFERRED_WEAPONS)).add((ResourceKey)ItemIds.BOW);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.DROWNED_PREFERRED_WEAPONS)).add((ResourceKey)ItemIds.TRIDENT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PIGLIN_PREFERRED_WEAPONS)).add(new ResourceKey[]{ItemIds.CROSSBOW, ItemIds.GOLDEN_SPEAR});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PILLAGER_PREFERRED_WEAPONS)).add((ResourceKey)ItemIds.CROSSBOW);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.WITHER_SKELETON_DISLIKED_WEAPONS)).add((ResourceKey)ItemIds.BOW)).add((ResourceKey)ItemIds.CROSSBOW);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.DECORATED_POT_SHERDS)).add(new ResourceKey[]{ItemIds.ANGLER_POTTERY_SHERD, ItemIds.ARCHER_POTTERY_SHERD, ItemIds.ARMS_UP_POTTERY_SHERD, ItemIds.BLADE_POTTERY_SHERD, ItemIds.BREWER_POTTERY_SHERD, ItemIds.BURN_POTTERY_SHERD, ItemIds.DANGER_POTTERY_SHERD, ItemIds.EXPLORER_POTTERY_SHERD, ItemIds.FRIEND_POTTERY_SHERD, ItemIds.HEART_POTTERY_SHERD, ItemIds.HEARTBREAK_POTTERY_SHERD, ItemIds.HOWL_POTTERY_SHERD, ItemIds.MINER_POTTERY_SHERD, ItemIds.MOURNER_POTTERY_SHERD, ItemIds.PLENTY_POTTERY_SHERD, ItemIds.PRIZE_POTTERY_SHERD, ItemIds.SHEAF_POTTERY_SHERD, ItemIds.SHELTER_POTTERY_SHERD, ItemIds.SKULL_POTTERY_SHERD, ItemIds.SNORT_POTTERY_SHERD, ItemIds.FLOW_POTTERY_SHERD, ItemIds.GUSTER_POTTERY_SHERD, ItemIds.SCRAPE_POTTERY_SHERD});
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.DECORATED_POT_INGREDIENTS)).add((ResourceKey)ItemIds.BRICK)).addTag((TagKey)ItemTags.DECORATED_POT_SHERDS);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FOOT_ARMOR)).add(new ResourceKey[]{ItemIds.LEATHER_BOOTS, ItemIds.COPPER_BOOTS, ItemIds.CHAINMAIL_BOOTS, ItemIds.GOLDEN_BOOTS, ItemIds.IRON_BOOTS, ItemIds.DIAMOND_BOOTS, ItemIds.NETHERITE_BOOTS});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.LEG_ARMOR)).add(new ResourceKey[]{ItemIds.LEATHER_LEGGINGS, ItemIds.COPPER_LEGGINGS, ItemIds.CHAINMAIL_LEGGINGS, ItemIds.GOLDEN_LEGGINGS, ItemIds.IRON_LEGGINGS, ItemIds.DIAMOND_LEGGINGS, ItemIds.NETHERITE_LEGGINGS});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CHEST_ARMOR)).add(new ResourceKey[]{ItemIds.LEATHER_CHESTPLATE, ItemIds.COPPER_CHESTPLATE, ItemIds.CHAINMAIL_CHESTPLATE, ItemIds.GOLDEN_CHESTPLATE, ItemIds.IRON_CHESTPLATE, ItemIds.DIAMOND_CHESTPLATE, ItemIds.NETHERITE_CHESTPLATE});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.HEAD_ARMOR)).add(new ResourceKey[]{ItemIds.LEATHER_HELMET, ItemIds.COPPER_HELMET, ItemIds.CHAINMAIL_HELMET, ItemIds.GOLDEN_HELMET, ItemIds.IRON_HELMET, ItemIds.DIAMOND_HELMET, ItemIds.NETHERITE_HELMET, ItemIds.TURTLE_HELMET});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SKULLS)).add(BlockItemIds.PLAYER_HEAD, BlockItemIds.CREEPER_HEAD, BlockItemIds.ZOMBIE_HEAD, BlockItemIds.SKELETON_SKULL, BlockItemIds.WITHER_SKELETON_SKULL, BlockItemIds.DRAGON_HEAD, BlockItemIds.PIGLIN_HEAD);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.TRIMMABLE_ARMOR)).addTag((TagKey)ItemTags.FOOT_ARMOR)).addTag((TagKey)ItemTags.LEG_ARMOR)).addTag((TagKey)ItemTags.CHEST_ARMOR)).addTag((TagKey)ItemTags.HEAD_ARMOR);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.TRIM_MATERIALS)).add(new ResourceKey[]{ItemIds.AMETHYST_SHARD, ItemIds.COPPER_INGOT, ItemIds.DIAMOND, ItemIds.EMERALD, ItemIds.GOLD_INGOT, ItemIds.IRON_INGOT, ItemIds.LAPIS_LAZULI, ItemIds.NETHERITE_INGOT, ItemIds.QUARTZ})).add(BlockItemIds.REDSTONE_DUST).add((ResourceKey)ItemIds.RESIN_BRICK);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.BOOKSHELF_BOOKS)).add(new ResourceKey[]{ItemIds.BOOK, ItemIds.WRITTEN_BOOK, ItemIds.ENCHANTED_BOOK, ItemIds.WRITABLE_BOOK, ItemIds.KNOWLEDGE_BOOK});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS)).add(BlockItemIds.ZOMBIE_HEAD, BlockItemIds.SKELETON_SKULL, BlockItemIds.CREEPER_HEAD, BlockItemIds.DRAGON_HEAD, BlockItemIds.WITHER_SKELETON_SKULL, BlockItemIds.PIGLIN_HEAD, BlockItemIds.PLAYER_HEAD);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SNIFFER_FOOD)).add(BlockItemIds.TORCHFLOWER_CROP);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.VILLAGER_PLANTABLE_SEEDS)).add(BlockItemIds.WHEAT_CROP, BlockItemIds.POTATO_CROP, BlockItemIds.CARROT_CROP, BlockItemIds.BEETROOT_CROP, BlockItemIds.TORCHFLOWER_CROP, BlockItemIds.PITCHER_CROP);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.VILLAGER_PICKS_UP)).addTag((TagKey)ItemTags.VILLAGER_PLANTABLE_SEEDS)).add(new ResourceKey[]{ItemIds.BREAD, ItemIds.WHEAT, ItemIds.BEETROOT});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.BOOK_CLONING_TARGET)).add((ResourceKey)ItemIds.WRITABLE_BOOK);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FOOT_ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.FOOT_ARMOR);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.LEG_ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.LEG_ARMOR);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CHEST_ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.CHEST_ARMOR);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.HEAD_ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.HEAD_ARMOR);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.FOOT_ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.LEG_ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.CHEST_ARMOR_ENCHANTABLE)).addTag((TagKey)ItemTags.HEAD_ARMOR_ENCHANTABLE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SWEEPING_ENCHANTABLE)).addTag((TagKey)ItemTags.SWORDS);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.MELEE_WEAPON_ENCHANTABLE)).addTag((TagKey)ItemTags.SWORDS)).addTag((TagKey)ItemTags.SPEARS);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.FIRE_ASPECT_ENCHANTABLE)).addTag((TagKey)ItemTags.MELEE_WEAPON_ENCHANTABLE)).add((ResourceKey)ItemIds.MACE);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SHARP_WEAPON_ENCHANTABLE)).addTag((TagKey)ItemTags.MELEE_WEAPON_ENCHANTABLE)).addTag((TagKey)ItemTags.AXES);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.WEAPON_ENCHANTABLE)).addTag((TagKey)ItemTags.SHARP_WEAPON_ENCHANTABLE)).add((ResourceKey)ItemIds.MACE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.MACE_ENCHANTABLE)).add((ResourceKey)ItemIds.MACE);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.MINING_ENCHANTABLE)).addTag((TagKey)ItemTags.AXES)).addTag((TagKey)ItemTags.PICKAXES)).addTag((TagKey)ItemTags.SHOVELS)).addTag((TagKey)ItemTags.HOES)).add((ResourceKey)ItemIds.SHEARS);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.MINING_LOOT_ENCHANTABLE)).addTag((TagKey)ItemTags.AXES)).addTag((TagKey)ItemTags.PICKAXES)).addTag((TagKey)ItemTags.SHOVELS)).addTag((TagKey)ItemTags.HOES);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FISHING_ENCHANTABLE)).add((ResourceKey)ItemIds.FISHING_ROD);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.TRIDENT_ENCHANTABLE)).add((ResourceKey)ItemIds.TRIDENT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.LUNGE_ENCHANTABLE)).addTag((TagKey)ItemTags.SPEARS);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.DURABILITY_ENCHANTABLE)).addTag((TagKey)ItemTags.FOOT_ARMOR)).addTag((TagKey)ItemTags.LEG_ARMOR)).addTag((TagKey)ItemTags.CHEST_ARMOR)).addTag((TagKey)ItemTags.HEAD_ARMOR)).add((ResourceKey)ItemIds.ELYTRA)).add((ResourceKey)ItemIds.SHIELD)).addTag((TagKey)ItemTags.SWORDS)).addTag((TagKey)ItemTags.AXES)).addTag((TagKey)ItemTags.PICKAXES)).addTag((TagKey)ItemTags.SHOVELS)).addTag((TagKey)ItemTags.HOES)).add((ResourceKey)ItemIds.BOW)).add((ResourceKey)ItemIds.CROSSBOW)).add((ResourceKey)ItemIds.TRIDENT)).add((ResourceKey)ItemIds.FLINT_AND_STEEL)).add((ResourceKey)ItemIds.SHEARS)).add((ResourceKey)ItemIds.BRUSH)).add((ResourceKey)ItemIds.FISHING_ROD)).add(new ResourceKey[]{ItemIds.CARROT_ON_A_STICK, ItemIds.WARPED_FUNGUS_ON_A_STICK})).add((ResourceKey)ItemIds.MACE)).addTag((TagKey)ItemTags.SPEARS);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.BOW_ENCHANTABLE)).add((ResourceKey)ItemIds.BOW);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.EQUIPPABLE_ENCHANTABLE)).addTag((TagKey)ItemTags.FOOT_ARMOR)).addTag((TagKey)ItemTags.LEG_ARMOR)).addTag((TagKey)ItemTags.CHEST_ARMOR)).addTag((TagKey)ItemTags.HEAD_ARMOR)).add((ResourceKey)ItemIds.ELYTRA)).addTag((TagKey)ItemTags.SKULLS)).add(BlockItemIds.CARVED_PUMPKIN);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CROSSBOW_ENCHANTABLE)).add((ResourceKey)ItemIds.CROSSBOW);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.VANISHING_ENCHANTABLE)).addTag((TagKey)ItemTags.DURABILITY_ENCHANTABLE)).add((ResourceKey)ItemIds.COMPASS)).add(BlockItemIds.CARVED_PUMPKIN).addTag((TagKey)ItemTags.SKULLS);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.DYES)).addAll(ItemIds.DYE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CAULDRON_CAN_REMOVE_DYE)).add(new ResourceKey[]{ItemIds.LEATHER_HELMET, ItemIds.LEATHER_CHESTPLATE, ItemIds.LEATHER_LEGGINGS, ItemIds.LEATHER_BOOTS, ItemIds.LEATHER_HORSE_ARMOR, ItemIds.WOLF_ARMOR});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FURNACE_MINECART_FUEL)).add(new ResourceKey[]{ItemIds.COAL, ItemIds.CHARCOAL});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.MEAT)).add(new ResourceKey[]{ItemIds.BEEF, ItemIds.CHICKEN, ItemIds.COOKED_BEEF, ItemIds.COOKED_CHICKEN, ItemIds.COOKED_MUTTON, ItemIds.COOKED_PORKCHOP, ItemIds.COOKED_RABBIT, ItemIds.MUTTON, ItemIds.PORKCHOP, ItemIds.RABBIT, ItemIds.ROTTEN_FLESH});
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.WOLF_FOOD)).addTag((TagKey)ItemTags.MEAT)).add(new ResourceKey[]{ItemIds.COD, ItemIds.COOKED_COD, ItemIds.SALMON, ItemIds.COOKED_SALMON, ItemIds.TROPICAL_FISH, ItemIds.PUFFERFISH, ItemIds.RABBIT_STEW});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.OCELOT_FOOD)).add(new ResourceKey[]{ItemIds.COD, ItemIds.SALMON});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CAT_FOOD)).add(new ResourceKey[]{ItemIds.COD, ItemIds.SALMON});
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.HORSE_FOOD)).add(new ResourceKey[]{ItemIds.WHEAT, ItemIds.SUGAR})).add(BlockItemIds.HAY_BLOCK).add((ResourceKey)ItemIds.APPLE)).add(BlockItemIds.CARROT_CROP).add(new ResourceKey[]{ItemIds.GOLDEN_CARROT, ItemIds.GOLDEN_APPLE, ItemIds.ENCHANTED_GOLDEN_APPLE});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.ZOMBIE_HORSE_FOOD)).add(BlockItemIds.RED_MUSHROOM);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.HORSE_TEMPT_ITEMS)).add(new ResourceKey[]{ItemIds.GOLDEN_CARROT, ItemIds.GOLDEN_APPLE, ItemIds.ENCHANTED_GOLDEN_APPLE});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.HARNESSES)).addAll(ItemIds.HARNESS);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.HAPPY_GHAST_FOOD)).add((ResourceKey)ItemIds.SNOWBALL);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.HAPPY_GHAST_TEMPT_ITEMS)).addTag((TagKey)ItemTags.HAPPY_GHAST_FOOD)).addTag((TagKey)ItemTags.HARNESSES);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CAMEL_FOOD)).add(BlockItemIds.CACTUS);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CAMEL_HUSK_FOOD)).add((ResourceKey)ItemIds.RABBIT_FOOT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.ARMADILLO_FOOD)).add((ResourceKey)ItemIds.SPIDER_EYE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CHICKEN_FOOD)).add(BlockItemIds.WHEAT_CROP, BlockItemIds.MELON_CROP, BlockItemIds.PUMPKIN_CROP, BlockItemIds.BEETROOT_CROP, BlockItemIds.TORCHFLOWER_CROP, BlockItemIds.PITCHER_CROP);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.FROG_FOOD)).add((ResourceKey)ItemIds.SLIME_BALL);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.HOGLIN_FOOD)).add(BlockItemIds.CRIMSON_FUNGUS);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.LLAMA_FOOD)).add((ResourceKey)ItemIds.WHEAT)).add(BlockItemIds.HAY_BLOCK);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.LLAMA_TEMPT_ITEMS)).add(BlockItemIds.HAY_BLOCK);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.NAUTILUS_TAMING_ITEMS)).add(new ResourceKey[]{ItemIds.PUFFERFISH_BUCKET, ItemIds.PUFFERFISH});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.NAUTILUS_BUCKET_FOOD)).add(new ResourceKey[]{ItemIds.PUFFERFISH_BUCKET, ItemIds.COD_BUCKET, ItemIds.SALMON_BUCKET, ItemIds.TROPICAL_FISH_BUCKET});
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.NAUTILUS_FOOD)).addTag((TagKey)ItemTags.FISHES)).addTag((TagKey)ItemTags.NAUTILUS_BUCKET_FOOD);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PANDA_FOOD)).add(BlockItemIds.BAMBOO);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.PANDA_EATS_FROM_GROUND)).addTag((TagKey)ItemTags.PANDA_FOOD)).add(BlockItemIds.CAKE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PIG_FOOD)).add(BlockItemIds.CARROT_CROP, BlockItemIds.POTATO_CROP).add((ResourceKey)ItemIds.BEETROOT);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.RABBIT_FOOD)).add(BlockItemIds.CARROT_CROP).add((ResourceKey)ItemIds.GOLDEN_CARROT)).add(BlockItemIds.DANDELION);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.STRIDER_FOOD)).add(BlockItemIds.WARPED_FUNGUS);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.STRIDER_TEMPT_ITEMS)).addTag((TagKey)ItemTags.STRIDER_FOOD)).add((ResourceKey)ItemIds.WARPED_FUNGUS_ON_A_STICK);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.TURTLE_FOOD)).add(BlockItemIds.SEAGRASS);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PARROT_FOOD)).add(BlockItemIds.WHEAT_CROP).add(BlockItemIds.MELON_CROP, BlockItemIds.PUMPKIN_CROP, BlockItemIds.BEETROOT_CROP, BlockItemIds.TORCHFLOWER_CROP, BlockItemIds.PITCHER_CROP);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.PARROT_POISONOUS_FOOD)).add((ResourceKey)ItemIds.COOKIE);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.COW_FOOD)).add((ResourceKey)ItemIds.WHEAT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SHEEP_FOOD)).add((ResourceKey)ItemIds.WHEAT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_FOOD)).add((ResourceKey)ItemIds.SLIME_BALL);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.GOAT_FOOD)).add((ResourceKey)ItemIds.WHEAT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.MAP_INVISIBILITY_EQUIPMENT)).add(BlockItemIds.CARVED_PUMPKIN);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.GAZE_DISGUISE_EQUIPMENT)).add(BlockItemIds.CARVED_PUMPKIN);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SHEARABLE_FROM_COPPER_GOLEM)).add(BlockItemIds.POPPY);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.METAL_NUGGETS)).add(new ResourceKey[]{ItemIds.COPPER_NUGGET, ItemIds.IRON_NUGGET, ItemIds.GOLD_NUGGET});
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_BOUNCY)).addTag((TagKey)ItemTags.PLANKS)).add(BlockItemIds.BAMBOO_MOSAIC).addTag((TagKey)ItemTags.LOGS)).addTag((TagKey)ItemTags.BAMBOO_BLOCKS);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY)).add(BlockItemIds.AMETHYST_BLOCK, BlockItemIds.ANDESITE, BlockItemIds.BASALT, BlockItemIds.BLACKSTONE, BlockItemIds.BRICKS, BlockItemIds.CALCITE).add(BlockItemIds.CHISELED_CINNABAR, BlockItemIds.CHISELED_DEEPSLATE, BlockItemIds.CHISELED_NETHER_BRICKS, BlockItemIds.CHISELED_POLISHED_BLACKSTONE, BlockItemIds.CHISELED_QUARTZ_BLOCK, BlockItemIds.CHISELED_RED_SANDSTONE, BlockItemIds.CHISELED_SANDSTONE, BlockItemIds.CHISELED_STONE_BRICKS, BlockItemIds.CHISELED_SULFUR, BlockItemIds.CHISELED_TUFF, BlockItemIds.CHISELED_TUFF_BRICKS).add(BlockItemIds.CINNABAR, BlockItemIds.CINNABAR_BRICKS, BlockItemIds.COBBLED_DEEPSLATE, BlockItemIds.COBBLESTONE).add(BlockItemIds.CRACKED_DEEPSLATE_BRICKS, BlockItemIds.CRACKED_DEEPSLATE_TILES, BlockItemIds.CRACKED_NETHER_BRICKS, BlockItemIds.CRACKED_POLISHED_BLACKSTONE_BRICKS, BlockItemIds.CRACKED_STONE_BRICKS).add(BlockItemIds.CRIMSON_NYLIUM, BlockItemIds.CRYING_OBSIDIAN, BlockItemIds.CUT_RED_SANDSTONE, BlockItemIds.CUT_SANDSTONE, BlockItemIds.DARK_PRISMARINE).add(BlockItemIds.DEEPSLATE, BlockItemIds.DEEPSLATE_BRICKS, BlockItemIds.DEEPSLATE_TILES).add(BlockItemIds.DIAMOND_BLOCK, BlockItemIds.DIORITE, BlockItemIds.DRIPSTONE_BLOCK, BlockItemIds.EMERALD_BLOCK, BlockItemIds.END_STONE, BlockItemIds.END_STONE_BRICKS, BlockItemIds.GILDED_BLACKSTONE, BlockItemIds.GLOWSTONE, BlockItemIds.GRANITE, BlockItemIds.LAPIS_BLOCK).add(BlockItemIds.MOSSY_COBBLESTONE, BlockItemIds.MOSSY_STONE_BRICKS, BlockItemIds.MUD_BRICKS, BlockItemIds.NETHER_BRICKS, BlockItemIds.NETHERRACK, BlockItemIds.OBSERVER, BlockItemIds.OBSIDIAN).add(BlockItemIds.POLISHED_ANDESITE, BlockItemIds.POLISHED_BASALT, BlockItemIds.POLISHED_BLACKSTONE, BlockItemIds.POLISHED_BLACKSTONE_BRICKS, BlockItemIds.POLISHED_CINNABAR, BlockItemIds.POLISHED_DEEPSLATE, BlockItemIds.POLISHED_DIORITE, BlockItemIds.POLISHED_GRANITE, BlockItemIds.POLISHED_SULFUR, BlockItemIds.POLISHED_TUFF).add(BlockItemIds.PRISMARINE, BlockItemIds.PRISMARINE_BRICKS, BlockItemIds.PURPUR_BLOCK, BlockItemIds.PURPUR_PILLAR, BlockItemIds.QUARTZ_BLOCK, BlockItemIds.QUARTZ_BRICKS, BlockItemIds.NETHER_QUARTZ_ORE, BlockItemIds.QUARTZ_PILLAR).add(BlockItemIds.RED_NETHER_BRICKS, BlockItemIds.RED_SANDSTONE, BlockItemIds.REDSTONE_LAMP, BlockItemIds.SANDSTONE, BlockItemIds.SEA_LANTERN).add(BlockItemIds.SMOOTH_BASALT, BlockItemIds.SMOOTH_QUARTZ, BlockItemIds.SMOOTH_RED_SANDSTONE, BlockItemIds.SMOOTH_SANDSTONE, BlockItemIds.SMOOTH_STONE).add(BlockItemIds.STONE, BlockItemIds.STONE_BRICKS, BlockItemIds.SULFUR, BlockItemIds.SULFUR_BRICKS, BlockItemIds.TUFF, BlockItemIds.TUFF_BRICKS, BlockItemIds.WARPED_NYLIUM).addTag((TagKey)ItemTags.CONCRETE)).addTag((TagKey)ItemTags.COAL_ORES)).addTag((TagKey)ItemTags.LAPIS_ORES)).addTag((TagKey)ItemTags.REDSTONE_ORES)).addTag((TagKey)ItemTags.DIAMOND_ORES)).addTag((TagKey)ItemTags.EMERALD_ORES)).addTag((TagKey)ItemTags.TERRACOTTA)).addTag((TagKey)ItemTags.GLAZED_TERRACOTTA);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_REGULAR)).addTag((TagKey)ItemTags.CONCRETE_POWDERS)).add(BlockItemIds.MUD, BlockItemIds.MUDDY_MANGROVE_ROOTS, BlockItemIds.PACKED_MUD).add(BlockItemIds.COAL_BLOCK).add(BlockItemIds.DIRT, BlockItemIds.COARSE_DIRT, BlockItemIds.ROOTED_DIRT, BlockItemIds.PODZOL, BlockItemIds.GRASS_BLOCK, BlockItemIds.CLAY).add(BlockItemIds.BONE_BLOCK);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT)).add(BlockItemIds.IRON_BLOCK, BlockItemIds.GOLD_BLOCK, BlockItemIds.RAW_COPPER_BLOCK, BlockItemIds.RAW_GOLD_BLOCK, BlockItemIds.RAW_IRON_BLOCK).addTag((TagKey)ItemTags.GOLD_ORES)).addTag((TagKey)ItemTags.IRON_ORES)).addTag((TagKey)ItemTags.COPPER_ORES)).add(BlockItemIds.NETHERITE_BLOCK, BlockItemIds.ANCIENT_DEBRIS).addAll(VanillaItemTagsProvider.toIds(BlockItemIds.COPPER_BLOCK)).addAll(VanillaItemTagsProvider.toIds(BlockItemIds.COPPER_BULB)).addAll(VanillaItemTagsProvider.toIds(BlockItemIds.CUT_COPPER)).addAll(VanillaItemTagsProvider.toIds(BlockItemIds.CHISELED_COPPER));
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_FLAT)).add(BlockItemIds.TUBE_CORAL_BLOCK, BlockItemIds.BRAIN_CORAL_BLOCK, BlockItemIds.BUBBLE_CORAL_BLOCK, BlockItemIds.FIRE_CORAL_BLOCK, BlockItemIds.HORN_CORAL_BLOCK).add(BlockItemIds.DEAD_TUBE_CORAL_BLOCK, BlockItemIds.DEAD_BRAIN_CORAL_BLOCK, BlockItemIds.DEAD_BUBBLE_CORAL_BLOCK, BlockItemIds.DEAD_FIRE_CORAL_BLOCK, BlockItemIds.DEAD_HORN_CORAL_BLOCK).add(BlockItemIds.SPONGE, BlockItemIds.WET_SPONGE, BlockItemIds.DRIED_KELP_BLOCK).addTag((TagKey)ItemTags.MOSS_BLOCKS)).add(BlockItemIds.RESIN_BLOCK, BlockItemIds.RESIN_BRICKS, BlockItemIds.CHISELED_RESIN_BRICKS).add(BlockItemIds.MELON, BlockItemIds.HAY_BLOCK, BlockItemIds.PUMPKIN, BlockItemIds.CARVED_PUMPKIN, BlockItemIds.JACK_O_LANTERN).add(BlockItemIds.OCHRE_FROGLIGHT, BlockItemIds.PEARLESCENT_FROGLIGHT, BlockItemIds.VERDANT_FROGLIGHT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_LIGHT)).addTag((TagKey)ItemTags.WOOL);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING)).add(BlockItemIds.BLUE_ICE, BlockItemIds.PACKED_ICE, BlockItemIds.SNOW_BLOCK);
        ((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING)).add(BlockItemIds.BROWN_MUSHROOM_BLOCK, BlockItemIds.RED_MUSHROOM_BLOCK, BlockItemIds.MUSHROOM_STEM, BlockItemIds.MYCELIUM).addTag((TagKey)ItemTags.WART_BLOCKS)).add(BlockItemIds.SHROOMLIGHT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_STICKY)).add(BlockItemIds.HONEYCOMB_BLOCK);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE)).add(BlockItemIds.SOUL_SAND, BlockItemIds.SOUL_SOIL);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE)).add(BlockItemIds.TNT);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_HOT)).add(BlockItemIds.MAGMA_BLOCK);
        ((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)((BlockItemTagAppender)this.tag((TagKey)ItemTags.SULFUR_CUBE_SWALLOWABLE)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_BOUNCY)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_REGULAR)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_FLAT)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_LIGHT)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_STICKY)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_HOT)).addTag((TagKey)ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.LOOM_DYES)).addTag((TagKey)ItemTags.DYES);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.LOOM_PATTERNS)).add(new ResourceKey[]{ItemIds.FLOWER_BANNER_PATTERN, ItemIds.CREEPER_BANNER_PATTERN, ItemIds.SKULL_BANNER_PATTERN, ItemIds.MOJANG_BANNER_PATTERN, ItemIds.GLOBE_BANNER_PATTERN, ItemIds.PIGLIN_BANNER_PATTERN, ItemIds.FLOW_BANNER_PATTERN, ItemIds.GUSTER_BANNER_PATTERN, ItemIds.FIELD_MASONED_BANNER_PATTERN, ItemIds.BORDURE_INDENTED_BANNER_PATTERN});
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.CAT_COLLAR_DYES)).addTag((TagKey)ItemTags.DYES);
        ((BlockItemTagAppender)this.tag((TagKey)ItemTags.WOLF_COLLAR_DYES)).addTag((TagKey)ItemTags.DYES);
    }

    private static ColorCollection<ResourceKey<Item>> toIds(ColorCollection<BlockItemId> ids) {
        return ids.map(BlockItemId::item);
    }

    private static WeatheringCopperCollection<ResourceKey<Item>> toIds(WeatheringCopperCollection<BlockItemId> ids) {
        return ids.map(BlockItemId::item);
    }
}

