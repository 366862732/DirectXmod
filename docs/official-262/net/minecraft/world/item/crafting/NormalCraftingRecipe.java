/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.crafting;

import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.Nullable;

public abstract class NormalCraftingRecipe
implements CraftingRecipe {
    protected final Recipe.CommonInfo commonInfo;
    protected final CraftingRecipe.CraftingBookInfo bookInfo;
    private @Nullable PlacementInfo placementInfo;

    protected NormalCraftingRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo) {
        this.commonInfo = commonInfo;
        this.bookInfo = bookInfo;
    }

    @Override
    public abstract RecipeSerializer<? extends NormalCraftingRecipe> getSerializer();

    @Override
    public final String group() {
        return this.bookInfo.group();
    }

    @Override
    public final CraftingBookCategory category() {
        return this.bookInfo.category();
    }

    @Override
    public final boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    protected abstract PlacementInfo createPlacementInfo();

    @Override
    public final PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = this.createPlacementInfo();
        }
        return this.placementInfo;
    }
}

