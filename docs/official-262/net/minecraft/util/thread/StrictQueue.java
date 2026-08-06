/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Queues
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.thread;

import com.google.common.collect.Queues;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

public interface StrictQueue<T extends Runnable> {
    public @Nullable Runnable pop();

    public boolean push(T var1);

    public boolean isEmpty();

    public int size();

    public static final class FixedPriorityQueue
    implements StrictQueue<RunnableWithPriority> {
        private final Queue<Runnable>[] queues;
        private final AtomicInteger size = new AtomicInteger();

        public FixedPriorityQueue(int size) {
            this.queues = new Queue[size];
            for (int i = 0; i < size; ++i) {
                this.queues[i] = Queues.newConcurrentLinkedQueue();
            }
        }

        @Override
        public @Nullable Runnable pop() {
            for (Queue<Runnable> queue : this.queues) {
                Runnable task = queue.poll();
                if (task == null) continue;
                this.size.decrementAndGet();
                return task;
            }
            return null;
        }

        @Override
        public boolean push(RunnableWithPriority task) {
            int priority = task.priority;
            if (priority >= this.queues.length || priority < 0) {
                throw new IndexOutOfBoundsException(String.format(Locale.ROOT, "Priority %d not supported. Expected range [0-%d]", priority, this.queues.length - 1));
            }
            this.queues[priority].add(task);
            this.size.incrementAndGet();
            return true;
        }

        @Override
        public boolean isEmpty() {
            return this.size.get() == 0;
        }

        @Override
        public int size() {
            return this.size.get();
        }
    }

    public record RunnableWithPriority(int priority, Runnable task) implements Runnable
    {
        @Override
        public void run() {
            this.task.run();
        }
    }

    public static final class QueueStrictQueue
    implements StrictQueue<Runnable> {
        private final Queue<Runnable> queue;

        public QueueStrictQueue(Queue<Runnable> queue) {
            this.queue = queue;
        }

        @Override
        public @Nullable Runnable pop() {
            return this.queue.poll();
        }

        @Override
        public boolean push(Runnable t) {
            return this.queue.add(t);
        }

        @Override
        public boolean isEmpty() {
            return this.queue.isEmpty();
        }

        @Override
        public int size() {
            return this.queue.size();
        }
    }
}

