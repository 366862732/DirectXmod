/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.task;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.task.RepeatedDelayStrategy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.TimeSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class DataFetcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Executor executor;
    private final TimeUnit resolution;
    private final TimeSource timeSource;

    public DataFetcher(Executor executor, TimeUnit resolution, TimeSource timeSource) {
        this.executor = executor;
        this.resolution = resolution;
        this.timeSource = timeSource;
    }

    public <T> Task<T> createTask(String id, Callable<T> updater, Duration period, RepeatedDelayStrategy repeatStrategy) {
        long periodInUnit = this.resolution.convert(period);
        if (periodInUnit == 0L) {
            throw new IllegalArgumentException("Period of " + String.valueOf(period) + " too short for selected resolution of " + String.valueOf((Object)this.resolution));
        }
        return new Task<T>(this, id, updater, periodInUnit, repeatStrategy);
    }

    public Subscription createSubscription() {
        return new Subscription(this);
    }

    @Environment(value=EnvType.CLIENT)
    public class Task<T> {
        private final String id;
        private final Callable<T> updater;
        private final long period;
        private final RepeatedDelayStrategy repeatStrategy;
        private @Nullable CompletableFuture<ComputationResult<T>> pendingTask;
        private @Nullable SuccessfulComputationResult<T> lastResult;
        private long nextUpdate;
        final /* synthetic */ DataFetcher this$0;

        private Task(DataFetcher this$0, String id, Callable<T> updater, long period, RepeatedDelayStrategy repeatStrategy) {
            DataFetcher dataFetcher = this$0;
            Objects.requireNonNull(dataFetcher);
            this.this$0 = dataFetcher;
            this.nextUpdate = -1L;
            this.id = id;
            this.updater = updater;
            this.period = period;
            this.repeatStrategy = repeatStrategy;
        }

        private void updateIfNeeded(long currentTime) {
            if (this.pendingTask != null) {
                ComputationResult result = this.pendingTask.getNow(null);
                if (result == null) {
                    return;
                }
                this.pendingTask = null;
                long completionTime = result.time;
                result.value().ifLeft(value -> {
                    this.lastResult = new SuccessfulComputationResult<Object>(value, completionTime);
                    this.nextUpdate = completionTime + this.period * this.repeatStrategy.delayCyclesAfterSuccess();
                }).ifRight(e -> {
                    long cycles = this.repeatStrategy.delayCyclesAfterFailure();
                    LOGGER.warn("Failed to process task {}, will repeat after {} cycles", new Object[]{this.id, cycles, e});
                    this.nextUpdate = completionTime + this.period * cycles;
                });
            }
            if (this.nextUpdate <= currentTime) {
                this.pendingTask = CompletableFuture.supplyAsync(() -> {
                    try {
                        T result = this.updater.call();
                        long completionTime = this.this$0.timeSource.get(this.this$0.resolution);
                        return new ComputationResult(Either.left(result), completionTime);
                    }
                    catch (Exception e) {
                        long completionTime = this.this$0.timeSource.get(this.this$0.resolution);
                        return new ComputationResult(Either.right((Object)e), completionTime);
                    }
                }, this.this$0.executor);
            }
        }

        public void reset() {
            this.pendingTask = null;
            this.lastResult = null;
            this.nextUpdate = -1L;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class Subscription {
        private final List<SubscribedTask<?>> subscriptions;
        final /* synthetic */ DataFetcher this$0;

        public Subscription(DataFetcher this$0) {
            DataFetcher dataFetcher = this$0;
            Objects.requireNonNull(dataFetcher);
            this.this$0 = dataFetcher;
            this.subscriptions = new ArrayList();
        }

        public <T> void subscribe(Task<T> task, Consumer<T> output) {
            SubscribedTask<T> subscription = new SubscribedTask<T>(this.this$0, task, output);
            this.subscriptions.add(subscription);
            subscription.runCallbackIfNeeded();
        }

        public void forceUpdate() {
            for (SubscribedTask<?> subscription : this.subscriptions) {
                subscription.runCallback();
            }
        }

        public void tick() {
            for (SubscribedTask<?> subscription : this.subscriptions) {
                subscription.update(this.this$0.timeSource.get(this.this$0.resolution));
            }
        }

        public void reset() {
            for (SubscribedTask<?> subscription : this.subscriptions) {
                subscription.reset();
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class SubscribedTask<T> {
        private final Task<T> task;
        private final Consumer<T> output;
        private long lastCheckTime;

        private SubscribedTask(DataFetcher dataFetcher, Task<T> task, Consumer<T> output) {
            Objects.requireNonNull(dataFetcher);
            this.lastCheckTime = -1L;
            this.task = task;
            this.output = output;
        }

        private void update(long currentTime) {
            this.task.updateIfNeeded(currentTime);
            this.runCallbackIfNeeded();
        }

        private void runCallbackIfNeeded() {
            SuccessfulComputationResult lastResult = this.task.lastResult;
            if (lastResult != null && this.lastCheckTime < lastResult.time) {
                this.output.accept(lastResult.value);
                this.lastCheckTime = lastResult.time;
            }
        }

        private void runCallback() {
            SuccessfulComputationResult lastResult = this.task.lastResult;
            if (lastResult != null) {
                this.output.accept(lastResult.value);
                this.lastCheckTime = lastResult.time;
            }
        }

        private void reset() {
            this.task.reset();
            this.lastCheckTime = -1L;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record SuccessfulComputationResult<T>(T value, long time) {
    }

    @Environment(value=EnvType.CLIENT)
    private record ComputationResult<T>(Either<T, Exception> value, long time) {
    }
}

