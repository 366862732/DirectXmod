/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TimerQuery
implements AutoCloseable {
    private static final int ROTATIONS = 3;
    private @Nullable CommandEncoder activeEncoder;
    private final GpuQueryPool queryPool;
    private int currentRotationIndex;
    private Status status = Status.NOT_RECORDING;
    private final long[] results = new long[3];

    public TimerQuery() {
        this.queryPool = RenderSystem.getDevice().createTimestampQueryPool(6);
    }

    public void beginProfile() {
        if (this.status != Status.NOT_RECORDING) {
            throw new IllegalStateException("Current profile not ended");
        }
        ++this.currentRotationIndex;
        this.currentRotationIndex %= 3;
        this.activeEncoder = RenderSystem.getDevice().createCommandEncoder();
        this.activeEncoder.writeTimestamp(this.queryPool, this.currentRotationIndex * 2);
        this.status = Status.STARTED;
    }

    public void endProfile() {
        if (this.status != Status.STARTED || this.activeEncoder == null) {
            throw new IllegalStateException("endProfile called before beginProfile");
        }
        this.activeEncoder.writeTimestamp(this.queryPool, this.currentRotationIndex * 2 + 1);
        this.activeEncoder = null;
        this.status = Status.AWAITING_VALUES;
    }

    public long get() {
        long average = 0L;
        for (int i = 0; i < this.results.length; ++i) {
            average += this.results[i];
        }
        return average / (long)this.results.length;
    }

    @Override
    public void close() {
        this.queryPool.close();
    }

    public Status getStatus() {
        if (this.status == Status.AWAITING_VALUES) {
            OptionalLong[] timestamps = this.queryPool.getValues(this.currentRotationIndex * 2, 2);
            OptionalLong startValue = timestamps[0];
            OptionalLong endValue = timestamps[1];
            if (startValue.isPresent() && endValue.isPresent()) {
                long delta = endValue.getAsLong() - startValue.getAsLong();
                this.results[this.currentRotationIndex] = (long)((float)delta * RenderSystem.getDevice().getDeviceInfo().timestampPeriod());
                this.status = Status.NOT_RECORDING;
            }
        }
        return this.status;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Status {
        NOT_RECORDING,
        STARTED,
        AWAITING_VALUES;

    }
}

