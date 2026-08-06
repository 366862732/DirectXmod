/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.thread;

import net.minecraft.util.thread.BlockableEventLoop;

public abstract class ReentrantBlockableEventLoop<R extends Runnable>
extends BlockableEventLoop<R> {
    private int reentrantCount;

    public ReentrantBlockableEventLoop(String name, boolean propagatesCrashes) {
        super(name, propagatesCrashes);
    }

    @Override
    protected boolean scheduleExecutables() {
        return this.runningTask() || super.scheduleExecutables();
    }

    protected boolean runningTask() {
        return this.reentrantCount != 0;
    }

    @Override
    protected void doRunTask(R task) {
        ++this.reentrantCount;
        try {
            super.doRunTask(task);
        }
        finally {
            --this.reentrantCount;
        }
    }
}

