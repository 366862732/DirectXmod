/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.component;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

public interface ConsumableListener {
    public void onConsume(Level var1, LivingEntity var2, ItemStack var3, Consumable var4);
}

