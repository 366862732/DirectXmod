/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.MacroFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.world.level.timers.FunctionCallback;
import net.minecraft.world.level.timers.FunctionTagCallback;
import net.minecraft.world.level.timers.TimerQueue;

public class ScheduleCommand {
    private static final SimpleCommandExceptionType ERROR_SAME_TICK = new SimpleCommandExceptionType((Message)Component.translatable("commands.schedule.same_tick"));
    private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType(s -> Component.translatableEscape("commands.schedule.cleared.failure", s));
    private static final SimpleCommandExceptionType ERROR_MACRO = new SimpleCommandExceptionType((Message)Component.translatableEscape("commands.schedule.macro", new Object[0]));
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SCHEDULE = (c, p) -> SharedSuggestionProvider.suggest(((CommandSourceStack)c.getSource()).getServer().getScheduledEvents().getEventsIds(), p);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("schedule").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("function").then(Commands.argument("function", FunctionArgument.functions()).suggests(FunctionCommand.SUGGEST_FUNCTION).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("time", TimeArgument.time()).executes(c -> ScheduleCommand.schedule((CommandSourceStack)c.getSource(), FunctionArgument.getFunctionOrTag((CommandContext<CommandSourceStack>)c, "function"), IntegerArgumentType.getInteger((CommandContext)c, (String)"time"), true))).then(Commands.literal("append").executes(c -> ScheduleCommand.schedule((CommandSourceStack)c.getSource(), FunctionArgument.getFunctionOrTag((CommandContext<CommandSourceStack>)c, "function"), IntegerArgumentType.getInteger((CommandContext)c, (String)"time"), false)))).then(Commands.literal("replace").executes(c -> ScheduleCommand.schedule((CommandSourceStack)c.getSource(), FunctionArgument.getFunctionOrTag((CommandContext<CommandSourceStack>)c, "function"), IntegerArgumentType.getInteger((CommandContext)c, (String)"time"), true))))))).then(Commands.literal("clear").then(Commands.argument("function", StringArgumentType.greedyString()).suggests(SUGGEST_SCHEDULE).executes(c -> ScheduleCommand.remove((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"function"))))));
    }

    private static int schedule(CommandSourceStack source, Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> callback, int time, boolean replace) throws CommandSyntaxException {
        if (time == 0) {
            throw ERROR_SAME_TICK.create();
        }
        long tickTime = source.getLevel().getGameTime() + (long)time;
        Identifier callbackId = (Identifier)callback.getFirst();
        TimerQueue<MinecraftServer> queue = source.getServer().getScheduledEvents();
        Optional function = ((Either)callback.getSecond()).left();
        if (function.isPresent()) {
            if (function.get() instanceof MacroFunction) {
                throw ERROR_MACRO.create();
            }
            String scheduleId = callbackId.toString();
            if (replace) {
                queue.remove(scheduleId);
            }
            queue.schedule(scheduleId, tickTime, new FunctionCallback(callbackId));
            source.sendSuccess(() -> Component.translatable("commands.schedule.created.function", Component.translationArg(callbackId), time, tickTime), true);
        } else {
            String scheduleId = "#" + String.valueOf(callbackId);
            if (replace) {
                queue.remove(scheduleId);
            }
            queue.schedule(scheduleId, tickTime, new FunctionTagCallback(callbackId));
            source.sendSuccess(() -> Component.translatable("commands.schedule.created.tag", Component.translationArg(callbackId), time, tickTime), true);
        }
        return Math.floorMod(tickTime, Integer.MAX_VALUE);
    }

    private static int remove(CommandSourceStack source, String id) throws CommandSyntaxException {
        int count = source.getServer().getScheduledEvents().remove(id);
        if (count == 0) {
            throw ERROR_CANT_REMOVE.create((Object)id);
        }
        source.sendSuccess(() -> Component.translatable("commands.schedule.cleared.success", count, id), true);
        return count;
    }
}

