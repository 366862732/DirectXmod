/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.crafting.display;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface DisplayContentsFactory<T> {

    public static interface ForRemainders<T>
    extends DisplayContentsFactory<T> {
        public T addRemainder(T var1, List<T> var2);
    }

    public static interface ForStacks<T>
    extends DisplayContentsFactory<T> {
        default public T forStack(Holder<Item> item) {
            return this.forStack(new ItemStack(item));
        }

        default public T forStack(Item item) {
            return this.forStack(new ItemStack(item));
        }

        public T forStack(ItemStack var1);
    }
}

