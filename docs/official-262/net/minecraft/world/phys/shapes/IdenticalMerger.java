/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 */
package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.phys.shapes.IndexMerger;

public class IdenticalMerger
implements IndexMerger {
    private final DoubleList coords;

    public IdenticalMerger(DoubleList coords) {
        this.coords = coords;
    }

    @Override
    public boolean forMergedIndexes(IndexMerger.IndexConsumer consumer) {
        int size = this.coords.size() - 1;
        for (int i = 0; i < size; ++i) {
            if (consumer.merge(i, i, i)) continue;
            return false;
        }
        return true;
    }

    @Override
    public int size() {
        return this.coords.size();
    }

    @Override
    public DoubleList getList() {
        return this.coords;
    }
}

