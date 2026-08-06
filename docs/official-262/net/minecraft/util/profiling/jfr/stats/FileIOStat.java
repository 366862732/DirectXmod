/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public record FileIOStat(Duration duration, @Nullable String path, long bytes) {
    public static Summary summary(Duration recordingDuration, List<FileIOStat> ioStats) {
        long totalBytes = ioStats.stream().mapToLong(it -> it.bytes).sum();
        return new Summary(totalBytes, (double)totalBytes / (double)recordingDuration.getSeconds(), ioStats.size(), (double)ioStats.size() / (double)recordingDuration.getSeconds(), ioStats.stream().map(FileIOStat::duration).reduce(Duration.ZERO, Duration::plus), ioStats.stream().filter(it -> it.path != null).collect(Collectors.groupingBy(stat -> stat.path, Collectors.summingLong(it -> it.bytes))).entrySet().stream().sorted(Map.Entry.comparingByValue().reversed()).map(e -> Pair.of((Object)((String)e.getKey()), (Object)((Long)e.getValue()))).limit(10L).toList());
    }

    public record Summary(long totalBytes, double bytesPerSecond, long counts, double countsPerSecond, Duration timeSpentInIO, List<Pair<String, Long>> topTenContributorsByTotalBytes) {
    }
}

