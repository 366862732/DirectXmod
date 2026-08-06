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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class ShieldDecorationRecipe
extends CustomRecipe {
    public static final MapCodec<ShieldDecorationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Ingredient.CODEC.fieldOf("banner").forGetter(o -> o.banner), (App)Ingredient.CODEC.fieldOf("target").forGetter(o -> o.target), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, ShieldDecorationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldDecorationRecipe> STREAM_CODEC = StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, o -> o.banner, Ingredient.CONTENTS_STREAM_CODEC, o -> o.target, ItemStackTemplate.STREAM_CODEC, o -> o.result, ShieldDecorationRecipe::new);
    public static final RecipeSerializer<ShieldDecorationRecipe> SERIALIZER = new RecipeSerializer<ShieldDecorationRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Ingredient banner;
    private final Ingredient target;
    private final ItemStackTemplate result;

    public ShieldDecorationRecipe(Ingredient banner, Ingredient target, ItemStackTemplate result) {
        this.banner = banner;
        this.target = target;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) {
            return false;
        }
        boolean hasClearTarget = false;
        boolean hasPatternBanner = false;
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.banner.test(itemStack) && itemStack.getItem() instanceof BannerItem) {
                if (hasPatternBanner) {
                    return false;
                }
                hasPatternBanner = true;
                continue;
            }
            if (this.target.test(itemStack)) {
                if (hasClearTarget) {
                    return false;
                }
                BannerPatternLayers patterns = itemStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
                if (!patterns.layers().isEmpty()) {
                    return false;
                }
                hasClearTarget = true;
                continue;
            }
            return false;
        }
        return hasClearTarget && hasPatternBanner;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        BannerPatternLayers patterns = null;
        DyeColor baseColor = DyeColor.WHITE;
        ItemStack target = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); ++slot) {
            Item item;
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.banner.test(itemStack) && (item = itemStack.getItem()) instanceof BannerItem) {
                BannerItem bannerItem = (BannerItem)item;
                patterns = itemStack.get(DataComponents.BANNER_PATTERNS);
                baseColor = bannerItem.getColor();
                continue;
            }
            if (!this.target.test(itemStack)) continue;
            target = itemStack;
        }
        ItemStack result = TransmuteRecipe.createWithOriginalComponents(this.result, target);
        result.set(DataComponents.BANNER_PATTERNS, patterns);
        result.set(DataComponents.BASE_COLOR, baseColor);
        return result;
    }

    @Override
    public RecipeSerializer<ShieldDecorationRecipe> getSerializer() {
        return SERIALIZER;
    }
}

