/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.google.common.base.Ticker
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  org.slf4j.Logger
 *  oshi.SystemInfo
 *  oshi.hardware.CentralProcessor
 */
package net.minecraft.util.profiling.metrics.profiling;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import net.minecraft.SystemReport;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.MetricsSamplerProvider;
import net.minecraft.util.profiling.metrics.profiling.ProfilerSamplerAdapter;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class ServerMetricsSamplersProvider
implements MetricsSamplerProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Set<MetricSampler> samplers = new ObjectOpenHashSet();
    private final ProfilerSamplerAdapter samplerFactory = new ProfilerSamplerAdapter();

    public ServerMetricsSamplersProvider(LongSupplier wallTimeSource, boolean isDedicatedServer) {
        this.samplers.add(ServerMetricsSamplersProvider.tickTimeSampler(wallTimeSource));
        if (isDedicatedServer) {
            this.samplers.addAll(ServerMetricsSamplersProvider.runtimeIndependentSamplers());
        }
    }

    public static Set<MetricSampler> runtimeIndependentSamplers() {
        ImmutableSet.Builder result = ImmutableSet.builder();
        try {
            CpuStats cpuStats = new CpuStats();
            IntStream.range(0, cpuStats.nrOfCpus).mapToObj(i -> MetricSampler.create("cpu#" + i, MetricCategory.CPU, () -> cpuStats.loadForCpu(i))).forEach(arg_0 -> ((ImmutableSet.Builder)result).add(arg_0));
        }
        catch (Throwable t) {
            LOGGER.warn("Failed to query cpu, no cpu stats will be recorded", t);
        }
        result.add((Object)MetricSampler.create("heap MiB", MetricCategory.JVM, () -> SystemReport.sizeInMiB(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
        result.addAll(MetricsRegistry.INSTANCE.getRegisteredSamplers());
        return result.build();
    }

    @Override
    public Set<MetricSampler> samplers(Supplier<ProfileCollector> profiler) {
        this.samplers.addAll(this.samplerFactory.newSamplersFoundInProfiler(profiler));
        return this.samplers;
    }

    public static MetricSampler tickTimeSampler(final LongSupplier timeSource) {
        Stopwatch stopwatch = Stopwatch.createUnstarted((Ticker)new Ticker(){

            public long read() {
                return timeSource.getAsLong();
            }
        });
        ToDoubleFunction<Stopwatch> timeSampler = watch -> {
            if (watch.isRunning()) {
                watch.stop();
            }
            long deltaTime = watch.elapsed(TimeUnit.NANOSECONDS);
            watch.reset();
            return deltaTime;
        };
        MetricSampler.ValueIncreasedByPercentage thresholdAlerter = new MetricSampler.ValueIncreasedByPercentage(2.0f);
        return MetricSampler.builder("ticktime", MetricCategory.TICK_LOOP, timeSampler, stopwatch).withBeforeTick(Stopwatch::start).withThresholdAlert(thresholdAlerter).build();
    }

    public static class CpuStats {
        private final SystemInfo systemInfo = new SystemInfo();
        private final CentralProcessor processor = this.systemInfo.getHardware().getProcessor();
        public final int nrOfCpus = this.processor.getLogicalProcessorCount();
        private long[][] previousCpuLoadTick = this.processor.getProcessorCpuLoadTicks();
        private double[] currentLoad = this.processor.getProcessorCpuLoadBetweenTicks(this.previousCpuLoadTick);
        private long lastPollMs;

        public double loadForCpu(int i) {
            long now = System.currentTimeMillis();
            if (this.lastPollMs == 0L || this.lastPollMs + 501L < now) {
                this.currentLoad = this.processor.getProcessorCpuLoadBetweenTicks(this.previousCpuLoadTick);
                this.previousCpuLoadTick = this.processor.getProcessorCpuLoadTicks();
                this.lastPollMs = now;
            }
            return this.currentLoad[i] * 100.0;
        }
    }
}

