/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.crafting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public abstract class AbstractCookingRecipe
extends SingleItemRecipe {
    protected final CookingBookInfo bookInfo;
    private final float experience;
    private final int cookingTime;

    public AbstractCookingRecipe(Recipe.CommonInfo commonInfo, CookingBookInfo bookInfo, Ingredient ingredient, ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, ingredient, result);
        this.bookInfo = bookInfo;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @Override
    public abstract RecipeSerializer<? extends AbstractCookingRecipe> getSerializer();

    @Override
    public abstract RecipeType<? extends AbstractCookingRecipe> getType();

    public float experience() {
        return this.experience;
    }

    public int cookingTime() {
        return this.cookingTime;
    }

    public CookingBookCategory category() {
        return this.bookInfo.category;
    }

    @Override
    public String group() {
        return this.bookInfo.group;
    }

    protected abstract Item furnaceIcon();

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new FurnaceRecipeDisplay(this.input().display(), SlotDisplay.AnyFuel.INSTANCE, new SlotDisplay.ItemStackSlotDisplay(this.result()), new SlotDisplay.ItemSlotDisplay(this.furnaceIcon()), this.cookingTime, this.experience));
    }

    public static <T extends AbstractCookingRecipe> MapCodec<T> cookingMapCodec(Factory<T> factory, int defaultCookingTime) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo), (App)CookingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo), (App)Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(SingleItemRecipe::result), (App)Codec.FLOAT.optionalFieldOf("experience", (Object)Float.valueOf(0.0f)).forGetter(AbstractCookingRecipe::experience), (App)Codec.INT.optionalFieldOf("cookingtime", (Object)defaultCookingTime).forGetter(AbstractCookingRecipe::cookingTime)).apply((Applicative)i, factory::create));
    }

    public static <T extends AbstractCookingRecipe> StreamCodec<RegistryFriendlyByteBuf, T> cookingStreamCodec(Factory<T> factory) {
        return StreamCodec.composite(Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo, CookingBookInfo.STREAM_CODEC, o -> o.bookInfo, Ingredient.CONTENTS_STREAM_CODEC, SingleItemRecipe::input, ItemStackTemplate.STREAM_CODEC, SingleItemRecipe::result, ByteBufCodecs.FLOAT, AbstractCookingRecipe::experience, ByteBufCodecs.INT, AbstractCookingRecipe::cookingTime, factory::create);
    }

    public static final class CookingBookInfo
    extends Record
    implements Recipe.BookInfo<CookingBookCategory> {
        private final CookingBookCategory category;
        private final String group;
        public static final MapCodec<CookingBookInfo> MAP_CODEC = Recipe.BookInfo.mapCodec(CookingBookCategory.CODEC, CookingBookCategory.MISC, CookingBookInfo::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, CookingBookInfo> STREAM_CODEC = Recipe.BookInfo.streamCodec(CookingBookCategory.STREAM_CODEC, CookingBookInfo::new);

        public CookingBookInfo(CookingBookCategory category, String group) {
            this.category = category;
            this.group = group;
        }

        @Override
        public final String toString() {
            return ObjectMethods.bootstrap("toString", new MethodHandle[]{CookingBookInfo.class, "category;group", "category", "group"}, this);
        }

        @Override
        public final int hashCode() {
            return (int)ObjectMethods.bootstrap("hashCode", new MethodHandle[]{CookingBookInfo.class, "category;group", "category", "group"}, this);
        }

        @Override
        public final boolean equals(Object o) {
            return (boolean)ObjectMethods.bootstrap("equals", new MethodHandle[]{CookingBookInfo.class, "category;group", "category", "group"}, this, o);
        }

        @Override
        public CookingBookCategory category() {
            return this.category;
        }

        @Override
        public String group() {
            return this.group;
        }
    }

    @FunctionalInterface
    public static interface Factory<T extends AbstractCookingRecipe> {
        public T create(Recipe.CommonInfo var1, CookingBookInfo var2, Ingredient var3, ItemStackTemplate var4, float var5, int var6);
    }
}

