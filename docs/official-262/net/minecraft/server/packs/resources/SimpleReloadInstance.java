/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.packs.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ProfiledReloadInstance;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class SimpleReloadInstance<S>
implements ReloadInstance {
    private static final int PREPARATION_PROGRESS_WEIGHT = 2;
    private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
    private static final int LISTENER_PROGRESS_WEIGHT = 1;
    private final CompletableFuture<Unit> allPreparations = new CompletableFuture();
    private @Nullable CompletableFuture<List<S>> allDone;
    private final Set<PreparableReloadListener> preparingListeners;
    private final int listenerCount;
    private final AtomicInteger startedTasks = new AtomicInteger();
    private final AtomicInteger finishedTasks = new AtomicInteger();
    private final AtomicInteger startedReloads = new AtomicInteger();
    private final AtomicInteger finishedReloads = new AtomicInteger();

    public static ReloadInstance of(ResourceManager resourceManager, List<PreparableReloadListener> listeners, Executor taskExecutor, Executor mainThreadExecutor, CompletableFuture<Unit> initialTask) {
        SimpleReloadInstance<Void> result = new SimpleReloadInstance<Void>(listeners);
        result.startTasks(taskExecutor, mainThreadExecutor, resourceManager, listeners, StateFactory.SIMPLE, initialTask);
        return result;
    }

    protected SimpleReloadInstance(List<PreparableReloadListener> listeners) {
        this.listenerCount = listeners.size();
        this.preparingListeners = new HashSet<PreparableReloadListener>(listeners);
    }

    protected void startTasks(Executor taskExecutor, Executor mainThreadExecutor, ResourceManager resourceManager, List<PreparableReloadListener> listeners, StateFactory<S> stateFactory, CompletableFuture<?> initialTask) {
        this.allDone = this.prepareTasks(taskExecutor, mainThreadExecutor, resourceManager, listeners, stateFactory, initialTask);
    }

    protected CompletableFuture<List<S>> prepareTasks(Executor taskExecutor, Executor mainThreadExecutor, ResourceManager resourceManager, List<PreparableReloadListener> listeners, StateFactory<S> stateFactory, CompletableFuture<?> initialTask) {
        Executor countingTaskExecutor = r -> {
            this.startedTasks.incrementAndGet();
            taskExecutor.execute(() -> {
                r.run();
                this.finishedTasks.incrementAndGet();
            });
        };
        Executor countingReloadExecutor = r -> {
            this.startedReloads.incrementAndGet();
            mainThreadExecutor.execute(() -> {
                r.run();
                this.finishedReloads.incrementAndGet();
            });
        };
        this.startedTasks.incrementAndGet();
        initialTask.thenRun(this.finishedTasks::incrementAndGet);
        PreparableReloadListener.SharedState sharedState = new PreparableReloadListener.SharedState(resourceManager);
        listeners.forEach(listener -> listener.prepareSharedState(sharedState));
        CompletableFuture<Object> barrier = initialTask;
        ArrayList<CompletableFuture<S>> allSteps = new ArrayList<CompletableFuture<S>>();
        for (PreparableReloadListener listener2 : listeners) {
            PreparableReloadListener.PreparationBarrier barrierForCurrentTask = this.createBarrierForListener(listener2, barrier, mainThreadExecutor);
            CompletableFuture<S> state = stateFactory.create(sharedState, barrierForCurrentTask, listener2, countingTaskExecutor, countingReloadExecutor);
            allSteps.add(state);
            barrier = state;
        }
        return Util.sequenceFailFast(allSteps);
    }

    private PreparableReloadListener.PreparationBarrier createBarrierForListener(final PreparableReloadListener listener, final CompletableFuture<?> previousBarrier, final Executor mainThreadExecutor) {
        return new PreparableReloadListener.PreparationBarrier(){
            final /* synthetic */ SimpleReloadInstance this$0;
            {
                SimpleReloadInstance simpleReloadInstance = this$0;
                Objects.requireNonNull(simpleReloadInstance);
                this.this$0 = simpleReloadInstance;
            }

            @Override
            public <T> CompletableFuture<T> wait(T t) {
                mainThreadExecutor.execute(() -> {
                    this.this$0.preparingListeners.remove(listener);
                    if (this.this$0.preparingListeners.isEmpty()) {
                        this.this$0.allPreparations.complete(Unit.INSTANCE);
                    }
                });
                return this.this$0.allPreparations.thenCombine((CompletionStage)previousBarrier, (v1, v2) -> t);
            }
        };
    }

    @Override
    public CompletableFuture<?> done() {
        return Objects.requireNonNull(this.allDone, "not started");
    }

    @Override
    public float getActualProgress() {
        int preparationsDone = this.listenerCount - this.preparingListeners.size();
        float doneCount = SimpleReloadInstance.weightProgress(this.finishedTasks.get(), this.finishedReloads.get(), preparationsDone);
        float totalCount = SimpleReloadInstance.weightProgress(this.startedTasks.get(), this.startedReloads.get(), this.listenerCount);
        return doneCount / totalCount;
    }

    private static int weightProgress(int preparationTasks, int reloadTasks, int listeners) {
        return preparationTasks * 2 + reloadTasks * 2 + listeners * 1;
    }

    public static ReloadInstance create(ResourceManager resourceManager, List<PreparableReloadListener> listeners, Executor backgroundExecutor, Executor mainThreadExecutor, CompletableFuture<Unit> initialTask, boolean enableProfiling) {
        if (enableProfiling) {
            return ProfiledReloadInstance.of(resourceManager, listeners, backgroundExecutor, mainThreadExecutor, initialTask);
        }
        return SimpleReloadInstance.of(resourceManager, listeners, backgroundExecutor, mainThreadExecutor, initialTask);
    }

    @FunctionalInterface
    protected static interface StateFactory<S> {
        public static final StateFactory<Void> SIMPLE = (currentReload, previousStep, listener, taskExecutor, reloadExecutor) -> listener.reload(currentReload, taskExecutor, previousStep, reloadExecutor);

        public CompletableFuture<S> create(PreparableReloadListener.SharedState var1, PreparableReloadListener.PreparationBarrier var2, PreparableReloadListener var3, Executor var4, Executor var5);
    }
}

