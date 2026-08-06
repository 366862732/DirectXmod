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

public class DispenserMenu
extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 9;
    private static final int INV_SLOT_START = 9;
    private static final int INV_SLOT_END = 36;
    private static final int USE_ROW_SLOT_START = 36;
    private static final int USE_ROW_SLOT_END = 45;
    private final Container dispenser;

    public DispenserMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(9));
    }

    public DispenserMenu(int containerId, Inventory inventory, Container dispenser) {
        super(MenuType.GENERIC_3x3, containerId);
        DispenserMenu.checkContainerSize(dispenser, 9);
        this.dispenser = dispenser;
        dispenser.startOpen(inventory.player);
        this.add3x3GridSlots(dispenser, 62, 17);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    protected void add3x3GridSlots(Container container, int left, int top) {
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                int slot = x + y * 3;
                this.addSlot(new Slot(container, slot, left + x * 18, top + y * 18));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.dispenser.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex < 9 ? !this.moveItemStackTo(stack, 9, 45, true) : !this.moveItemStackTo(stack, 0, 9, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.dispenser.stopOpen(player);
    }
}

