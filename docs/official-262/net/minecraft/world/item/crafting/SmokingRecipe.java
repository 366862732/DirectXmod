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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class SmokingRecipe
extends AbstractCookingRecipe {
    public static final MapCodec<SmokingRecipe> MAP_CODEC = SmokingRecipe.cookingMapCodec(SmokingRecipe::new, 100);
    public static final StreamCodec<RegistryFriendlyByteBuf, SmokingRecipe> STREAM_CODEC = SmokingRecipe.cookingStreamCodec(SmokingRecipe::new);
    public static final RecipeSerializer<SmokingRecipe> SERIALIZER = new RecipeSerializer<SmokingRecipe>(MAP_CODEC, STREAM_CODEC);

    public SmokingRecipe(Recipe.CommonInfo commonInfo, AbstractCookingRecipe.CookingBookInfo bookInfo, Ingredient ingredient, ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, ingredient, result, experience, cookingTime);
    }

    @Override
    protected Item furnaceIcon() {
        return Items.SMOKER;
    }

    @Override
    public RecipeType<SmokingRecipe> getType() {
        return RecipeType.SMOKING;
    }

    @Override
    public RecipeSerializer<SmokingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.SMOKER_FOOD;
    }
}

