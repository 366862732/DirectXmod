/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.data.recipes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.BredAnimalsTrigger;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.EnterBlockTrigger;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.CustomCraftingRecipeBuilder;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.recipes.SmithingTrimRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.data.recipes.TransmuteRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BannerDuplicateRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.DyeRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import org.jspecify.annotations.Nullable;

public abstract class RecipeProvider {
    protected final HolderLookup.Provider registries;
    private final HolderGetter<Item> items;
    protected final RecipeOutput output;
    private static final Map<BlockFamily.Variant, FamilyCraftingRecipeProvider> SHAPE_BUILDERS = ImmutableMap.builder().put((Object)BlockFamily.Variant.BUTTON, (context, result, base) -> context.buttonBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.CHISELED, (context, result, base) -> context.chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.CUT, (context, result, base) -> context.cutBuilder(RecipeCategory.BUILDING_BLOCKS, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.DOOR, (context, result, base) -> context.doorBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.CUSTOM_FENCE, (context, result, base) -> context.fenceBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.FENCE, (context, result, base) -> context.fenceBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.CUSTOM_FENCE_GATE, (context, result, base) -> context.fenceGateBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.FENCE_GATE, (context, result, base) -> context.fenceGateBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.SIGN, (context, result, base) -> context.signBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.CUSTOM_HANGING_SIGN, (context, result, base) -> context.hangingSignBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.HANGING_SIGN, (context, result, base) -> context.hangingSignBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.SLAB, (context, result, base) -> context.slabBuilder(RecipeCategory.BUILDING_BLOCKS, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.STAIRS, (context, result, base) -> context.stairBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.PRESSURE_PLATE, (context, result, base) -> context.pressurePlateBuilder(RecipeCategory.REDSTONE, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.POLISHED, (context, result, base) -> context.polishedBuilder(RecipeCategory.BUILDING_BLOCKS, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.TRAPDOOR, (context, result, base) -> context.trapdoorBuilder(result, Ingredient.of(base))).put((Object)BlockFamily.Variant.WALL, (context, result, base) -> context.wallBuilder(RecipeCategory.DECORATIONS, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.BRICKS, (context, result, base) -> context.bricksBuilder(RecipeCategory.BUILDING_BLOCKS, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.TILES, (context, result, base) -> context.tilesBuilder(RecipeCategory.BUILDING_BLOCKS, result, Ingredient.of(base))).put((Object)BlockFamily.Variant.PILLAR, (context, result, base) -> context.pillarBuilder(RecipeCategory.BUILDING_BLOCKS, result, Ingredient.of(base))).build();
    private static final Map<BlockFamily.Variant, FamilyStonecutterRecipeProvider> STONECUTTER_RECIPE_BUILDERS = ImmutableMap.builder().put((Object)BlockFamily.Variant.SLAB, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 2)).put((Object)BlockFamily.Variant.STAIRS, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).put((Object)BlockFamily.Variant.BRICKS, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).put((Object)BlockFamily.Variant.WALL, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.DECORATIONS, result, base, 1)).put((Object)BlockFamily.Variant.CHISELED, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).put((Object)BlockFamily.Variant.POLISHED, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).put((Object)BlockFamily.Variant.CUT, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).put((Object)BlockFamily.Variant.TILES, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).put((Object)BlockFamily.Variant.PILLAR, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).put((Object)BlockFamily.Variant.COBBLED, (context, result, base) -> context.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, result, base, 1)).build();

    protected RecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        this.registries = registries;
        this.items = registries.lookupOrThrow(Registries.ITEM);
        this.output = output;
    }

    protected abstract void buildRecipes();

    protected void generateForEnabledBlockFamilies(FeatureFlagSet flagSet) {
        BlockFamilies.getAllFamilies().forEach(family -> this.generateRecipes((BlockFamily)family, flagSet));
    }

    protected void oneToOneConversionRecipe(ItemLike product, ItemLike resource, @Nullable String group) {
        this.oneToOneConversionRecipe(product, resource, group, 1);
    }

    protected void oneToOneConversionRecipe(ItemLike product, ItemLike resource, @Nullable String group, int productCount) {
        this.shapeless(RecipeCategory.MISC, product, productCount).requires(resource).group(group).unlockedBy(RecipeProvider.getHasName(resource), (Criterion)this.has(resource)).save(this.output, RecipeProvider.getConversionRecipeName(product, resource));
    }

    protected void oreSmelting(List<ItemLike> smeltables, RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemLike result, float experience, int cookingTime, String group) {
        this.oreCooking(SmeltingRecipe::new, smeltables, craftingCategory, cookingCategory, result, experience, cookingTime, group, "_from_smelting");
    }

    protected void oreBlasting(List<ItemLike> smeltables, RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemLike result, float experience, int cookingTime, String group) {
        this.oreCooking(BlastingRecipe::new, smeltables, craftingCategory, cookingCategory, result, experience, cookingTime, group, "_from_blasting");
    }

    private <T extends AbstractCookingRecipe> void oreCooking(AbstractCookingRecipe.Factory<T> factory, List<ItemLike> smeltables, RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemLike result, float experience, int cookingTime, String group, String fromDesc) {
        for (ItemLike item : smeltables) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(item), craftingCategory, cookingCategory, result, experience, cookingTime, factory).group(group).unlockedBy(RecipeProvider.getHasName(item), (Criterion)this.has(item)).save(this.output, RecipeProvider.getItemName(result) + fromDesc + "_" + RecipeProvider.getItemName(item));
        }
    }

