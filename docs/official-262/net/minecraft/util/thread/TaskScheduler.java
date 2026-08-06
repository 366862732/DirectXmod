/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface TaskScheduler<R extends Runnable>
extends AutoCloseable {
    public String name();

    public void schedule(R var1);

    @Override
    default public void close() {
    }

    public R wrapRunnable(Runnable var1);

    default public <Source> CompletableFuture<Source> scheduleWithResult(Consumer<CompletableFuture<Source>> futureConsumer) {
        CompletableFuture future = new CompletableFuture();
        this.schedule(this.wrapRunnable(() -> futureConsumer.accept(future)));
        return future;
    }

    public static TaskScheduler<Runnable> wrapExecutor(final String name, final Executor executor) {
        return new TaskScheduler<Runnable>(){

            @Override
            public String name() {
                return name;
            }

            @Override
            public void schedule(Runnable runnable) {
                executor.execute(runnable);
            }

            @Override
            public Runnable wrapRunnable(Runnable runnable) {
                return runnable;
            }

            public String toString() {
                return name;
            }
        };
    }
}

