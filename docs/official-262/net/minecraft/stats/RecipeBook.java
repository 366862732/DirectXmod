/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.stats;

import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;

public class RecipeBook {
    protected final RecipeBookSettings bookSettings = new RecipeBookSettings();

    public boolean isOpen(RecipeBookType recipeBookType) {
        return this.bookSettings.isOpen(recipeBookType);
    }

    public void setOpen(RecipeBookType recipeBookType, boolean open) {
        this.bookSettings.setOpen(recipeBookType, open);
    }

    public boolean isFiltering(RecipeBookType type) {
        return this.bookSettings.isFiltering(type);
    }

    public void setFiltering(RecipeBookType type, boolean filtering) {
        this.bookSettings.setFiltering(type, filtering);
    }

    public void setBookSettings(RecipeBookSettings settings) {
        this.bookSettings.replaceFrom(settings);
    }

    public RecipeBookSettings getBookSettings() {
        return this.bookSettings;
    }

    public void setBookSetting(RecipeBookType bookType, boolean open, boolean filtering) {
        this.bookSettings.setOpen(bookType, open);
        this.bookSettings.setFiltering(bookType, filtering);
    }
}