    protected void netheriteSmithing(Item base, RecipeCategory category, Item result) {
        SmithingTransformRecipeBuilder.smithing(Ingredient.of((ItemLike)Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of((ItemLike)base), this.tag(ItemTags.NETHERITE_TOOL_MATERIALS), category, result).unlocks("has_netherite_ingot", this.has(ItemTags.NETHERITE_TOOL_MATERIALS)).save(this.output, RecipeProvider.getItemName(result) + "_smithing");
    }

    protected void trimSmithing(Item trimTemplate, ResourceKey<TrimPattern> patternId, ResourceKey<Recipe<?>> id) {
        Holder.Reference<TrimPattern> pattern = this.registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(patternId);
        SmithingTrimRecipeBuilder.smithingTrim(Ingredient.of((ItemLike)trimTemplate), this.tag(ItemTags.TRIMMABLE_ARMOR), this.tag(ItemTags.TRIM_MATERIALS), pattern, RecipeCategory.MISC).unlocks("has_smithing_trim_template", this.has(trimTemplate)).save(this.output, id);
    }

    protected void twoByTwoPacker(RecipeCategory category, ItemLike result, ItemLike ingredient) {
        this.shaped(category, result, 1).define(Character.valueOf('#'), ingredient).pattern("##").pattern("##").unlockedBy(RecipeProvider.getHasName(ingredient), (Criterion)this.has(ingredient)).save(this.output);
    }

    protected void threeByThreePacker(RecipeCategory category, ItemLike result, ItemLike ingredient, String unlockedBy) {
        this.shapeless(category, result).requires(ingredient, 9).unlockedBy(unlockedBy, (Criterion)this.has(ingredient)).save(this.output);
    }

    protected void threeByThreePacker(RecipeCategory category, ItemLike result, ItemLike ingredient) {
        this.threeByThreePacker(category, result, ingredient, RecipeProvider.getHasName(ingredient));
    }

    protected void planksFromLog(ItemLike result, TagKey<Item> logs, int count) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, result, count).requires(logs).group("planks").unlockedBy("has_log", (Criterion)this.has(logs)).save(this.output);
    }

    protected void planksFromLogs(ItemLike result, TagKey<Item> logs, int count) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, result, count).requires(logs).group("planks").unlockedBy("has_logs", (Criterion)this.has(logs)).save(this.output);
    }

    protected void woodFromLogs(ItemLike result, ItemLike log) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, result, 3).define(Character.valueOf('#'), log).pattern("##").pattern("##").group("bark").unlockedBy("has_log", (Criterion)this.has(log)).save(this.output);
    }

    protected void woodenBoat(ItemLike result, ItemLike planks) {
        this.shaped(RecipeCategory.TRANSPORTATION, result).define(Character.valueOf('#'), planks).pattern("# #").pattern("###").group("boat").unlockedBy("in_water", (Criterion)RecipeProvider.insideOf(Blocks.WATER)).save(this.output);
    }

    protected void chestBoat(ItemLike chestBoat, ItemLike boat) {
        this.shapeless(RecipeCategory.TRANSPORTATION, chestBoat).requires(Blocks.CHEST).requires(boat).group("chest_boat").unlockedBy("has_boat", (Criterion)this.has(ItemTags.BOATS)).save(this.output);
    }

    private RecipeBuilder buttonBuilder(ItemLike result, Ingredient base) {
        return this.shapeless(RecipeCategory.REDSTONE, result).requires(base);
    }

    protected RecipeBuilder doorBuilder(ItemLike result, Ingredient base) {
        return this.shaped(RecipeCategory.REDSTONE, result, 3).define(Character.valueOf('#'), base).pattern("##").pattern("##").pattern("##");
    }

    private RecipeBuilder fenceBuilder(ItemLike result, Ingredient base) {
        int count = result == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
        Item base2 = result == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
        return this.shaped(RecipeCategory.DECORATIONS, result, count).define(Character.valueOf('W'), base).define(Character.valueOf('#'), base2).pattern("W#W").pattern("W#W");
    }

    private RecipeBuilder fenceGateBuilder(ItemLike result, Ingredient planks) {
        return this.shaped(RecipeCategory.REDSTONE, result).define(Character.valueOf('#'), Items.STICK).define(Character.valueOf('W'), planks).pattern("#W#").pattern("#W#");
    }

    protected void pressurePlate(ItemLike result, ItemLike base) {
        this.pressurePlateBuilder(RecipeCategory.REDSTONE, result, Ingredient.of(base)).unlockedBy(RecipeProvider.getHasName(base), this.has(base)).save(this.output);
    }

    private RecipeBuilder pressurePlateBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result).define(Character.valueOf('#'), base).pattern("##");
    }

    protected void slab(RecipeCategory category, ItemLike result, ItemLike base) {
        this.slabBuilder(category, result, Ingredient.of(base)).unlockedBy(RecipeProvider.getHasName(base), this.has(base)).save(this.output);
    }

    protected void shelf(ItemLike result, ItemLike strippedLogs) {
        this.shaped(RecipeCategory.DECORATIONS, result, 6).define(Character.valueOf('#'), strippedLogs).pattern("###").pattern("   ").pattern("###").group("shelf").unlockedBy(RecipeProvider.getHasName(strippedLogs), (Criterion)this.has(strippedLogs)).save(this.output);
    }

    protected RecipeBuilder slabBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result, 6).define(Character.valueOf('#'), base).pattern("###");
    }

    protected RecipeBuilder stairBuilder(ItemLike result, Ingredient base) {
        return this.shaped(RecipeCategory.BUILDING_BLOCKS, result, 4).define(Character.valueOf('#'), base).pattern("#  ").pattern("## ").pattern("###");
    }

    protected RecipeBuilder trapdoorBuilder(ItemLike result, Ingredient base) {
        return this.shaped(RecipeCategory.REDSTONE, result, 2).define(Character.valueOf('#'), base).pattern("###").pattern("###");
    }

    private RecipeBuilder signBuilder(ItemLike result, Ingredient planks) {
        return this.shaped(RecipeCategory.DECORATIONS, result, 3).group("sign").define(Character.valueOf('#'), planks).define(Character.valueOf('X'), Items.STICK).pattern("###").pattern("###").pattern(" X ");
    }

    protected RecipeBuilder hangingSignBuilder(ItemLike result, Ingredient ingredient) {
        return this.shaped(RecipeCategory.DECORATIONS, result, 6).group("hanging_sign").define(Character.valueOf('#'), ingredient).define(Character.valueOf('X'), Items.IRON_CHAIN).pattern("X X").pattern("###").pattern("###");
    }

    protected void colorItemWithDye(List<Item> dyes, List<Item> items, String groupName, RecipeCategory category) {
        this.colorWithDye(dyes, items, null, groupName, category);
    }

    protected void colorWithDye(List<Item> dyes, List<Item> dyedItems, @Nullable Item uncoloredItem, String groupName, RecipeCategory category) {
        for (int dyeIndex = 0; dyeIndex < dyes.size(); ++dyeIndex) {
            Item dye = dyes.get(dyeIndex);
            Item dyedItem = dyedItems.get(dyeIndex);
            Stream<Item> sourceItems = dyedItems.stream().filter(b -> !b.equals(dyedItem));
            if (uncoloredItem != null) {
                sourceItems = Stream.concat(sourceItems, Stream.of(uncoloredItem));
            }
            this.shapeless(category, dyedItem).requires(dye).requires(Ingredient.of(sourceItems)).group(groupName).unlockedBy("has_needed_dye", (Criterion)this.has(dye)).save(this.output, "dye_" + RecipeProvider.getItemName(dyedItem));
        }
    }

    protected void carpet(ItemLike result, ItemLike sourceItem) {
        this.shaped(RecipeCategory.DECORATIONS, result, 3).define(Character.valueOf('#'), sourceItem).pattern("##").group("carpet").unlockedBy(RecipeProvider.getHasName(sourceItem), (Criterion)this.has(sourceItem)).save(this.output);
    }

    protected void bedFromPlanksAndWool(ItemLike result, ItemLike wool) {
        this.shaped(RecipeCategory.DECORATIONS, result).define(Character.valueOf('#'), wool).define(Character.valueOf('X'), ItemTags.PLANKS).pattern("###").pattern("XXX").group("bed").unlockedBy(RecipeProvider.getHasName(wool), (Criterion)this.has(wool)).save(this.output);
    }

    protected void banner(ItemLike result, ItemLike wool) {
        this.shaped(RecipeCategory.DECORATIONS, result).define(Character.valueOf('#'), wool).define(Character.valueOf('|'), Items.STICK).pattern("###").pattern("###").pattern(" | ").group("banner").unlockedBy(RecipeProvider.getHasName(wool), (Criterion)this.has(wool)).save(this.output);
        SpecialRecipeBuilder.special(() -> new BannerDuplicateRecipe(Ingredient.of(result), new ItemStackTemplate(result.asItem()))).save(this.output, RecipeProvider.getItemName(result) + "_duplicate");
    }

    protected void stainedGlassFromGlassAndDye(ItemLike result, ItemLike dye) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, result, 8).define(Character.valueOf('#'), Blocks.GLASS).define(Character.valueOf('X'), dye).pattern("###").pattern("#X#").pattern("###").group("stained_glass").unlockedBy("has_glass", (Criterion)this.has(Blocks.GLASS)).save(this.output);
    }

    protected void dryGhast(ItemLike result) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, result, 1).define(Character.valueOf('#'), Items.GHAST_TEAR).define(Character.valueOf('X'), Items.SOUL_SAND).pattern("###").pattern("#X#").pattern("###").group("dry_ghast").unlockedBy(RecipeProvider.getHasName(Items.GHAST_TEAR), (Criterion)this.has(Items.GHAST_TEAR)).save(this.output);
    }

    protected void harness(ItemLike result, ItemLike wool) {
        this.shaped(RecipeCategory.COMBAT, result).define(Character.valueOf('#'), wool).define(Character.valueOf('G'), Items.GLASS).define(Character.valueOf('L'), Items.LEATHER).pattern("LLL").pattern("G#G").group("harness").unlockedBy("has_dried_ghast", (Criterion)this.has(Blocks.DRIED_GHAST)).save(this.output);
    }

    protected void stainedGlassPaneFromStainedGlass(ItemLike result, ItemLike stainedGlass) {
        this.shaped(RecipeCategory.DECORATIONS, result, 16).define(Character.valueOf('#'), stainedGlass).pattern("###").pattern("###").group("stained_glass_pane").unlockedBy("has_glass", (Criterion)this.has(stainedGlass)).save(this.output);
    }

    protected void stainedGlassPaneFromGlassPaneAndDye(ItemLike result, ItemLike dye) {
        ((ShapedRecipeBuilder)this.shaped(RecipeCategory.DECORATIONS, result, 8).define(Character.valueOf('#'), Blocks.GLASS_PANE).define(Character.valueOf('$'), dye).pattern("###").pattern("#$#").pattern("###").group("stained_glass_pane").unlockedBy("has_glass_pane", (Criterion)this.has(Blocks.GLASS_PANE))).unlockedBy(RecipeProvider.getHasName(dye), (Criterion)this.has(dye)).save(this.output, RecipeProvider.getConversionRecipeName(result, Blocks.GLASS_PANE));
    }

    protected void coloredTerracottaFromTerracottaAndDye(ItemLike result, ItemLike dye) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, result, 8).define(Character.valueOf('#'), Blocks.TERRACOTTA).define(Character.valueOf('X'), dye).pattern("###").pattern("#X#").pattern("###").group("stained_terracotta").unlockedBy("has_terracotta", (Criterion)this.has(Blocks.TERRACOTTA)).save(this.output);
    }

    protected void concretePowder(ItemLike result, ItemLike dye) {
        ((ShapelessRecipeBuilder)this.shapeless(RecipeCategory.BUILDING_BLOCKS, result, 8).requires(dye).requires(Blocks.SAND, 4).requires(Blocks.GRAVEL, 4).group("concrete_powder").unlockedBy("has_sand", (Criterion)this.has(Blocks.SAND))).unlockedBy("has_gravel", (Criterion)this.has(Blocks.GRAVEL)).save(this.output);
    }

    protected void candle(ItemLike result, ItemLike dye) {
        this.shapeless(RecipeCategory.DECORATIONS, result).requires(Blocks.CANDLE).requires(dye).group("dyed_candle").unlockedBy(RecipeProvider.getHasName(dye), (Criterion)this.has(dye)).save(this.output);
    }

    protected void wall(RecipeCategory category, ItemLike result, ItemLike base) {
        this.wallBuilder(category, result, Ingredient.of(base)).unlockedBy(RecipeProvider.getHasName(base), this.has(base)).save(this.output);
    }

    private RecipeBuilder wallBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result, 6).define(Character.valueOf('#'), base).pattern("###").pattern("###");
    }

    private RecipeBuilder bricksBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result, 4).define(Character.valueOf('#'), base).pattern("##").pattern("##");
    }

    private RecipeBuilder tilesBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result, 4).define(Character.valueOf('#'), base).pattern("##").pattern("##");
    }

    private RecipeBuilder pillarBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result, 2).define(Character.valueOf('#'), base).pattern("#").pattern("#");
    }

    protected void polished(RecipeCategory category, ItemLike result, ItemLike base) {
        this.polishedBuilder(category, result, Ingredient.of(base)).unlockedBy(RecipeProvider.getHasName(base), this.has(base)).save(this.output);
    }

    private RecipeBuilder polishedBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result, 4).define(Character.valueOf('S'), base).pattern("SS").pattern("SS");
    }

    protected void cut(RecipeCategory category, ItemLike result, ItemLike base) {
        this.cutBuilder(category, result, Ingredient.of(base)).unlockedBy(RecipeProvider.getHasName(base), (Criterion)this.has(base)).save(this.output);
    }

    private ShapedRecipeBuilder cutBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result, 4).define(Character.valueOf('#'), base).pattern("##").pattern("##");
    }

    protected void chiseled(RecipeCategory category, ItemLike result, ItemLike base) {
        this.chiseledBuilder(category, result, Ingredient.of(base)).unlockedBy(RecipeProvider.getHasName(base), (Criterion)this.has(base)).save(this.output);
    }

    protected void mosaicBuilder(RecipeCategory category, ItemLike result, ItemLike base) {
        this.shaped(category, result).define(Character.valueOf('#'), base).pattern("#").pattern("#").unlockedBy(RecipeProvider.getHasName(base), (Criterion)this.has(base)).save(this.output);
    }

    protected ShapedRecipeBuilder chiseledBuilder(RecipeCategory category, ItemLike result, Ingredient base) {
        return this.shaped(category, result).define(Character.valueOf('#'), base).pattern("#").pattern("#");
    }

    protected void stonecutterResultFromBase(RecipeCategory category, ItemLike result, ItemLike base) {
        this.stonecutterResultFromBase(category, result, base, 1);
    }

    protected void stonecutterResultFromBase(RecipeCategory category, ItemLike result, ItemLike base, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(base), category, result, count).unlockedBy(RecipeProvider.getHasName(base), (Criterion)this.has(base)).save(this.output, RecipeProvider.getConversionRecipeName(result, base) + "_stonecutting");
    }

    private void smeltingResultFromBase(ItemLike result, ItemLike base) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(base), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, result, 0.1f, 200).unlockedBy(RecipeProvider.getHasName(base), (Criterion)this.has(base)).save(this.output);
    }

    protected void nineBlockStorageRecipes(RecipeCategory unpackedFormCategory, ItemLike unpackedForm, RecipeCategory packedFormCategory, ItemLike packedForm) {
        this.nineBlockStorageRecipes(unpackedFormCategory, unpackedForm, packedFormCategory, packedForm, RecipeProvider.getSimpleRecipeName(packedForm), null, RecipeProvider.getSimpleRecipeName(unpackedForm), null);
    }

    protected void nineBlockStorageRecipesWithCustomPacking(RecipeCategory unpackedFormCategory, ItemLike unpackedForm, RecipeCategory packedFormCategory, ItemLike packedForm, String packingRecipeId, String packingRecipeGroup) {
        this.nineBlockStorageRecipes(unpackedFormCategory, unpackedForm, packedFormCategory, packedForm, packingRecipeId, packingRecipeGroup, RecipeProvider.getSimpleRecipeName(unpackedForm), null);
    }

    protected void nineBlockStorageRecipesRecipesWithCustomUnpacking(RecipeCategory unpackedFormCategory, ItemLike unpackedForm, RecipeCategory packedFormCategory, ItemLike packedForm, String unpackingRecipeId, String unpackingRecipeGroup) {
        this.nineBlockStorageRecipes(unpackedFormCategory, unpackedForm, packedFormCategory, packedForm, RecipeProvider.getSimpleRecipeName(packedForm), null, unpackingRecipeId, unpackingRecipeGroup);
    }

    private void nineBlockStorageRecipes(RecipeCategory unpackedFormCategory, ItemLike unpackedForm, RecipeCategory packedFormCategory, ItemLike packedForm, String packingRecipeId, @Nullable String packingRecipeGroup, String unpackingRecipeId, @Nullable String unpackingRecipeGroup) {
        ((ShapelessRecipeBuilder)this.shapeless(unpackedFormCategory, unpackedForm, 9).requires(packedForm).group(unpackingRecipeGroup).unlockedBy(RecipeProvider.getHasName(packedForm), (Criterion)this.has(packedForm))).save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.parse(unpackingRecipeId)));
        ((ShapedRecipeBuilder)this.shaped(packedFormCategory, packedForm).define(Character.valueOf('#'), unpackedForm).pattern("###").pattern("###").pattern("###").group(packingRecipeGroup).unlockedBy(RecipeProvider.getHasName(unpackedForm), (Criterion)this.has(unpackedForm))).save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.parse(packingRecipeId)));
    }

    protected void copySmithingTemplate(ItemLike smithingTemplate, ItemLike baseMaterial) {
        this.shaped(RecipeCategory.MISC, smithingTemplate, 2).define(Character.valueOf('#'), Items.DIAMOND).define(Character.valueOf('C'), baseMaterial).define(Character.valueOf('S'), smithingTemplate).pattern("#S#").pattern("#C#").pattern("###").unlockedBy(RecipeProvider.getHasName(smithingTemplate), (Criterion)this.has(smithingTemplate)).save(this.output);
    }

    protected void copySmithingTemplate(ItemLike smithingTemplate, Ingredient baseMaterials) {
        this.shaped(RecipeCategory.MISC, smithingTemplate, 2).define(Character.valueOf('#'), Items.DIAMOND).define(Character.valueOf('C'), baseMaterials).define(Character.valueOf('S'), smithingTemplate).pattern("#S#").pattern("#C#").pattern("###").unlockedBy(RecipeProvider.getHasName(smithingTemplate), (Criterion)this.has(smithingTemplate)).save(this.output);
    }

    protected <T extends AbstractCookingRecipe> void cookRecipes(String source, AbstractCookingRecipe.Factory<T> factory, int cookingTime) {
        this.simpleCookingRecipe(source, factory, cookingTime, Items.BEEF, Items.COOKED_BEEF, 0.35f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.COD, Items.COOKED_COD, 0.35f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.KELP, Items.DRIED_KELP, 0.1f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.SALMON, Items.COOKED_SALMON, 0.35f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.MUTTON, Items.COOKED_MUTTON, 0.35f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.POTATO, Items.BAKED_POTATO, 0.35f);
        this.simpleCookingRecipe(source, factory, cookingTime, Items.RABBIT, Items.COOKED_RABBIT, 0.35f);
    }

    private <T extends AbstractCookingRecipe> void simpleCookingRecipe(String source, AbstractCookingRecipe.Factory<T> factory, int cookingTime, ItemLike base, ItemLike result, float experience) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(base), RecipeCategory.FOOD, CookingBookCategory.FOOD, result, experience, cookingTime, factory).unlockedBy(RecipeProvider.getHasName(base), (Criterion)this.has(base)).save(this.output, RecipeProvider.getItemName(result) + "_from_" + source);
    }

    protected void waxRecipes(FeatureFlagSet flagSet) {
        HoneycombItem.WAXABLES.get().forEach((block, waxedBlock) -> {
            if (!waxedBlock.requiredFeatures().isSubsetOf(flagSet)) {
                return;
            }
            Pair pair = (Pair)HoneycombItem.WAXED_RECIPES.getOrDefault(waxedBlock, (Object)Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)RecipeProvider.getItemName(waxedBlock)));
            RecipeCategory recipeCategory = (RecipeCategory)((Object)((Object)pair.getFirst()));
            String group = (String)pair.getSecond();
            this.shapeless(recipeCategory, (ItemLike)waxedBlock).requires((ItemLike)block).requires(Items.HONEYCOMB).group(group).unlockedBy(RecipeProvider.getHasName(block), (Criterion)this.has((ItemLike)block)).save(this.output, RecipeProvider.getConversionRecipeName(waxedBlock, Items.HONEYCOMB));
        });
    }

    protected void grate(Block grateBlock, Block material) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, grateBlock, 4).define(Character.valueOf('M'), material).pattern(" M ").pattern("M M").pattern(" M ").group(RecipeProvider.getItemName(grateBlock)).unlockedBy(RecipeProvider.getHasName(material), (Criterion)this.has(material)).save(this.output);
    }

    protected void copperBulb(Block copperBulb, Block copperMaterial) {
        ((ShapedRecipeBuilder)this.shaped(RecipeCategory.REDSTONE, copperBulb, 4).define(Character.valueOf('C'), copperMaterial).define(Character.valueOf('R'), Items.REDSTONE).define(Character.valueOf('B'), Items.BLAZE_ROD).pattern(" C ").pattern("CBC").pattern(" R ").unlockedBy(RecipeProvider.getHasName(copperMaterial), (Criterion)this.has(copperMaterial))).group(RecipeProvider.getItemName(copperBulb)).save(this.output);
    }

    protected void waxedChiseled(Block result, Block material) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, result).define(Character.valueOf('M'), material).pattern(" M ").pattern(" M ").group(RecipeProvider.getItemName(result)).unlockedBy(RecipeProvider.getHasName(material), (Criterion)this.has(material)).save(this.output);
    }

    protected void suspiciousStew(Item item, SuspiciousEffectHolder effectHolder) {
        ItemStackTemplate stew = new ItemStackTemplate(Items.SUSPICIOUS_STEW, DataComponentPatch.builder().set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effectHolder.getSuspiciousEffects()).build());
        this.shapeless(RecipeCategory.FOOD, stew).requires(Items.BOWL).requires(Items.BROWN_MUSHROOM).requires(Items.RED_MUSHROOM).requires(item).group("suspicious_stew").unlockedBy(RecipeProvider.getHasName(item), (Criterion)this.has(item)).save(this.output, RecipeProvider.getItemName(stew.item().value()) + "_from_" + RecipeProvider.getItemName(item));
    }

    protected void dyedItem(Item target, String group) {
        CustomCraftingRecipeBuilder.customCrafting(RecipeCategory.MISC, (commonInfo, bookInfo) -> new DyeRecipe((Recipe.CommonInfo)commonInfo, (CraftingRecipe.CraftingBookInfo)bookInfo, Ingredient.of((ItemLike)target), this.tag(ItemTags.DYES), new ItemStackTemplate(target))).unlockedBy(RecipeProvider.getHasName(target), this.has(target)).group(group).save(this.output, RecipeProvider.getItemName(target) + "_dyed");
    }

    protected void dyedShulkerBoxRecipe(Item dye, Item dyedResult) {
        TransmuteRecipeBuilder.transmute(RecipeCategory.DECORATIONS, this.tag(ItemTags.SHULKER_BOXES), Ingredient.of((ItemLike)dye), dyedResult).group("shulker_box_dye").unlockedBy("has_shulker_box", (Criterion)this.has(ItemTags.SHULKER_BOXES)).save(this.output);
    }

    protected void dyedBundleRecipe(Item dye, Item dyedResult) {
        TransmuteRecipeBuilder.transmute(RecipeCategory.TOOLS, this.tag(ItemTags.BUNDLES), Ingredient.of((ItemLike)dye), dyedResult).group("bundle_dye").unlockedBy(RecipeProvider.getHasName(dye), (Criterion)this.has(dye)).save(this.output);
    }

    protected void generateRecipes(BlockFamily family, FeatureFlagSet flagSet) {
        family.getVariants().forEach((variant, result) -> {
            if (!result.requiredFeatures().isSubsetOf(flagSet)) {
                return;
            }
            if (family.shouldGenerateCraftingRecipe()) {
                this.generateCraftingRecipe(family, (BlockFamily.Variant)((Object)variant), (Block)result, this.getBaseBlockForCrafting(family, (BlockFamily.Variant)((Object)variant)));
            }
            if (family.shouldGenerateSmeltingRecipe()) {
                this.generateSmeltingRecipe((BlockFamily.Variant)((Object)variant), (Block)result, this.getBaseBlockForCrafting(family, (BlockFamily.Variant)((Object)variant)));
            }
            if (family.shouldGenerateStonecutterRecipe()) {
                this.generateStonecutterRecipe(family, (BlockFamily.Variant)((Object)variant), family.getBaseBlock());
            }
        });
    }

    private void generateCraftingRecipe(BlockFamily family, BlockFamily.Variant variant, Block result, ItemLike base) {
        FamilyCraftingRecipeProvider recipeFunction = SHAPE_BUILDERS.get((Object)variant);
        if (recipeFunction != null) {
            RecipeBuilder builder = recipeFunction.create(this, result, base);
            family.getRecipeGroupPrefix().ifPresent(prefix -> builder.group(variant.getPrefixedRecipeGroup((String)prefix)));
            builder.unlockedBy(this.getCraftingCriterionName(family, variant, base), this.has(base));
            builder.save(this.output);
        }
    }

    private void generateSmeltingRecipe(BlockFamily.Variant variant, Block result, ItemLike base) {
        if (variant == BlockFamily.Variant.CRACKED) {
            this.smeltingResultFromBase(result, base);
        }
        if (variant == BlockFamily.Variant.COBBLED) {
            this.smeltingResultFromBase(base, result);
        }
    }

    private void generateStonecutterRecipe(BlockFamily family, BlockFamily.Variant variant, Block base) {
        BlockFamily childVariantFamily;
        FamilyStonecutterRecipeProvider recipeFunction = STONECUTTER_RECIPE_BUILDERS.get((Object)variant);
        if (recipeFunction != null) {
            recipeFunction.create(this, family.get(variant), base);
        }
        if ((variant == BlockFamily.Variant.POLISHED || variant == BlockFamily.Variant.CUT || variant == BlockFamily.Variant.BRICKS || variant == BlockFamily.Variant.TILES || variant == BlockFamily.Variant.PILLAR || variant == BlockFamily.Variant.COBBLED) && (childVariantFamily = BlockFamilies.getFamily(family.get(variant))) != null) {
            childVariantFamily.getVariants().forEach((childVariant, r) -> this.generateStonecutterRecipe(childVariantFamily, (BlockFamily.Variant)((Object)childVariant), base));
        }
    }

    private Block getBaseBlockForCrafting(BlockFamily family, BlockFamily.Variant variant) {
        BlockFamily.Variant baseVariant = variant.getBaseVariantForCrafting();
        if (baseVariant != null) {
            if (!family.getVariants().containsKey((Object)baseVariant)) {
                throw new IllegalStateException(baseVariant.name() + " is not defined for the family.");
            }
            return family.get(baseVariant);
        }
        return family.getBaseBlock();
    }

    private String getCraftingCriterionName(BlockFamily family, BlockFamily.Variant variant, ItemLike base) {
        if (base != family.getBaseBlock()) {
            return "has_" + variant.getBaseVariantForCrafting().getRecipeGroup();
        }
        return family.getRecipeUnlockedBy().orElseGet(() -> RecipeProvider.getHasName(base));
    }

    private static Criterion<EnterBlockTrigger.TriggerInstance> insideOf(Block block) {
        return CriteriaTriggers.ENTER_BLOCK.createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(block.builtInRegistryHolder()), Optional.empty()));
    }

    protected Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimal() {
        return CriteriaTriggers.BRED_ANIMALS.createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    private Criterion<InventoryChangeTrigger.TriggerInstance> has(MinMaxBounds.Ints count, ItemLike item) {
        return RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(this.items, item).withCount(count));
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(this.items, item));
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(this.items, tag));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate.Builder ... predicates) {
        return RecipeProvider.inventoryTrigger((ItemPredicate[])Arrays.stream(predicates).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate ... predicates) {
        return CriteriaTriggers.INVENTORY_CHANGED.createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(predicates)));
    }

    protected static String getHasName(ItemLike baseBlock) {
        return "has_" + RecipeProvider.getItemName(baseBlock);
    }

    protected static String getItemName(ItemLike itemLike) {
        return BuiltInRegistries.ITEM.getKey(itemLike.asItem()).getPath();
    }

    protected static String getSimpleRecipeName(ItemLike itemLike) {
        return RecipeProvider.getItemName(itemLike);
    }

    protected static String getConversionRecipeName(ItemLike product, ItemLike material) {
        return RecipeProvider.getItemName(product) + "_from_" + RecipeProvider.getItemName(material);
    }

    protected static String getSmeltingRecipeName(ItemLike product) {
        return RecipeProvider.getItemName(product) + "_from_smelting";
    }

    protected static String getBlastingRecipeName(ItemLike product) {
        return RecipeProvider.getItemName(product) + "_from_blasting";
    }

    protected Ingredient tag(TagKey<Item> id) {
        return Ingredient.of(this.items.getOrThrow(id));
    }

    protected ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item) {
        return ShapedRecipeBuilder.shaped(this.items, category, item);
    }

    protected ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item, int count) {
        return ShapedRecipeBuilder.shaped(this.items, category, item, count);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemStackTemplate result) {
        return ShapelessRecipeBuilder.shapeless(this.items, category, result);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item) {
        return ShapelessRecipeBuilder.shapeless(this.items, category, item);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item, int count) {
        return ShapelessRecipeBuilder.shapeless(this.items, category, item, count);
    }

    @FunctionalInterface
    private static interface FamilyCraftingRecipeProvider {
        public RecipeBuilder create(RecipeProvider var1, ItemLike var2, ItemLike var3);
    }

    @FunctionalInterface
    private static interface FamilyStonecutterRecipeProvider {
        public void create(RecipeProvider var1, ItemLike var2, ItemLike var3);
    }

    protected static abstract class Runner
    implements DataProvider {
        private final PackOutput packOutput;
        private final CompletableFuture<HolderLookup.Provider> registries;

        protected Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            this.packOutput = packOutput;
            this.registries = registries;
        }

        @Override
        public final CompletableFuture<?> run(final CachedOutput cache) {
            return this.registries.thenCompose(registries -> {
                PackOutput.PathProvider recipePathProvider = this.packOutput.createRegistryElementsPathProvider(Registries.RECIPE);
                PackOutput.PathProvider advancementPathProvider = this.packOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
                final HashSet allRecipes = Sets.newHashSet();
                final ArrayList tasks = new ArrayList();
                RecipeOutput recipeOutput = new RecipeOutput(){
                    final /* synthetic */ HolderLookup.Provider val$registries;
                    final /* synthetic */ PackOutput.PathProvider val$recipePathProvider;
                    final /* synthetic */ PackOutput.PathProvider val$advancementPathProvider;
                    {
                        this.val$registries = provider;
                        this.val$recipePathProvider = pathProvider;
                        this.val$advancementPathProvider = pathProvider2;
                        Objects.requireNonNull(this$0);
                    }

                    @Override
                    public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancementHolder) {
                        if (!allRecipes.add(id)) {
                            throw new IllegalStateException("Duplicate recipe " + String.valueOf(id.identifier()));
                        }
                        this.saveRecipe(id, recipe);
                        if (advancementHolder != null) {
                            this.saveAdvancement(advancementHolder);
                        }
                    }

                    @Override
                    public Advancement.Builder advancement() {
                        return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                    }

                    @Override
                    public void includeRootAdvancement() {
                        AdvancementHolder root = Advancement.Builder.recipeAdvancement().addCriterion("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())).build(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                        this.saveAdvancement(root);
                    }

                    private void saveRecipe(ResourceKey<Recipe<?>> id, Recipe<?> recipe) {
                        tasks.add(DataProvider.saveStable(cache, this.val$registries, Recipe.CODEC, recipe, this.val$recipePathProvider.json(id.identifier())));
                    }

                    private void saveAdvancement(AdvancementHolder advancementHolder) {
                        tasks.add(DataProvider.saveStable(cache, this.val$registries, Advancement.CODEC, advancementHolder.value(), this.val$advancementPathProvider.json(advancementHolder.id())));
                    }
                };
                this.createRecipeProvider((HolderLookup.Provider)registries, recipeOutput).buildRecipes();
                return CompletableFuture.allOf((CompletableFuture[])tasks.toArray(CompletableFuture[]::new));
            });
        }

        protected abstract RecipeProvider createRecipeProvider(HolderLookup.Provider var1, RecipeOutput var2);
    }
}

