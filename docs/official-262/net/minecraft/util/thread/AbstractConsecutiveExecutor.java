/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.util.thread.TaskScheduler;
import org.slf4j.Logger;

public abstract class AbstractConsecutiveExecutor<T extends Runnable>
implements Runnable,
TaskScheduler<T>,
ProfilerMeasured {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.SLEEPING);
    private final StrictQueue<T> queue;
    private final Executor executor;
    private final String name;

    public AbstractConsecutiveExecutor(StrictQueue<T> queue, Executor executor, String name) {
        this.executor = executor;
        this.queue = queue;
        this.name = name;
        MetricsRegistry.INSTANCE.add(this);
    }

    private boolean canBeScheduled() {
        return !this.isClosed() && !this.queue.isEmpty();
    }

    @Override
    public void close() {
        this.status.set(Status.CLOSED);
    }

    private boolean pollTask() {
        if (!this.isRunning()) {
            return false;
        }
        Runnable runnable = this.queue.pop();
        if (runnable == null) {
            return false;
        }
        Util.runNamed(runnable, this.name);
        return true;
    }

    @Override
    public void run() {
        try {
            this.pollTask();
        }
        finally {
            this.setSleeping();
            this.registerForExecution();
        }
    }

    public void runAll() {
        try {
            while (this.pollTask()) {
            }
        }
        finally {
            this.setSleeping();
            this.registerForExecution();
        }
    }

    @Override
    public void schedule(T task) {
        this.queue.push(task);
        this.registerForExecution();
    }

    private void registerForExecution() {
        if (this.canBeScheduled() && this.setRunning()) {
            try {
                this.executor.execute(this);
            }
            catch (RejectedExecutionException e) {
                try {
                    this.executor.execute(this);
                }
                catch (RejectedExecutionException e2) {
                    LOGGER.error("Could not schedule ConsecutiveExecutor", (Throwable)e2);
                }
            }
        }
    }

    public int size() {
        return this.queue.size();
    }

    public boolean hasWork() {
        return this.isRunning() && !this.queue.isEmpty();
    }

    public String toString() {
        return this.name + " " + String.valueOf((Object)this.status.get()) + " " + this.queue.isEmpty();
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public List<MetricSampler> profiledMetrics() {
        return ImmutableList.of((Object)MetricSampler.create(this.name + "-queue-size", MetricCategory.CONSECUTIVE_EXECUTORS, this::size));
    }

    private boolean setRunning() {
        return this.status.compareAndSet(Status.SLEEPING, Status.RUNNING);
    }

    private void setSleeping() {
        this.status.compareAndSet(Status.RUNNING, Status.SLEEPING);
    }

    private boolean isRunning() {
        return this.status.get() == Status.RUNNING;
    }

    private boolean isClosed() {
        return this.status.get() == Status.CLOSED;
    }

    private static enum Status {
        SLEEPING,
        RUNNING,
        CLOSED;

    }
}

