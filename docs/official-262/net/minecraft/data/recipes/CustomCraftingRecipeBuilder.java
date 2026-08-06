/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.data.recipes;

import java.util.function.BiFunction;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public class CustomCraftingRecipeBuilder {
    private final RecipeCategory category;
    private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
    private @Nullable String group;
    private final Factory factory;

    public CustomCraftingRecipeBuilder(RecipeCategory category, Factory factory) {
        this.category = category;
        this.factory = factory;
    }

    public static CustomCraftingRecipeBuilder customCrafting(RecipeCategory category, Factory factory) {
        return new CustomCraftingRecipeBuilder(category, factory);
    }

    public CustomCraftingRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.advancementBuilder.unlockedBy(name, criterion);
        return this;
    }

    public CustomCraftingRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, String name) {
        this.save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
    }

    public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
        Recipe.CommonInfo commonInfo = RecipeBuilder.createCraftingCommonInfo(true);
        CraftingRecipe.CraftingBookInfo bookInfo = RecipeBuilder.createCraftingBookInfo(this.category, this.group);
        Recipe recipe = (Recipe)this.factory.apply(commonInfo, bookInfo);
        output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
    }

    @FunctionalInterface
    public static interface Factory
    extends BiFunction<Recipe.CommonInfo, CraftingRecipe.CraftingBookInfo, Recipe<?>> {
    }
}

