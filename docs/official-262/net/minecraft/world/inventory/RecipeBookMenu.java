/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeHolder;

public abstract class RecipeBookMenu
extends AbstractContainerMenu {
    public RecipeBookMenu(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    public abstract PostPlaceAction handlePlacement(boolean var1, boolean var2, RecipeHolder<?> var3, ServerLevel var4, Inventory var5);

    public abstract void fillCraftSlotsStackedContents(StackedItemContents var1);

    public abstract RecipeBookType getRecipeBookType();

    public static enum PostPlaceAction {
        NOTHING,
        PLACE_GHOST_RECIPE;

    }
}

