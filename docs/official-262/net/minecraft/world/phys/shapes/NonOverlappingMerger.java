/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.AbstractDoubleList
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 */
package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.phys.shapes.IndexMerger;

public class NonOverlappingMerger
extends AbstractDoubleList
implements IndexMerger {
    private final DoubleList lower;
    private final DoubleList upper;
    private final boolean swap;

    protected NonOverlappingMerger(DoubleList lower, DoubleList upper, boolean swap) {
        this.lower = lower;
        this.upper = upper;
        this.swap = swap;
    }

    @Override
    public int size() {
        return this.lower.size() + this.upper.size();
    }

    @Override
    public boolean forMergedIndexes(IndexMerger.IndexConsumer consumer) {
        if (this.swap) {
            return this.forNonSwappedIndexes((firstIndex, secondIndex, resultIndex) -> consumer.merge(secondIndex, firstIndex, resultIndex));
        }
        return this.forNonSwappedIndexes(consumer);
    }

    private boolean forNonSwappedIndexes(IndexMerger.IndexConsumer consumer) {
        int lowerSize = this.lower.size();
        for (int i = 0; i < lowerSize; ++i) {
            if (consumer.merge(i, -1, i)) continue;
            return false;
        }
        int upperSize = this.upper.size() - 1;
        for (int i = 0; i < upperSize; ++i) {
            if (consumer.merge(lowerSize - 1, i, lowerSize + i)) continue;
            return false;
        }
        return true;
    }

    public double getDouble(int index) {
        if (index < this.lower.size()) {
            return this.lower.getDouble(index);
        }
        return this.upper.getDouble(index - this.lower.size());
    }

    @Override
    public DoubleList getList() {
        return this;
    }
}

