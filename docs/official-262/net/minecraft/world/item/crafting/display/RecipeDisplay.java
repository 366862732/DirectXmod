/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.item.crafting.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public interface RecipeDisplay {
    public static final Codec<RecipeDisplay> CODEC = BuiltInRegistries.RECIPE_DISPLAY.byNameCodec().dispatch(RecipeDisplay::type, Type::codec);
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeDisplay> STREAM_CODEC = ByteBufCodecs.registry(Registries.RECIPE_DISPLAY).dispatch(RecipeDisplay::type, Type::streamCodec);

    public SlotDisplay result();

    public SlotDisplay craftingStation();

    public Type<? extends RecipeDisplay> type();

    default public boolean isEnabled(FeatureFlagSet enabledFeatures) {
        return this.result().isEnabled(enabledFeatures) && this.craftingStation().isEnabled(enabledFeatures);
    }

    public record Type<T extends RecipeDisplay>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
    }
}

