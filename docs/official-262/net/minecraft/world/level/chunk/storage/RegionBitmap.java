/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.ints.IntArraySet
 *  it.unimi.dsi.fastutil.ints.IntCollection
 *  it.unimi.dsi.fastutil.ints.IntSet
 */
package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionBitmap {
    private final BitSet used = new BitSet();

    public void force(int position, int size) {
        this.used.set(position, position + size);
    }

    public void free(int position, int size) {
        this.used.clear(position, position + size);
    }

    public int allocate(int size) {
        int current = 0;
        while (true) {
            int freeStart;
            int freeEnd;
            if ((freeEnd = this.used.nextSetBit(freeStart = this.used.nextClearBit(current))) == -1 || freeEnd - freeStart >= size) {
                this.force(freeStart, size);
                return freeStart;
            }
            current = freeEnd;
        }
    }

    @VisibleForTesting
    public IntSet getUsed() {
        return (IntSet)this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
    }
}

