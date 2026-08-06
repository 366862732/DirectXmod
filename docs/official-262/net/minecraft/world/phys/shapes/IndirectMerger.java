/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.DoubleArrayList
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 *  it.unimi.dsi.fastutil.doubles.DoubleLists
 */
package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;
import net.minecraft.world.phys.shapes.IndexMerger;

public class IndirectMerger
implements IndexMerger {
    private static final DoubleList EMPTY = DoubleLists.unmodifiable((DoubleList)DoubleArrayList.wrap((double[])new double[]{0.0}));
    private final double[] result;
    private final int[] firstIndices;
    private final int[] secondIndices;
    private final int resultLength;

    public IndirectMerger(DoubleList first, DoubleList second, boolean firstOnlyMatters, boolean secondOnlyMatters) {
        double lastValue = Double.NaN;
        int firstSize = first.size();
        int secondSize = second.size();
        int capacity = firstSize + secondSize;
        this.result = new double[capacity];
        this.firstIndices = new int[capacity];
        this.secondIndices = new int[capacity];
        boolean canSkipFirst = !firstOnlyMatters;
        boolean canSkipSecond = !secondOnlyMatters;
        int resultIndex = 0;
        int firstIndex = 0;
        int secondIndex = 0;
        while (true) {
            double nextValue;
            boolean choseFirst;
            boolean ranOutOfSecond;
            boolean ranOutOfFirst = firstIndex >= firstSize;
            boolean bl = ranOutOfSecond = secondIndex >= secondSize;
            if (ranOutOfFirst && ranOutOfSecond) break;
            boolean bl2 = choseFirst = !ranOutOfFirst && (ranOutOfSecond || first.getDouble(firstIndex) < second.getDouble(secondIndex) + 1.0E-7);
            if (choseFirst) {
                ++firstIndex;
                if (canSkipFirst && (secondIndex == 0 || ranOutOfSecond)) {
                    continue;
                }
            } else {
                ++secondIndex;
                if (canSkipSecond && (firstIndex == 0 || ranOutOfFirst)) continue;
            }
            int currentFirstIndex = firstIndex - 1;
            int currentSecondIndex = secondIndex - 1;
            double d = nextValue = choseFirst ? first.getDouble(currentFirstIndex) : second.getDouble(currentSecondIndex);
            if (!(lastValue >= nextValue - 1.0E-7)) {
                this.firstIndices[resultIndex] = currentFirstIndex;
                this.secondIndices[resultIndex] = currentSecondIndex;
                this.result[resultIndex] = nextValue;
                ++resultIndex;
                lastValue = nextValue;
                continue;
            }
            this.firstIndices[resultIndex - 1] = currentFirstIndex;
            this.secondIndices[resultIndex - 1] = currentSecondIndex;
        }
        this.resultLength = Math.max(1, resultIndex);
    }

    @Override
    public boolean forMergedIndexes(IndexMerger.IndexConsumer consumer) {
        int length = this.resultLength - 1;
        for (int i = 0; i < length; ++i) {
            if (consumer.merge(this.firstIndices[i], this.secondIndices[i], i)) continue;
            return false;
        }
        return true;
    }

    @Override
    public int size() {
        return this.resultLength;
    }

    @Override
    public DoubleList getList() {
        return this.resultLength <= 1 ? EMPTY : DoubleArrayList.wrap((double[])this.result, (int)this.resultLength);
    }
}

