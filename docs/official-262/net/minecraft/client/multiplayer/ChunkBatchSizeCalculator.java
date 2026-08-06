/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class ChunkBatchSizeCalculator {
    private static final int MAX_OLD_SAMPLES_WEIGHT = 49;
    private static final int CLAMP_COEFFICIENT = 3;
    private double aggregatedNanosPerChunk = 2000000.0;
    private int oldSamplesWeight = 1;
    private volatile long chunkBatchStartTime = Util.getNanos();

    public void onBatchStart() {
        this.chunkBatchStartTime = Util.getNanos();
    }

    public void onBatchFinished(int batchSize) {
        if (batchSize > 0) {
            double batchDuration = Util.getNanos() - this.chunkBatchStartTime;
            double nanosPerChunk = batchDuration / (double)batchSize;
            double clampedNanosPerChunk = Mth.clamp(nanosPerChunk, this.aggregatedNanosPerChunk / 3.0, this.aggregatedNanosPerChunk * 3.0);
            this.aggregatedNanosPerChunk = (this.aggregatedNanosPerChunk * (double)this.oldSamplesWeight + clampedNanosPerChunk) / (double)(this.oldSamplesWeight + 1);
            this.oldSamplesWeight = Math.min(49, this.oldSamplesWeight + 1);
        }
    }

    public float getDesiredChunksPerTick() {
        return (float)(7000000.0 / this.aggregatedNanosPerChunk);
    }
}

