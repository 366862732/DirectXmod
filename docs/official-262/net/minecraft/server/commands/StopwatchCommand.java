/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.DoubleArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;

public class StopwatchCommand {
    private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.stopwatch.already_exists", id));
    public static final DynamicCommandExceptionType ERROR_DOES_NOT_EXIST = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.stopwatch.does_not_exist", id));
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_STOPWATCHES = (c, p) -> SharedSuggestionProvider.suggestResource(((CommandSourceStack)c.getSource()).getServer().getStopwatches().ids(), p);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stopwatch").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("create").then(Commands.argument("id", IdentifierArgument.id()).executes(c -> StopwatchCommand.createStopwatch((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id")))))).then(Commands.literal("query").then(((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id()).suggests(SUGGEST_STOPWATCHES).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes(c -> StopwatchCommand.queryStopwatch((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), DoubleArgumentType.getDouble((CommandContext)c, (String)"scale"))))).executes(c -> StopwatchCommand.queryStopwatch((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"), 1.0))))).then(Commands.literal("restart").then(Commands.argument("id", IdentifierArgument.id()).suggests(SUGGEST_STOPWATCHES).executes(c -> StopwatchCommand.restartStopwatch((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id")))))).then(Commands.literal("remove").then(Commands.argument("id", IdentifierArgument.id()).suggests(SUGGEST_STOPWATCHES).executes(c -> StopwatchCommand.removeStopwatch((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "id"))))));
    }

    private static int createStopwatch(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        Stopwatch now;
        MinecraftServer server = source.getServer();
        Stopwatches stopwatches = server.getStopwatches();
        if (!stopwatches.add(id, now = new Stopwatch(Stopwatches.currentTime()))) {
            throw ERROR_ALREADY_EXISTS.create((Object)id);
        }
        source.sendSuccess(() -> Component.translatable("commands.stopwatch.create.success", Component.translationArg(id)), true);
        return 1;
    }

    private static int queryStopwatch(CommandSourceStack source, Identifier id, double scale) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        Stopwatches stopwatches = server.getStopwatches();
        Stopwatch stopwatch = stopwatches.get(id);
        if (stopwatch == null) {
            throw ERROR_DOES_NOT_EXIST.create((Object)id);
        }
        long currentTime = Stopwatches.currentTime();
        double elapsedSeconds = stopwatch.elapsedSeconds(currentTime);
        source.sendSuccess(() -> Component.translatable("commands.stopwatch.query", Component.translationArg(id), elapsedSeconds), true);
        return (int)(elapsedSeconds * scale);
    }

    private static int restartStopwatch(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        Stopwatches stopwatches = server.getStopwatches();
        if (!stopwatches.update(id, stopwatch -> new Stopwatch(Stopwatches.currentTime()))) {
            throw ERROR_DOES_NOT_EXIST.create((Object)id);
        }
        source.sendSuccess(() -> Component.translatable("commands.stopwatch.restart.success", Component.translationArg(id)), true);
        return 1;
    }

    private static int removeStopwatch(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        Stopwatches stopwatches = server.getStopwatches();
        if (!stopwatches.remove(id)) {
            throw ERROR_DOES_NOT_EXIST.create((Object)id);
        }
        source.sendSuccess(() -> Component.translatable("commands.stopwatch.remove.success", Component.translationArg(id)), true);
        return 1;
    }
}

