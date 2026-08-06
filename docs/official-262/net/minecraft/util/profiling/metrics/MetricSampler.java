/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufAllocator
 *  it.unimi.dsi.fastutil.ints.Int2DoubleMap
 *  it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.jspecify.annotations.Nullable;

public class MetricSampler {
    private final String name;
    private final SamplingPhase samplingPhase;
    private final MetricCategory category;
    private final DoubleSupplier sampler;
    private final ByteBuf ticks;
    private final ByteBuf values;
    private volatile boolean isRunning;
    private final @Nullable Runnable beforeTick;
    final @Nullable ThresholdTest thresholdTest;
    private double currentValue;

    protected MetricSampler(String name, SamplingPhase samplingPhase, MetricCategory category, DoubleSupplier sampler, @Nullable Runnable beforeTick, @Nullable ThresholdTest thresholdTest) {
        this.name = name;
        this.samplingPhase = samplingPhase;
        this.category = category;
        this.beforeTick = beforeTick;
        this.sampler = sampler;
        this.thresholdTest = thresholdTest;
        this.values = ByteBufAllocator.DEFAULT.buffer();
        this.ticks = ByteBufAllocator.DEFAULT.buffer();
        this.isRunning = true;
    }

    public static MetricSampler create(String name, MetricCategory category, DoubleSupplier sampler) {
        return new MetricSampler(name, SamplingPhase.END_TICK, category, sampler, null, null);
    }

    public static MetricSampler createExtractSampler(String name, MetricCategory category, DoubleSupplier sampler) {
        return new MetricSampler(name, SamplingPhase.EXTRACT, category, sampler, null, null);
    }

    public static <T> MetricSamplerBuilder<T> builder(String metricName, MetricCategory category, ToDoubleFunction<T> sampler, T context) {
        if (sampler == null) {
            throw new IllegalStateException();
        }
        return new MetricSamplerBuilder<T>(metricName, category, sampler, context);
    }

    public void onStartTick() {
        if (!this.isRunning) {
            throw new IllegalStateException("Not running");
        }
        if (this.beforeTick != null) {
            this.beforeTick.run();
        }
    }

    public void onEndTick(int currentTick) {
        this.verifyRunning();
        this.currentValue = this.sampler.getAsDouble();
        this.values.writeDouble(this.currentValue);
        this.ticks.writeInt(currentTick);
    }

    public void onFinished() {
        this.verifyRunning();
        this.values.release();
        this.ticks.release();
        this.isRunning = false;
    }

    private void verifyRunning() {
        if (!this.isRunning) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Sampler for metric %s not started!", this.name));
        }
    }

    public DoubleSupplier getSampler() {
        return this.sampler;
    }

    public String getName() {
        return this.name;
    }

    public SamplingPhase samplingPhase() {
        return this.samplingPhase;
    }

    public MetricCategory getCategory() {
        return this.category;
    }

    public SamplerResult result() {
        Int2DoubleOpenHashMap result = new Int2DoubleOpenHashMap();
        int firstTick = Integer.MIN_VALUE;
        int lastTick = Integer.MIN_VALUE;
        while (this.values.isReadable(8)) {
            int currentTick = this.ticks.readInt();
            if (firstTick == Integer.MIN_VALUE) {
                firstTick = currentTick;
            }
            result.put(currentTick, this.values.readDouble());
            lastTick = currentTick;
        }
        return new SamplerResult(firstTick, lastTick, (Int2DoubleMap)result);
    }

    public boolean triggersThreshold() {
        return this.thresholdTest != null && this.thresholdTest.test(this.currentValue);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        MetricSampler that = (MetricSampler)o;
        return this.name.equals(that.name) && this.category.equals((Object)that.category);
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public static enum SamplingPhase {
        EXTRACT,
        END_TICK;

    }

    public static interface ThresholdTest {
        public boolean test(double var1);
    }

    public static class MetricSamplerBuilder<T> {
        private final String name;
        private final MetricCategory category;
        private final DoubleSupplier sampler;
        private final T context;
        private SamplingPhase samplingPhase = SamplingPhase.END_TICK;
        private @Nullable Runnable beforeTick;
        private @Nullable ThresholdTest thresholdTest;

        public MetricSamplerBuilder(String name, MetricCategory category, ToDoubleFunction<T> sampler, T context) {
            this.name = name;
            this.category = category;
            this.sampler = () -> sampler.applyAsDouble(context);
            this.context = context;
        }

        public MetricSamplerBuilder<T> withBeforeTick(Consumer<T> beforeTick) {
            this.beforeTick = () -> beforeTick.accept(this.context);
            return this;
        }

        public MetricSamplerBuilder<T> withThresholdAlert(ThresholdTest thresholdTest) {
            this.thresholdTest = thresholdTest;
            return this;
        }

        public MetricSamplerBuilder<T> withSamplingPhase(SamplingPhase samplingPhase) {
            this.samplingPhase = samplingPhase;
            return this;
        }

        public MetricSampler build() {
            return new MetricSampler(this.name, this.samplingPhase, this.category, this.sampler, this.beforeTick, this.thresholdTest);
        }
    }

    public static class SamplerResult {
        private final Int2DoubleMap recording;
        private final int firstTick;
        private final int lastTick;

        public SamplerResult(int firstTick, int lastTick, Int2DoubleMap recording) {
            this.firstTick = firstTick;
            this.lastTick = lastTick;
            this.recording = recording;
        }

        public double valueAtTick(int tick) {
            return this.recording.get(tick);
        }

        public int getFirstTick() {
            return this.firstTick;
        }

        public int getLastTick() {
            return this.lastTick;
        }
    }

    public static class ValueIncreasedByPercentage
    implements ThresholdTest {
        private final float percentageIncreaseThreshold;
        private double previousValue = Double.MIN_VALUE;

        public ValueIncreasedByPercentage(float percentageIncreaseThreshold) {
            this.percentageIncreaseThreshold = percentageIncreaseThreshold;
        }

        @Override
        public boolean test(double value) {
            boolean result = this.previousValue == Double.MIN_VALUE || value <= this.previousValue ? false : (value - this.previousValue) / this.previousValue >= (double)this.percentageIncreaseThreshold;
            this.previousValue = value;
            return result;
        }
    }
}

