/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.recipebook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

@Environment(value=EnvType.CLIENT)
public class RecipeCollection {
    public static final RecipeCollection EMPTY = new RecipeCollection(List.of());
    private final List<RecipeDisplayEntry> entries;
    private final Set<RecipeDisplayId> craftable = new HashSet<RecipeDisplayId>();
    private final Set<RecipeDisplayId> selected = new HashSet<RecipeDisplayId>();

    public RecipeCollection(List<RecipeDisplayEntry> recipes) {
        this.entries = recipes;
    }

    public void selectRecipes(StackedItemContents stackedContents, Predicate<RecipeDisplay> selector) {
        for (RecipeDisplayEntry entry : this.entries) {
            boolean isSelected = selector.test(entry.display());
            if (isSelected) {
                this.selected.add(entry.id());
            } else {
                this.selected.remove(entry.id());
            }
            if (isSelected && entry.canCraft(stackedContents)) {
                this.craftable.add(entry.id());
                continue;
            }
            this.craftable.remove(entry.id());
        }
    }

    public boolean isCraftable(RecipeDisplayId recipe) {
        return this.craftable.contains(recipe);
    }

    public boolean hasCraftable() {
        return !this.craftable.isEmpty();
    }

    public boolean hasAnySelected() {
        return !this.selected.isEmpty();
    }

    public List<RecipeDisplayEntry> getRecipes() {
        return this.entries;
    }

    public List<RecipeDisplayEntry> getSelectedRecipes(CraftableStatus selector) {
        Predicate<RecipeDisplayId> predicate = switch (selector.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.selected::contains;
            case 1 -> this.craftable::contains;
            case 2 -> recipe -> this.selected.contains(recipe) && !this.craftable.contains(recipe);
        };
        ArrayList<RecipeDisplayEntry> result = new ArrayList<RecipeDisplayEntry>();
        for (RecipeDisplayEntry entries : this.entries) {
            if (!predicate.test(entries.id())) continue;
            result.add(entries);
        }
        return result;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum CraftableStatus {
        ANY,
        CRAFTABLE,
        NOT_CRAFTABLE;

    }
}

