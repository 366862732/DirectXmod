/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipeHelper {
    public static <T> void placeRecipe(int gridWidth, int gridHeight, Recipe<?> recipe, Iterable<T> entries, Output<T> output) {
        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shapedRecipe = (ShapedRecipe)recipe;
            PlaceRecipeHelper.placeRecipe(gridWidth, gridHeight, shapedRecipe.getWidth(), shapedRecipe.getHeight(), entries, output);
        } else {
            PlaceRecipeHelper.placeRecipe(gridWidth, gridHeight, gridWidth, gridHeight, entries, output);
        }
    }

    public static <T> void placeRecipe(int gridWidth, int gridHeight, int recipeWidth, int recipeHeight, Iterable<T> entries, Output<T> output) {
        Iterator<T> iterator = entries.iterator();
        int gridIndex = 0;
        block0: for (int gridYPos = 0; gridYPos < gridHeight; ++gridYPos) {
            boolean shouldCenterRecipe = (float)recipeHeight < (float)gridHeight / 2.0f;
            int startPosCenterRecipe = Mth.floor((float)gridHeight / 2.0f - (float)recipeHeight / 2.0f);
            if (shouldCenterRecipe && startPosCenterRecipe > gridYPos) {
                gridIndex += gridWidth;
                ++gridYPos;
            }
            for (int gridXPos = 0; gridXPos < gridWidth; ++gridXPos) {
                boolean addIngredientToSlot;
                if (!iterator.hasNext()) {
                    return;
                }
                shouldCenterRecipe = (float)recipeWidth < (float)gridWidth / 2.0f;
                startPosCenterRecipe = Mth.floor((float)gridWidth / 2.0f - (float)recipeWidth / 2.0f);
                int totalRecipeWidthInGrid = recipeWidth;
                boolean bl = addIngredientToSlot = gridXPos < recipeWidth;
                if (shouldCenterRecipe) {
                    totalRecipeWidthInGrid = startPosCenterRecipe + recipeWidth;
                    boolean bl2 = addIngredientToSlot = startPosCenterRecipe <= gridXPos && gridXPos < startPosCenterRecipe + recipeWidth;
                }
                if (addIngredientToSlot) {
                    output.addItemToSlot(iterator.next(), gridIndex, gridXPos, gridYPos);
                } else if (totalRecipeWidthInGrid == gridXPos) {
                    gridIndex += gridWidth - gridXPos;
                    continue block0;
                }
                ++gridIndex;
            }
        }
    }

    @FunctionalInterface
    public static interface Output<T> {
        public void addItemToSlot(T var1, int var2, int var3, int var4);
    }
}

