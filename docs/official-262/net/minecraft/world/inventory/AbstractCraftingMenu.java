/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.List;
import java.util.Objects;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public abstract class AbstractCraftingMenu
extends RecipeBookMenu {
    private final int width;
    private final int height;
    protected final CraftingContainer craftSlots;
    protected final ResultContainer resultSlots = new ResultContainer();

    public AbstractCraftingMenu(MenuType<?> menuType, int containerId, int width, int height) {
        super(menuType, containerId);
        this.width = width;
        this.height = height;
        this.craftSlots = new TransientCraftingContainer(this, width, height);
    }

    protected Slot addResultSlot(Player player, int x, int y) {
        return this.addSlot(new ResultSlot(player, this.craftSlots, this.resultSlots, 0, x, y));
    }

    protected void addCraftingGridSlots(int left, int top) {
        for (int y = 0; y < this.width; ++y) {
            for (int x = 0; x < this.height; ++x) {
                this.addSlot(new Slot(this.craftSlots, x + y * this.width, left + x * 18, top + y * 18));
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> recipe, ServerLevel level, Inventory inventory) {
        RecipeHolder<CraftingRecipe> typedRecipe = recipe;
        this.beginPlacingRecipe();
        try {
            List<Slot> inputSlots = this.getInputGridSlots();
            RecipeBookMenu.PostPlaceAction postPlaceAction = ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>(this){
                final /* synthetic */ AbstractCraftingMenu this$0;
                {
                    AbstractCraftingMenu abstractCraftingMenu = this$0;
                    Objects.requireNonNull(abstractCraftingMenu);
                    this.this$0 = abstractCraftingMenu;
                }

                @Override
                public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
                    this.this$0.fillCraftSlotsStackedContents(stackedContents);
                }

                @Override
                public void clearCraftingContent() {
                    this.this$0.resultSlots.clearContent();
                    this.this$0.craftSlots.clearContent();
                }

                @Override
                public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
                    return recipe.value().matches(this.this$0.craftSlots.asCraftInput(), this.this$0.owner().level());
                }
            }, this.width, this.height, inputSlots, inputSlots, inventory, typedRecipe, useMaxItems, allowDroppingItemsToClear);
            return postPlaceAction;
        }
        finally {
            this.finishPlacingRecipe(level, typedRecipe);
        }
    }

    protected void beginPlacingRecipe() {
    }

    protected void finishPlacingRecipe(ServerLevel level, RecipeHolder<CraftingRecipe> recipe) {
    }

    public abstract Slot getResultSlot();

    public abstract List<Slot> getInputGridSlots();

    public int getGridWidth() {
        return this.width;
    }

    public int getGridHeight() {
        return this.height;
    }

    protected abstract Player owner();

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
        this.craftSlots.fillStackedContents(stackedContents);
    }
}

