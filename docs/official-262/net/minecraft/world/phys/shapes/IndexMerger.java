/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 */
package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public interface IndexMerger {
    public DoubleList getList();

    public boolean forMergedIndexes(IndexConsumer var1);

    public int size();

    public static interface IndexConsumer {
        public boolean merge(int var1, int var2, int var3);
    }
}

