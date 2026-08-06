/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.data.recipes;

import java.util.Objects;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public interface RecipeBuilder {
    public static final Identifier ROOT_RECIPE_ADVANCEMENT = Identifier.withDefaultNamespace("recipes/root");

    public RecipeBuilder unlockedBy(String var1, Criterion<?> var2);

    public RecipeBuilder group(@Nullable String var1);

    public ResourceKey<Recipe<?>> defaultId();

    public void save(RecipeOutput var1, ResourceKey<Recipe<?>> var2);

    default public void save(RecipeOutput output) {
        this.save(output, this.defaultId());
    }

    default public void save(RecipeOutput output, String id) {
        ResourceKey<Recipe<?>> defaultKey = this.defaultId();
        ResourceKey<Recipe<?>> overriddenKey = ResourceKey.create(Registries.RECIPE, Identifier.parse(id));
        if (overriddenKey == defaultKey) {
            throw new IllegalStateException("Recipe " + id + " should remove its 'save' argument as it is equal to default one");
        }
        this.save(output, overriddenKey);
    }

    public static CraftingBookCategory determineCraftingBookCategory(RecipeCategory category) {
        return switch (category) {
            case RecipeCategory.BUILDING_BLOCKS -> CraftingBookCategory.BUILDING;
            case RecipeCategory.TOOLS, RecipeCategory.COMBAT -> CraftingBookCategory.EQUIPMENT;
            case RecipeCategory.REDSTONE -> CraftingBookCategory.REDSTONE;
            default -> CraftingBookCategory.MISC;
        };
    }

    public static Recipe.CommonInfo createCraftingCommonInfo(boolean showNotification) {
        return new Recipe.CommonInfo(showNotification);
    }

    public static CraftingRecipe.CraftingBookInfo createCraftingBookInfo(RecipeCategory category, @Nullable String group) {
        return new CraftingRecipe.CraftingBookInfo(RecipeBuilder.determineCraftingBookCategory(category), Objects.requireNonNullElse(group, ""));
    }

    public static ResourceKey<Recipe<?>> getDefaultRecipeId(ItemInstance result) {
        return ResourceKey.create(Registries.RECIPE, result.typeHolder().unwrapKey().orElseThrow().identifier());
    }
}

