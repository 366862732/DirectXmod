/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.crafting;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

public class RecipeCache {
    private final @Nullable Entry[] entries;
    private WeakReference<@Nullable RecipeManager> cachedRecipeManager = new WeakReference<Object>(null);

    public RecipeCache(int capacity) {
        this.entries = new Entry[capacity];
    }

    public Optional<RecipeHolder<CraftingRecipe>> get(ServerLevel level, CraftingInput input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        this.validateRecipeManager(level);
        for (int i = 0; i < this.entries.length; ++i) {
            Entry entry = this.entries[i];
            if (entry == null || !entry.matches(input)) continue;
            this.moveEntryToFront(i);
            return Optional.ofNullable(entry.value());
        }
        return this.compute(input, level);
    }

    private void validateRecipeManager(ServerLevel level) {
        RecipeManager recipeManager = level.recipeAccess();
        if (recipeManager != this.cachedRecipeManager.get()) {
            this.cachedRecipeManager = new WeakReference<RecipeManager>(recipeManager);
            Arrays.fill(this.entries, null);
        }
    }

    private Optional<RecipeHolder<CraftingRecipe>> compute(CraftingInput input, ServerLevel level) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        this.insert(input, recipe.orElse(null));
        return recipe;
    }

    private void moveEntryToFront(int index) {
        if (index > 0) {
            Entry entry = this.entries[index];
            System.arraycopy(this.entries, 0, this.entries, 1, index);
            this.entries[0] = entry;
        }
    }

    private void insert(CraftingInput input, @Nullable RecipeHolder<CraftingRecipe> recipe) {
        NonNullList<ItemStack> key = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); ++i) {
            key.set(i, input.getItem(i).copyWithCount(1));
        }
        System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
        this.entries[0] = new Entry(key, input.width(), input.height(), recipe);
    }

    private record Entry(NonNullList<ItemStack> key, int width, int height, @Nullable RecipeHolder<CraftingRecipe> value) {
        public boolean matches(CraftingInput input) {
            if (this.width != input.width() || this.height != input.height()) {
                return false;
            }
            for (int i = 0; i < this.key.size(); ++i) {
                if (ItemStack.isSameItemSameComponents(this.key.get(i), input.getItem(i))) continue;
                return false;
            }
            return true;
        }
    }
}

