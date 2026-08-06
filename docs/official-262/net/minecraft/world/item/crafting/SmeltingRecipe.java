/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class SmeltingRecipe
extends AbstractCookingRecipe {
    public static final MapCodec<SmeltingRecipe> MAP_CODEC = SmeltingRecipe.cookingMapCodec(SmeltingRecipe::new, 200);
    public static final StreamCodec<RegistryFriendlyByteBuf, SmeltingRecipe> STREAM_CODEC = SmeltingRecipe.cookingStreamCodec(SmeltingRecipe::new);
    public static final RecipeSerializer<SmeltingRecipe> SERIALIZER = new RecipeSerializer<SmeltingRecipe>(MAP_CODEC, STREAM_CODEC);

    public SmeltingRecipe(Recipe.CommonInfo commonInfo, AbstractCookingRecipe.CookingBookInfo bookInfo, Ingredient ingredient, ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, ingredient, result, experience, cookingTime);
    }

    @Override
    protected Item furnaceIcon() {
        return Items.FURNACE;
    }

    @Override
    public RecipeSerializer<SmeltingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<SmeltingRecipe> getType() {
        return RecipeType.SMELTING;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return switch (this.category()) {
            default -> throw new MatchException(null, null);
            case CookingBookCategory.BLOCKS -> RecipeBookCategories.FURNACE_BLOCKS;
            case CookingBookCategory.FOOD -> RecipeBookCategories.FURNACE_FOOD;
            case CookingBookCategory.MISC -> RecipeBookCategories.FURNACE_MISC;
        };
    }
}

