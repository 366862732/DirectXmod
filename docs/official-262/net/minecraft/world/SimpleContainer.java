/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SimpleContainer
implements Container,
StackedContentsCompatible {
    private final int size;
    private final NonNullList<ItemStack> items;

    public SimpleContainer(int size) {
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public SimpleContainer(ItemStack ... itemstacks) {
        this.size = itemstacks.length;
        this.items = NonNullList.of(ItemStack.EMPTY, itemstacks);
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.items.size()) {
            return ItemStack.EMPTY;
        }
        return this.items.get(slot);
    }

    public List<ItemStack> removeAllItems() {
        List<ItemStack> itemsRemoved = this.items.stream().filter(item -> !item.isEmpty()).collect(Collectors.toList());
        this.clearContent();
        return itemsRemoved;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, count);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    public ItemStack removeItemType(Item itemType, int count) {
        ItemStack removed = new ItemStack(itemType, 0);
        for (int slot = this.size - 1; slot >= 0; --slot) {
            ItemStack current = this.getItem(slot);
            if (!current.getItem().equals(itemType)) continue;
            int stillNeeded = count - removed.getCount();
            ItemStack removedFromThisSlot = current.split(stillNeeded);
            removed.grow(removedFromThisSlot.getCount());
            if (removed.getCount() == count) break;
        }
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    public ItemStack addItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remainingItems = itemStack.copy();
        this.moveItemToOccupiedSlotsWithSameType(remainingItems);
        if (remainingItems.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.moveItemToEmptySlots(remainingItems);
        if (remainingItems.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return remainingItems;
    }

    public boolean canAddItem(ItemStack itemStack) {
        boolean hasSpace = false;
        for (ItemStack targetStack : this.items) {
            if (!targetStack.isEmpty() && (!ItemStack.isSameItemSameComponents(targetStack, itemStack) || targetStack.getCount() >= targetStack.getMaxStackSize())) continue;
            hasSpace = true;
            break;
        }
        return hasSpace;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemStack = this.items.get(slot);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.items.set(slot, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.items.set(slot, itemStack);
        itemStack.limitSize(this.getMaxStackSize(itemStack));
        this.setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public void fillStackedContents(StackedItemContents contents) {
        for (ItemStack itemStack : this.items) {
            contents.accountStack(itemStack);
        }
    }

    public String toString() {
        return this.items.stream().filter(item -> !item.isEmpty()).toList().toString();
    }

    private void moveItemToEmptySlots(ItemStack sourceStack) {
        for (int slot = 0; slot < this.size; ++slot) {
            ItemStack targetStack = this.getItem(slot);
            if (!targetStack.isEmpty()) continue;
            this.setItem(slot, sourceStack.copyAndClear());
            return;
        }
    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack sourceStack) {
        for (int slot = 0; slot < this.size; ++slot) {
            ItemStack targetStack = this.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(targetStack, sourceStack)) continue;
            this.moveItemsBetweenStacks(sourceStack, targetStack);
            if (!sourceStack.isEmpty()) continue;
            return;
        }
    }

    private void moveItemsBetweenStacks(ItemStack sourceStack, ItemStack targetStack) {
        int maxCount = this.getMaxStackSize(targetStack);
        int diff = Math.min(sourceStack.getCount(), maxCount - targetStack.getCount());
        if (diff > 0) {
            targetStack.grow(diff);
            sourceStack.shrink(diff);
            this.setChanged();
        }
    }

    public void fromItemList(ValueInput.TypedInputList<ItemStack> items) {
        this.clearContent();
        for (ItemStack stack : items) {
            this.addItem(stack);
        }
    }

    public void storeAsItemList(ValueOutput.TypedOutputList<ItemStack> output) {
        for (int i = 0; i < this.getContainerSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) continue;
            output.add(itemStack);
        }
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }
}

