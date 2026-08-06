/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class ChunkTaskPriorityQueue {
    public static final int PRIORITY_LEVEL_COUNT = ChunkLevel.MAX_LEVEL + 2;
    private final List<Long2ObjectLinkedOpenHashMap<List<Runnable>>> queuesPerPriority = IntStream.range(0, PRIORITY_LEVEL_COUNT).mapToObj(priority -> new Long2ObjectLinkedOpenHashMap()).toList();
    private volatile int topPriorityQueueIndex = PRIORITY_LEVEL_COUNT;
    private final String name;

    public ChunkTaskPriorityQueue(String name) {
        this.name = name;
    }

    protected void resortChunkTasks(int oldPriority, ChunkPos pos, int newPriority) {
        if (oldPriority >= PRIORITY_LEVEL_COUNT) {
            return;
        }
        Long2ObjectLinkedOpenHashMap<List<Runnable>> oldQueue = this.queuesPerPriority.get(oldPriority);
        List oldTasks = (List)oldQueue.remove(pos.pack());
        if (oldPriority == this.topPriorityQueueIndex) {
            while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
                ++this.topPriorityQueueIndex;
            }
        }
        if (oldTasks != null && !oldTasks.isEmpty()) {
            ((List)this.queuesPerPriority.get(newPriority).computeIfAbsent(pos.pack(), k -> Lists.newArrayList())).addAll(oldTasks);
            this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, newPriority);
        }
    }

    protected void submit(Runnable task, long chunkPos, int level) {
        ((List)this.queuesPerPriority.get(level).computeIfAbsent(chunkPos, p -> Lists.newArrayList())).add(task);
        this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, level);
    }

    protected void release(long pos, boolean unschedule) {
        for (Long2ObjectLinkedOpenHashMap<List<Runnable>> queue : this.queuesPerPriority) {
            List tasks = (List)queue.get(pos);
            if (tasks == null) continue;
            if (unschedule) {
                tasks.clear();
            }
            if (!tasks.isEmpty()) continue;
            queue.remove(pos);
        }
        while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
            ++this.topPriorityQueueIndex;
        }
    }

    public @Nullable TasksForChunk pop() {
        if (!this.hasWork()) {
            return null;
        }
        int index = this.topPriorityQueueIndex;
        Long2ObjectLinkedOpenHashMap<List<Runnable>> queue = this.queuesPerPriority.get(index);
        long chunkPos = queue.firstLongKey();
        List tasks = (List)queue.removeFirst();
        while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
            ++this.topPriorityQueueIndex;
        }
        return new TasksForChunk(chunkPos, tasks);
    }

    public boolean hasWork() {
        return this.topPriorityQueueIndex < PRIORITY_LEVEL_COUNT;
    }

    public String toString() {
        return this.name + " " + this.topPriorityQueueIndex + "...";
    }

    public record TasksForChunk(long chunkPos, List<Runnable> tasks) {
    }
}

