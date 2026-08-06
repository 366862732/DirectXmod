/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class Slot {
    private final int slot;
    public final Container container;
    public int index;
    public final int x;
    public final int y;

    public Slot(Container container, int slot, int x, int y) {
        this.container = container;
        this.slot = slot;
        this.x = x;
        this.y = y;
    }

    public void onQuickCraft(ItemStack picked, ItemStack original) {
        int count = original.getCount() - picked.getCount();
        if (count > 0) {
            this.onQuickCraft(original, count);
        }
    }

    protected void onQuickCraft(ItemStack picked, int count) {
    }

    protected void onSwapCraft(int count) {
    }

    protected void checkTakeAchievements(ItemStack carried) {
    }

    public void onTake(Player player, ItemStack carried) {
        this.setChanged();
    }

    public boolean mayPlace(ItemStack itemStack) {
        return true;
    }

    public ItemStack getItem() {
        return this.container.getItem(this.slot);
    }

    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    public void setByPlayer(ItemStack itemStack) {
        this.setByPlayer(itemStack, this.getItem());
    }

    public void setByPlayer(ItemStack itemStack, ItemStack previous) {
        this.set(itemStack);
    }

    public void set(ItemStack itemStack) {
        this.container.setItem(this.slot, itemStack);
        this.setChanged();
    }

    public void setChanged() {
        this.container.setChanged();
    }

    public int getMaxStackSize() {
        return this.container.getMaxStackSize();
    }

    public int getMaxStackSize(ItemStack itemStack) {
        return Math.min(this.getMaxStackSize(), itemStack.getMaxStackSize());
    }

    public @Nullable Identifier getNoItemIcon() {
        return null;
    }

    public ItemStack remove(int amount) {
        return this.container.removeItem(this.slot, amount);
    }

    public boolean mayPickup(Player player) {
        return true;
    }

    public boolean isActive() {
        return true;
    }

    public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
        if (!this.mayPickup(player)) {
            return Optional.empty();
        }
        if (!this.allowModification(player) && maxAmount < this.getItem().getCount()) {
            return Optional.empty();
        }
        ItemStack result = this.remove(amount = Math.min(amount, maxAmount));
        if (result.isEmpty()) {
            return Optional.empty();
        }
        if (this.getItem().isEmpty()) {
            this.setByPlayer(ItemStack.EMPTY, result);
        }
        return Optional.of(result);
    }

    public ItemStack safeTake(int amount, int maxAmount, Player player) {
        Optional<ItemStack> result = this.tryRemove(amount, maxAmount, player);
        result.ifPresent(item -> this.onTake(player, (ItemStack)item));
        return result.orElse(ItemStack.EMPTY);
    }

    public ItemStack safeClone(Player player) {
        ItemStack item = this.getItem();
        return item.copyWithCount(item.getMaxStackSize());
    }

    public ItemStack safeInsert(ItemStack stack) {
        return this.safeInsert(stack, stack.getCount());
    }

    public ItemStack safeInsert(ItemStack inputStack, int inputAmount) {
        if (inputStack.isEmpty() || !this.mayPlace(inputStack)) {
            return inputStack;
        }
        ItemStack slotStack = this.getItem();
        int transferableItemCount = Math.min(Math.min(inputAmount, inputStack.getCount()), this.getMaxStackSize(inputStack) - slotStack.getCount());
        if (transferableItemCount <= 0) {
            return inputStack;
        }
        if (slotStack.isEmpty()) {
            this.setByPlayer(inputStack.split(transferableItemCount));
        } else if (ItemStack.isSameItemSameComponents(slotStack, inputStack)) {
            inputStack.shrink(transferableItemCount);
            slotStack.grow(transferableItemCount);
            this.setByPlayer(slotStack);
        }
        return inputStack;
    }

    public boolean allowModification(Player player) {
        return this.mayPickup(player) && this.mayPlace(this.getItem());
    }

    public int getContainerSlot() {
        return this.slot;
    }

    public boolean isHighlightable() {
        return true;
    }

    public boolean isFake() {
        return false;
    }
}

