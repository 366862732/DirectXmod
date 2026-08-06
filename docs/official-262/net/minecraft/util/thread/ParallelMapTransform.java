/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class ParallelMapTransform {
    private static final int DEFAULT_TASKS_PER_THREAD = 16;

    public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> input, BiFunction<K, U, @Nullable V> operation, int maxTaskCount, Executor executor) {
        int inputSize = input.size();
        if (inputSize == 0) {
            return CompletableFuture.completedFuture(Map.of());
        }
        if (inputSize == 1) {
            Map.Entry<K, U> element = input.entrySet().iterator().next();
            Object key = element.getKey();
            Object value = element.getValue();
            return CompletableFuture.supplyAsync(() -> {
                Object result = operation.apply(key, value);
                return result != null ? Map.of(key, result) : Map.of();
            }, executor);
        }
        SplitterBase splitter = inputSize <= maxTaskCount ? new SingleTaskSplitter<K, U, V>(operation, inputSize) : new BatchedTaskSplitter<K, U, V>(operation, inputSize, maxTaskCount);
        return splitter.scheduleTasks(input, executor);
    }

    public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> input, BiFunction<K, U, @Nullable V> operation, Executor executor) {
        int maxTaskCount = Util.maxAllowedExecutorThreads() * 16;
        return ParallelMapTransform.schedule(input, operation, maxTaskCount, executor);
    }

    private static class SingleTaskSplitter<K, U, V>
    extends SplitterBase<K, U, V> {
        private SingleTaskSplitter(BiFunction<K, U, V> operation, int size) {
            super(operation, size, size);
        }

        @Override
        protected int batchSize(int index) {
            return 1;
        }

        @Override
        protected CompletableFuture<?> scheduleBatch(Container<K, U, V> container, int startIndex, int endIndex, Executor executor) {
            assert (startIndex + 1 == endIndex);
            return CompletableFuture.runAsync(() -> container.applyOperation(startIndex), executor);
        }

        @Override
        protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> allTasksDone, Container<K, U, V> container) {
            return allTasksDone.thenApply(ignored -> {
                HashMap result = new HashMap(container.size());
                for (int i = 0; i < container.size(); ++i) {
                    container.copyOut(i, result);
                }
                return result;
            });
        }
    }

    private static class BatchedTaskSplitter<K, U, V>
    extends SplitterBase<K, U, V> {
        private final Map<K, V> result;
        private final int batchSize;
        private final int firstUndersizedBatchIndex;

        private BatchedTaskSplitter(BiFunction<K, U, V> operation, int size, int maxTasks) {
            super(operation, size, maxTasks);
            this.result = new HashMap(size);
            this.batchSize = Mth.positiveCeilDiv(size, maxTasks);
            int fullCapacity = this.batchSize * maxTasks;
            int leftoverCapacity = fullCapacity - size;
            this.firstUndersizedBatchIndex = maxTasks - leftoverCapacity;
            assert (this.firstUndersizedBatchIndex > 0 && this.firstUndersizedBatchIndex <= maxTasks);
        }

        @Override
        protected CompletableFuture<?> scheduleBatch(Container<K, U, V> container, int startIndex, int endIndex, Executor executor) {
            int batchSize = endIndex - startIndex;
            assert (batchSize == this.batchSize || batchSize == this.batchSize - 1);
            return CompletableFuture.runAsync(BatchedTaskSplitter.createTask(this.result, startIndex, endIndex, container), executor);
        }

        @Override
        protected int batchSize(int index) {
            return index < this.firstUndersizedBatchIndex ? this.batchSize : this.batchSize - 1;
        }

        private static <K, U, V> Runnable createTask(Map<K, V> result, int startIndex, int endIndex, Container<K, U, V> container) {
            return () -> {
                for (int i = startIndex; i < endIndex; ++i) {
                    container.applyOperation(i);
                }
                Map map = result;
                synchronized (map) {
                    for (int i = startIndex; i < endIndex; ++i) {
                        container.copyOut(i, result);
                    }
                }
            };
        }

        @Override
        protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> allTasksDone, Container<K, U, V> container) {
            Map result = this.result;
            return allTasksDone.thenApply(ignored -> result);
        }
    }

    private static abstract class SplitterBase<K, U, V> {
        private int lastScheduledIndex;
        private int currentIndex;
        private final CompletableFuture<?>[] tasks;
        private int batchIndex;
        private final Container<K, U, V> container;

        private SplitterBase(BiFunction<K, U, V> operation, int size, int taskCount) {
            this.container = new Container<K, U, V>(operation, size);
            this.tasks = new CompletableFuture[taskCount];
        }

        private int pendingBatchSize() {
            return this.currentIndex - this.lastScheduledIndex;
        }

        public CompletableFuture<Map<K, V>> scheduleTasks(Map<K, U> input, Executor executor) {
            input.forEach((key, inputValue) -> {
                this.container.put(this.currentIndex++, key, inputValue);
                if (this.pendingBatchSize() == this.batchSize(this.batchIndex)) {
                    this.tasks[this.batchIndex++] = this.scheduleBatch(this.container, this.lastScheduledIndex, this.currentIndex, executor);
                    this.lastScheduledIndex = this.currentIndex;
                }
            });
            assert (this.currentIndex == this.container.size());
            assert (this.lastScheduledIndex == this.currentIndex);
            assert (this.batchIndex == this.tasks.length);
            return this.scheduleFinalOperation(CompletableFuture.allOf(this.tasks), this.container);
        }

        protected abstract int batchSize(int var1);

        protected abstract CompletableFuture<?> scheduleBatch(Container<K, U, V> var1, int var2, int var3, Executor var4);

        protected abstract CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> var1, Container<K, U, V> var2);
    }

    private record Container<K, U, V>(BiFunction<K, U, V> operation, @Nullable Object[] keys, @Nullable Object[] values) {
        public Container(BiFunction<K, U, V> operation, int size) {
            this(operation, new Object[size], new Object[size]);
        }

        public void put(int index, K key, U input) {
            this.keys[index] = key;
            this.values[index] = input;
        }

        private @Nullable K key(int index) {
            return (K)this.keys[index];
        }

        private @Nullable V output(int index) {
            return (V)this.values[index];
        }

        private @Nullable U input(int index) {
            return (U)this.values[index];
        }

        public void applyOperation(int index) {
            this.values[index] = this.operation.apply(this.key(index), this.input(index));
        }

        public void copyOut(int index, Map<K, V> output) {
            V value = this.output(index);
            if (value != null) {
                K key = this.key(index);
                output.put(key, value);
            }
        }

        public int size() {
            return this.keys.length;
        }
    }
}

