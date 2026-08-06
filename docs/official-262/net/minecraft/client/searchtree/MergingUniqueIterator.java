/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.AbstractIterator
 *  com.google.common.collect.Iterators
 *  com.google.common.collect.PeekingIterator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.searchtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.Comparator;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class MergingUniqueIterator<T>
extends AbstractIterator<T> {
    private final PeekingIterator<T> firstIterator;
    private final PeekingIterator<T> secondIterator;
    private final Comparator<T> comparator;

    public MergingUniqueIterator(Iterator<T> firstIterator, Iterator<T> secondIterator, Comparator<T> comparator) {
        this.firstIterator = Iterators.peekingIterator(firstIterator);
        this.secondIterator = Iterators.peekingIterator(secondIterator);
        this.comparator = comparator;
    }

    protected T computeNext() {
        boolean secondEmpty;
        boolean firstEmpty = !this.firstIterator.hasNext();
        boolean bl = secondEmpty = !this.secondIterator.hasNext();
        if (firstEmpty && secondEmpty) {
            return (T)this.endOfData();
        }
        if (firstEmpty) {
            return (T)this.secondIterator.next();
        }
        if (secondEmpty) {
            return (T)this.firstIterator.next();
        }
        int compare = this.comparator.compare(this.firstIterator.peek(), this.secondIterator.peek());
        if (compare == 0) {
            this.secondIterator.next();
        }
        return (T)(compare <= 0 ? this.firstIterator.next() : this.secondIterator.next());
    }
}

