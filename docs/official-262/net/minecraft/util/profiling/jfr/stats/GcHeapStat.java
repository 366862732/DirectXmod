/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;

public record GcHeapStat(Instant timestamp, long heapUsed, Timing timing) {
    public static GcHeapStat from(RecordedEvent event) {
        return new GcHeapStat(event.getStartTime(), event.getLong("heapUsed"), event.getString("when").equalsIgnoreCase("before gc") ? Timing.BEFORE_GC : Timing.AFTER_GC);
    }

    public static Summary summary(Duration recordingDuration, List<GcHeapStat> heapStats, Duration gcTotalDuration, int totalGCs) {
        return new Summary(recordingDuration, gcTotalDuration, totalGCs, GcHeapStat.calculateAllocationRatePerSecond(heapStats));
    }

    private static double calculateAllocationRatePerSecond(List<GcHeapStat> heapStats) {
        long totalAllocations = 0L;
        Map<Timing, List<GcHeapStat>> byTiming = heapStats.stream().collect(Collectors.groupingBy(it -> it.timing));
        List<GcHeapStat> beforeGcs = byTiming.get((Object)Timing.BEFORE_GC);
        List<GcHeapStat> afterGcs = byTiming.get((Object)Timing.AFTER_GC);
        for (int i = 1; i < beforeGcs.size(); ++i) {
            GcHeapStat beforeGC = beforeGcs.get(i);
            GcHeapStat previousGC = afterGcs.get(i - 1);
            totalAllocations += beforeGC.heapUsed - previousGC.heapUsed;
        }
        Duration totalDuration = Duration.between(heapStats.get((int)1).timestamp, heapStats.get((int)(heapStats.size() - 1)).timestamp);
        return (double)totalAllocations / (double)totalDuration.getSeconds();
    }

    public static enum Timing {
        BEFORE_GC,
        AFTER_GC;

    }

    public record Summary(Duration duration, Duration gcTotalDuration, int totalGCs, double allocationRateBytesPerSecond) {
        public float gcOverHead() {
            return (float)this.gcTotalDuration.toMillis() / (float)this.duration.toMillis();
        }
    }
}

