/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.ticks;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ContainerSingleItem
extends Container {
    public ItemStack getTheItem();

    default public ItemStack splitTheItem(int count) {
        return this.getTheItem().split(count);
    }

    public void setTheItem(ItemStack var1);

    default public ItemStack removeTheItem() {
        return this.splitTheItem(this.getMaxStackSize());
    }

    @Override
    default public int getContainerSize() {
        return 1;
    }

    @Override
    default public boolean isEmpty() {
        return this.getTheItem().isEmpty();
    }

    @Override
    default public void clearContent() {
        this.removeTheItem();
    }

    @Override
    default public ItemStack removeItemNoUpdate(int slot) {
        return this.removeItem(slot, this.getMaxStackSize());
    }

    @Override
    default public ItemStack getItem(int slot) {
        return slot == 0 ? this.getTheItem() : ItemStack.EMPTY;
    }

    @Override
    default public ItemStack removeItem(int slot, int count) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        return this.splitTheItem(count);
    }

    @Override
    default public void setItem(int slot, ItemStack itemStack) {
        if (slot == 0) {
            this.setTheItem(itemStack);
        }
    }

    public static interface BlockContainerSingleItem
    extends ContainerSingleItem {
        public BlockEntity getContainerBlockEntity();

        @Override
        default public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this.getContainerBlockEntity(), player);
        }
    }
}

