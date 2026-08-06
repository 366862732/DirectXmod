/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class EndFlashState {
    public static final int SOUND_DELAY_IN_TICKS = 30;
    private static final int FLASH_INTERVAL_IN_TICKS = 600;
    private static final int MAX_FLASH_OFFSET_IN_TICKS = 200;
    private static final int MIN_FLASH_DURATION_IN_TICKS = 100;
    private static final int MAX_FLASH_DURATION_IN_TICKS = 380;
    private long flashSeed;
    private int offset;
    private int duration;
    private float intensity;
    private float oldIntensity;
    private float xAngle;
    private float yAngle;

    public void tick(long clockTime) {
        this.calculateFlashParameters(clockTime);
        this.oldIntensity = this.intensity;
        this.intensity = this.calculateIntensity(clockTime);
    }

    private void calculateFlashParameters(long clockTime) {
        long newSeed = clockTime / 600L;
        if (newSeed != this.flashSeed) {
            RandomSource randomSource = RandomSource.createThreadLocalInstance(newSeed);
            randomSource.nextFloat();
            this.offset = Mth.randomBetweenInclusive(randomSource, 0, 200);
            this.duration = Mth.randomBetweenInclusive(randomSource, 100, Math.min(380, 600 - this.offset));
            this.xAngle = Mth.randomBetween(randomSource, -60.0f, 10.0f);
            this.yAngle = Mth.randomBetween(randomSource, -180.0f, 180.0f);
            this.flashSeed = newSeed;
        }
    }

    private float calculateIntensity(long clockTime) {
        long clockTimeWithinInterval = clockTime % 600L;
        if (clockTimeWithinInterval < (long)this.offset || clockTimeWithinInterval > (long)(this.offset + this.duration)) {
            return 0.0f;
        }
        return Mth.sin((float)(clockTimeWithinInterval - (long)this.offset) * (float)Math.PI / (float)this.duration);
    }

    public float getXAngle() {
        return this.xAngle;
    }

    public float getYAngle() {
        return this.yAngle;
    }

    public float getIntensity(float partialTicks) {
        return Mth.lerp(partialTicks, this.oldIntensity, this.intensity);
    }

    public boolean flashStartedThisTick() {
        return this.intensity > 0.0f && this.oldIntensity <= 0.0f;
    }
}

