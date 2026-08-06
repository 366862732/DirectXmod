/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world;

public record Stopwatch(long creationTime, long accumulatedElapsedTime) {
    public Stopwatch(long creationTime) {
        this(creationTime, 0L);
    }

    public long elapsedMilliseconds(long currentTime) {
        long timeSinceInstanceCreation = currentTime - this.creationTime;
        return this.accumulatedElapsedTime + timeSinceInstanceCreation;
    }

    public double elapsedSeconds(long currentTime) {
        return (double)this.elapsedMilliseconds(currentTime) / 1000.0;
    }
}

