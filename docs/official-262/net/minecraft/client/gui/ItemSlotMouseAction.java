/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public interface ItemSlotMouseAction {
    public boolean matches(Slot var1);

    public boolean onMouseScrolled(double var1, double var3, int var5, ItemStack var6);

    public void onStopHovering(Slot var1);

    public void onSlotClicked(Slot var1, ContainerInput var2);
}

