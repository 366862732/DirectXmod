/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectList
 *  it.unimi.dsi.fastutil.objects.ObjectListIterator
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTaskDispatcher;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ThreadedLevelLightEngine
extends LevelLightEngine
implements AutoCloseable {
    public static final int DEFAULT_BATCH_SIZE = 1000;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ConsecutiveExecutor consecutiveExecutor;
    private final ObjectList<Pair<TaskType, Runnable>> lightTasks = new ObjectArrayList();
    private final ChunkMap chunkMap;
    private final ChunkTaskDispatcher taskDispatcher;
    private final int taskPerBatch = 1000;
    private final AtomicBoolean scheduled = new AtomicBoolean();

    public ThreadedLevelLightEngine(LightChunkGetter lightChunkGetter, ChunkMap chunkMap, boolean hasSkyLight, ConsecutiveExecutor consecutiveExecutor, ChunkTaskDispatcher taskDispatcher) {
        super(lightChunkGetter, true, hasSkyLight);
        this.chunkMap = chunkMap;
        this.taskDispatcher = taskDispatcher;
        this.consecutiveExecutor = consecutiveExecutor;
    }

    @Override
    public void close() {
    }

    @Override
    public int runLightUpdates() {
        throw Util.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
    }

    @Override
    public void checkBlock(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        this.addTask(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), TaskType.PRE_UPDATE, Util.name(() -> super.checkBlock(immutable), () -> "checkBlock " + String.valueOf(immutable)));
    }

    protected void updateChunkStatus(ChunkPos pos) {
        this.addTask(pos.x(), pos.z(), () -> 0, TaskType.PRE_UPDATE, Util.name(() -> {
            int sectionY;
            super.retainData(pos, false);
            super.setLightEnabled(pos, false);
            for (sectionY = this.getMinLightSection(); sectionY < this.getMaxLightSection(); ++sectionY) {
                super.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, sectionY), null);
                super.queueSectionData(LightLayer.SKY, SectionPos.of(pos, sectionY), null);
            }
            for (sectionY = this.levelHeightAccessor.getMinSectionY(); sectionY <= this.levelHeightAccessor.getMaxSectionY(); ++sectionY) {
                super.updateSectionStatus(SectionPos.of(pos, sectionY), true);
            }
        }, () -> "updateChunkStatus " + String.valueOf(pos) + " true"));
    }

    @Override
    public void updateSectionStatus(SectionPos pos, boolean sectionEmpty) {
        this.addTask(pos.x(), pos.z(), () -> 0, TaskType.PRE_UPDATE, Util.name(() -> super.updateSectionStatus(pos, sectionEmpty), () -> "updateSectionStatus " + String.valueOf(pos) + " " + sectionEmpty));
    }

    @Override
    public void propagateLightSources(ChunkPos pos) {
        this.addTask(pos.x(), pos.z(), TaskType.PRE_UPDATE, Util.name(() -> super.propagateLightSources(pos), () -> "propagateLight " + String.valueOf(pos)));
    }

    @Override
    public void setLightEnabled(ChunkPos pos, boolean enable) {
        this.addTask(pos.x(), pos.z(), TaskType.PRE_UPDATE, Util.name(() -> super.setLightEnabled(pos, enable), () -> "enableLight " + String.valueOf(pos) + " " + enable));
    }

    @Override
    public void queueSectionData(LightLayer layer, SectionPos pos, @Nullable DataLayer data) {
        this.addTask(pos.x(), pos.z(), () -> 0, TaskType.PRE_UPDATE, Util.name(() -> super.queueSectionData(layer, pos, data), () -> "queueData " + String.valueOf(pos)));
    }

    private void addTask(int chunkX, int chunkZ, TaskType type, Runnable runnable) {
        this.addTask(chunkX, chunkZ, this.chunkMap.getChunkQueueLevel(ChunkPos.pack(chunkX, chunkZ)), type, runnable);
    }

    private void addTask(int chunkX, int chunkZ, IntSupplier level, TaskType type, Runnable runnable) {
        this.taskDispatcher.submit(() -> {
            this.lightTasks.add((Object)Pair.of((Object)((Object)type), (Object)runnable));
            if (this.lightTasks.size() >= 1000) {
                this.runUpdate();
            }
        }, ChunkPos.pack(chunkX, chunkZ), level);
    }

    @Override
    public void retainData(ChunkPos pos, boolean retain) {
        this.addTask(pos.x(), pos.z(), () -> 0, TaskType.PRE_UPDATE, Util.name(() -> super.retainData(pos, retain), () -> "retainData " + String.valueOf(pos)));
    }

    public CompletableFuture<ChunkAccess> initializeLight(ChunkAccess chunk, boolean lighted) {
        ChunkPos pos = chunk.getPos();
        this.addTask(pos.x(), pos.z(), TaskType.PRE_UPDATE, Util.name(() -> {
            LevelChunkSection[] sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); ++sectionIndex) {
                LevelChunkSection section = sections[sectionIndex];
                if (section.hasOnlyAir()) continue;
                int sectionY = this.levelHeightAccessor.getSectionYFromSectionIndex(sectionIndex);
                super.updateSectionStatus(SectionPos.of(pos, sectionY), false);
            }
        }, () -> "initializeLight: " + String.valueOf(pos)));
        return CompletableFuture.supplyAsync(() -> {
            super.setLightEnabled(pos, lighted);
            super.retainData(pos, false);
            return chunk;
        }, r -> this.addTask(pos.x(), pos.z(), TaskType.POST_UPDATE, r));
    }

    public CompletableFuture<ChunkAccess> lightChunk(ChunkAccess centerChunk, boolean lighted) {
        ChunkPos pos = centerChunk.getPos();
        centerChunk.setLightCorrect(false);
        this.addTask(pos.x(), pos.z(), TaskType.PRE_UPDATE, Util.name(() -> {
            if (!lighted) {
                super.propagateLightSources(pos);
            }
            if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                LOGGER.debug("LIT {}", (Object)pos);
            }
        }, () -> "lightChunk " + String.valueOf(pos) + " " + lighted));
        return CompletableFuture.supplyAsync(() -> {
            centerChunk.setLightCorrect(true);
            return centerChunk;
        }, r -> this.addTask(pos.x(), pos.z(), TaskType.POST_UPDATE, r));
    }

    public void tryScheduleUpdate() {
        if ((!this.lightTasks.isEmpty() || super.hasLightWork()) && this.scheduled.compareAndSet(false, true)) {
            this.consecutiveExecutor.schedule(() -> {
                this.runUpdate();
                this.scheduled.set(false);
            });
        }
    }

    private void runUpdate() {
        Pair task;
        int count;
        int totalSize = Math.min(this.lightTasks.size(), 1000);
        ObjectListIterator iterator = this.lightTasks.iterator();
        for (count = 0; iterator.hasNext() && count < totalSize; ++count) {
            task = (Pair)iterator.next();
            if (task.getFirst() != TaskType.PRE_UPDATE) continue;
            ((Runnable)task.getSecond()).run();
        }
        iterator.back(count);
        super.runLightUpdates();
        for (count = 0; iterator.hasNext() && count < totalSize; ++count) {
            task = (Pair)iterator.next();
            if (task.getFirst() == TaskType.POST_UPDATE) {
                ((Runnable)task.getSecond()).run();
            }
            iterator.remove();
        }
    }

    public CompletableFuture<?> waitForPendingTasks(int chunkX, int chunkZ) {
        return CompletableFuture.runAsync(() -> {}, r -> this.addTask(chunkX, chunkZ, TaskType.POST_UPDATE, r));
    }

    private static enum TaskType {
        PRE_UPDATE,
        POST_UPDATE;

    }
}

