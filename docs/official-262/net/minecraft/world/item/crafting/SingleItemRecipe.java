/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.crafting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class SingleItemRecipe
implements Recipe<SingleRecipeInput> {
    protected final Recipe.CommonInfo commonInfo;
    private final Ingredient input;
    private final ItemStackTemplate result;
    private @Nullable PlacementInfo placementInfo;

    public SingleItemRecipe(Recipe.CommonInfo commonInfo, Ingredient input, ItemStackTemplate result) {
        this.commonInfo = commonInfo;
        this.input = input;
        this.result = result;
    }

    @Override
    public abstract RecipeSerializer<? extends SingleItemRecipe> getSerializer();

    @Override
    public abstract RecipeType<? extends SingleItemRecipe> getType();

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    public Ingredient input() {
        return this.input;
    }

    protected ItemStackTemplate result() {
        return this.result;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.create(this.input);
        }
        return this.placementInfo;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    public static <T extends SingleItemRecipe> MapCodec<T> simpleMapCodec(Factory<T> factory) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo), (App)Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(SingleItemRecipe::result)).apply((Applicative)i, factory::create));
    }

    public static <T extends SingleItemRecipe> StreamCodec<RegistryFriendlyByteBuf, T> simpleStreamCodec(Factory<T> factory) {
        return StreamCodec.composite(Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo, Ingredient.CONTENTS_STREAM_CODEC, SingleItemRecipe::input, ItemStackTemplate.STREAM_CODEC, SingleItemRecipe::result, factory::create);
    }

    @FunctionalInterface
    public static interface Factory<T extends SingleItemRecipe> {
        public T create(Recipe.CommonInfo var1, Ingredient var2, ItemStackTemplate var3);
    }
}

