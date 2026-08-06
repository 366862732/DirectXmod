/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class CartographyTableMenu
extends AbstractContainerMenu {
    public static final int MAP_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final ContainerLevelAccess access;
    private long lastSoundTime;
    public final Container container = new SimpleContainer(this, 2){
        final /* synthetic */ CartographyTableMenu this$0;
        {
            CartographyTableMenu cartographyTableMenu = this$0;
            Objects.requireNonNull(cartographyTableMenu);
            this.this$0 = cartographyTableMenu;
            super(size);
        }

        @Override
        public void setChanged() {
            this.this$0.slotsChanged(this);
            super.setChanged();
        }
    };
    private final ResultContainer resultContainer = new ResultContainer(this){
        final /* synthetic */ CartographyTableMenu this$0;
        {
            CartographyTableMenu cartographyTableMenu = this$0;
            Objects.requireNonNull(cartographyTableMenu);
            this.this$0 = cartographyTableMenu;
        }

        @Override
        public void setChanged() {
            this.this$0.slotsChanged(this);
            super.setChanged();
        }
    };

    public CartographyTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public CartographyTableMenu(int containerId, Inventory inventory, final ContainerLevelAccess access) {
        super(MenuType.CARTOGRAPHY_TABLE, containerId);
        this.access = access;
        this.addSlot(new Slot(this, this.container, 0, 15, 15){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.has(DataComponents.MAP_ID);
            }
        });
        this.addSlot(new Slot(this, this.container, 1, 15, 52){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.PAPER) || itemStack.is(Items.MAP) || itemStack.is(Items.GLASS_PANE);
            }
        });
        this.addSlot(new Slot(this, this.resultContainer, 2, 145, 39){
            final /* synthetic */ CartographyTableMenu this$0;
            {
                CartographyTableMenu cartographyTableMenu = this$0;
                Objects.requireNonNull(cartographyTableMenu);
                this.this$0 = cartographyTableMenu;
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                ((Slot)this.this$0.slots.get(0)).remove(1);
                ((Slot)this.this$0.slots.get(1)).remove(1);
                carried.getItem().onCraftedBy(carried, player);
                access.execute((level, pos) -> {
                    long gameTime = level.getGameTime();
                    if (this.this$0.lastSoundTime != gameTime) {
                        level.playSound(null, (BlockPos)pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                        this.this$0.lastSoundTime = gameTime;
                    }
                });
                super.onTake(player, carried);
            }

            @Override
            public ItemStack safeClone(Player player) {
                ItemStack result = super.safeClone(player);
                result.getItem().onCraftedBy(result, player);
                return result;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        return CartographyTableMenu.stillValid(this.access, player, Blocks.CARTOGRAPHY_TABLE);
    }

    @Override
    public void slotsChanged(Container container) {
        ItemStack mapStack = this.container.getItem(0);
        ItemStack additionalStack = this.container.getItem(1);
        ItemStack resultStack = this.resultContainer.getItem(2);
        if (!resultStack.isEmpty() && (mapStack.isEmpty() || additionalStack.isEmpty())) {
            this.resultContainer.removeItemNoUpdate(2);
        } else if (!mapStack.isEmpty() && !additionalStack.isEmpty()) {
            this.setupResultSlot(mapStack, additionalStack, resultStack);
        }
    }

    private void setupResultSlot(ItemStack mapStack, ItemStack additionalStack, ItemStack resultStack) {
        this.access.execute((level, pos) -> {
            ItemStack result;
            MapItemSavedData mapData = MapItem.getSavedData(mapStack, level);
            if (mapData == null) {
                return;
            }
            if (additionalStack.is(Items.PAPER) && !mapData.locked && mapData.scale < 4) {
                result = mapStack.copyWithCount(1);
                result.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
                this.broadcastChanges();
            } else if (additionalStack.is(Items.GLASS_PANE) && !mapData.locked) {
                result = mapStack.copyWithCount(1);
                result.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.LOCK);
                this.broadcastChanges();
            } else if (additionalStack.is(Items.MAP)) {
                result = mapStack.copyWithCount(2);
                this.broadcastChanges();
            } else {
                this.resultContainer.removeItemNoUpdate(2);
                this.broadcastChanges();
                return;
            }
            if (!ItemStack.matches(result, resultStack)) {
                this.resultContainer.setItem(2, result);
                this.broadcastChanges();
            }
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultContainer && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 2) {
                stack.getItem().onCraftedBy(stack, player);
                if (!this.moveItemStackTo(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex == 1 || slotIndex == 0 ? !this.moveItemStackTo(stack, 3, 39, false) : (stack.has(DataComponents.MAP_ID) ? !this.moveItemStackTo(stack, 0, 1, false) : (stack.is(Items.PAPER) || stack.is(Items.MAP) || stack.is(Items.GLASS_PANE) ? !this.moveItemStackTo(stack, 1, 2, false) : (slotIndex >= 3 && slotIndex < 30 ? !this.moveItemStackTo(stack, 30, 39, false) : slotIndex >= 30 && slotIndex < 39 && !this.moveItemStackTo(stack, 3, 30, false))))) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            slot.setChanged();
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            this.broadcastChanges();
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(2);
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
    }
}

