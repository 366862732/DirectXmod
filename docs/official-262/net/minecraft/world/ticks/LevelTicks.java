/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2LongMap
 *  it.unimi.dsi.fastutil.longs.Long2LongMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2LongMaps
 *  it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
 */
package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickAccess;

public class LevelTicks<T>
implements LevelTickAccess<T> {
    private static final Comparator<LevelChunkTicks<?>> CONTAINER_DRAIN_ORDER = (o1, o2) -> ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare(o1.peek(), o2.peek());
    private final LongPredicate tickCheck;
    private final Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectOpenHashMap();
    private final Long2LongMap nextTickForContainer = (Long2LongMap)Util.make(new Long2LongOpenHashMap(), m -> m.defaultReturnValue(Long.MAX_VALUE));
    private final Queue<LevelChunkTicks<T>> containersToTick = new PriorityQueue(CONTAINER_DRAIN_ORDER);
    private final Queue<ScheduledTick<T>> toRunThisTick = new ArrayDeque<ScheduledTick<T>>();
    private final List<ScheduledTick<T>> alreadyRunThisTick = new ArrayList<ScheduledTick<T>>();
    private final Set<ScheduledTick<?>> toRunThisTickSet = new ObjectOpenCustomHashSet(ScheduledTick.UNIQUE_TICK_HASH);
    private final BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> chunkScheduleUpdater = (container, newTick) -> {
        if (newTick.equals(container.peek())) {
            this.updateContainerScheduling((ScheduledTick<T>)newTick);
        }
    };

    public LevelTicks(LongPredicate tickCheck) {
        this.tickCheck = tickCheck;
    }

    public void addContainer(ChunkPos pos, LevelChunkTicks<T> container) {
        long posKey = pos.pack();
        this.allContainers.put(posKey, container);
        ScheduledTick<T> nextTick = container.peek();
        if (nextTick != null) {
            this.nextTickForContainer.put(posKey, nextTick.triggerTick());
        }
        container.setOnTickAdded(this.chunkScheduleUpdater);
    }

    public void removeContainer(ChunkPos pos) {
        long chunkKey = pos.pack();
        LevelChunkTicks removedContainer = (LevelChunkTicks)this.allContainers.remove(chunkKey);
        this.nextTickForContainer.remove(chunkKey);
        if (removedContainer != null) {
            removedContainer.setOnTickAdded(null);
        }
    }

    @Override
    public void schedule(ScheduledTick<T> tick) {
        long chunkKey = ChunkPos.pack(tick.pos());
        LevelChunkTicks tickContainer = (LevelChunkTicks)this.allContainers.get(chunkKey);
        if (tickContainer == null) {
            Util.logAndPauseIfInIde("Trying to schedule tick in not loaded position " + String.valueOf(tick.pos()));
            return;
        }
        tickContainer.schedule(tick);
    }

    public void tick(long currentTick, int maxTicksToProcess, BiConsumer<BlockPos, T> output) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("collect");
        this.collectTicks(currentTick, maxTicksToProcess, profiler);
        profiler.popPush("run");
        profiler.incrementCounter("ticksToRun", this.toRunThisTick.size());
        this.runCollectedTicks(output);
        profiler.popPush("cleanup");
        this.cleanupAfterTick();
        profiler.pop();
    }

    private void collectTicks(long currentTick, int maxTicksToProcess, ProfilerFiller profiler) {
        this.sortContainersToTick(currentTick);
        profiler.incrementCounter("containersToTick", this.containersToTick.size());
        this.drainContainers(currentTick, maxTicksToProcess);
        this.rescheduleLeftoverContainers();
    }

    private void sortContainersToTick(long currentTick) {
        ObjectIterator it = Long2LongMaps.fastIterator((Long2LongMap)this.nextTickForContainer);
        while (it.hasNext()) {
            Long2LongMap.Entry entry = (Long2LongMap.Entry)it.next();
            long chunkPos = entry.getLongKey();
            long nextTick = entry.getLongValue();
            if (nextTick > currentTick) continue;
            LevelChunkTicks candidateContainer = (LevelChunkTicks)this.allContainers.get(chunkPos);
            if (candidateContainer == null) {
                it.remove();
                continue;
            }
            ScheduledTick scheduledTick = candidateContainer.peek();
            if (scheduledTick == null) {
                it.remove();
                continue;
            }
            if (scheduledTick.triggerTick() > currentTick) {
                entry.setValue(scheduledTick.triggerTick());
                continue;
            }
            if (!this.tickCheck.test(chunkPos)) continue;
            it.remove();
            this.containersToTick.add(candidateContainer);
        }
    }

    private void drainContainers(long currentTick, int maxTicksToProcess) {
        LevelChunkTicks<T> topContainer;
        while (this.canScheduleMoreTicks(maxTicksToProcess) && (topContainer = this.containersToTick.poll()) != null) {
            ScheduledTick<T> tick = topContainer.poll();
            this.scheduleForThisTick(tick);
            this.drainFromCurrentContainer(this.containersToTick, topContainer, currentTick, maxTicksToProcess);
            ScheduledTick<T> nextTick = topContainer.peek();
            if (nextTick == null) continue;
            if (nextTick.triggerTick() <= currentTick && this.canScheduleMoreTicks(maxTicksToProcess)) {
                this.containersToTick.add(topContainer);
                continue;
            }
            this.updateContainerScheduling(nextTick);
        }
    }

    private void rescheduleLeftoverContainers() {
        for (LevelChunkTicks levelChunkTicks : this.containersToTick) {
            this.updateContainerScheduling(levelChunkTicks.peek());
        }
    }

    private void updateContainerScheduling(ScheduledTick<T> nextTick) {
        this.nextTickForContainer.put(ChunkPos.pack(nextTick.pos()), nextTick.triggerTick());
    }

    private void drainFromCurrentContainer(Queue<LevelChunkTicks<T>> containersToTick, LevelChunkTicks<T> currentContainer, long currentTick, int maxTicksToProcess) {
        ScheduledTick<T> nextFromCurrentContainer;
        ScheduledTick<T> nextFromNextContainer;
        if (!this.canScheduleMoreTicks(maxTicksToProcess)) {
            return;
        }
        LevelChunkTicks<T> nextBestContainer = containersToTick.peek();
        ScheduledTick<T> scheduledTick = nextFromNextContainer = nextBestContainer != null ? nextBestContainer.peek() : null;
        while (this.canScheduleMoreTicks(maxTicksToProcess) && (nextFromCurrentContainer = currentContainer.peek()) != null && nextFromCurrentContainer.triggerTick() <= currentTick && (nextFromNextContainer == null || ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare(nextFromCurrentContainer, nextFromNextContainer) <= 0)) {
            currentContainer.poll();
            this.scheduleForThisTick(nextFromCurrentContainer);
        }
    }

    private void scheduleForThisTick(ScheduledTick<T> tick) {
        this.toRunThisTick.add(tick);
    }

    private boolean canScheduleMoreTicks(int maxTicksToProcess) {
        return this.toRunThisTick.size() < maxTicksToProcess;
    }

    private void runCollectedTicks(BiConsumer<BlockPos, T> output) {
        while (!this.toRunThisTick.isEmpty()) {
            ScheduledTick<T> entry = this.toRunThisTick.poll();
            if (!this.toRunThisTickSet.isEmpty()) {
                this.toRunThisTickSet.remove(entry);
            }
            this.alreadyRunThisTick.add(entry);
            output.accept(entry.pos(), (BlockPos)entry.type());
        }
    }

    private void cleanupAfterTick() {
        this.toRunThisTick.clear();
        this.containersToTick.clear();
        this.alreadyRunThisTick.clear();
        this.toRunThisTickSet.clear();
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T block) {
        LevelChunkTicks tickContainer = (LevelChunkTicks)this.allContainers.get(ChunkPos.pack(pos));
        return tickContainer != null && tickContainer.hasScheduledTick(pos, block);
    }

    @Override
    public boolean willTickThisTick(BlockPos pos, T type) {
        this.calculateTickSetIfNeeded();
        return this.toRunThisTickSet.contains(ScheduledTick.probe(type, pos));
    }

    private void calculateTickSetIfNeeded() {
        if (this.toRunThisTickSet.isEmpty() && !this.toRunThisTick.isEmpty()) {
            this.toRunThisTickSet.addAll(this.toRunThisTick);
        }
    }

    private void forContainersInArea(BoundingBox bb, PosAndContainerConsumer<T> ouput) {
        int xMin = SectionPos.posToSectionCoord(bb.minX());
        int zMin = SectionPos.posToSectionCoord(bb.minZ());
        int xMax = SectionPos.posToSectionCoord(bb.maxX());
        int zMax = SectionPos.posToSectionCoord(bb.maxZ());
        for (int x = xMin; x <= xMax; ++x) {
            for (int z = zMin; z <= zMax; ++z) {
                long containerPos = ChunkPos.pack(x, z);
                LevelChunkTicks container = (LevelChunkTicks)this.allContainers.get(containerPos);
                if (container == null) continue;
                ouput.accept(containerPos, container);
            }
        }
    }

    public void clearArea(BoundingBox area) {
        Predicate<ScheduledTick> tickInsideBB = t -> area.isInside(t.pos());
        this.forContainersInArea(area, (pos, container) -> {
            ScheduledTick previousTop = container.peek();
            container.removeIf(tickInsideBB);
            ScheduledTick newTop = container.peek();
            if (newTop != previousTop) {
                if (newTop != null) {
                    this.updateContainerScheduling(newTop);
                } else {
                    this.nextTickForContainer.remove(pos);
                }
            }
        });
        this.alreadyRunThisTick.removeIf(tickInsideBB);
        this.toRunThisTick.removeIf(tickInsideBB);
    }

    public void copyArea(BoundingBox area, Vec3i offset) {
        this.copyAreaFrom(this, area, offset);
    }

    public void copyAreaFrom(LevelTicks<T> source, BoundingBox area, Vec3i offset) {
        ArrayList ticksToAdd = new ArrayList();
        Predicate<ScheduledTick> tickInsideBB = t -> area.isInside(t.pos());
        source.alreadyRunThisTick.stream().filter(tickInsideBB).forEach(ticksToAdd::add);
        source.toRunThisTick.stream().filter(tickInsideBB).forEach(ticksToAdd::add);
        source.forContainersInArea(area, (pos, container) -> container.getAll().filter(tickInsideBB).forEach(ticksToAdd::add));
        LongSummaryStatistics info = ticksToAdd.stream().mapToLong(ScheduledTick::subTickOrder).summaryStatistics();
        long minSubTick = info.getMin();
        long maxSubTick = info.getMax();
        ticksToAdd.forEach(tick -> this.schedule(new ScheduledTick(tick.type(), tick.pos().offset(offset), tick.triggerTick(), tick.priority(), tick.subTickOrder() - minSubTick + maxSubTick + 1L)));
    }

    @Override
    public int count() {
        return this.allContainers.values().stream().mapToInt(TickAccess::count).sum();
    }

    @FunctionalInterface
    private static interface PosAndContainerConsumer<T> {
        public void accept(long var1, LevelChunkTicks<T> var3);
    }
}

