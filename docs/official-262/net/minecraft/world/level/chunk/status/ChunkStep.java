/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk.status;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStatusTask;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.jspecify.annotations.Nullable;

public record ChunkStep(ChunkStatus targetStatus, ChunkDependencies directDependencies, ChunkDependencies accumulatedDependencies, int blockStateWriteRadius, ChunkStatusTask task) {
    public int getAccumulatedRadiusOf(ChunkStatus status) {
        if (status == this.targetStatus) {
            return 0;
        }
        return this.accumulatedDependencies.getRadiusOf(status);
    }

    public CompletableFuture<ChunkAccess> apply(WorldGenContext context, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunk) {
        if (chunk.getPersistedStatus().isBefore(this.targetStatus)) {
            ProfiledDuration profiledDuration = JvmProfiler.INSTANCE.onChunkGenerate(chunk.getPos(), context.level().dimension(), this.targetStatus.getName());
            return this.task.doWork(context, this, cache, chunk).thenApply(newCenterChunk -> this.completeChunkGeneration((ChunkAccess)newCenterChunk, profiledDuration));
        }
        return this.task.doWork(context, this, cache, chunk);
    }

    private ChunkAccess completeChunkGeneration(ChunkAccess newCenterChunk, @Nullable ProfiledDuration profiledDuration) {
        ProtoChunk protochunk;
        if (newCenterChunk instanceof ProtoChunk && (protochunk = (ProtoChunk)newCenterChunk).getPersistedStatus().isBefore(this.targetStatus)) {
            protochunk.setPersistedStatus(this.targetStatus);
        }
        if (profiledDuration != null) {
            profiledDuration.finish(true);
        }
        return newCenterChunk;
    }

    public static class Builder {
        private final ChunkStatus status;
        private final @Nullable ChunkStep parent;
        private ChunkStatus[] directDependenciesByRadius;
        private int blockStateWriteRadius = -1;
        private ChunkStatusTask task = ChunkStatusTasks::passThrough;

        protected Builder(ChunkStatus status) {
            if (status.getParent() != status) {
                throw new IllegalArgumentException("Not starting with the first status: " + String.valueOf(status));
            }
            this.status = status;
            this.parent = null;
            this.directDependenciesByRadius = new ChunkStatus[0];
        }

        protected Builder(ChunkStatus status, ChunkStep parent) {
            if (parent.targetStatus.getIndex() != status.getIndex() - 1) {
                throw new IllegalArgumentException("Out of order status: " + String.valueOf(status));
            }
            this.status = status;
            this.parent = parent;
            this.directDependenciesByRadius = new ChunkStatus[]{parent.targetStatus};
        }

        public Builder addRequirement(ChunkStatus status, int radius) {
            if (status.isOrAfter(this.status)) {
                throw new IllegalArgumentException("Status " + String.valueOf(status) + " can not be required by " + String.valueOf(this.status));
            }
            int newLength = radius + 1;
            ChunkStatus[] previous = this.directDependenciesByRadius;
            if (newLength > previous.length) {
                this.directDependenciesByRadius = new ChunkStatus[newLength];
                Arrays.fill(this.directDependenciesByRadius, status);
            }
            for (int i = 0; i < Math.min(newLength, previous.length); ++i) {
                this.directDependenciesByRadius[i] = ChunkStatus.max(previous[i], status);
            }
            return this;
        }

        public Builder blockStateWriteRadius(int radius) {
            this.blockStateWriteRadius = radius;
            return this;
        }

        public Builder setTask(ChunkStatusTask task) {
            this.task = task;
            return this;
        }

        public ChunkStep build() {
            return new ChunkStep(this.status, new ChunkDependencies((ImmutableList<ChunkStatus>)ImmutableList.copyOf((Object[])this.directDependenciesByRadius)), new ChunkDependencies((ImmutableList<ChunkStatus>)ImmutableList.copyOf((Object[])this.buildAccumulatedDependencies())), this.blockStateWriteRadius, this.task);
        }

        private ChunkStatus[] buildAccumulatedDependencies() {
            if (this.parent == null) {
                return this.directDependenciesByRadius;
            }
            int radiusOfParent = this.getRadiusOfParent(this.parent.targetStatus);
            ChunkDependencies parentDependencies = this.parent.accumulatedDependencies;
            ChunkStatus[] accumulatedDependencies = new ChunkStatus[Math.max(radiusOfParent + parentDependencies.size(), this.directDependenciesByRadius.length)];
            for (int distance = 0; distance < accumulatedDependencies.length; ++distance) {
                int distanceInParent = distance - radiusOfParent;
                accumulatedDependencies[distance] = distanceInParent < 0 || distanceInParent >= parentDependencies.size() ? this.directDependenciesByRadius[distance] : (distance >= this.directDependenciesByRadius.length ? parentDependencies.get(distanceInParent) : ChunkStatus.max(this.directDependenciesByRadius[distance], parentDependencies.get(distanceInParent)));
            }
            return accumulatedDependencies;
        }

        private int getRadiusOfParent(ChunkStatus status) {
            for (int i = this.directDependenciesByRadius.length - 1; i >= 0; --i) {
                if (!this.directDependenciesByRadius[i].isOrAfter(status)) continue;
                return i;
            }
            return 0;
        }
    }
}

