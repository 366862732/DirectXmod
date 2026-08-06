/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet
 */
package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.Objects;

public class LeveledPriorityQueue {
    private final int levelCount;
    private final LongLinkedOpenHashSet[] queues;
    private int firstQueuedLevel;

    public LeveledPriorityQueue(int levelCount, final int minSize) {
        this.levelCount = levelCount;
        this.queues = new LongLinkedOpenHashSet[levelCount];
        for (int i = 0; i < levelCount; ++i) {
            this.queues[i] = new LongLinkedOpenHashSet(this, minSize, 0.5f){
                final /* synthetic */ LeveledPriorityQueue this$0;
                {
                    LeveledPriorityQueue leveledPriorityQueue = this$0;
                    Objects.requireNonNull(leveledPriorityQueue);
                    this.this$0 = leveledPriorityQueue;
                    super(expected, f);
                }

                protected void rehash(int newN) {
                    if (newN > minSize) {
                        super.rehash(newN);
                    }
                }
            };
        }
        this.firstQueuedLevel = levelCount;
    }

    public long removeFirstLong() {
        LongLinkedOpenHashSet queue = this.queues[this.firstQueuedLevel];
        long result = queue.removeFirstLong();
        if (queue.isEmpty()) {
            this.checkFirstQueuedLevel(this.levelCount);
        }
        return result;
    }

    public boolean isEmpty() {
        return this.firstQueuedLevel >= this.levelCount;
    }

    public void dequeue(long node, int key, int upperBound) {
        LongLinkedOpenHashSet queue = this.queues[key];
        queue.remove(node);
        if (queue.isEmpty() && this.firstQueuedLevel == key) {
            this.checkFirstQueuedLevel(upperBound);
        }
    }

    public void enqueue(long node, int key) {
        this.queues[key].add(node);
        if (this.firstQueuedLevel > key) {
            this.firstQueuedLevel = key;
        }
    }

    private void checkFirstQueuedLevel(int upperBound) {
        int oldLevel = this.firstQueuedLevel;
        this.firstQueuedLevel = upperBound;
        for (int i = oldLevel + 1; i < upperBound; ++i) {
            if (this.queues[i].isEmpty()) continue;
            this.firstQueuedLevel = i;
            break;
        }
    }
}

