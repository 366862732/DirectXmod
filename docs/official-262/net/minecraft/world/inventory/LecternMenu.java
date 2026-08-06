/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.Objects;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LecternMenu
extends AbstractContainerMenu {
    private static final int DATA_COUNT = 1;
    private static final int SLOT_COUNT = 1;
    public static final int BUTTON_PREV_PAGE = 1;
    public static final int BUTTON_NEXT_PAGE = 2;
    public static final int BUTTON_TAKE_BOOK = 3;
    public static final int BUTTON_PAGE_JUMP_RANGE_START = 100;
    private final Container lectern;
    private final ContainerData lecternData;

    public LecternMenu(int containerId) {
        this(containerId, new SimpleContainer(1), new SimpleContainerData(1));
    }

    public LecternMenu(int containerId, Container lectern, ContainerData lecternData) {
        super(MenuType.LECTERN, containerId);
        LecternMenu.checkContainerSize(lectern, 1);
        LecternMenu.checkContainerDataCount(lecternData, 1);
        this.lectern = lectern;
        this.lecternData = lecternData;
        this.addSlot(new Slot(this, lectern, 0, 0, 0){
            final /* synthetic */ LecternMenu this$0;
            {
                LecternMenu lecternMenu = this$0;
                Objects.requireNonNull(lecternMenu);
                this.this$0 = lecternMenu;
                super(container, slot, x, y);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                this.this$0.slotsChanged(this.container);
            }
        });
        this.addDataSlots(lecternData);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId >= 100) {
            int pageToSet = buttonId - 100;
            this.setData(0, pageToSet);
            return true;
        }
        switch (buttonId) {
            case 2: {
                int currentPage = this.lecternData.get(0);
                this.setData(0, currentPage + 1);
                return true;
            }
            case 1: {
                int currentPage = this.lecternData.get(0);
                this.setData(0, currentPage - 1);
                return true;
            }
            case 3: {
                if (!player.mayBuild()) {
                    return false;
                }
                ItemStack book = this.lectern.removeItemNoUpdate(0);
                this.lectern.setChanged();
                if (!player.getInventory().add(book)) {
                    player.drop(book, false);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        this.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.lectern.stillValid(player);
    }

    public ItemStack getBook() {
        return this.lectern.getItem(0);
    }

    public int getPage() {
        return this.lecternData.get(0);
    }
}

