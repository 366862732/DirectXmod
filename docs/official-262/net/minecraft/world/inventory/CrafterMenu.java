/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.NonInteractiveResultSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.CrafterBlock;

public class CrafterMenu
extends AbstractContainerMenu
implements ContainerListener {
    protected static final int SLOT_COUNT = 9;
    private static final int INV_SLOT_START = 9;
    private static final int INV_SLOT_END = 36;
    private static final int USE_ROW_SLOT_START = 36;
    private static final int USE_ROW_SLOT_END = 45;
    private final ResultContainer resultContainer = new ResultContainer();
    private final ContainerData containerData;
    private final Player player;
    private final CraftingContainer container;

    public CrafterMenu(int containerId, Inventory inventory) {
        super(MenuType.CRAFTER_3x3, containerId);
        this.player = inventory.player;
        this.containerData = new SimpleContainerData(10);
        this.container = new TransientCraftingContainer(this, 3, 3);
        this.addSlots(inventory);
    }

    public CrafterMenu(int containerId, Inventory inventory, CraftingContainer container, ContainerData containerData) {
        super(MenuType.CRAFTER_3x3, containerId);
        this.player = inventory.player;
        this.containerData = containerData;
        this.container = container;
        CrafterMenu.checkContainerSize(container, 9);
        container.startOpen(inventory.player);
        this.addSlots(inventory);
        this.addSlotListener(this);
    }

    private void addSlots(Inventory inventory) {
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                int slot = x + y * 3;
                this.addSlot(new CrafterSlot(this.container, slot, 26 + x * 18, 17 + y * 18, this));
            }
        }
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addSlot(new NonInteractiveResultSlot(this.resultContainer, 0, 134, 35));
        this.addDataSlots(this.containerData);
        this.refreshRecipeResult();
    }

    public void setSlotState(int slotId, boolean isEnabled) {
        CrafterSlot slot = (CrafterSlot)this.getSlot(slotId);
        this.containerData.set(slot.index, isEnabled ? 0 : 1);
        this.broadcastChanges();
    }

    public boolean isSlotDisabled(int slotId) {
        if (slotId > -1 && slotId < 9) {
            return this.containerData.get(slotId) == 1;
        }
        return false;
    }

    public boolean isPowered() {
        return this.containerData.get(9) == 1;
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
                slot.set(ItemStack.EMPTY);
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
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private void refreshRecipeResult() {
        Player player = this.player;
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            ServerLevel level = serverPlayer.level();
            CraftingInput craftInput = this.container.asCraftInput();
            ItemStack result = CrafterBlock.getPotentialResults(level, craftInput).map(recipe -> ((CraftingRecipe)recipe.value()).assemble(craftInput)).orElse(ItemStack.EMPTY);
            this.resultContainer.setItem(0, result);
        }
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
        this.refreshRecipeResult();
    }

    @Override
    public void dataChanged(AbstractContainerMenu container, int id, int value) {
    }
}

