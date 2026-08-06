/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.debugchart;

import net.minecraft.util.debugchart.AbstractSampleLogger;
import net.minecraft.util.debugchart.SampleStorage;

public class LocalSampleLogger
extends AbstractSampleLogger
implements SampleStorage {
    public static final int CAPACITY = 240;
    private final long[][] samples;
    private int start;
    private int size;

    public LocalSampleLogger(int dimensions) {
        this(dimensions, new long[dimensions]);
    }

    public LocalSampleLogger(int dimensions, long[] defaults) {
        super(dimensions, defaults);
        this.samples = new long[240][dimensions];
    }

    @Override
    protected void useSample() {
        int nextIndex = this.wrapIndex(this.start + this.size);
        System.arraycopy(this.sample, 0, this.samples[nextIndex], 0, this.sample.length);
        if (this.size < 240) {
            ++this.size;
        } else {
            this.start = this.wrapIndex(this.start + 1);
        }
    }

    @Override
    public int capacity() {
        return this.samples.length;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public long get(int index) {
        return this.get(index, 0);
    }

    @Override
    public long get(int index, int dimension) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException(index + " out of bounds for length " + this.size);
        }
        long[] sampleArray = this.samples[this.wrapIndex(this.start + index)];
        if (dimension < 0 || dimension >= sampleArray.length) {
            throw new IndexOutOfBoundsException(dimension + " out of bounds for dimensions " + sampleArray.length);
        }
        return sampleArray[dimension];
    }

    private int wrapIndex(int index) {
        return index % 240;
    }

    @Override
    public void reset() {
        this.start = 0;
        this.size = 0;
    }
}

