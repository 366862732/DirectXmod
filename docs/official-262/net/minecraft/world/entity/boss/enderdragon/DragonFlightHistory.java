/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.boss.enderdragon;

import java.util.Arrays;
import net.minecraft.util.Mth;

public class DragonFlightHistory {
    public static final int LENGTH = 64;
    private static final int MASK = 63;
    private final Sample[] samples = new Sample[64];
    private int head = -1;

    public DragonFlightHistory() {
        Arrays.fill(this.samples, new Sample(0.0, 0.0f));
    }

    public void copyFrom(DragonFlightHistory history) {
        System.arraycopy(history.samples, 0, this.samples, 0, 64);
        this.head = history.head;
    }

    public void record(double y, float yRot) {
        Sample sample = new Sample(y, yRot);
        if (this.head < 0) {
            Arrays.fill(this.samples, sample);
        }
        if (++this.head == 64) {
            this.head = 0;
        }
        this.samples[this.head] = sample;
    }

    public Sample get(int delay) {
        return this.samples[this.head - delay & 0x3F];
    }

    public Sample get(int delay, float partialTicks) {
        Sample sample = this.get(delay);
        Sample sampleOld = this.get(delay + 1);
        return new Sample(Mth.lerp((double)partialTicks, sampleOld.y, sample.y), Mth.rotLerp(partialTicks, sampleOld.yRot, sample.yRot));
    }

    public record Sample(double y, float yRot) {
    }
}

