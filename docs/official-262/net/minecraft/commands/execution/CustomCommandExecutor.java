/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.Command
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.execution;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.TraceCallbacks;
import org.jspecify.annotations.Nullable;

public interface CustomCommandExecutor<T> {
    public void run(T var1, ContextChain<T> var2, ChainModifiers var3, ExecutionControl<T> var4);

    public static abstract class WithErrorHandling<T extends ExecutionCommandSource<T>>
    implements CustomCommandExecutor<T> {
        @Override
        public final void run(T sender, ContextChain<T> currentStep, ChainModifiers modifiers, ExecutionControl<T> output) {
            try {
                this.runGuarded(sender, currentStep, modifiers, output);
            }
            catch (CommandSyntaxException e) {
                this.onError(e, sender, modifiers, output.tracer());
                sender.callback().onFailure();
            }
        }

        protected void onError(CommandSyntaxException e, T sender, ChainModifiers modifiers, @Nullable TraceCallbacks tracer) {
            sender.handleError(e, modifiers.isForked(), tracer);
        }

        protected abstract void runGuarded(T var1, ContextChain<T> var2, ChainModifiers var3, ExecutionControl<T> var4) throws CommandSyntaxException;
    }

    public static interface CommandAdapter<T>
    extends CustomCommandExecutor<T>,
    Command<T> {
        default public int run(CommandContext<T> context) throws CommandSyntaxException {
            throw new UnsupportedOperationException("This function should not run");
        }
    }
}

