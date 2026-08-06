/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.context.CommandContextBuilder
 *  com.mojang.brigadier.context.ParsedArgument
 *  com.mojang.brigadier.tree.ArgumentCommandNode
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.ArgumentVisitor;
import net.minecraft.commands.arguments.SignedArgument;
import org.jspecify.annotations.Nullable;

public record SignableCommand<S>(List<Argument<S>> arguments) {
    public static <S> boolean hasSignableArguments(ParseResults<S> command) {
        return !SignableCommand.of(command).arguments().isEmpty();
    }

    public static <S> SignableCommand<S> of(ParseResults<S> command) {
        final String commandString = command.getReader().getString();
        final ArrayList<Argument<S>> arguments = new ArrayList<Argument<S>>();
        ArgumentVisitor.visitArguments(command, new ArgumentVisitor.Output<S>(){

            @Override
            public <T> void accept(CommandContextBuilder<S> context, ArgumentCommandNode<S, T> argument, @Nullable ParsedArgument<S, T> value) {
                if (value != null && argument.getType() instanceof SignedArgument) {
                    String stringValue = value.getRange().get(commandString);
                    arguments.add(new Argument(argument, stringValue));
                }
            }
        }, true);
        return new SignableCommand<S>(arguments);
    }

    public @Nullable Argument<S> getArgument(String name) {
        for (Argument<S> argument : this.arguments) {
            if (!name.equals(argument.name())) continue;
            return argument;
        }
        return null;
    }

    public record Argument<S>(ArgumentCommandNode<S, ?> node, String value) {
        public String name() {
            return this.node.getName();
        }
    }
}

