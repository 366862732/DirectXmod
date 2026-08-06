/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.recipebook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

@Environment(value=EnvType.CLIENT)
public interface RecipeUpdateListener {
    public void recipesUpdated();

    public void fillGhostRecipe(RecipeDisplay var1);
}

