/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipePropertySet;

public class BlastFurnaceMenu
extends AbstractFurnaceMenu {
    public BlastFurnaceMenu(int containerId, Inventory inventory) {
        super(MenuType.BLAST_FURNACE, RecipePropertySet.BLAST_FURNACE_INPUT, RecipeBookType.BLAST_FURNACE, containerId, inventory);
    }

    public BlastFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MenuType.BLAST_FURNACE, RecipePropertySet.BLAST_FURNACE_INPUT, RecipeBookType.BLAST_FURNACE, containerId, inventory, container, data);
    }
}

