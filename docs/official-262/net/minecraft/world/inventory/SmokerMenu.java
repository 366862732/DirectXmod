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

public class SmokerMenu
extends AbstractFurnaceMenu {
    public SmokerMenu(int containerId, Inventory inventory) {
        super(MenuType.SMOKER, RecipePropertySet.SMOKER_INPUT, RecipeBookType.SMOKER, containerId, inventory);
    }

    public SmokerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MenuType.SMOKER, RecipePropertySet.SMOKER_INPUT, RecipeBookType.SMOKER, containerId, inventory, container, data);
    }
}

