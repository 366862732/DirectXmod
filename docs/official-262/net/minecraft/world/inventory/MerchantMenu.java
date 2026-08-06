/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantMenu
extends AbstractContainerMenu {
    protected static final int PAYMENT1_SLOT = 0;
    protected static final int PAYMENT2_SLOT = 1;
    protected static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private static final int SELLSLOT1_X = 136;
    private static final int SELLSLOT2_X = 162;
    private static final int BUYSLOT_X = 220;
    private static final int ROW_Y = 37;
    private final Merchant trader;
    private final MerchantContainer tradeContainer;
    private int merchantLevel;
    private boolean showProgressBar;
    private boolean canRestock;

    public MerchantMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ClientSideMerchant(inventory.player));
    }

    public MerchantMenu(int containerId, Inventory inventory, Merchant merchant) {
        super(MenuType.MERCHANT, containerId);
        this.trader = merchant;
        this.tradeContainer = new MerchantContainer(merchant);
        this.addSlot(new Slot(this.tradeContainer, 0, 136, 37));
        this.addSlot(new Slot(this.tradeContainer, 1, 162, 37));
        this.addSlot(new MerchantResultSlot(inventory.player, merchant, this.tradeContainer, 2, 220, 37));
        this.addStandardInventorySlots(inventory, 108, 84);
    }

    public void setShowProgressBar(boolean show) {
        this.showProgressBar = show;
    }

    @Override
    public void slotsChanged(Container container) {
        this.tradeContainer.updateSellItem();
        super.slotsChanged(container);
    }

    public void setSelectionHint(int hint) {
        this.tradeContainer.setSelectionHint(hint);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.trader.stillValid(player);
    }

    public int getTraderXp() {
        return this.trader.getVillagerXp();
    }

    public int getFutureTraderXp() {
        return this.tradeContainer.getFutureXp();
    }

    public void setXp(int xp) {
        this.trader.overrideXp(xp);
    }

    public int getTraderLevel() {
        return this.merchantLevel;
    }

    public void setMerchantLevel(int level) {
        this.merchantLevel = level;
    }

    public void setCanRestock(boolean canRestock) {
        this.canRestock = canRestock;
    }

    public boolean canRestock() {
        return this.canRestock;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 2) {
                if (!this.moveItemStackTo(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
                this.playTradeSound();
            } else if (slotIndex == 0 || slotIndex == 1 ? !this.moveItemStackTo(stack, 3, 39, false) : (slotIndex >= 3 && slotIndex < 30 ? !this.moveItemStackTo(stack, 30, 39, false) : slotIndex >= 30 && slotIndex < 39 && !this.moveItemStackTo(stack, 3, 30, false))) {
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

    private void playTradeSound() {
        if (!this.trader.isClientSide()) {
            Entity entity = (Entity)((Object)this.trader);
            entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), this.trader.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0f, 1.0f, false);
        }
    }

    @Override
    public void removed(Player player) {
        ServerPlayer serverPlayer;
        super.removed(player);
        this.trader.setTradingPlayer(null);
        if (this.trader.isClientSide()) {
            return;
        }
        if (!player.isAlive() || player instanceof ServerPlayer && (serverPlayer = (ServerPlayer)player).hasDisconnected()) {
            ItemStack itemStack = this.tradeContainer.removeItemNoUpdate(0);
            if (!itemStack.isEmpty()) {
                player.drop(itemStack, false);
            }
            if (!(itemStack = this.tradeContainer.removeItemNoUpdate(1)).isEmpty()) {
                player.drop(itemStack, false);
            }
        } else if (player instanceof ServerPlayer) {
            player.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(0));
            player.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(1));
        }
    }

    public void tryMoveItems(int newTradeIndex) {
        ItemStack oldCostB;
        if (newTradeIndex < 0 || this.getOffers().size() <= newTradeIndex) {
            return;
        }
        ItemStack oldCostA = this.tradeContainer.getItem(0);
        if (!oldCostA.isEmpty()) {
            if (!this.moveItemStackTo(oldCostA, 3, 39, true)) {
                return;
            }
            this.tradeContainer.setItem(0, oldCostA);
        }
        if (!(oldCostB = this.tradeContainer.getItem(1)).isEmpty()) {
            if (!this.moveItemStackTo(oldCostB, 3, 39, true)) {
                return;
            }
            this.tradeContainer.setItem(1, oldCostB);
        }
        if (this.tradeContainer.getItem(0).isEmpty() && this.tradeContainer.getItem(1).isEmpty()) {
            MerchantOffer merchantOffer = (MerchantOffer)this.getOffers().get(newTradeIndex);
            this.moveFromInventoryToPaymentSlot(0, merchantOffer.getItemCostA());
            merchantOffer.getItemCostB().ifPresent(costB -> this.moveFromInventoryToPaymentSlot(1, (ItemCost)costB));
        }
    }

    private void moveFromInventoryToPaymentSlot(int paymentSlot, ItemCost cost) {
        for (int i = 3; i < 39; ++i) {
            ItemStack currentPaymentItem;
            ItemStack inventoryItem = ((Slot)this.slots.get(i)).getItem();
            if (inventoryItem.isEmpty() || !cost.test(inventoryItem) || !(currentPaymentItem = this.tradeContainer.getItem(paymentSlot)).isEmpty() && !ItemStack.isSameItemSameComponents(inventoryItem, currentPaymentItem)) continue;
            int maxStackSize = inventoryItem.getMaxStackSize();
            int moveCount = Math.min(maxStackSize - currentPaymentItem.getCount(), inventoryItem.getCount());
            ItemStack newPaymentItem = inventoryItem.copyWithCount(currentPaymentItem.getCount() + moveCount);
            inventoryItem.shrink(moveCount);
            this.tradeContainer.setItem(paymentSlot, newPaymentItem);
            if (newPaymentItem.getCount() >= maxStackSize) break;
        }
    }

    public void setOffers(MerchantOffers offers) {
        this.trader.overrideOffers(offers);
    }

    public MerchantOffers getOffers() {
        return this.trader.getOffers();
    }

    public boolean showProgressBar() {
        return this.showProgressBar;
    }
}

