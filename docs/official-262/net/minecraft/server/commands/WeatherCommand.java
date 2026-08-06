/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;

public class WeatherCommand {
    private static final int DEFAULT_TIME = -1;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("weather").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((LiteralArgumentBuilder)Commands.literal("clear").executes(c -> WeatherCommand.setClear((CommandSourceStack)c.getSource(), -1))).then(Commands.argument("duration", TimeArgument.time(1)).executes(c -> WeatherCommand.setClear((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"duration")))))).then(((LiteralArgumentBuilder)Commands.literal("rain").executes(c -> WeatherCommand.setRain((CommandSourceStack)c.getSource(), -1))).then(Commands.argument("duration", TimeArgument.time(1)).executes(c -> WeatherCommand.setRain((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"duration")))))).then(((LiteralArgumentBuilder)Commands.literal("thunder").executes(c -> WeatherCommand.setThunder((CommandSourceStack)c.getSource(), -1))).then(Commands.argument("duration", TimeArgument.time(1)).executes(c -> WeatherCommand.setThunder((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"duration"))))));
    }

    private static int getDuration(CommandSourceStack source, int input, IntProvider defaultDistribution) {
        if (input == -1) {
            return defaultDistribution.sample(source.getLevel().getRandom());
        }
        return input;
    }

    private static int setClear(CommandSourceStack source, int duration) {
        source.getServer().setWeatherParameters(WeatherCommand.getDuration(source, duration, ServerLevel.RAIN_DELAY), 0, false, false);
        source.sendSuccess(() -> Component.translatable("commands.weather.set.clear"), true);
        return duration;
    }

    private static int setRain(CommandSourceStack source, int duration) {
        source.getServer().setWeatherParameters(0, WeatherCommand.getDuration(source, duration, ServerLevel.RAIN_DURATION), true, false);
        source.sendSuccess(() -> Component.translatable("commands.weather.set.rain"), true);
        return duration;
    }

    private static int setThunder(CommandSourceStack source, int duration) {
        source.getServer().setWeatherParameters(0, WeatherCommand.getDuration(source, duration, ServerLevel.THUNDER_DURATION), true, true);
        source.sendSuccess(() -> Component.translatable("commands.weather.set.thunder"), true);
        return duration;
    }
}

