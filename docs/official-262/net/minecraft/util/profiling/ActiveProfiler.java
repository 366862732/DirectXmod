/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  it.unimi.dsi.fastutil.longs.LongList
 *  it.unimi.dsi.fastutil.objects.Object2LongMap
 *  it.unimi.dsi.fastutil.objects.Object2LongMaps
 *  it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectArraySet
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.FilledProfileResults;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerPathEntry;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ActiveProfiler
implements ProfileCollector {
    private static final long WARNING_TIME_NANOS = Duration.ofMillis(100L).toNanos();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<String> paths = Lists.newArrayList();
    private final LongList startTimes = new LongArrayList();
    private final Map<String, PathEntry> entries = Maps.newHashMap();
    private final IntSupplier getTickTime;
    private final LongSupplier getRealTime;
    private final long startTimeNano;
    private final int startTimeTicks;
    private String path = "";
    private boolean started;
    private @Nullable PathEntry currentEntry;
    private final BooleanSupplier suppressWarnings;
    private final Set<Pair<String, MetricCategory>> chartedPaths = new ObjectArraySet();

    public ActiveProfiler(LongSupplier getRealTime, IntSupplier getTickTime, BooleanSupplier suppressWarnings) {
        this.startTimeNano = getRealTime.getAsLong();
        this.getRealTime = getRealTime;
        this.startTimeTicks = getTickTime.getAsInt();
        this.getTickTime = getTickTime;
        this.suppressWarnings = suppressWarnings;
    }

    @Override
    public void startTick() {
        if (this.started) {
            LOGGER.error("Profiler tick already started - missing endTick()?");
            return;
        }
        this.started = true;
        this.path = "";
        this.paths.clear();
        this.push("root");
    }

    @Override
    public void endTick() {
        if (!this.started) {
            LOGGER.error("Profiler tick already ended - missing startTick()?");
            return;
        }
        this.pop();
        this.started = false;
        if (!this.path.isEmpty()) {
            LOGGER.error("Profiler tick ended before path was fully popped (remainder: '{}'). Mismatched push/pop?", LogUtils.defer(() -> ProfileResults.demanglePath(this.path)));
        }
    }

    @Override
    public void push(String name) {
        if (!this.started) {
            LOGGER.error("Cannot push '{}' to profiler if profiler tick hasn't started - missing startTick()?", (Object)name);
            return;
        }
        if (!this.path.isEmpty()) {
            this.path = this.path + "\u001e";
        }
        this.path = this.path + name;
        this.paths.add(this.path);
        this.startTimes.add(Util.getNanos());
        this.currentEntry = null;
    }

    @Override
    public void push(Supplier<String> name) {
        this.push(name.get());
    }

    @Override
    public void markForCharting(MetricCategory category) {
        this.chartedPaths.add((Pair<String, MetricCategory>)Pair.of((Object)this.path, (Object)((Object)category)));
    }

    @Override
    public void pop() {
        if (!this.started) {
            LOGGER.error("Cannot pop from profiler if profiler tick hasn't started - missing startTick()?");
            return;
        }
        if (this.startTimes.isEmpty()) {
            LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
            return;
        }
        long endTime = Util.getNanos();
        long startTime = this.startTimes.removeLong(this.startTimes.size() - 1);
        this.paths.removeLast();
        long time = endTime - startTime;
        PathEntry currentEntry = this.getCurrentEntry();
        currentEntry.accumulatedDuration += time;
        ++currentEntry.count;
        currentEntry.maxDuration = Math.max(currentEntry.maxDuration, time);
        currentEntry.minDuration = Math.min(currentEntry.minDuration, time);
        if (time > WARNING_TIME_NANOS && !this.suppressWarnings.getAsBoolean()) {
            LOGGER.warn("Something's taking too long! '{}' took aprox {} ms", LogUtils.defer(() -> ProfileResults.demanglePath(this.path)), LogUtils.defer(() -> (double)time / 1000000.0));
        }
        this.path = this.paths.isEmpty() ? "" : this.paths.getLast();
        this.currentEntry = null;
    }

    @Override
    public void popPush(String name) {
        this.pop();
        this.push(name);
    }

    @Override
    public void popPush(Supplier<String> name) {
        this.pop();
        this.push(name);
    }

    private PathEntry getCurrentEntry() {
        if (this.currentEntry == null) {
            this.currentEntry = this.entries.computeIfAbsent(this.path, key -> new PathEntry());
        }
        return this.currentEntry;
    }

    @Override
    public void incrementCounter(String name, int amount) {
        this.getCurrentEntry().counters.addTo((Object)name, (long)amount);
    }

    @Override
    public void incrementCounter(Supplier<String> name, int amount) {
        this.getCurrentEntry().counters.addTo((Object)name.get(), (long)amount);
    }

    @Override
    public ProfileResults getResults() {
        return new FilledProfileResults(this.entries, this.startTimeNano, this.startTimeTicks, this.getRealTime.getAsLong(), this.getTickTime.getAsInt());
    }

    @Override
    public @Nullable PathEntry getEntry(String path) {
        return this.entries.get(path);
    }

    @Override
    public Set<Pair<String, MetricCategory>> getChartedPaths() {
        return this.chartedPaths;
    }

    public static class PathEntry
    implements ProfilerPathEntry {
        private long maxDuration = Long.MIN_VALUE;
        private long minDuration = Long.MAX_VALUE;
        private long accumulatedDuration;
        private long count;
        private final Object2LongOpenHashMap<String> counters = new Object2LongOpenHashMap();

        @Override
        public long getDuration() {
            return this.accumulatedDuration;
        }

        @Override
        public long getMaxDuration() {
            return this.maxDuration;
        }

        @Override
        public long getCount() {
            return this.count;
        }

        @Override
        public Object2LongMap<String> getCounters() {
            return Object2LongMaps.unmodifiable(this.counters);
        }
    }
}

