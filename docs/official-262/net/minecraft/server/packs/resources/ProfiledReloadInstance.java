/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ProfiledReloadInstance
extends SimpleReloadInstance<State> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Stopwatch total = Stopwatch.createUnstarted();

    public static ReloadInstance of(ResourceManager resourceManager, List<PreparableReloadListener> listeners, Executor taskExecutor, Executor mainThreadExecutor, CompletableFuture<Unit> initialTask) {
        ProfiledReloadInstance result = new ProfiledReloadInstance(listeners);
        result.startTasks(taskExecutor, mainThreadExecutor, resourceManager, listeners, (currentReload, previousStep, listener, parentTaskExecutor, parentReloadExecutor) -> {
            AtomicLong preparationNanos = new AtomicLong();
            AtomicLong preparationCount = new AtomicLong();
            AtomicLong reloadNanos = new AtomicLong();
            AtomicLong reloadCount = new AtomicLong();
            CompletableFuture<Void> reload = listener.reload(currentReload, ProfiledReloadInstance.profiledExecutor(parentTaskExecutor, preparationNanos, preparationCount, listener.getName()), previousStep, ProfiledReloadInstance.profiledExecutor(parentReloadExecutor, reloadNanos, reloadCount, listener.getName()));
            return reload.thenApplyAsync(v -> {
                LOGGER.debug("Finished reloading {}", (Object)listener.getName());
                return new State(listener.getName(), preparationNanos, preparationCount, reloadNanos, reloadCount);
            }, mainThreadExecutor);
        }, initialTask);
        return result;
    }

    private ProfiledReloadInstance(List<PreparableReloadListener> listeners) {
        super(listeners);
        this.total.start();
    }

    @Override
    protected CompletableFuture<List<State>> prepareTasks(Executor taskExecutor, Executor mainThreadExecutor, ResourceManager resourceManager, List<PreparableReloadListener> listeners, SimpleReloadInstance.StateFactory<State> stateFactory, CompletableFuture<?> initialTask) {
        return super.prepareTasks(taskExecutor, mainThreadExecutor, resourceManager, listeners, stateFactory, initialTask).thenApplyAsync(this::finish, mainThreadExecutor);
    }

    private static Executor profiledExecutor(Executor executor, AtomicLong accumulatedNanos, AtomicLong taskCount, String name) {
        return r -> executor.execute(() -> {
            ProfilerFiller profiler = Profiler.get();
            profiler.push(name);
            long nanos = Util.getNanos();
            r.run();
            accumulatedNanos.addAndGet(Util.getNanos() - nanos);
            taskCount.incrementAndGet();
            profiler.pop();
        });
    }

    private List<State> finish(List<State> result) {
        this.total.stop();
        long blockingTime = 0L;
        LOGGER.info("Resource reload finished after {} ms", (Object)this.total.elapsed(TimeUnit.MILLISECONDS));
        for (State state : result) {
            long prepTime = TimeUnit.NANOSECONDS.toMillis(state.preparationNanos.get());
            long prepCount = state.preparationCount.get();
            long reloadTime = TimeUnit.NANOSECONDS.toMillis(state.reloadNanos.get());
            long reloadCount = state.reloadCount.get();
            long totalTime = prepTime + reloadTime;
            long totalCount = prepCount + reloadCount;
            String name = state.name;
            LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", new Object[]{name, totalCount, totalTime, prepCount, prepTime, reloadCount, reloadTime});
            blockingTime += reloadTime;
        }
        LOGGER.info("Total blocking time: {} ms", (Object)blockingTime);
        return result;
    }

    public record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
    }
}

