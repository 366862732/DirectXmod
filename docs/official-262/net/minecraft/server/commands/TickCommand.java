/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.util.TimeUtil;

public class TickCommand {
    private static final float MAX_TICKRATE = 10000.0f;
    private static final String DEFAULT_TICKRATE = String.valueOf(20);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tick").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))).then(Commands.literal("query").executes(c -> TickCommand.tickQuery((CommandSourceStack)c.getSource())))).then(Commands.literal("rate").then(Commands.argument("rate", FloatArgumentType.floatArg((float)1.0f, (float)10000.0f)).suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE}, b)).executes(c -> TickCommand.setTickingRate((CommandSourceStack)c.getSource(), FloatArgumentType.getFloat((CommandContext)c, (String)"rate")))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("step").executes(c -> TickCommand.step((CommandSourceStack)c.getSource(), 1))).then(Commands.literal("stop").executes(c -> TickCommand.stopStepping((CommandSourceStack)c.getSource())))).then(Commands.argument("time", TimeArgument.time(1)).suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, b)).executes(c -> TickCommand.step((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"time")))))).then(((LiteralArgumentBuilder)Commands.literal("sprint").then(Commands.literal("stop").executes(c -> TickCommand.stopSprinting((CommandSourceStack)c.getSource())))).then(Commands.argument("time", TimeArgument.time(1)).suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, b)).executes(c -> TickCommand.sprint((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"time")))))).then(Commands.literal("unfreeze").executes(c -> TickCommand.setFreeze((CommandSourceStack)c.getSource(), false)))).then(Commands.literal("freeze").executes(c -> TickCommand.setFreeze((CommandSourceStack)c.getSource(), true))));
    }

    private static String nanosToMilisString(long nanos) {
        return String.format(Locale.ROOT, "%.1f", Float.valueOf((float)nanos / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND));
    }

    private static int setTickingRate(CommandSourceStack source, float rate) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        manager.setTickRate(rate);
        String tickRateString = String.format(Locale.ROOT, "%.1f", Float.valueOf(rate));
        source.sendSuccess(() -> Component.translatable("commands.tick.rate.success", tickRateString), true);
        return (int)rate;
    }

    private static int tickQuery(CommandSourceStack source) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        String busyTime = TickCommand.nanosToMilisString(source.getServer().getAverageTickTimeNanos());
        float tickRate = manager.tickrate();
        String tickRateString = String.format(Locale.ROOT, "%.1f", Float.valueOf(tickRate));
        if (manager.isSprinting()) {
            source.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), false);
            source.sendSuccess(() -> Component.translatable("commands.tick.query.rate.sprinting", tickRateString, busyTime), false);
        } else {
            if (manager.isFrozen()) {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), false);
            } else if (manager.nanosecondsPerTick() < source.getServer().getAverageTickTimeNanos()) {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.lagging"), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), false);
            }
            String milliSecondsPerTickTarget = TickCommand.nanosToMilisString(manager.nanosecondsPerTick());
            source.sendSuccess(() -> Component.translatable("commands.tick.query.rate.running", tickRateString, busyTime, milliSecondsPerTickTarget), false);
        }
        long[] samples = Arrays.copyOf(source.getServer().getTickTimesNanos(), source.getServer().getTickTimesNanos().length);
        Arrays.sort(samples);
        String p50 = TickCommand.nanosToMilisString(samples[samples.length / 2]);
        String p95 = TickCommand.nanosToMilisString(samples[(int)((double)samples.length * 0.95)]);
        String p99 = TickCommand.nanosToMilisString(samples[(int)((double)samples.length * 0.99)]);
        source.sendSuccess(() -> Component.translatable("commands.tick.query.percentiles", p50, p95, p99, samples.length), false);
        return (int)tickRate;
    }

    private static int sprint(CommandSourceStack source, int time) {
        boolean interrupted = source.getServer().tickRateManager().requestGameToSprint(time);
        if (interrupted) {
            source.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
        }
        source.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), true);
        return 1;
    }

    private static int setFreeze(CommandSourceStack source, boolean freeze) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        if (freeze) {
            if (manager.isSprinting()) {
                manager.stopSprinting();
            }
            if (manager.isSteppingForward()) {
                manager.stopStepping();
            }
        }
        manager.setFrozen(freeze);
        if (freeze) {
            source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
        }
        return freeze ? 1 : 0;
    }

    private static int step(CommandSourceStack source, int advance) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        boolean success = manager.stepGameIfPaused(advance);
        if (success) {
            source.sendSuccess(() -> Component.translatable("commands.tick.step.success", advance), true);
        } else {
            source.sendFailure(Component.translatable("commands.tick.step.fail"));
        }
        return 1;
    }

    private static int stopStepping(CommandSourceStack source) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        boolean success = manager.stopStepping();
        if (success) {
            source.sendSuccess(() -> Component.translatable("commands.tick.step.stop.success"), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.tick.step.stop.fail"));
        return 0;
    }

    private static int stopSprinting(CommandSourceStack source) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        boolean success = manager.stopSprinting();
        if (success) {
            source.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.tick.sprint.stop.fail"));
        return 0;
    }
}

