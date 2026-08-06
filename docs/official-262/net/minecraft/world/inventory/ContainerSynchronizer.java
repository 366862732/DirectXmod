/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;

public interface ContainerSynchronizer {
    public void sendInitialData(AbstractContainerMenu var1, List<ItemStack> var2, ItemStack var3, int[] var4);

    public void sendSlotChange(AbstractContainerMenu var1, int var2, ItemStack var3);

    public void sendCarriedChange(AbstractContainerMenu var1, ItemStack var2);

    public void sendDataChange(AbstractContainerMenu var1, int var2, int var3);

    public RemoteSlot createSlot();
}

