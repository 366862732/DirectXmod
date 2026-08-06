/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.GeneratingChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

public class ChunkGenerationTask {
    private final GeneratingChunkMap chunkMap;
    private final ChunkPos pos;
    private @Nullable ChunkStatus scheduledStatus = null;
    public final ChunkStatus targetStatus;
    private volatile boolean markedForCancellation;
    private final List<CompletableFuture<ChunkResult<ChunkAccess>>> scheduledLayer = new ArrayList<CompletableFuture<ChunkResult<ChunkAccess>>>();
    private final StaticCache2D<GenerationChunkHolder> cache;
    private boolean needsGeneration;

    private ChunkGenerationTask(GeneratingChunkMap chunkMap, ChunkStatus targetStatus, ChunkPos pos, StaticCache2D<GenerationChunkHolder> cache) {
        this.chunkMap = chunkMap;
        this.targetStatus = targetStatus;
        this.pos = pos;
        this.cache = cache;
    }

    public static ChunkGenerationTask create(GeneratingChunkMap chunkMap, ChunkStatus targetStatus, ChunkPos pos) {
        int worstCaseRadius = ChunkPyramid.GENERATION_PYRAMID.getStepTo(targetStatus).getAccumulatedRadiusOf(ChunkStatus.EMPTY);
        StaticCache2D<GenerationChunkHolder> cache = StaticCache2D.create(pos.x(), pos.z(), worstCaseRadius, (x, z) -> chunkMap.acquireGeneration(ChunkPos.pack(x, z)));
        return new ChunkGenerationTask(chunkMap, targetStatus, pos, cache);
    }

    public @Nullable CompletableFuture<?> runUntilWait() {
        CompletableFuture<?> waitingFor;
        while ((waitingFor = this.waitForScheduledLayer()) == null) {
            if (this.markedForCancellation || this.scheduledStatus == this.targetStatus) {
                this.releaseClaim();
                return null;
            }
            this.scheduleNextLayer();
        }
        return waitingFor;
    }

    private void scheduleNextLayer() {
        ChunkStatus statusToSchedule;
        if (this.scheduledStatus == null) {
            statusToSchedule = ChunkStatus.EMPTY;
        } else if (!this.needsGeneration && this.scheduledStatus == ChunkStatus.EMPTY && !this.canLoadWithoutGeneration()) {
            this.needsGeneration = true;
            statusToSchedule = ChunkStatus.EMPTY;
        } else {
            statusToSchedule = ChunkStatus.getStatusList().get(this.scheduledStatus.getIndex() + 1);
        }
        this.scheduleLayer(statusToSchedule, this.needsGeneration);
        this.scheduledStatus = statusToSchedule;
    }

    public void markForCancellation() {
        this.markedForCancellation = true;
    }

    private void releaseClaim() {
        GenerationChunkHolder chunkHolder = this.cache.get(this.pos.x(), this.pos.z());
        chunkHolder.removeTask(this);
        this.cache.forEach(this.chunkMap::releaseGeneration);
    }

    private boolean canLoadWithoutGeneration() {
        if (this.targetStatus == ChunkStatus.EMPTY) {
            return true;
        }
        ChunkStatus highestGeneratedStatus = this.cache.get(this.pos.x(), this.pos.z()).getPersistedStatus();
        if (highestGeneratedStatus == null || highestGeneratedStatus.isBefore(this.targetStatus)) {
            return false;
        }
        ChunkDependencies dependencies = ChunkPyramid.LOADING_PYRAMID.getStepTo(this.targetStatus).accumulatedDependencies();
        int range = dependencies.getRadius();
        for (int x = this.pos.x() - range; x <= this.pos.x() + range; ++x) {
            for (int z = this.pos.z() - range; z <= this.pos.z() + range; ++z) {
                int distance = this.pos.getChessboardDistance(x, z);
                ChunkStatus requiredStatus = dependencies.get(distance);
                ChunkStatus persistedStatus = this.cache.get(x, z).getPersistedStatus();
                if (persistedStatus != null && !persistedStatus.isBefore(requiredStatus)) continue;
                return false;
            }
        }
        return true;
    }

    public GenerationChunkHolder getCenter() {
        return this.cache.get(this.pos.x(), this.pos.z());
    }

    private void scheduleLayer(ChunkStatus status, boolean needsGeneration) {
        try (Zone zone = Profiler.get().zone("scheduleLayer");){
            zone.addText(status::getName);
            int radius = this.getRadiusForLayer(status, needsGeneration);
            for (int x = this.pos.x() - radius; x <= this.pos.x() + radius; ++x) {
                for (int z = this.pos.z() - radius; z <= this.pos.z() + radius; ++z) {
                    GenerationChunkHolder chunkHolder = this.cache.get(x, z);
                    if (!this.markedForCancellation && this.scheduleChunkInLayer(status, needsGeneration, chunkHolder)) continue;
                    return;
                }
            }
        }
    }

    private int getRadiusForLayer(ChunkStatus status, boolean needsGeneration) {
        ChunkPyramid pyramid = needsGeneration ? ChunkPyramid.GENERATION_PYRAMID : ChunkPyramid.LOADING_PYRAMID;
        return pyramid.getStepTo(this.targetStatus).getAccumulatedRadiusOf(status);
    }

    private boolean scheduleChunkInLayer(ChunkStatus status, boolean needsGeneration, GenerationChunkHolder chunkHolder) {
        ChunkPyramid pyramid;
        ChunkStatus persistedStatus = chunkHolder.getPersistedStatus();
        boolean generate = persistedStatus != null && status.isAfter(persistedStatus);
        ChunkPyramid chunkPyramid = pyramid = generate ? ChunkPyramid.GENERATION_PYRAMID : ChunkPyramid.LOADING_PYRAMID;
        if (generate && !needsGeneration) {
            throw new IllegalStateException("Can't load chunk, but didn't expect to need to generate");
        }
        CompletableFuture<ChunkResult<ChunkAccess>> future = chunkHolder.applyStep(pyramid.getStepTo(status), this.chunkMap, this.cache);
        ChunkResult now = future.getNow(null);
        if (now == null) {
            this.scheduledLayer.add(future);
            return true;
        }
        if (now.isSuccess()) {
            return true;
        }
        this.markForCancellation();
        return false;
    }

    private @Nullable CompletableFuture<?> waitForScheduledLayer() {
        while (!this.scheduledLayer.isEmpty()) {
            CompletableFuture<ChunkResult<ChunkAccess>> lastFuture = this.scheduledLayer.getLast();
            ChunkResult resultNow = lastFuture.getNow(null);
            if (resultNow == null) {
                return lastFuture;
            }
            this.scheduledLayer.removeLast();
            if (resultNow.isSuccess()) continue;
            this.markForCancellation();
        }
        return null;
    }
}

