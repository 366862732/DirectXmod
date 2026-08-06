/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.execution;

import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import org.jspecify.annotations.Nullable;

public interface ExecutionControl<T> {
    public void queueNext(EntryAction<T> var1);

    public void tracer(@Nullable TraceCallbacks var1);

    public @Nullable TraceCallbacks tracer();

    public Frame currentFrame();

    public static <T extends ExecutionCommandSource<T>> ExecutionControl<T> create(final ExecutionContext<T> context, final Frame frame) {
        return new ExecutionControl<T>(){

            @Override
            public void queueNext(EntryAction<T> action) {
                context.queueNext(new CommandQueueEntry(frame, action));
            }

            @Override
            public void tracer(@Nullable TraceCallbacks tracer) {
                context.tracer(tracer);
            }

            @Override
            public @Nullable TraceCallbacks tracer() {
                return context.tracer();
            }

            @Override
            public Frame currentFrame() {
                return frame;
            }
        };
    }
}

