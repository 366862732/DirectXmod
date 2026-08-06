/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.commands.execution.tasks;

import java.util.List;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;

public class ContinuationTask<T, P>
implements EntryAction<T> {
    private final TaskProvider<T, P> taskFactory;
    private final List<P> arguments;
    private final CommandQueueEntry<T> selfEntry;
    private int index;

    private ContinuationTask(TaskProvider<T, P> taskFactory, List<P> arguments, Frame frame) {
        this.taskFactory = taskFactory;
        this.arguments = arguments;
        this.selfEntry = new CommandQueueEntry(frame, this);
    }

    @Override
    public void execute(ExecutionContext<T> context, Frame frame) {
        P argument = this.arguments.get(this.index);
        context.queueNext(this.taskFactory.create(frame, argument));
        if (++this.index < this.arguments.size()) {
            context.queueNext(this.selfEntry);
        }
    }

    public static <T, P> void schedule(ExecutionContext<T> context, Frame frame, List<P> arguments, TaskProvider<T, P> taskFactory) {
        int argumentCount = arguments.size();
        switch (argumentCount) {
            case 0: {
                break;
            }
            case 1: {
                context.queueNext(taskFactory.create(frame, arguments.get(0)));
                break;
            }
            case 2: {
                context.queueNext(taskFactory.create(frame, arguments.get(0)));
                context.queueNext(taskFactory.create(frame, arguments.get(1)));
                break;
            }
            default: {
                context.queueNext(new ContinuationTask<T, P>(taskFactory, arguments, (Frame)frame).selfEntry);
            }
        }
    }

    @FunctionalInterface
    public static interface TaskProvider<T, P> {
        public CommandQueueEntry<T> create(Frame var1, P var2);
    }
}

