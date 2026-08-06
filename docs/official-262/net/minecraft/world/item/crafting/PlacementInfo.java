/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 */
package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.Ingredient;

public class PlacementInfo {
    public static final int EMPTY_SLOT = -1;
    public static final PlacementInfo NOT_PLACEABLE = new PlacementInfo(List.of(), IntList.of());
    private final List<Ingredient> ingredients;
    private final IntList slotsToIngredientIndex;

    private PlacementInfo(List<Ingredient> ingredients, IntList slotsToIngredientIndex) {
        this.ingredients = ingredients;
        this.slotsToIngredientIndex = slotsToIngredientIndex;
    }

    public static PlacementInfo create(Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return NOT_PLACEABLE;
        }
        return new PlacementInfo(List.of(ingredient), IntList.of((int)0));
    }

    public static PlacementInfo createFromOptionals(List<Optional<Ingredient>> ingredients) {
        int ingredientCount = ingredients.size();
        ArrayList<Ingredient> presentIngredients = new ArrayList<Ingredient>(ingredientCount);
        IntArrayList slotsToIngredientIndex = new IntArrayList(ingredientCount);
        int placementIndex = 0;
        for (Optional<Ingredient> maybeIngredient : ingredients) {
            if (maybeIngredient.isPresent()) {
                Ingredient ingredient = maybeIngredient.get();
                if (ingredient.isEmpty()) {
                    return NOT_PLACEABLE;
                }
                presentIngredients.add(ingredient);
                slotsToIngredientIndex.add(placementIndex++);
                continue;
            }
            slotsToIngredientIndex.add(-1);
        }
        return new PlacementInfo(presentIngredients, (IntList)slotsToIngredientIndex);
    }

    public static PlacementInfo create(List<Ingredient> ingredients) {
        int ingredientCount = ingredients.size();
        IntArrayList slotsToIngredientIndex = new IntArrayList(ingredientCount);
        for (int i = 0; i < ingredientCount; ++i) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.isEmpty()) {
                return NOT_PLACEABLE;
            }
            slotsToIngredientIndex.add(i);
        }
        return new PlacementInfo(ingredients, (IntList)slotsToIngredientIndex);
    }

    public IntList slotsToIngredientIndex() {
        return this.slotsToIngredientIndex;
    }

    public List<Ingredient> ingredients() {
        return this.ingredients;
    }

    public boolean isImpossibleToPlace() {
        return this.slotsToIngredientIndex.isEmpty();
    }
}

