/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.CrashReport;
import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.GeneratingChunkMap;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.jspecify.annotations.Nullable;

public abstract class GenerationChunkHolder {
    private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
    private static final ChunkResult<ChunkAccess> NOT_DONE_YET = ChunkResult.error("Not done yet");
    public static final ChunkResult<ChunkAccess> UNLOADED_CHUNK = ChunkResult.error("Unloaded chunk");
    public static final CompletableFuture<ChunkResult<ChunkAccess>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_CHUNK);
    protected final ChunkPos pos;
    private volatile @Nullable ChunkStatus highestAllowedStatus;
    private final AtomicReference<@Nullable ChunkStatus> startedWork = new AtomicReference();
    private final AtomicReferenceArray<@Nullable CompletableFuture<ChunkResult<ChunkAccess>>> futures = new AtomicReferenceArray(CHUNK_STATUSES.size());
    private final AtomicReference<@Nullable ChunkGenerationTask> task = new AtomicReference();
    private final AtomicInteger generationRefCount = new AtomicInteger();
    private volatile CompletableFuture<Void> generationSaveSyncFuture = CompletableFuture.completedFuture(null);

    public GenerationChunkHolder(ChunkPos pos) {
        this.pos = pos;
        if (!pos.isValid()) {
            throw new IllegalStateException("Trying to create chunk out of reasonable bounds: " + String.valueOf(pos));
        }
    }

    public CompletableFuture<ChunkResult<ChunkAccess>> scheduleChunkGenerationTask(ChunkStatus status, ChunkMap scheduler) {
        if (this.isStatusDisallowed(status)) {
            return UNLOADED_CHUNK_FUTURE;
        }
        CompletableFuture<ChunkResult<ChunkAccess>> future = this.getOrCreateFuture(status);
        if (future.isDone()) {
            return future;
        }
        ChunkGenerationTask task = this.task.get();
        if (task == null || status.isAfter(task.targetStatus)) {
            this.rescheduleChunkTask(scheduler, status);
        }
        return future;
    }

    CompletableFuture<ChunkResult<ChunkAccess>> applyStep(ChunkStep step, GeneratingChunkMap chunkMap, StaticCache2D<GenerationChunkHolder> cache) {
        if (this.isStatusDisallowed(step.targetStatus())) {
            return UNLOADED_CHUNK_FUTURE;
        }
        if (this.acquireStatusBump(step.targetStatus())) {
            return chunkMap.applyStep(this, step, cache).handle((chunk, exception) -> {
                if (exception != null) {
                    CrashReport report = CrashReport.forThrowable(exception, "Exception chunk generation/loading");
                    BlockableEventLoop.relayDelayCrash(report);
                } else {
                    this.completeFuture(step.targetStatus(), (ChunkAccess)chunk);
                }
                return ChunkResult.of(chunk);
            });
        }
        return this.getOrCreateFuture(step.targetStatus());
    }

    protected void updateHighestAllowedStatus(ChunkMap scheduler) {
        boolean statusDropped;
        ChunkStatus newStatus;
        ChunkStatus oldStatus = this.highestAllowedStatus;
        this.highestAllowedStatus = newStatus = ChunkLevel.generationStatus(this.getTicketLevel());
        boolean bl = statusDropped = oldStatus != null && (newStatus == null || newStatus.isBefore(oldStatus));
        if (statusDropped) {
            this.failAndClearPendingFuturesBetween(newStatus, oldStatus);
            if (this.task.get() != null) {
                this.rescheduleChunkTask(scheduler, this.findHighestStatusWithPendingFuture(newStatus));
            }
        }
    }

    public void replaceProtoChunk(ImposterProtoChunk chunk) {
        CompletableFuture<ChunkResult<ImposterProtoChunk>> imposterFuture = CompletableFuture.completedFuture(ChunkResult.of(chunk));
        for (int i = 0; i < this.futures.length() - 1; ++i) {
            CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(i);
            Objects.requireNonNull(future);
            ChunkAccess maybeProtoChunk = future.getNow(NOT_DONE_YET).orElse(null);
            if (maybeProtoChunk instanceof ProtoChunk) {
                if (this.futures.compareAndSet(i, future, imposterFuture)) continue;
                throw new IllegalStateException("Future changed by other thread while trying to replace it");
            }
            throw new IllegalStateException("Trying to replace a ProtoChunk, but found " + String.valueOf(maybeProtoChunk));
        }
    }

    void removeTask(ChunkGenerationTask task) {
        this.task.compareAndSet(task, null);
    }

    private void rescheduleChunkTask(ChunkMap scheduler, @Nullable ChunkStatus status) {
        ChunkGenerationTask newTask = status != null ? scheduler.scheduleGenerationTask(status, this.getPos()) : null;
        ChunkGenerationTask oldTask = this.task.getAndSet(newTask);
        if (oldTask != null) {
            oldTask.markForCancellation();
        }
    }

    private CompletableFuture<ChunkResult<ChunkAccess>> getOrCreateFuture(ChunkStatus status) {
        if (this.isStatusDisallowed(status)) {
            return UNLOADED_CHUNK_FUTURE;
        }
        int index = status.getIndex();
        CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(index);
        while (future == null) {
            CompletableFuture<ChunkResult<ChunkAccess>> newValue = new CompletableFuture<ChunkResult<ChunkAccess>>();
            future = this.futures.compareAndExchange(index, null, newValue);
            if (future != null) continue;
            if (this.isStatusDisallowed(status)) {
                this.failAndClearPendingFuture(index, newValue);
                return UNLOADED_CHUNK_FUTURE;
            }
            return newValue;
        }
        return future;
    }

    private void failAndClearPendingFuturesBetween(@Nullable ChunkStatus fromExclusive, ChunkStatus toInclusive) {
        int start = fromExclusive == null ? 0 : fromExclusive.getIndex() + 1;
        int end = toInclusive.getIndex();
        for (int i = start; i <= end; ++i) {
            CompletableFuture<ChunkResult<ChunkAccess>> previous = this.futures.get(i);
            if (previous == null) continue;
            this.failAndClearPendingFuture(i, previous);
        }
    }

    private void failAndClearPendingFuture(int index, CompletableFuture<ChunkResult<ChunkAccess>> previous) {
        if (previous.complete(UNLOADED_CHUNK) && !this.futures.compareAndSet(index, previous, null)) {
            throw new IllegalStateException("Nothing else should replace the future here");
        }
    }

    private void completeFuture(ChunkStatus status, ChunkAccess chunk) {
        ChunkResult<ChunkAccess> result = ChunkResult.of(chunk);
        int index = status.getIndex();
        while (true) {
            CompletableFuture<ChunkResult<ChunkAccess>> future;
            if ((future = this.futures.get(index)) == null) {
                if (!this.futures.compareAndSet(index, null, CompletableFuture.completedFuture(result))) continue;
                return;
            }
            if (future.complete(result)) {
                return;
            }
            if (future.getNow(NOT_DONE_YET).isSuccess()) {
                throw new IllegalStateException("Trying to complete a future but found it to be completed successfully already");
            }
            Thread.yield();
        }
    }

    private @Nullable ChunkStatus findHighestStatusWithPendingFuture(@Nullable ChunkStatus newStatus) {
        if (newStatus == null) {
            return null;
        }
        ChunkStatus highestStatus = newStatus;
        ChunkStatus alreadyStarted = this.startedWork.get();
        while (alreadyStarted == null || highestStatus.isAfter(alreadyStarted)) {
            if (this.futures.get(highestStatus.getIndex()) != null) {
                return highestStatus;
            }
            if (highestStatus == ChunkStatus.EMPTY) break;
            highestStatus = highestStatus.getParent();
        }
        return null;
    }

    private boolean acquireStatusBump(ChunkStatus status) {
        ChunkStatus parent = status == ChunkStatus.EMPTY ? null : status.getParent();
        ChunkStatus previousStarted = this.startedWork.compareAndExchange(parent, status);
        if (previousStarted == parent) {
            return true;
        }
        if (previousStarted == null || status.isAfter(previousStarted)) {
            throw new IllegalStateException("Unexpected last startedWork status: " + String.valueOf(previousStarted) + " while trying to start: " + String.valueOf(status));
        }
        return false;
    }

    private boolean isStatusDisallowed(ChunkStatus status) {
        ChunkStatus highestAllowedStatus = this.highestAllowedStatus;
        return highestAllowedStatus == null || status.isAfter(highestAllowedStatus);
    }

    protected abstract void addSaveDependency(CompletableFuture<?> var1);

    public void increaseGenerationRefCount() {
        if (this.generationRefCount.getAndIncrement() == 0) {
            this.generationSaveSyncFuture = new CompletableFuture();
            this.addSaveDependency(this.generationSaveSyncFuture);
        }
    }

    public void decreaseGenerationRefCount() {
        CompletableFuture<Void> future = this.generationSaveSyncFuture;
        int newValue = this.generationRefCount.decrementAndGet();
        if (newValue == 0) {
            future.complete(null);
        }
        if (newValue < 0) {
            throw new IllegalStateException("More releases than claims. Count: " + newValue);
        }
    }

    public @Nullable ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
        CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(status.getIndex());
        return future == null ? null : (ChunkAccess)future.getNow(NOT_DONE_YET).orElse(null);
    }

    public @Nullable ChunkAccess getChunkIfPresent(ChunkStatus status) {
        if (this.isStatusDisallowed(status)) {
            return null;
        }
        return this.getChunkIfPresentUnchecked(status);
    }

    public @Nullable ChunkAccess getLatestChunk() {
        ChunkStatus status = this.startedWork.get();
        if (status == null) {
            return null;
        }
        ChunkAccess chunk = this.getChunkIfPresentUnchecked(status);
        if (chunk != null) {
            return chunk;
        }
        return this.getChunkIfPresentUnchecked(status.getParent());
    }

    public @Nullable ChunkStatus getPersistedStatus() {
        CompletableFuture<ChunkResult<ChunkAccess>> future = this.futures.get(ChunkStatus.EMPTY.getIndex());
        ChunkAccess chunkAccess = future == null ? null : (ChunkAccess)future.getNow(NOT_DONE_YET).orElse(null);
        return chunkAccess == null ? null : chunkAccess.getPersistedStatus();
    }

    public ChunkPos getPos() {
        return this.pos;
    }

    public FullChunkStatus getFullStatus() {
        return ChunkLevel.fullStatus(this.getTicketLevel());
    }

    public abstract int getTicketLevel();

    public abstract int getQueueLevel();

    @VisibleForDebug
    public List<Pair<ChunkStatus, @Nullable CompletableFuture<ChunkResult<ChunkAccess>>>> getAllFutures() {
        ArrayList<Pair<ChunkStatus, CompletableFuture<ChunkResult<ChunkAccess>>>> result = new ArrayList<Pair<ChunkStatus, CompletableFuture<ChunkResult<ChunkAccess>>>>();
        for (int i = 0; i < CHUNK_STATUSES.size(); ++i) {
            result.add((Pair<ChunkStatus, CompletableFuture<ChunkResult<ChunkAccess>>>)Pair.of((Object)CHUNK_STATUSES.get(i), this.futures.get(i)));
        }
        return result;
    }

    @VisibleForDebug
    public @Nullable ChunkStatus getLatestStatus() {
        ChunkStatus status = this.startedWork.get();
        if (status == null) {
            return null;
        }
        ChunkAccess chunk = this.getChunkIfPresentUnchecked(status);
        if (chunk != null) {
            return status;
        }
        return status.getParent();
    }
}

