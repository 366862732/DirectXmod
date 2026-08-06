/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.metrics.profiling;

import net.minecraft.util.profiling.ProfilerFiller;

public interface MetricsRecorder {
    public void end();

    public void cancel();

    public void startTick();

    public void sampleDuringExtract();

    public boolean isRecording();

    public ProfilerFiller getProfiler();

    public void endTick();
}

