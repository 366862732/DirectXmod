/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.data.recipes;

import java.util.Objects;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class SimpleCookingRecipeBuilder
implements RecipeBuilder {
    private final RecipeCategory craftingCategory;
    private final CookingBookCategory cookingCategory;
    private final ItemStackTemplate result;
    private final Ingredient ingredient;
    private final float experience;
    private final int cookingTime;
    private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
    private @Nullable String group;
    private final AbstractCookingRecipe.Factory<?> factory;

    private SimpleCookingRecipeBuilder(RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemStackTemplate result, Ingredient ingredient, float experience, int cookingTime, AbstractCookingRecipe.Factory<?> factory) {
        this.craftingCategory = craftingCategory;
        this.cookingCategory = cookingCategory;
        this.result = result;
        this.ingredient = ingredient;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.factory = factory;
    }

    private SimpleCookingRecipeBuilder(RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemLike result, Ingredient ingredient, float experience, int cookingTime, AbstractCookingRecipe.Factory<?> factory) {
        this(craftingCategory, cookingCategory, new ItemStackTemplate(result.asItem()), ingredient, experience, cookingTime, factory);
    }

    public static <T extends AbstractCookingRecipe> SimpleCookingRecipeBuilder generic(Ingredient ingredient, RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemLike result, float experience, int cookingTime, AbstractCookingRecipe.Factory<T> factory) {
        return new SimpleCookingRecipeBuilder(craftingCategory, cookingCategory, result, ingredient, experience, cookingTime, factory);
    }

    public static SimpleCookingRecipeBuilder campfireCooking(Ingredient ingredient, RecipeCategory craftingCategory, ItemLike result, float experience, int cookingTime) {
        return new SimpleCookingRecipeBuilder(craftingCategory, CookingBookCategory.FOOD, result, ingredient, experience, cookingTime, CampfireCookingRecipe::new);
    }

    public static SimpleCookingRecipeBuilder blasting(Ingredient ingredient, RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemLike result, float experience, int cookingTime) {
        return new SimpleCookingRecipeBuilder(craftingCategory, cookingCategory, result, ingredient, experience, cookingTime, BlastingRecipe::new);
    }

    public static SimpleCookingRecipeBuilder smelting(Ingredient ingredient, RecipeCategory craftingCategory, CookingBookCategory cookingCategory, ItemLike result, float experience, int cookingTime) {
        return new SimpleCookingRecipeBuilder(craftingCategory, cookingCategory, result, ingredient, experience, cookingTime, SmeltingRecipe::new);
    }

    public static SimpleCookingRecipeBuilder smoking(Ingredient ingredient, RecipeCategory craftingCategory, ItemLike result, float experience, int cookingTime) {
        return new SimpleCookingRecipeBuilder(craftingCategory, CookingBookCategory.FOOD, result, ingredient, experience, cookingTime, SmokingRecipe::new);
    }

    @Override
    public SimpleCookingRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.advancementBuilder.unlockedBy(name, criterion);
        return this;
    }

    @Override
    public SimpleCookingRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        return RecipeBuilder.getDefaultRecipeId(this.result);
    }

    @Override
    public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
        Object recipe = this.factory.create(RecipeBuilder.createCraftingCommonInfo(true), new AbstractCookingRecipe.CookingBookInfo(this.cookingCategory, Objects.requireNonNullElse(this.group, "")), this.ingredient, this.result, this.experience, this.cookingTime);
        output.accept(id, (Recipe<?>)recipe, this.advancementBuilder.build(output, id, this.craftingCategory));
    }
}

