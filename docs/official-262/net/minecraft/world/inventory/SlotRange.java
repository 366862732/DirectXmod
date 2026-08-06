/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntList
 */
package net.minecraft.world.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.StringRepresentable;

public interface SlotRange
extends StringRepresentable {
    public IntList slots();

    default public int size() {
        return this.slots().size();
    }

    public static SlotRange of(final String name, final IntList slots) {
        return new SlotRange(){

            @Override
            public IntList slots() {
                return slots;
            }

            @Override
            public String getSerializedName() {
                return name;
            }

            public String toString() {
                return name;
            }
        };
    }
}

