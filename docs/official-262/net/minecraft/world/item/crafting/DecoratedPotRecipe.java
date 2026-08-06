/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.crafting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotDecorations;

public class DecoratedPotRecipe
extends CustomRecipe {
    public static final MapCodec<DecoratedPotRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Ingredient.CODEC.fieldOf("back").forGetter(o -> o.backPattern), (App)Ingredient.CODEC.fieldOf("left").forGetter(o -> o.leftPattern), (App)Ingredient.CODEC.fieldOf("right").forGetter(o -> o.rightPattern), (App)Ingredient.CODEC.fieldOf("front").forGetter(o -> o.frontPattern), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, DecoratedPotRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DecoratedPotRecipe> STREAM_CODEC = StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, o -> o.backPattern, Ingredient.CONTENTS_STREAM_CODEC, o -> o.leftPattern, Ingredient.CONTENTS_STREAM_CODEC, o -> o.rightPattern, Ingredient.CONTENTS_STREAM_CODEC, o -> o.frontPattern, ItemStackTemplate.STREAM_CODEC, o -> o.result, DecoratedPotRecipe::new);
    public static final RecipeSerializer<DecoratedPotRecipe> SERIALIZER = new RecipeSerializer<DecoratedPotRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Ingredient backPattern;
    private final Ingredient leftPattern;
    private final Ingredient rightPattern;
    private final Ingredient frontPattern;
    private final ItemStackTemplate result;

    public DecoratedPotRecipe(Ingredient wallPattern, ItemStackTemplate result) {
        this(wallPattern, wallPattern, wallPattern, wallPattern, result);
    }

    public DecoratedPotRecipe(Ingredient backPattern, Ingredient leftPattern, Ingredient rightPattern, Ingredient frontPattern, ItemStackTemplate result) {
        this.backPattern = backPattern;
        this.leftPattern = leftPattern;
        this.rightPattern = rightPattern;
        this.frontPattern = frontPattern;
        this.result = result;
    }

    private static ItemStack back(CraftingInput input) {
        return input.getItem(1, 0);
    }

    private static ItemStack left(CraftingInput input) {
        return input.getItem(0, 1);
    }

    private static ItemStack right(CraftingInput input) {
        return input.getItem(2, 1);
    }

    private static ItemStack front(CraftingInput input) {
        return input.getItem(1, 2);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 4) {
            return false;
        }
        return this.backPattern.test(DecoratedPotRecipe.back(input)) && this.leftPattern.test(DecoratedPotRecipe.left(input)) && this.rightPattern.test(DecoratedPotRecipe.right(input)) && this.frontPattern.test(DecoratedPotRecipe.front(input));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        PotDecorations decorations = new PotDecorations(DecoratedPotRecipe.back(input).getItem(), DecoratedPotRecipe.left(input).getItem(), DecoratedPotRecipe.right(input).getItem(), DecoratedPotRecipe.front(input).getItem());
        DataComponentPatch components = DataComponentPatch.builder().set(DataComponents.POT_DECORATIONS, decorations).build();
        return this.result.apply(components);
    }

    @Override
    public RecipeSerializer<DecoratedPotRecipe> getSerializer() {
        return SERIALIZER;
    }
}

