/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ParsedCommandNode
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.tree.CommandNode
 */
package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class HelpCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.help.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("help").executes(s -> {
            Map usage = dispatcher.getSmartUsage((CommandNode)dispatcher.getRoot(), (Object)((CommandSourceStack)s.getSource()));
            for (String line : usage.values()) {
                ((CommandSourceStack)s.getSource()).sendSuccess(() -> Component.literal("/" + line), false);
            }
            return usage.size();
        })).then(Commands.argument("command", StringArgumentType.greedyString()).executes(s -> {
            ParseResults command = dispatcher.parse(StringArgumentType.getString((CommandContext)s, (String)"command"), (Object)((CommandSourceStack)s.getSource()));
            if (command.getContext().getNodes().isEmpty()) {
                throw ERROR_FAILED.create();
            }
            Map usage = dispatcher.getSmartUsage(((ParsedCommandNode)Iterables.getLast((Iterable)command.getContext().getNodes())).getNode(), (Object)((CommandSourceStack)s.getSource()));
            for (String line : usage.values()) {
                ((CommandSourceStack)s.getSource()).sendSuccess(() -> Component.literal("/" + command.getReader().getString() + " " + line), false);
            }
            return usage.size();
        })));
    }
}

