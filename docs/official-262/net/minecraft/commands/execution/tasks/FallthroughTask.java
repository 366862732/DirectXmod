/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.commands.execution.tasks;

import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;

public class FallthroughTask<T extends ExecutionCommandSource<T>>
implements EntryAction<T> {
    private static final FallthroughTask<? extends ExecutionCommandSource<?>> INSTANCE = new FallthroughTask();

    public static <T extends ExecutionCommandSource<T>> EntryAction<T> instance() {
        return INSTANCE;
    }

    @Override
    public void execute(ExecutionContext<T> context, Frame frame) {
        frame.returnFailure();
        frame.discard();
    }
}

