/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Queues
 *  com.mojang.jtracy.TracyClient
 *  com.mojang.jtracy.Zone
 *  com.mojang.logging.LogUtils
 *  javax.annotation.CheckReturnValue
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import net.minecraft.util.thread.TaskScheduler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BlockableEventLoop<R extends Runnable>
implements Executor,
TaskScheduler<R>,
ProfilerMeasured {
    public static final long BLOCK_TIME_NANOS = 100000L;
    private static volatile @Nullable Supplier<CrashReport> delayedCrash;
    private final boolean propagatesCrashes;
    private final String name;
    private static final Logger LOGGER;
    private final Queue<R> pendingRunnables = Queues.newConcurrentLinkedQueue();
    private int blockingCount;

    protected BlockableEventLoop(String name, boolean propagatesCrashes) {
        this.propagatesCrashes = propagatesCrashes;
        this.name = name;
        MetricsRegistry.INSTANCE.add(this);
    }

    protected abstract boolean shouldRun(R var1);

    public boolean isSameThread() {
        return Thread.currentThread() == this.getRunningThread();
    }

    protected abstract Thread getRunningThread();

    protected boolean scheduleExecutables() {
        return !this.isSameThread();
    }

    public int getPendingTasksCount() {
        return this.pendingRunnables.size();
    }

    @Override
    public String name() {
        return this.name;
    }

    public <V> CompletableFuture<V> submit(Supplier<V> supplier) {
        if (this.scheduleExecutables()) {
            return CompletableFuture.supplyAsync(supplier, this);
        }
        return CompletableFuture.completedFuture(supplier.get());
    }

    private CompletableFuture<Void> submitAsync(Runnable runnable) {
        return CompletableFuture.supplyAsync(() -> {
            runnable.run();
            return null;
        }, this);
    }

    @CheckReturnValue
    public CompletableFuture<Void> submit(Runnable runnable) {
        if (this.scheduleExecutables()) {
            return this.submitAsync(runnable);
        }
        runnable.run();
        return CompletableFuture.completedFuture(null);
    }

    public void executeBlocking(Runnable runnable) {
        if (!this.isSameThread()) {
            this.submitAsync(runnable).join();
        } else {
            runnable.run();
        }
    }

    @Override
    public void schedule(R r) {
        this.pendingRunnables.add(r);
        LockSupport.unpark(this.getRunningThread());
    }

    @Override
    public void execute(Runnable command) {
        Object task = this.wrapRunnable(command);
        if (this.scheduleExecutables()) {
            this.schedule(task);
        } else {
            this.doRunTask(task);
        }
    }

    public void executeIfPossible(Runnable command) {
        this.execute(command);
    }

    protected void dropAllTasks() {
        this.pendingRunnables.clear();
    }

    protected void runAllTasks() {
        while (this.pollTask()) {
        }
    }

    protected boolean shouldRunAllTasks() {
        return this.blockingCount > 0;
    }

    protected boolean pollTask() {
        this.throwDelayedException();
        Runnable task = (Runnable)this.pendingRunnables.peek();
        if (task == null) {
            return false;
        }
        if (!this.shouldRunAllTasks() && !this.shouldRun(task)) {
            return false;
        }
        this.doRunTask((Runnable)this.pendingRunnables.remove());
        return true;
    }

    public void managedBlock(BooleanSupplier condition) {
        ++this.blockingCount;
        try {
            while (!condition.getAsBoolean()) {
                if (this.pollTask()) continue;
                this.waitForTasks();
            }
        }
        finally {
            --this.blockingCount;
        }
    }

    protected void waitForTasks() {
        Thread.yield();
        LockSupport.parkNanos("waiting for tasks", 100000L);
    }

    protected void doRunTask(R task) {
        block8: {
            try (Zone ignored = TracyClient.beginZone((String)"Task", (boolean)SharedConstants.IS_RUNNING_IN_IDE);){
                task.run();
            }
            catch (Exception e) {
                LOGGER.error(LogUtils.FATAL_MARKER, "Error executing task on {}", (Object)this.name(), (Object)e);
                if (!BlockableEventLoop.isNonRecoverable(e)) break block8;
                throw e;
            }
        }
    }

    @Override
    public List<MetricSampler> profiledMetrics() {
        return ImmutableList.of((Object)MetricSampler.create(this.name + "-pending-tasks", MetricCategory.EVENT_LOOPS, this::getPendingTasksCount));
    }

    public static boolean isNonRecoverable(Throwable t) {
        if (t instanceof ReportedException) {
            ReportedException r = (ReportedException)t;
            return BlockableEventLoop.isNonRecoverable(r.getCause());
        }
        return t instanceof OutOfMemoryError || t instanceof StackOverflowError;
    }

    private void throwDelayedException() {
        Supplier<CrashReport> delayedCrash;
        if (this.propagatesCrashes && (delayedCrash = BlockableEventLoop.delayedCrash) != null) {
            throw new ReportedException(delayedCrash.get());
        }
    }

    public void delayCrash(CrashReport crashReport) {
        delayedCrash = () -> crashReport;
    }

    public static synchronized void relayDelayCrash(CrashReport crashReport) {
        Supplier<CrashReport> delayedCrash = BlockableEventLoop.delayedCrash;
        if (delayedCrash == null) {
            BlockableEventLoop.delayedCrash = () -> crashReport;
        } else {
            delayedCrash.get().getException().addSuppressed(crashReport.getException());
        }
    }

    static {
        LOGGER = LogUtils.getLogger();
    }
}

