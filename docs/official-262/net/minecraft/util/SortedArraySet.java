/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrays
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class SortedArraySet<T>
extends AbstractSet<T> {
    private static final int DEFAULT_INITIAL_CAPACITY = 10;
    private final Comparator<T> comparator;
    private T[] contents;
    private int size;

    private SortedArraySet(int initialCapacity, Comparator<T> comparator) {
        this.comparator = comparator;
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity (" + initialCapacity + ") is negative");
        }
        this.contents = SortedArraySet.castRawArray(new Object[initialCapacity]);
    }

    public static <T extends Comparable<T>> SortedArraySet<T> create() {
        return SortedArraySet.create(10);
    }

    public static <T extends Comparable<T>> SortedArraySet<T> create(int initialCapacity) {
        return new SortedArraySet(initialCapacity, Comparator.naturalOrder());
    }

    public static <T> SortedArraySet<T> create(Comparator<T> comparator) {
        return SortedArraySet.create(comparator, 10);
    }

    public static <T> SortedArraySet<T> create(Comparator<T> comparator, int initialCapacity) {
        return new SortedArraySet<T>(initialCapacity, comparator);
    }

    private static <T> T[] castRawArray(Object[] array) {
        return array;
    }

    private int findIndex(T t) {
        return Arrays.binarySearch(this.contents, 0, this.size, t, this.comparator);
    }

    private static int getInsertionPosition(int position) {
        return -position - 1;
    }

    @Override
    public boolean add(T t) {
        int position = this.findIndex(t);
        if (position >= 0) {
            return false;
        }
        int pos = SortedArraySet.getInsertionPosition(position);
        this.addInternal(t, pos);
        return true;
    }

    private void grow(int capacity) {
        if (capacity <= this.contents.length) {
            return;
        }
        if (this.contents != ObjectArrays.DEFAULT_EMPTY_ARRAY) {
            capacity = Util.growByHalf(this.contents.length, capacity);
        } else if (capacity < 10) {
            capacity = 10;
        }
        Object[] t = new Object[capacity];
        System.arraycopy(this.contents, 0, t, 0, this.size);
        this.contents = SortedArraySet.castRawArray(t);
    }

    private void addInternal(T t, int pos) {
        this.grow(this.size + 1);
        if (pos != this.size) {
            System.arraycopy(this.contents, pos, this.contents, pos + 1, this.size - pos);
        }
        this.contents[pos] = t;
        ++this.size;
    }

    private void removeInternal(int position) {
        --this.size;
        if (position != this.size) {
            System.arraycopy(this.contents, position + 1, this.contents, position, this.size - position);
        }
        this.contents[this.size] = null;
    }

    private T getInternal(int position) {
        return this.contents[position];
    }

    public T addOrGet(T t) {
        int position = this.findIndex(t);
        if (position >= 0) {
            return this.getInternal(position);
        }
        this.addInternal(t, SortedArraySet.getInsertionPosition(position));
        return t;
    }

    @Override
    public boolean remove(Object o) {
        int position = this.findIndex(o);
        if (position >= 0) {
            this.removeInternal(position);
            return true;
        }
        return false;
    }

    public @Nullable T get(T t) {
        int position = this.findIndex(t);
        if (position >= 0) {
            return this.getInternal(position);
        }
        return null;
    }

    public T first() {
        return this.getInternal(0);
    }

    public T last() {
        return this.getInternal(this.size - 1);
    }

    @Override
    public boolean contains(Object o) {
        int result = this.findIndex(o);
        return result >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator(this);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(this.contents, this.size, Object[].class);
    }

    @Override
    public <U> U[] toArray(U[] a) {
        if (a.length < this.size) {
            return Arrays.copyOf(this.contents, this.size, a.getClass());
        }
        System.arraycopy(this.contents, 0, a, 0, this.size);
        if (a.length > this.size) {
            a[this.size] = null;
        }
        return a;
    }

    @Override
    public void clear() {
        Arrays.fill(this.contents, 0, this.size, null);
        this.size = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SortedArraySet) {
            SortedArraySet that = (SortedArraySet)o;
            if (this.comparator.equals(that.comparator)) {
                return this.size == that.size && Arrays.equals(this.contents, that.contents);
            }
        }
        return super.equals(o);
    }

    private class ArrayIterator
    implements Iterator<T> {
        private int index;
        private int last;
        final /* synthetic */ SortedArraySet this$0;

        private ArrayIterator(SortedArraySet sortedArraySet) {
            SortedArraySet sortedArraySet2 = sortedArraySet;
            Objects.requireNonNull(sortedArraySet2);
            this.this$0 = sortedArraySet2;
            this.last = -1;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.this$0.size;
        }

        @Override
        public T next() {
            if (this.index >= this.this$0.size) {
                throw new NoSuchElementException();
            }
            this.last = this.index++;
            return this.this$0.contents[this.last];
        }

        @Override
        public void remove() {
            if (this.last == -1) {
                throw new IllegalStateException();
            }
            this.this$0.removeInternal(this.last);
            --this.index;
            this.last = -1;
        }
    }
}

