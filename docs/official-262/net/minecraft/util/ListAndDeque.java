/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.RandomAccess;
import org.jspecify.annotations.Nullable;

public interface ListAndDeque<T>
extends List<T>,
RandomAccess,
Cloneable,
Serializable,
Deque<T> {
    @Override
    public ListAndDeque<T> reversed();

    @Override
    public T getFirst();

    @Override
    public T getLast();

    @Override
    public void addFirst(T var1);

    @Override
    public void addLast(T var1);

    @Override
    public T removeFirst();

    @Override
    public T removeLast();

    @Override
    default public boolean offer(T value) {
        return this.offerLast(value);
    }

    @Override
    default public T remove() {
        return this.removeFirst();
    }

    @Override
    default public @Nullable T poll() {
        return (T)this.pollFirst();
    }

    @Override
    default public T element() {
        return this.getFirst();
    }

    @Override
    default public @Nullable T peek() {
        return (T)this.peekFirst();
    }

    @Override
    default public void push(T value) {
        this.addFirst(value);
    }

    @Override
    default public T pop() {
        return this.removeFirst();
    }
}

