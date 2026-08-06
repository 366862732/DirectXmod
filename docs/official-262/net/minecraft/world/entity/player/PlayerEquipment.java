/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.player;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerEquipment
extends EntityEquipment {
    private final Player player;

    public PlayerEquipment(Player player) {
        this.player = player;
    }

    @Override
    public ItemStack set(EquipmentSlot slot, ItemStack itemStack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.player.getInventory().setSelectedItem(itemStack);
        }
        return super.set(slot, itemStack);
    }

    @Override
    public ItemStack get(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.player.getInventory().getSelectedItem();
        }
        return super.get(slot);
    }

    @Override
    public boolean isEmpty() {
        return this.player.getInventory().getSelectedItem().isEmpty() && super.isEmpty();
    }
}

