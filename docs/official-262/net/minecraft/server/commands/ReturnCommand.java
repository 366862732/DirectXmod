/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.tree.CommandNode
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.FallthroughTask;

public class ReturnCommand {
    public static <T extends ExecutionCommandSource<T>> void register(CommandDispatcher<T> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.literal((String)"return").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(RequiredArgumentBuilder.argument((String)"value", (ArgumentType)IntegerArgumentType.integer()).executes(new ReturnValueCustomExecutor()))).then(LiteralArgumentBuilder.literal((String)"fail").executes(new ReturnFailCustomExecutor()))).then(LiteralArgumentBuilder.literal((String)"run").forward((CommandNode)dispatcher.getRoot(), new ReturnFromCommandCustomModifier(), false)));
    }

    private static class ReturnValueCustomExecutor<T extends ExecutionCommandSource<T>>
    implements CustomCommandExecutor.CommandAdapter<T> {
        private ReturnValueCustomExecutor() {
        }

        @Override
        public void run(T sender, ContextChain<T> currentStep, ChainModifiers modifiers, ExecutionControl<T> output) {
            int returnValue = IntegerArgumentType.getInteger((CommandContext)currentStep.getTopContext(), (String)"value");
            sender.callback().onSuccess(returnValue);
            Frame frame = output.currentFrame();
            frame.returnSuccess(returnValue);
            frame.discard();
        }
    }

    private static class ReturnFailCustomExecutor<T extends ExecutionCommandSource<T>>
    implements CustomCommandExecutor.CommandAdapter<T> {
        private ReturnFailCustomExecutor() {
        }

        @Override
        public void run(T sender, ContextChain<T> currentStep, ChainModifiers modifiers, ExecutionControl<T> output) {
            sender.callback().onFailure();
            Frame frame = output.currentFrame();
            frame.returnFailure();
            frame.discard();
        }
    }

    private static class ReturnFromCommandCustomModifier<T extends ExecutionCommandSource<T>>
    implements CustomModifierExecutor.ModifierAdapter<T> {
        private ReturnFromCommandCustomModifier() {
        }

        @Override
        public void apply(T originalSource, List<T> currentSources, ContextChain<T> currentStep, ChainModifiers modifiers, ExecutionControl<T> output) {
            if (currentSources.isEmpty()) {
                if (modifiers.isReturn()) {
                    output.queueNext(FallthroughTask.instance());
                }
                return;
            }
            output.currentFrame().discard();
            ContextChain nextState = currentStep.nextStage();
            String command = nextState.getTopContext().getInput();
            output.queueNext(new BuildContexts.Continuation<T>(command, nextState, modifiers.setReturn(), originalSource, currentSources));
        }
    }
}

