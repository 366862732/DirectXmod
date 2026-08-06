/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NonInteractiveResultSlot
extends Slot {
    public NonInteractiveResultSlot(Container container, int id, int x, int y) {
        super(container, id, x, y);
    }

    @Override
    public void onQuickCraft(ItemStack picked, ItemStack original) {
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
        return Optional.empty();
    }

    @Override
    public ItemStack safeTake(int amount, int maxAmount, Player player) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack) {
        return stack;
    }

    @Override
    public ItemStack safeInsert(ItemStack inputStack, int inputAmount) {
        return this.safeInsert(inputStack);
    }

    @Override
    public boolean allowModification(Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onTake(Player player, ItemStack carried) {
    }

    @Override
    public boolean isHighlightable() {
        return false;
    }

    @Override
    public boolean isFake() {
        return true;
    }
}

