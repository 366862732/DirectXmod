/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.inventory;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class ItemCombinerMenu
extends AbstractContainerMenu {
    private static final int INVENTORY_SLOTS_PER_ROW = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final int INPUT_SLOT_START = 0;
    protected final ContainerLevelAccess access;
    protected final Player player;
    protected final Container inputSlots;
    protected final ResultContainer resultSlots = new ResultContainer(this){
        final /* synthetic */ ItemCombinerMenu this$0;
        {
            ItemCombinerMenu itemCombinerMenu = this$0;
            Objects.requireNonNull(itemCombinerMenu);
            this.this$0 = itemCombinerMenu;
        }

        @Override
        public void setChanged() {
            this.this$0.slotsChanged(this);
        }
    };
    private final int resultSlotIndex;

    protected boolean mayPickup(Player player, boolean hasItem) {
        return true;
    }

    protected abstract void onTake(Player var1, ItemStack var2);

    protected abstract boolean isValidBlock(BlockState var1);

    public ItemCombinerMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition itemInputSlots) {
        super(menuType, containerId);
        this.access = access;
        this.player = inventory.player;
        this.inputSlots = this.createContainer(itemInputSlots.getNumOfInputSlots());
        this.resultSlotIndex = itemInputSlots.getResultSlotIndex();
        this.createInputSlots(itemInputSlots);
        this.createResultSlot(itemInputSlots);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    private void createInputSlots(ItemCombinerMenuSlotDefinition itemInputSlots) {
        for (final ItemCombinerMenuSlotDefinition.SlotDefinition slot : itemInputSlots.getSlots()) {
            this.addSlot(new Slot(this, this.inputSlots, slot.slotIndex(), slot.x(), slot.y()){
                {
                    Objects.requireNonNull(this$0);
                    super(container, slot2, x, y);
                }

                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return slot.mayPlace().test(itemStack);
                }
            });
        }
    }

    private void createResultSlot(ItemCombinerMenuSlotDefinition itemInputSlots) {
        this.addSlot(new Slot(this, this.resultSlots, itemInputSlots.getResultSlot().slotIndex(), itemInputSlots.getResultSlot().x(), itemInputSlots.getResultSlot().y()){
            final /* synthetic */ ItemCombinerMenu this$0;
            {
                ItemCombinerMenu itemCombinerMenu = this$0;
                Objects.requireNonNull(itemCombinerMenu);
                this.this$0 = itemCombinerMenu;
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return this.this$0.mayPickup(player, this.hasItem());
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                this.this$0.onTake(player, carried);
            }
        });
    }

    public abstract void createResult();

    private SimpleContainer createContainer(int size) {
        return new SimpleContainer(this, size){
            final /* synthetic */ ItemCombinerMenu this$0;
            {
                ItemCombinerMenu itemCombinerMenu = this$0;
                Objects.requireNonNull(itemCombinerMenu);
                this.this$0 = itemCombinerMenu;
                super(size);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                this.this$0.slotsChanged(this);
            }
        };
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.inputSlots) {
            this.createResult();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (!this.isValidBlock(level.getBlockState((BlockPos)pos))) {
                return false;
            }
            return player.isWithinBlockInteractionRange((BlockPos)pos, 4.0);
        }, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            int inventorySlotStart = this.getInventorySlotStart();
            int useRowSlotEnd = this.getUseRowEnd();
            if (slotIndex == this.getResultSlot()) {
                if (!this.moveItemStackTo(stack, inventorySlotStart, useRowSlotEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= 0 && slotIndex < this.getResultSlot() ? !this.moveItemStackTo(stack, inventorySlotStart, useRowSlotEnd, false) : (this.canMoveIntoInputSlots(stack) && slotIndex >= this.getInventorySlotStart() && slotIndex < this.getUseRowEnd() ? !this.moveItemStackTo(stack, 0, this.getResultSlot(), false) : (slotIndex >= this.getInventorySlotStart() && slotIndex < this.getInventorySlotEnd() ? !this.moveItemStackTo(stack, this.getUseRowStart(), this.getUseRowEnd(), false) : slotIndex >= this.getUseRowStart() && slotIndex < this.getUseRowEnd() && !this.moveItemStackTo(stack, this.getInventorySlotStart(), this.getInventorySlotEnd(), false)))) {
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

    protected boolean canMoveIntoInputSlots(ItemStack stack) {
        return true;
    }

    public int getResultSlot() {
        return this.resultSlotIndex;
    }

    private int getInventorySlotStart() {
        return this.getResultSlot() + 1;
    }

    private int getInventorySlotEnd() {
        return this.getInventorySlotStart() + 27;
    }

    private int getUseRowStart() {
        return this.getInventorySlotEnd();
    }

    private int getUseRowEnd() {
        return this.getUseRowStart() + 9;
    }
}

