/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public class ServerPlaceRecipe<R extends Recipe<?>> {
    private static final int ITEM_NOT_FOUND = -1;
    private final Inventory inventory;
    private final CraftingMenuAccess<R> menu;
    private final boolean useMaxItems;
    private final int gridWidth;
    private final int gridHeight;
    private final List<Slot> inputGridSlots;
    private final List<Slot> slotsToClear;

    public static <I extends RecipeInput, R extends Recipe<I>> RecipeBookMenu.PostPlaceAction placeRecipe(CraftingMenuAccess<R> menu, int gridWidth, int gridHeight, List<Slot> inputGridSlots, List<Slot> slotsToClear, Inventory inventory, RecipeHolder<R> recipe, boolean useMaxItems, boolean allowDroppingItemsToClear) {
        ServerPlaceRecipe<R> placer = new ServerPlaceRecipe<R>(menu, inventory, useMaxItems, gridWidth, gridHeight, inputGridSlots, slotsToClear);
        if (!allowDroppingItemsToClear && !placer.testClearGrid()) {
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
        StackedItemContents availableItems = new StackedItemContents();
        inventory.fillStackedContents(availableItems);
        menu.fillCraftSlotsStackedContents(availableItems);
        return placer.tryPlaceRecipe(recipe, availableItems);
    }

    private ServerPlaceRecipe(CraftingMenuAccess<R> menu, Inventory inventory, boolean useMaxItems, int gridWidth, int gridHeight, List<Slot> inputGridSlots, List<Slot> slotsToClear) {
        this.menu = menu;
        this.inventory = inventory;
        this.useMaxItems = useMaxItems;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.inputGridSlots = inputGridSlots;
        this.slotsToClear = slotsToClear;
    }

    private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(RecipeHolder<R> recipe, StackedItemContents availableItems) {
        if (availableItems.canCraft((Recipe<?>)recipe.value(), null)) {
            this.placeRecipe(recipe, availableItems);
            this.inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
        this.clearGrid();
        this.inventory.setChanged();
        return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
    }

    private void clearGrid() {
        for (Slot slot : this.slotsToClear) {
            ItemStack itemStackCopy = slot.getItem().copy();
            this.inventory.placeItemBackInInventory(itemStackCopy, false);
            slot.set(itemStackCopy);
        }
        this.menu.clearCraftingContent();
    }

    private void placeRecipe(RecipeHolder<R> recipe, StackedItemContents availableItems) {
        boolean recipeMatchesPlaced = this.menu.recipeMatches(recipe);
        int biggestCraftableStack = availableItems.getBiggestCraftableStack((Recipe<?>)recipe.value(), null);
        if (recipeMatchesPlaced) {
            for (Slot inputSlot : this.inputGridSlots) {
                ItemStack itemStack = inputSlot.getItem();
                if (itemStack.isEmpty() || Math.min(biggestCraftableStack, itemStack.getMaxStackSize()) >= itemStack.getCount() + 1) continue;
                return;
            }
        }
        int amountToCraft = this.calculateAmountToCraft(biggestCraftableStack, recipeMatchesPlaced);
        ArrayList<Holder<Item>> itemsUsedPerIngredient = new ArrayList<Holder<Item>>();
        if (!availableItems.canCraft((Recipe<?>)recipe.value(), amountToCraft, itemsUsedPerIngredient::add)) {
            return;
        }
        int adjustedAmountToCraft = ServerPlaceRecipe.clampToMaxStackSize(amountToCraft, itemsUsedPerIngredient);
        if (adjustedAmountToCraft != amountToCraft) {
            itemsUsedPerIngredient.clear();
            if (!availableItems.canCraft((Recipe<?>)recipe.value(), adjustedAmountToCraft, itemsUsedPerIngredient::add)) {
                return;
            }
        }
        this.clearGrid();
        PlaceRecipeHelper.placeRecipe(this.gridWidth, this.gridHeight, recipe.value(), recipe.value().placementInfo().slotsToIngredientIndex(), (ingredientIndex, gridIndex, gridXPos, gridYPos) -> {
            if (ingredientIndex == -1) {
                return;
            }
            Slot targetGridSlot = this.inputGridSlots.get(gridIndex);
            Holder itemUsed = (Holder)itemsUsedPerIngredient.get((int)ingredientIndex);
            int remainingCount = adjustedAmountToCraft;
            while (remainingCount > 0) {
                if ((remainingCount = this.moveItemToGrid(targetGridSlot, itemUsed, remainingCount)) != -1) continue;
                return;
            }
        });
    }

    private static int clampToMaxStackSize(int value, List<Holder<Item>> items) {
        for (Holder<Item> item : items) {
            value = Math.min(value, item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1));
        }
        return value;
    }

    private int calculateAmountToCraft(int biggestCraftableStack, boolean recipeMatchesPlaced) {
        if (this.useMaxItems) {
            return biggestCraftableStack;
        }
        if (recipeMatchesPlaced) {
            int smallestStackSize = Integer.MAX_VALUE;
            for (Slot inputSlot : this.inputGridSlots) {
                ItemStack itemStack = inputSlot.getItem();
                if (itemStack.isEmpty() || smallestStackSize <= itemStack.getCount()) continue;
                smallestStackSize = itemStack.getCount();
            }
            if (smallestStackSize != Integer.MAX_VALUE) {
                ++smallestStackSize;
            }
            return smallestStackSize;
        }
        return 1;
    }

    private int moveItemToGrid(Slot targetSlot, Holder<Item> itemInInventory, int count) {
        ItemStack itemInTargetSlot = targetSlot.getItem();
        int inventorySlotId = this.inventory.findSlotMatchingCraftingIngredient(itemInInventory, itemInTargetSlot);
        if (inventorySlotId == -1) {
            return -1;
        }
        ItemStack inventoryItem = this.inventory.getItem(inventorySlotId);
        ItemStack takenStack = count < inventoryItem.getCount() ? this.inventory.removeItem(inventorySlotId, count) : this.inventory.removeItemNoUpdate(inventorySlotId);
        int takenCount = takenStack.getCount();
        if (itemInTargetSlot.isEmpty()) {
            targetSlot.set(takenStack);
        } else {
            itemInTargetSlot.grow(takenCount);
        }
        return count - takenCount;
    }

    private boolean testClearGrid() {
        ArrayList freeSlots = Lists.newArrayList();
        int freeSlotsInInventory = this.getAmountOfFreeSlotsInInventory();
        for (Slot inputSlot : this.inputGridSlots) {
            ItemStack itemStack = inputSlot.getItem().copy();
            if (itemStack.isEmpty()) continue;
            int slotId = this.inventory.getSlotWithRemainingSpace(itemStack);
            if (slotId == -1 && freeSlots.size() <= freeSlotsInInventory) {
                for (ItemStack itemStackInList : freeSlots) {
                    if (!ItemStack.isSameItem(itemStackInList, itemStack) || itemStackInList.getCount() == itemStackInList.getMaxStackSize() || itemStackInList.getCount() + itemStack.getCount() > itemStackInList.getMaxStackSize()) continue;
                    itemStackInList.grow(itemStack.getCount());
                    itemStack.setCount(0);
                    break;
                }
                if (itemStack.isEmpty()) continue;
                if (freeSlots.size() < freeSlotsInInventory) {
                    freeSlots.add(itemStack);
                    continue;
                }
                return false;
            }
            if (slotId != -1) continue;
            return false;
        }
        return true;
    }

    private int getAmountOfFreeSlotsInInventory() {
        int freeSlots = 0;
        for (ItemStack item : this.inventory.getNonEquipmentItems()) {
            if (!item.isEmpty()) continue;
            ++freeSlots;
        }
        return freeSlots;
    }

    public static interface CraftingMenuAccess<T extends Recipe<?>> {
        public void fillCraftSlotsStackedContents(StackedItemContents var1);

        public void clearCraftingContent();

        public boolean recipeMatches(RecipeHolder<T> var1);
    }
}

