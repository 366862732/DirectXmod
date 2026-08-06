/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Splitter
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2LongMap
 *  it.unimi.dsi.fastutil.objects.Object2LongMaps
 *  org.apache.commons.io.IOUtils
 *  org.apache.commons.lang3.ObjectUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import java.io.BufferedWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.ReportType;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerPathEntry;
import net.minecraft.util.profiling.ResultField;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class FilledProfileResults
implements ProfileResults {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ProfilerPathEntry EMPTY = new ProfilerPathEntry(){

        @Override
        public long getDuration() {
            return 0L;
        }

        @Override
        public long getMaxDuration() {
            return 0L;
        }

        @Override
        public long getCount() {
            return 0L;
        }

        @Override
        public Object2LongMap<String> getCounters() {
            return Object2LongMaps.emptyMap();
        }
    };
    private static final Splitter SPLITTER = Splitter.on((char)'\u001e');
    private static final Comparator<Map.Entry<String, CounterCollector>> COUNTER_ENTRY_COMPARATOR = Map.Entry.comparingByValue(Comparator.comparingLong(c -> c.totalValue)).reversed();
    private final Map<String, ? extends ProfilerPathEntry> entries;
    private final long startTimeNano;
    private final int startTimeTicks;
    private final long endTimeNano;
    private final int endTimeTicks;
    private final int tickDuration;

    public FilledProfileResults(Map<String, ? extends ProfilerPathEntry> entries, long startTimeNano, int startTimeTicks, long endTimeNano, int endTimeTicks) {
        this.entries = entries;
        this.startTimeNano = startTimeNano;
        this.startTimeTicks = startTimeTicks;
        this.endTimeNano = endTimeNano;
        this.endTimeTicks = endTimeTicks;
        this.tickDuration = endTimeTicks - startTimeTicks;
    }

    private ProfilerPathEntry getEntry(String path) {
        ProfilerPathEntry result = this.entries.get(path);
        return result != null ? result : EMPTY;
    }

    @Override
    public List<ResultField> getTimes(String path) {
        String rawPath = path;
        ProfilerPathEntry rootEntry = this.getEntry("root");
        long globalTime = rootEntry.getDuration();
        ProfilerPathEntry currentEntry = this.getEntry((String)path);
        long selfTime = currentEntry.getDuration();
        long selfCount = currentEntry.getCount();
        ArrayList result = Lists.newArrayList();
        if (!((String)path).isEmpty()) {
            path = (String)path + "\u001e";
        }
        long totalTime = 0L;
        for (String key : this.entries.keySet()) {
            if (!FilledProfileResults.isDirectChild((String)path, key)) continue;
            totalTime += this.getEntry(key).getDuration();
        }
        float oldTime = totalTime;
        if (totalTime < selfTime) {
            totalTime = selfTime;
        }
        if (globalTime < totalTime) {
            globalTime = totalTime;
        }
        for (String key : this.entries.keySet()) {
            if (!FilledProfileResults.isDirectChild((String)path, key)) continue;
            ProfilerPathEntry entry = this.getEntry(key);
            long time = entry.getDuration();
            double timePercentage = (double)time * 100.0 / (double)totalTime;
            double globalPercentage = (double)time * 100.0 / (double)globalTime;
            String name = key.substring(((String)path).length());
            result.add(new ResultField(name, timePercentage, globalPercentage, entry.getCount()));
        }
        if ((float)totalTime > oldTime) {
            result.add(new ResultField("unspecified", (double)((float)totalTime - oldTime) * 100.0 / (double)totalTime, (double)((float)totalTime - oldTime) * 100.0 / (double)globalTime, selfCount));
        }
        Collections.sort(result);
        result.add(0, new ResultField(rawPath, 100.0, (double)totalTime * 100.0 / (double)globalTime, selfCount));
        return result;
    }

    private static boolean isDirectChild(String path, String test) {
        return test.length() > path.length() && test.startsWith(path) && test.indexOf(30, path.length() + 1) < 0;
    }

    private Map<String, CounterCollector> getCounterValues() {
        TreeMap result = Maps.newTreeMap();
        this.entries.forEach((path, entry) -> {
            Object2LongMap<String> counters = entry.getCounters();
            if (!counters.isEmpty()) {
                List pathSegments = SPLITTER.splitToList((CharSequence)path);
                counters.forEach((counter, value) -> result.computeIfAbsent(counter, k -> new CounterCollector()).addValue(pathSegments.iterator(), value));
            }
        });
        return result;
    }

    @Override
    public long getStartTimeNano() {
        return this.startTimeNano;
    }

    @Override
    public int getStartTimeTicks() {
        return this.startTimeTicks;
    }

    @Override
    public long getEndTimeNano() {
        return this.endTimeNano;
    }

    @Override
    public int getEndTimeTicks() {
        return this.endTimeTicks;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean saveResults(Path file) {
        boolean bl;
        BufferedWriter writer = null;
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, new OpenOption[0]);
            writer.write(this.getProfilerResults(this.getNanoDuration(), this.getTickDuration()));
            bl = true;
        }
        catch (Throwable t) {
            boolean bl2;
            try {
                LOGGER.error("Could not save profiler results to {}", (Object)file, (Object)t);
                bl2 = false;
            }
            catch (Throwable throwable) {
                IOUtils.closeQuietly(writer);
                throw throwable;
            }
            IOUtils.closeQuietly((Writer)writer);
            return bl2;
        }
        IOUtils.closeQuietly((Writer)writer);
        return bl;
    }

    protected String getProfilerResults(long timespan, int tickspan) {
        StringBuilder builder = new StringBuilder();
        ReportType.PROFILE.appendHeader(builder, List.of());
        builder.append("Version: ").append(SharedConstants.getCurrentVersion().id()).append('\n');
        builder.append("Time span: ").append(timespan / 1000000L).append(" ms\n");
        builder.append("Tick span: ").append(tickspan).append(" ticks\n");
        builder.append("// This is approximately ").append(String.format(Locale.ROOT, "%.2f", Float.valueOf((float)tickspan / ((float)timespan / 1.0E9f)))).append(" ticks per second. It should be ").append(20).append(" ticks per second\n\n");
        builder.append("--- BEGIN PROFILE DUMP ---\n\n");
        this.appendProfilerResults(0, "root", builder);
        builder.append("--- END PROFILE DUMP ---\n\n");
        Map<String, CounterCollector> counters = this.getCounterValues();
        if (!counters.isEmpty()) {
            builder.append("--- BEGIN COUNTER DUMP ---\n\n");
            this.appendCounters(counters, builder, tickspan);
            builder.append("--- END COUNTER DUMP ---\n\n");
        }
        return builder.toString();
    }

    @Override
    public String getProfilerResults() {
        StringBuilder builder = new StringBuilder();
        this.appendProfilerResults(0, "root", builder);
        return builder.toString();
    }

    private static StringBuilder indentLine(StringBuilder builder, int depth) {
        builder.append(String.format(Locale.ROOT, "[%02d] ", depth));
        for (int j = 0; j < depth; ++j) {
            builder.append("|   ");
        }
        return builder;
    }

    private void appendProfilerResults(int depth, String path, StringBuilder builder) {
        List<ResultField> results = this.getTimes(path);
        Object2LongMap<String> counters = ((ProfilerPathEntry)ObjectUtils.firstNonNull((Object[])new ProfilerPathEntry[]{this.entries.get(path), EMPTY})).getCounters();
        counters.forEach((id, value) -> FilledProfileResults.indentLine(builder, depth).append('#').append((String)id).append(' ').append(value).append('/').append(value / (long)this.tickDuration).append('\n'));
        if (results.size() < 3) {
            return;
        }
        for (int i = 1; i < results.size(); ++i) {
            ResultField result = results.get(i);
            FilledProfileResults.indentLine(builder, depth).append(result.name).append('(').append(result.count).append('/').append(String.format(Locale.ROOT, "%.0f", Float.valueOf((float)result.count / (float)this.tickDuration))).append(')').append(" - ").append(String.format(Locale.ROOT, "%.2f", result.percentage)).append("%/").append(String.format(Locale.ROOT, "%.2f", result.globalPercentage)).append("%\n");
            if ("unspecified".equals(result.name)) continue;
            try {
                this.appendProfilerResults(depth + 1, path + "\u001e" + result.name, builder);
                continue;
            }
            catch (Exception e) {
                builder.append("[[ EXCEPTION ").append(e).append(" ]]");
            }
        }
    }

    private void appendCounterResults(int depth, String name, CounterCollector result, int tickspan, StringBuilder builder) {
        FilledProfileResults.indentLine(builder, depth).append(name).append(" total:").append(result.selfValue).append('/').append(result.totalValue).append(" average: ").append(result.selfValue / (long)tickspan).append('/').append(result.totalValue / (long)tickspan).append('\n');
        result.children.entrySet().stream().sorted(COUNTER_ENTRY_COMPARATOR).forEach(e -> this.appendCounterResults(depth + 1, (String)e.getKey(), (CounterCollector)e.getValue(), tickspan, builder));
    }

    private void appendCounters(Map<String, CounterCollector> counters, StringBuilder builder, int tickspan) {
        counters.forEach((counter, counterRoot) -> {
            builder.append("-- Counter: ").append((String)counter).append(" --\n");
            this.appendCounterResults(0, "root", counterRoot.children.get("root"), tickspan, builder);
            builder.append("\n\n");
        });
    }

    @Override
    public int getTickDuration() {
        return this.tickDuration;
    }

    private static class CounterCollector {
        private long selfValue;
        private long totalValue;
        private final Map<String, CounterCollector> children = Maps.newHashMap();

        private CounterCollector() {
        }

        public void addValue(Iterator<String> path, long value) {
            this.totalValue += value;
            if (!path.hasNext()) {
                this.selfValue += value;
            } else {
                this.children.computeIfAbsent(path.next(), k -> new CounterCollector()).addValue(path, value);
            }
        }
    }
}

