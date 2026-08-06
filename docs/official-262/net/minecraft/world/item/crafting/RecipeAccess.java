/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public interface RecipeAccess {
    public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> var1);

    public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes();
}

