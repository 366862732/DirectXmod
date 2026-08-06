/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import java.util.concurrent.locks.LockSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class FramerateLimiter {
    private static final double OVERSHOOT_SMOOTHING = 0.1;
    private static final long MAX_CURRENT_OVERSHOOT_NS = 25000000L;
    private static final long MAX_AVERAGE_OVERSHOOT_NS = 2000000L;
    private static final long ONE_SECOND_IN_NANOSECONDS = 1000000000L;
    private static final long SPIN_SAFETY_BUFFER_NS = 500000L;
    private static long lastFrameTime = System.nanoTime();
    private static long averageOvershootNs = 0L;
    private static int lastFramerateLimit;

    public static void limitDisplayFPS(int framerateLimit) {
        long remainingTimeNs;
        long targetTimePerFrame = 1000000000L / (long)framerateLimit;
        long targetTimeNs = lastFrameTime + targetTimePerFrame;
        if (framerateLimit != lastFramerateLimit) {
            averageOvershootNs = 0L;
            lastFramerateLimit = framerateLimit;
        }
        while ((remainingTimeNs = targetTimeNs - System.nanoTime()) > 0L) {
            if (remainingTimeNs > averageOvershootNs + 500000L) {
                long sleepStartTimeNs = System.nanoTime();
                long expectedSleepTimeNs = remainingTimeNs - averageOvershootNs - 500000L;
                if (Thread.interrupted()) continue;
                LockSupport.parkNanos(expectedSleepTimeNs);
                long sleepDurationNs = System.nanoTime() - sleepStartTimeNs;
                long currentOvershootNs = sleepDurationNs - expectedSleepTimeNs;
                if (currentOvershootNs <= 0L || currentOvershootNs >= 25000000L) continue;
                averageOvershootNs = (long)(0.1 * (double)currentOvershootNs + 0.9 * (double)averageOvershootNs);
                averageOvershootNs = Math.min(averageOvershootNs, 2000000L);
                continue;
            }
            Thread.onSpinWait();
        }
        lastFrameTime = System.nanoTime();
    }
}

