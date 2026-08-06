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
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

public interface Recipe<T extends RecipeInput> {
    public static final Codec<Recipe<?>> CODEC = BuiltInRegistries.RECIPE_SERIALIZER.byNameCodec().dispatch(Recipe::getSerializer, RecipeSerializer::codec);
    public static final Codec<ResourceKey<Recipe<?>>> KEY_CODEC = ResourceKey.codec(Registries.RECIPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER).dispatch(Recipe::getSerializer, RecipeSerializer::streamCodec);

    public boolean matches(T var1, Level var2);

    public ItemStack assemble(T var1);

    default public boolean isSpecial() {
        return false;
    }

    public boolean showNotification();

    public String group();

    public RecipeSerializer<? extends Recipe<T>> getSerializer();

    public RecipeType<? extends Recipe<T>> getType();

    public PlacementInfo placementInfo();

    default public List<RecipeDisplay> display() {
        return List.of();
    }

    public RecipeBookCategory recipeBookCategory();

    public static interface BookInfo<CategoryType> {
        public CategoryType category();

        public String group();

        public static <CategoryType, SelfType extends BookInfo<CategoryType>> MapCodec<SelfType> mapCodec(Codec<CategoryType> categoryCodec, CategoryType defaultCategory, Constructor<CategoryType, SelfType> constructor) {
            return RecordCodecBuilder.mapCodec(i -> i.group((App)categoryCodec.optionalFieldOf("category", defaultCategory).forGetter(BookInfo::category), (App)Codec.STRING.optionalFieldOf("group", (Object)"").forGetter(BookInfo::group)).apply((Applicative)i, (BiFunction)constructor));
        }

        public static <CategoryType, SelfType extends BookInfo<CategoryType>> StreamCodec<RegistryFriendlyByteBuf, SelfType> streamCodec(StreamCodec<? super RegistryFriendlyByteBuf, CategoryType> categoryCodec, Constructor<CategoryType, SelfType> constructor) {
            return StreamCodec.composite(categoryCodec, BookInfo::category, ByteBufCodecs.STRING_UTF8, BookInfo::group, constructor);
        }

        @FunctionalInterface
        public static interface Constructor<CategoryType, SelfType extends BookInfo<CategoryType>>
        extends BiFunction<CategoryType, String, SelfType> {
        }
    }

    public record CommonInfo(boolean showNotification) {
        public static final MapCodec<CommonInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.optionalFieldOf("show_notification", (Object)true).forGetter(CommonInfo::showNotification)).apply((Applicative)i, CommonInfo::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, CommonInfo> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, CommonInfo::showNotification, CommonInfo::new);
    }
}

