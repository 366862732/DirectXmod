/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.crafting;

import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.jspecify.annotations.Nullable;

public abstract class SimpleSmithingRecipe
implements SmithingRecipe {
    protected final Recipe.CommonInfo commonInfo;
    private @Nullable PlacementInfo placementInfo;

    protected SimpleSmithingRecipe(Recipe.CommonInfo commonInfo) {
        this.commonInfo = commonInfo;
    }

    @Override
    public abstract RecipeSerializer<? extends SimpleSmithingRecipe> getSerializer();

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = this.createPlacementInfo();
        }
        return this.placementInfo;
    }

    protected abstract PlacementInfo createPlacementInfo();

    @Override
    public String group() {
        return "";
    }

    @Override
    public final boolean showNotification() {
        return this.commonInfo.showNotification();
    }
}

