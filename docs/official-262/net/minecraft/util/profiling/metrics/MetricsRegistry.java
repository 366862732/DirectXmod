/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling.metrics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import org.jspecify.annotations.Nullable;

public class MetricsRegistry {
    public static final MetricsRegistry INSTANCE = new MetricsRegistry();
    private final WeakHashMap<ProfilerMeasured, Void> measuredInstances = new WeakHashMap();

    private MetricsRegistry() {
    }

    public void add(ProfilerMeasured profilerMeasured) {
        this.measuredInstances.put(profilerMeasured, null);
    }

    public List<MetricSampler> getRegisteredSamplers() {
        Map<String, List<MetricSampler>> samplersByName = this.measuredInstances.keySet().stream().flatMap(measuredInstance -> measuredInstance.profiledMetrics().stream()).collect(Collectors.groupingBy(MetricSampler::getName));
        return MetricsRegistry.aggregateDuplicates(samplersByName);
    }

    private static List<MetricSampler> aggregateDuplicates(Map<String, List<MetricSampler>> potentialDuplicates) {
        return potentialDuplicates.entrySet().stream().map(entry -> {
            String samplerName = (String)entry.getKey();
            List duplicateSamplers = (List)entry.getValue();
            return duplicateSamplers.size() > 1 ? new AggregatedMetricSampler(samplerName, duplicateSamplers) : (MetricSampler)duplicateSamplers.get(0);
        }).collect(Collectors.toList());
    }

    private static class AggregatedMetricSampler
    extends MetricSampler {
        private final List<MetricSampler> delegates;

        private AggregatedMetricSampler(String name, List<MetricSampler> delegates) {
            super(name, MetricSampler.SamplingPhase.END_TICK, delegates.get(0).getCategory(), () -> AggregatedMetricSampler.averageValueFromDelegates(delegates), () -> AggregatedMetricSampler.beforeTick(delegates), AggregatedMetricSampler.thresholdTest(delegates));
            this.delegates = delegates;
        }

        private static MetricSampler.ThresholdTest thresholdTest(List<MetricSampler> delegates) {
            return value -> delegates.stream().anyMatch(delegate -> {
                if (delegate.thresholdTest != null) {
                    return delegate.thresholdTest.test(value);
                }
                return false;
            });
        }

        private static void beforeTick(List<MetricSampler> delegates) {
            for (MetricSampler delegate : delegates) {
                delegate.onStartTick();
            }
        }

        private static double averageValueFromDelegates(List<MetricSampler> delegates) {
            double aggregatedValue = 0.0;
            for (MetricSampler delegate : delegates) {
                aggregatedValue += delegate.getSampler().getAsDouble();
            }
            return aggregatedValue / (double)delegates.size();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            AggregatedMetricSampler that = (AggregatedMetricSampler)o;
            return this.delegates.equals(that.delegates);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.delegates);
        }
    }
}

