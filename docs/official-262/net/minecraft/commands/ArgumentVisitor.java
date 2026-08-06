/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.context.CommandContextBuilder
 *  com.mojang.brigadier.context.ParsedArgument
 *  com.mojang.brigadier.context.ParsedCommandNode
 *  com.mojang.brigadier.tree.ArgumentCommandNode
 *  com.mojang.brigadier.tree.CommandNode
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class ArgumentVisitor {
    public static <S> void visitArguments(ParseResults<S> command, Output<S> output, boolean rejectRootRedirects) {
        CommandContextBuilder child;
        CommandContextBuilder rootContext;
        CommandContextBuilder context = rootContext = command.getContext();
        ArgumentVisitor.visitNodeArguments(context, output);
        while (!((child = context.getChild()) == null || rejectRootRedirects && child.getRootNode() == rootContext.getRootNode())) {
            ArgumentVisitor.visitNodeArguments(child, output);
            context = child;
        }
    }

    private static <S> void visitNodeArguments(CommandContextBuilder<S> context, Output<S> output) {
        Map values = context.getArguments();
        for (ParsedCommandNode node : context.getNodes()) {
            CommandNode commandNode = node.getNode();
            if (!(commandNode instanceof ArgumentCommandNode)) continue;
            ArgumentCommandNode argument = (ArgumentCommandNode)commandNode;
            ParsedArgument value = (ParsedArgument)values.get(argument.getName());
            ArgumentVisitor.callVisitor(context, output, argument, value);
        }
    }

    private static <S, T> void callVisitor(CommandContextBuilder<S> context, Output<S> output, ArgumentCommandNode<S, T> argument, @Nullable ParsedArgument<S, ?> value) {
        output.accept(context, argument, value);
    }

    @FunctionalInterface
    public static interface Output<S> {
        public <T> void accept(CommandContextBuilder<S> var1, ArgumentCommandNode<S, T> var2, @Nullable ParsedArgument<S, T> var3);
    }
}

