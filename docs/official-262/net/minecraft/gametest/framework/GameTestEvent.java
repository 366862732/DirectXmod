/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.gametest.framework;

import org.jspecify.annotations.Nullable;

public class GameTestEvent {
    public final @Nullable Long expectedDelay;
    public final @Nullable Long minimumDelay;
    public final Runnable assertion;

    private GameTestEvent(@Nullable Long expectedDelay, @Nullable Long minimumDelay, Runnable assertion) {
        this.expectedDelay = expectedDelay;
        this.minimumDelay = minimumDelay;
        this.assertion = assertion;
    }

    public static GameTestEvent create(Runnable runnable) {
        return new GameTestEvent(null, null, runnable);
    }

    public static GameTestEvent create(long expectedTick, Runnable runnable) {
        return new GameTestEvent(expectedTick, null, runnable);
    }

    public static GameTestEvent createWithMinimumDelay(long minimumDelay, Runnable runnable) {
        return new GameTestEvent(null, minimumDelay, runnable);
    }
}

