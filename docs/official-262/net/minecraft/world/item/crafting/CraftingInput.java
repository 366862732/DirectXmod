/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public class CraftingInput
implements RecipeInput {
    public static final CraftingInput EMPTY = new CraftingInput(0, 0, List.of());
    private final int width;
    private final int height;
    private final List<ItemStack> items;
    private final StackedItemContents stackedContents = new StackedItemContents();
    private final int ingredientCount;

    private CraftingInput(int width, int height, List<ItemStack> items) {
        this.width = width;
        this.height = height;
        this.items = items;
        int ingredientCount = 0;
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            ++ingredientCount;
            this.stackedContents.accountStack(item, 1);
        }
        this.ingredientCount = ingredientCount;
    }

    public static CraftingInput of(int width, int height, List<ItemStack> items) {
        return CraftingInput.ofPositioned(width, height, items).input();
    }

    public static Positioned ofPositioned(int width, int height, List<ItemStack> items) {
        if (width == 0 || height == 0) {
            return Positioned.EMPTY;
        }
        int left = width - 1;
        int right = 0;
        int top = height - 1;
        int bottom = 0;
        for (int y = 0; y < height; ++y) {
            boolean rowEmpty = true;
            for (int x = 0; x < width; ++x) {
                ItemStack item = items.get(x + y * width);
                if (item.isEmpty()) continue;
                left = Math.min(left, x);
                right = Math.max(right, x);
                rowEmpty = false;
            }
            if (rowEmpty) continue;
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        int newWidth = right - left + 1;
        int newHeight = bottom - top + 1;
        if (newWidth <= 0 || newHeight <= 0) {
            return Positioned.EMPTY;
        }
        if (newWidth == width && newHeight == height) {
            return new Positioned(new CraftingInput(width, height, items), left, top);
        }
        ArrayList<ItemStack> newItems = new ArrayList<ItemStack>(newWidth * newHeight);
        for (int y = 0; y < newHeight; ++y) {
            for (int x = 0; x < newWidth; ++x) {
                int index = x + left + (y + top) * width;
                newItems.add(items.get(index));
            }
        }
        return new Positioned(new CraftingInput(newWidth, newHeight, newItems), left, top);
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    public ItemStack getItem(int x, int y) {
        return this.items.get(x + y * this.width);
    }

    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.ingredientCount == 0;
    }

    public StackedItemContents stackedContents() {
        return this.stackedContents;
    }

    public List<ItemStack> items() {
        return this.items;
    }

    public int ingredientCount() {
        return this.ingredientCount;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CraftingInput) {
            CraftingInput input = (CraftingInput)obj;
            return this.width == input.width && this.height == input.height && this.ingredientCount == input.ingredientCount && ItemStack.listMatches(this.items, input.items);
        }
        return false;
    }

    public int hashCode() {
        int result = ItemStack.hashStackList(this.items);
        result = 31 * result + this.width;
        result = 31 * result + this.height;
        return result;
    }

    public record Positioned(CraftingInput input, int left, int top) {
        public static final Positioned EMPTY = new Positioned(EMPTY, 0, 0);
    }
}

