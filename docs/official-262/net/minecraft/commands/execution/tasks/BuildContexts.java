/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.brigadier.Command
 *  com.mojang.brigadier.RedirectModifier
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.context.ContextChain$Stage
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.commands.execution.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.execution.tasks.ContinuationTask;
import net.minecraft.commands.execution.tasks.ExecuteCommand;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.network.chat.Component;

public class BuildContexts<T extends ExecutionCommandSource<T>> {
    @VisibleForTesting
    public static final DynamicCommandExceptionType ERROR_FORK_LIMIT_REACHED = new DynamicCommandExceptionType(limit -> Component.translatableEscape("command.forkLimit", limit));
    private final String commandInput;
    private final ContextChain<T> command;

    public BuildContexts(String commandInput, ContextChain<T> command) {
        this.commandInput = commandInput;
        this.command = command;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void execute(T originalSource, List<T> initialSources, ExecutionContext<T> context, Frame frame, ChainModifiers initialModifiers) {
        ContextChain currentStage = this.command;
        ChainModifiers modifiers = initialModifiers;
        List<Object> currentSources = initialSources;
        if (currentStage.getStage() != ContextChain.Stage.EXECUTE) {
            context.profiler().push(() -> "prepare " + this.commandInput);
            try {
                int forkLimit = context.forkLimit();
                while (currentStage.getStage() != ContextChain.Stage.EXECUTE) {
                    RedirectModifier modifier;
                    CommandContext contextToRun = currentStage.getTopContext();
                    if (contextToRun.isForked()) {
                        modifiers = modifiers.setForked();
                    }
                    if ((modifier = contextToRun.getRedirectModifier()) instanceof CustomModifierExecutor) {
                        CustomModifierExecutor customModifierExecutor = (CustomModifierExecutor)modifier;
                        customModifierExecutor.apply(originalSource, currentSources, currentStage, modifiers, ExecutionControl.create(context, frame));
                        return;
                    }
                    if (modifier != null) {
                        context.incrementCost();
                        boolean forkedMode = modifiers.isForked();
                        ObjectArrayList nextSources = new ObjectArrayList();
                        for (ExecutionCommandSource executionCommandSource : currentSources) {
                            Collection newSources;
                            block21: {
                                try {
                                    newSources = ContextChain.runModifier((CommandContext)contextToRun, (Object)executionCommandSource, (c, s, r) -> {}, (boolean)forkedMode);
                                    if (nextSources.size() + newSources.size() < forkLimit) break block21;
                                    originalSource.handleError(ERROR_FORK_LIMIT_REACHED.create((Object)forkLimit), forkedMode, context.tracer());
                                    return;
                                }
                                catch (CommandSyntaxException e) {
                                    executionCommandSource.handleError(e, forkedMode, context.tracer());
                                    if (forkedMode) continue;
                                    context.profiler().pop();
                                    return;
                                }
                            }
                            nextSources.addAll(newSources);
                        }
                        currentSources = nextSources;
                    }
                    currentStage = currentStage.nextStage();
                }
            }
            finally {
                context.profiler().pop();
            }
        }
        if (currentSources.isEmpty()) {
            if (modifiers.isReturn()) {
                context.queueNext(new CommandQueueEntry(frame, FallthroughTask.instance()));
            }
            return;
        }
        CommandContext executeContext = currentStage.getTopContext();
        Command command = executeContext.getCommand();
        if (command instanceof CustomCommandExecutor) {
            CustomCommandExecutor customCommandExecutor = (CustomCommandExecutor)command;
            ExecutionControl executionControl = ExecutionControl.create(context, frame);
            for (ExecutionCommandSource executionCommandSource : currentSources) {
                customCommandExecutor.run(executionCommandSource, currentStage, modifiers, executionControl);
            }
        } else {
            if (modifiers.isReturn()) {
                ExecutionCommandSource returningSource = (ExecutionCommandSource)currentSources.get(0);
                returningSource = returningSource.withCallback(CommandResultCallback.chain(returningSource.callback(), frame.returnValueConsumer()));
                currentSources = List.of(returningSource);
            }
            ExecuteCommand action = new ExecuteCommand(this.commandInput, modifiers, executeContext);
            ContinuationTask.schedule(context, frame, currentSources, (frame1, entrySource) -> new CommandQueueEntry<ExecutionCommandSource>(frame1, action.bind(entrySource)));
        }
    }

    protected void traceCommandStart(ExecutionContext<T> context, Frame frame) {
        TraceCallbacks tracer = context.tracer();
        if (tracer != null) {
            tracer.onCommand(frame.depth(), this.commandInput);
        }
    }

    public String toString() {
        return this.commandInput;
    }

    public static class TopLevel<T extends ExecutionCommandSource<T>>
    extends BuildContexts<T>
    implements EntryAction<T> {
        private final T source;

        public TopLevel(String commandInput, ContextChain<T> command, T source) {
            super(commandInput, command);
            this.source = source;
        }

        @Override
        public void execute(ExecutionContext<T> context, Frame frame) {
            this.traceCommandStart(context, frame);
            this.execute(this.source, List.of(this.source), context, frame, ChainModifiers.DEFAULT);
        }
    }

    public static class Continuation<T extends ExecutionCommandSource<T>>
    extends BuildContexts<T>
    implements EntryAction<T> {
        private final ChainModifiers modifiers;
        private final T originalSource;
        private final List<T> sources;

        public Continuation(String commandInput, ContextChain<T> command, ChainModifiers modifiers, T originalSource, List<T> sources) {
            super(commandInput, command);
            this.originalSource = originalSource;
            this.sources = sources;
            this.modifiers = modifiers;
        }

        @Override
        public void execute(ExecutionContext<T> context, Frame frame) {
            this.execute(this.originalSource, this.sources, context, frame, this.modifiers);
        }
    }

    public static class Unbound<T extends ExecutionCommandSource<T>>
    extends BuildContexts<T>
    implements UnboundEntryAction<T> {
        public Unbound(String commandInput, ContextChain<T> command) {
            super(commandInput, command);
        }

        @Override
        public void execute(T sender, ExecutionContext<T> context, Frame frame) {
            this.traceCommandStart(context, frame);
            this.execute(sender, List.of(sender), context, frame, ChainModifiers.DEFAULT);
        }
    }
}

