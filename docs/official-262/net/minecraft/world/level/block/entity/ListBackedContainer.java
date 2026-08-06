/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public interface ListBackedContainer
extends Container {
    public NonNullList<ItemStack> getItems();

    default public int count() {
        return (int)this.getItems().stream().filter(Predicate.not(ItemStack::isEmpty)).count();
    }

    @Override
    default public int getContainerSize() {
        return this.getItems().size();
    }

    @Override
    default public void clearContent() {
        this.getItems().clear();
    }

    @Override
    default public boolean isEmpty() {
        return this.getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    default public ItemStack getItem(int slot) {
        return this.getItems().get(slot);
    }

    @Override
    default public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(this.getItems(), slot, count);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    default public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.removeItem(this.getItems(), slot, this.getMaxStackSize());
    }

    @Override
    default public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return this.acceptsItemType(itemStack) && (this.getItem(slot).isEmpty() || this.getItem(slot).getCount() < this.getMaxStackSize(itemStack));
    }

    default public boolean acceptsItemType(ItemStack itemStack) {
        return true;
    }

    @Override
    default public void setItem(int slot, ItemStack itemStack) {
        this.setItemNoUpdate(slot, itemStack);
        this.setChanged();
    }

    default public void setItemNoUpdate(int slot, ItemStack itemStack) {
        this.getItems().set(slot, itemStack);
        itemStack.limitSize(this.getMaxStackSize(itemStack));
    }
}

