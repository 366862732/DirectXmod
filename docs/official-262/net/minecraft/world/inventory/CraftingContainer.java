/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

public interface CraftingContainer
extends Container,
StackedContentsCompatible {
    public int getWidth();

    public int getHeight();

    public List<ItemStack> getItems();

    default public CraftingInput asCraftInput() {
        return this.asPositionedCraftInput().input();
    }

    default public CraftingInput.Positioned asPositionedCraftInput() {
        return CraftingInput.ofPositioned(this.getWidth(), this.getHeight(), this.getItems());
    }
}

