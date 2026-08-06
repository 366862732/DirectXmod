/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public interface CraftingRecipe
extends Recipe<CraftingInput> {
    @Override
    default public RecipeType<CraftingRecipe> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer();

    public CraftingBookCategory category();

    default public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return CraftingRecipe.defaultCraftingReminder(input);
    }

    public static NonNullList<ItemStack> defaultCraftingReminder(CraftingInput input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < result.size(); ++slot) {
            Item item = input.getItem(slot).getItem();
            ItemStackTemplate remainder = item.getCraftingRemainder();
            result.set(slot, remainder != null ? remainder.create() : ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    default public RecipeBookCategory recipeBookCategory() {
        return switch (this.category()) {
            default -> throw new MatchException(null, null);
            case CraftingBookCategory.BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
            case CraftingBookCategory.EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
            case CraftingBookCategory.REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
            case CraftingBookCategory.MISC -> RecipeBookCategories.CRAFTING_MISC;
        };
    }

    public static final class CraftingBookInfo
    extends Record
    implements Recipe.BookInfo<CraftingBookCategory> {
        private final CraftingBookCategory category;
        private final String group;
        public static final MapCodec<CraftingBookInfo> MAP_CODEC = Recipe.BookInfo.mapCodec(CraftingBookCategory.CODEC, CraftingBookCategory.MISC, CraftingBookInfo::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, CraftingBookInfo> STREAM_CODEC = Recipe.BookInfo.streamCodec(CraftingBookCategory.STREAM_CODEC, CraftingBookInfo::new);

        public CraftingBookInfo(CraftingBookCategory category, String group) {
            this.category = category;
            this.group = group;
        }

        @Override
        public final String toString() {
            return ObjectMethods.bootstrap("toString", new MethodHandle[]{CraftingBookInfo.class, "category;group", "category", "group"}, this);
        }

        @Override
        public final int hashCode() {
            return (int)ObjectMethods.bootstrap("hashCode", new MethodHandle[]{CraftingBookInfo.class, "category;group", "category", "group"}, this);
        }

        @Override
        public final boolean equals(Object o) {
            return (boolean)ObjectMethods.bootstrap("equals", new MethodHandle[]{CraftingBookInfo.class, "category;group", "category", "group"}, this, o);
        }

        @Override
        public CraftingBookCategory category() {
            return this.category;
        }

        @Override
        public String group() {
            return this.group;
        }
    }
}

