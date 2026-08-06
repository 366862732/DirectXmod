/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HopperMenu
extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 5;
    private final Container hopper;

    public HopperMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(5));
    }

    public HopperMenu(int containerId, Inventory inventory, Container hopper) {
        super(MenuType.HOPPER, containerId);
        this.hopper = hopper;
        HopperMenu.checkContainerSize(hopper, 5);
        hopper.startOpen(inventory.player);
        for (int x = 0; x < 5; ++x) {
            this.addSlot(new Slot(hopper, x, 44 + x * 18, 20));
        }
        this.addStandardInventorySlots(inventory, 8, 51);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.hopper.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex < this.hopper.getContainerSize() ? !this.moveItemStackTo(stack, this.hopper.getContainerSize(), this.slots.size(), true) : !this.moveItemStackTo(stack, 0, this.hopper.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.hopper.stopOpen(player);
    }
}

