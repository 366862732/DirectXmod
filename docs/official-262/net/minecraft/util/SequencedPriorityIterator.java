/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.AbstractIterator
 *  com.google.common.collect.Queues
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMaps
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

public final class SequencedPriorityIterator<T>
extends AbstractIterator<T> {
    private static final int MIN_PRIO = Integer.MIN_VALUE;
    private @Nullable Deque<T> highestPrioQueue = null;
    private int highestPrio = Integer.MIN_VALUE;
    private final Int2ObjectMap<Deque<T>> queuesByPriority = new Int2ObjectOpenHashMap();

    public void add(T data, int priority) {
        if (priority == this.highestPrio && this.highestPrioQueue != null) {
            this.highestPrioQueue.addLast(data);
            return;
        }
        Deque queue = (Deque)this.queuesByPriority.computeIfAbsent(priority, order -> Queues.newArrayDeque());
        queue.addLast(data);
        if (priority >= this.highestPrio) {
            this.highestPrioQueue = queue;
            this.highestPrio = priority;
        }
    }

    protected @Nullable T computeNext() {
        if (this.highestPrioQueue == null) {
            return (T)this.endOfData();
        }
        T result = this.highestPrioQueue.removeFirst();
        if (result == null) {
            return (T)this.endOfData();
        }
        if (this.highestPrioQueue.isEmpty()) {
            this.switchCacheToNextHighestPrioQueue();
        }
        return result;
    }

    private void switchCacheToNextHighestPrioQueue() {
        int foundHighestPrio = Integer.MIN_VALUE;
        Deque foundHighestPrioQueue = null;
        for (Int2ObjectMap.Entry entry : Int2ObjectMaps.fastIterable(this.queuesByPriority)) {
            Deque queue = (Deque)entry.getValue();
            int prio = entry.getIntKey();
            if (prio <= foundHighestPrio || queue.isEmpty()) continue;
            foundHighestPrio = prio;
            foundHighestPrioQueue = queue;
            if (prio != this.highestPrio - 1) continue;
            break;
        }
        this.highestPrio = foundHighestPrio;
        this.highestPrioQueue = foundHighestPrioQueue;
    }
}

