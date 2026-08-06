/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.Hash$Strategy
 *  it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ItemStackLinkedSet {
    private static final Hash.Strategy<? super ItemStack> TYPE_AND_TAG = new Hash.Strategy<ItemStack>(){

        public int hashCode(@Nullable ItemStack item) {
            return ItemStack.hashItemAndComponents(item);
        }

        public boolean equals(@Nullable ItemStack a, @Nullable ItemStack b) {
            return a == b || a != null && b != null && a.isEmpty() == b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
        }
    };

    public static Set<ItemStack> createTypeAndComponentsSet() {
        return new ObjectLinkedOpenCustomHashSet(TYPE_AND_TAG);
    }
}

