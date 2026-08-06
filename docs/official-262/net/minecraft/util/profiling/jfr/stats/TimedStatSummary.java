/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.profiling.jfr.Percentiles;
import net.minecraft.util.profiling.jfr.stats.TimedStat;
import org.jspecify.annotations.Nullable;

public record TimedStatSummary<T extends TimedStat>(T fastest, T slowest, @Nullable T secondSlowest, int count, Map<Integer, Double> percentilesNanos, Duration totalDuration) {
    public static <T extends TimedStat> Optional<TimedStatSummary<T>> summary(List<T> values) {
        if (values.isEmpty()) {
            return Optional.empty();
        }
        List<TimedStat> sorted = values.stream().sorted(Comparator.comparing(TimedStat::duration)).toList();
        Duration totalDuration = sorted.stream().map(TimedStat::duration).reduce(Duration::plus).orElse(Duration.ZERO);
        TimedStat fastest = sorted.getFirst();
        TimedStat slowest = sorted.getLast();
        TimedStat secondSlowest = sorted.size() > 1 ? sorted.get(sorted.size() - 2) : null;
        int count = sorted.size();
        Map<Integer, Double> percentilesNanos = Percentiles.evaluate(sorted.stream().mapToLong(it -> it.duration().toNanos()).toArray());
        return Optional.of(new TimedStatSummary<TimedStat>(fastest, slowest, secondSlowest, count, percentilesNanos, totalDuration));
    }
}

