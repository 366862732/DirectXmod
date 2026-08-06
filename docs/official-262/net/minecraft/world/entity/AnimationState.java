/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import java.util.function.Consumer;

public class AnimationState {
    private static final int STOPPED = Integer.MIN_VALUE;
    private int startTick = Integer.MIN_VALUE;

    public void start(int tickCount) {
        this.startTick = tickCount;
    }

    public void startIfStopped(int tickCount) {
        if (!this.isStarted()) {
            this.start(tickCount);
        }
    }

    public void animateWhen(boolean condition, int tickCount) {
        if (condition) {
            this.startIfStopped(tickCount);
        } else {
            this.stop();
        }
    }

    public void stop() {
        this.startTick = Integer.MIN_VALUE;
    }

    public void ifStarted(Consumer<AnimationState> timer) {
        if (this.isStarted()) {
            timer.accept(this);
        }
    }

    public void fastForward(int ticks, float timeScale) {
        if (!this.isStarted()) {
            return;
        }
        this.startTick -= (int)((float)ticks * timeScale);
    }

    public long getTimeInMillis(float ageInTicks) {
        float timeInTicks = ageInTicks - (float)this.startTick;
        return (long)(timeInTicks * 50.0f);
    }

    public boolean isStarted() {
        return this.startTick != Integer.MIN_VALUE;
    }

    public void copyFrom(AnimationState state) {
        this.startTick = state.startTick;
    }
}

