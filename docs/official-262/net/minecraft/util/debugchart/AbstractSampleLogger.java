/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.debugchart;

import net.minecraft.util.debugchart.SampleLogger;

public abstract class AbstractSampleLogger
implements SampleLogger {
    protected final long[] defaults;
    protected final long[] sample;

    protected AbstractSampleLogger(int dimensions, long[] defaults) {
        if (defaults.length != dimensions) {
            throw new IllegalArgumentException("defaults have incorrect length of " + defaults.length);
        }
        this.sample = new long[dimensions];
        this.defaults = defaults;
    }

    @Override
    public void logFullSample(long[] sample) {
        System.arraycopy(sample, 0, this.sample, 0, sample.length);
        this.useSample();
        this.resetSample();
    }

    @Override
    public void logSample(long sample) {
        this.sample[0] = sample;
        this.useSample();
        this.resetSample();
    }

    @Override
    public void logPartialSample(long sample, int dimension) {
        if (dimension < 1 || dimension >= this.sample.length) {
            throw new IndexOutOfBoundsException(dimension + " out of bounds for dimensions " + this.sample.length);
        }
        this.sample[dimension] = sample;
    }

    protected abstract void useSample();

    protected void resetSample() {
        System.arraycopy(this.defaults, 0, this.sample, 0, this.defaults.length);
    }
}

