/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 */
package net.minecraft.commands.execution.tasks;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;

public class ExecuteCommand<T extends ExecutionCommandSource<T>>
implements UnboundEntryAction<T> {
    private final String commandInput;
    private final ChainModifiers modifiers;
    private final CommandContext<T> executionContext;

    public ExecuteCommand(String commandInput, ChainModifiers modifiers, CommandContext<T> executionContext) {
        this.commandInput = commandInput;
        this.modifiers = modifiers;
        this.executionContext = executionContext;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void execute(T sender, ExecutionContext<T> context, Frame frame) {
        context.profiler().push(() -> "execute " + this.commandInput);
        try {
            context.incrementCost();
            int result = ContextChain.runExecutable(this.executionContext, sender, ExecutionCommandSource.resultConsumer(), (boolean)this.modifiers.isForked());
            TraceCallbacks tracer = context.tracer();
            if (tracer != null) {
                tracer.onReturn(frame.depth(), this.commandInput, result);
            }
        }
        catch (CommandSyntaxException e) {
            sender.handleError(e, this.modifiers.isForked(), context.tracer());
        }
        finally {
            context.profiler().pop();
        }
    }
}

