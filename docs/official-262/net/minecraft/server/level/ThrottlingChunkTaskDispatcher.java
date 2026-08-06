/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.server.level.ChunkTaskDispatcher;
import net.minecraft.server.level.ChunkTaskPriorityQueue;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class ThrottlingChunkTaskDispatcher
extends ChunkTaskDispatcher {
    private final LongSet chunkPositionsInExecution = new LongOpenHashSet();
    private final int maxChunksInExecution;
    private final String executorSchedulerName;

    public ThrottlingChunkTaskDispatcher(TaskScheduler<Runnable> executor, Executor dispatcherExecutor, int maxChunksInExecution) {
        super(executor, dispatcherExecutor);
        this.maxChunksInExecution = maxChunksInExecution;
        this.executorSchedulerName = executor.name();
    }

    @Override
    protected void onRelease(long key) {
        this.chunkPositionsInExecution.remove(key);
    }

    @Override
    protected @Nullable ChunkTaskPriorityQueue.TasksForChunk popTasks() {
        return this.chunkPositionsInExecution.size() < this.maxChunksInExecution ? super.popTasks() : null;
    }

    @Override
    protected void scheduleForExecution(ChunkTaskPriorityQueue.TasksForChunk tasksForChunk) {
        this.chunkPositionsInExecution.add(tasksForChunk.chunkPos());
        super.scheduleForExecution(tasksForChunk);
    }

    @VisibleForTesting
    public String getDebugStatus() {
        return this.executorSchedulerName + "=[" + this.chunkPositionsInExecution.longStream().mapToObj(key -> key + ":" + String.valueOf(ChunkPos.unpack(key))).collect(Collectors.joining(",")) + "], s=" + this.sleeping;
    }
}

