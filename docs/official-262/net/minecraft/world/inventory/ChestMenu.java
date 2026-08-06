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

public class ChestMenu
extends AbstractContainerMenu {
    private final Container container;
    private final int containerRows;

    private ChestMenu(MenuType<?> menuType, int containerId, Inventory inventory, int rows) {
        this(menuType, containerId, inventory, new SimpleContainer(9 * rows), rows);
    }

    public static ChestMenu oneRow(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, 1);
    }

    public static ChestMenu twoRows(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x2, containerId, inventory, 2);
    }

    public static ChestMenu threeRows(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x3, containerId, inventory, 3);
    }

    public static ChestMenu fourRows(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x4, containerId, inventory, 4);
    }

    public static ChestMenu fiveRows(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x5, containerId, inventory, 5);
    }

    public static ChestMenu sixRows(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x6, containerId, inventory, 6);
    }

    public static ChestMenu threeRows(int containerId, Inventory inventory, Container container) {
        return new ChestMenu(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
    }

    public static ChestMenu sixRows(int containerId, Inventory inventory, Container container) {
        return new ChestMenu(MenuType.GENERIC_9x6, containerId, inventory, container, 6);
    }

    public ChestMenu(MenuType<?> menuType, int containerId, Inventory inventory, Container container, int rows) {
        super(menuType, containerId);
        ChestMenu.checkContainerSize(container, rows * 9);
        this.container = container;
        this.containerRows = rows;
        container.startOpen(inventory.player);
        int chestGridTop = 18;
        this.addChestGrid(container, 8, 18);
        int inventoryTop = 18 + this.containerRows * 18 + 13;
        this.addStandardInventorySlots(inventory, 8, inventoryTop);
    }

    private void addChestGrid(Container container, int left, int top) {
        for (int y = 0; y < this.containerRows; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(container, x + y * 9, left + x * 18, top + y * 18));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex < this.containerRows * 9 ? !this.moveItemStackTo(stack, this.containerRows * 9, this.slots.size(), true) : !this.moveItemStackTo(stack, 0, this.containerRows * 9, false)) {
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
        this.container.stopOpen(player);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getRowCount() {
        return this.containerRows;
    }
}

