/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public interface RecipeInput {
    public ItemStack getItem(int var1);

    public int size();

    default public boolean isEmpty() {
        for (int i = 0; i < this.size(); ++i) {
            if (this.getItem(i).isEmpty()) continue;
            return false;
        }
        return true;
    }
}

