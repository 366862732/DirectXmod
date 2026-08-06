/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 */
package net.minecraft.world.item.crafting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe
extends CustomRecipe {
    public static final MapCodec<FireworkStarFadeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Ingredient.CODEC.fieldOf("target").forGetter(o -> o.target), (App)Ingredient.CODEC.fieldOf("dye").forGetter(o -> o.dye), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, FireworkStarFadeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireworkStarFadeRecipe> STREAM_CODEC = StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, o -> o.target, Ingredient.CONTENTS_STREAM_CODEC, o -> o.dye, ItemStackTemplate.STREAM_CODEC, o -> o.result, FireworkStarFadeRecipe::new);
    public static final RecipeSerializer<FireworkStarFadeRecipe> SERIALIZER = new RecipeSerializer<FireworkStarFadeRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Ingredient target;
    private final Ingredient dye;
    private final ItemStackTemplate result;

    public FireworkStarFadeRecipe(Ingredient target, Ingredient dye, ItemStackTemplate result) {
        this.target = target;
        this.dye = dye;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() < 2) {
            return false;
        }
        boolean hasDye = false;
        boolean hasTarget = false;
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.dye.test(itemStack) && itemStack.has(DataComponents.DYE)) {
                hasDye = true;
                continue;
            }
            if (this.target.test(itemStack)) {
                if (hasTarget) {
                    return false;
                }
                hasTarget = true;
                continue;
            }
            return false;
        }
        return hasTarget && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        IntArrayList colors = new IntArrayList();
        ItemStack targetStack = null;
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (this.dye.test(itemStack)) {
                DyeColor dye = itemStack.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
                colors.add(dye.getFireworkColor());
                continue;
            }
            if (!this.target.test(itemStack)) continue;
            targetStack = itemStack;
        }
        if (targetStack == null || colors.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = TransmuteRecipe.createWithOriginalComponents(this.result, targetStack);
        result.update(DataComponents.FIREWORK_EXPLOSION, FireworkExplosion.DEFAULT, colors, FireworkExplosion::withFadeColors);
        return result;
    }

    @Override
    public RecipeSerializer<FireworkStarFadeRecipe> getSerializer() {
        return SERIALIZER;
    }
}

