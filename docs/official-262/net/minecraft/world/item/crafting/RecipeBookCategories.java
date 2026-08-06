/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.crafting;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeBookCategory;

public class RecipeBookCategories {
    public static final RecipeBookCategory CRAFTING_BUILDING_BLOCKS = RecipeBookCategories.register("crafting_building_blocks");
    public static final RecipeBookCategory CRAFTING_REDSTONE = RecipeBookCategories.register("crafting_redstone");
    public static final RecipeBookCategory CRAFTING_EQUIPMENT = RecipeBookCategories.register("crafting_equipment");
    public static final RecipeBookCategory CRAFTING_MISC = RecipeBookCategories.register("crafting_misc");
    public static final RecipeBookCategory FURNACE_FOOD = RecipeBookCategories.register("furnace_food");
    public static final RecipeBookCategory FURNACE_BLOCKS = RecipeBookCategories.register("furnace_blocks");
    public static final RecipeBookCategory FURNACE_MISC = RecipeBookCategories.register("furnace_misc");
    public static final RecipeBookCategory BLAST_FURNACE_BLOCKS = RecipeBookCategories.register("blast_furnace_blocks");
    public static final RecipeBookCategory BLAST_FURNACE_MISC = RecipeBookCategories.register("blast_furnace_misc");
    public static final RecipeBookCategory SMOKER_FOOD = RecipeBookCategories.register("smoker_food");
    public static final RecipeBookCategory STONECUTTER = RecipeBookCategories.register("stonecutter");
    public static final RecipeBookCategory SMITHING = RecipeBookCategories.register("smithing");
    public static final RecipeBookCategory CAMPFIRE = RecipeBookCategories.register("campfire");

    private static RecipeBookCategory register(String id) {
        return Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, id, new RecipeBookCategory());
    }

    public static RecipeBookCategory bootstrap(Registry<RecipeBookCategory> registry) {
        return CAMPFIRE;
    }
}

