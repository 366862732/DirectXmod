/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.inventory;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.jspecify.annotations.Nullable;

public class BeaconMenu
extends AbstractContainerMenu {
    private static final int PAYMENT_SLOT = 0;
    private static final int SLOT_COUNT = 1;
    private static final int DATA_COUNT = 3;
    private static final int INV_SLOT_START = 1;
    private static final int INV_SLOT_END = 28;
    private static final int USE_ROW_SLOT_START = 28;
    private static final int USE_ROW_SLOT_END = 37;
    private static final int NO_EFFECT = 0;
    private final Container beacon = new SimpleContainer(this, 1){
        {
            Objects.requireNonNull(this$0);
            super(size);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack itemStack) {
            return itemStack.is(ItemTags.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final PaymentSlot paymentSlot;
    private final ContainerLevelAccess access;
    private final ContainerData beaconData;

    public BeaconMenu(int containerId, Container inventory) {
        this(containerId, inventory, new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    public BeaconMenu(int containerId, Container inventory, ContainerData beaconData, ContainerLevelAccess access) {
        super(MenuType.BEACON, containerId);
        BeaconMenu.checkContainerDataCount(beaconData, 3);
        this.beaconData = beaconData;
        this.access = access;
        this.paymentSlot = new PaymentSlot(this.beacon, 0, 136, 110);
        this.addSlot(this.paymentSlot);
        this.addDataSlots(beaconData);
        this.addStandardInventorySlots(inventory, 36, 137);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack itemStack = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());
        if (!itemStack.isEmpty()) {
            player.drop(itemStack, false);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return BeaconMenu.stillValid(this.access, player, Blocks.BEACON);
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 0) {
                if (!this.moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (!this.paymentSlot.hasItem() && this.paymentSlot.mayPlace(stack) && stack.getCount() == 1 ? !this.moveItemStackTo(stack, 0, 1, false) : (slotIndex >= 1 && slotIndex < 28 ? !this.moveItemStackTo(stack, 28, 37, false) : (slotIndex >= 28 && slotIndex < 37 ? !this.moveItemStackTo(stack, 1, 28, false) : !this.moveItemStackTo(stack, 1, 37, false)))) {
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

    public int getLevels() {
        return this.beaconData.get(0);
    }

    public static int encodeEffect(@Nullable Holder<MobEffect> mobEffect) {
        return mobEffect == null ? 0 : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().getId(mobEffect) + 1;
    }

    public static @Nullable Holder<MobEffect> decodeEffect(int id) {
        return id == 0 ? null : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().byId(id - 1);
    }

    public @Nullable Holder<MobEffect> getPrimaryEffect() {
        return BeaconMenu.decodeEffect(this.beaconData.get(1));
    }

    public @Nullable Holder<MobEffect> getSecondaryEffect() {
        return BeaconMenu.decodeEffect(this.beaconData.get(2));
    }

    public boolean updateEffects(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary) {
        if (this.paymentSlot.hasItem()) {
            Holder secondaryEffect;
            int levels = this.getLevels();
            Holder primaryEffect = primary.orElse(null);
            if (!BeaconBlockEntity.validateEffects(primaryEffect, secondaryEffect = (Holder)secondary.orElse(null), levels)) {
                return false;
            }
            this.beaconData.set(1, BeaconMenu.encodeEffect(primaryEffect));
            this.beaconData.set(2, BeaconMenu.encodeEffect(secondaryEffect));
            this.paymentSlot.remove(1);
            this.access.execute(Level::blockEntityChanged);
            return true;
        }
        return false;
    }

    public boolean hasPayment() {
        return !this.beacon.getItem(0).isEmpty();
    }

    private static class PaymentSlot
    extends Slot {
        public PaymentSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return itemStack.is(ItemTags.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}

