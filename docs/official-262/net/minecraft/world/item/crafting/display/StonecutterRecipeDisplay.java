/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.crafting.display;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record StonecutterRecipeDisplay(SlotDisplay input, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay
{
    public static final MapCodec<StonecutterRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)SlotDisplay.CODEC.fieldOf("input").forGetter(StonecutterRecipeDisplay::input), (App)SlotDisplay.CODEC.fieldOf("result").forGetter(StonecutterRecipeDisplay::result), (App)SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(StonecutterRecipeDisplay::craftingStation)).apply((Applicative)i, StonecutterRecipeDisplay::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StonecutterRecipeDisplay> STREAM_CODEC = StreamCodec.composite(SlotDisplay.STREAM_CODEC, StonecutterRecipeDisplay::input, SlotDisplay.STREAM_CODEC, StonecutterRecipeDisplay::result, SlotDisplay.STREAM_CODEC, StonecutterRecipeDisplay::craftingStation, StonecutterRecipeDisplay::new);
    public static final RecipeDisplay.Type<StonecutterRecipeDisplay> TYPE = new RecipeDisplay.Type<StonecutterRecipeDisplay>(MAP_CODEC, STREAM_CODEC);

    public RecipeDisplay.Type<StonecutterRecipeDisplay> type() {
        return TYPE;
    }
}

