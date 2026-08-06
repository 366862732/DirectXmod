/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.DoubleArgumentType
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec2;

public class WorldBorderCommand {
    private static final SimpleCommandExceptionType ERROR_SAME_CENTER = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.center.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_SIZE = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.set.failed.nochange"));
    private static final SimpleCommandExceptionType ERROR_TOO_SMALL = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.set.failed.small"));
    private static final SimpleCommandExceptionType ERROR_TOO_BIG = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.set.failed.big", 5.9999968E7));
    private static final SimpleCommandExceptionType ERROR_TOO_FAR_OUT = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.set.failed.far", 2.9999984E7));
    private static final SimpleCommandExceptionType ERROR_SAME_WARNING_TIME = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.warning.time.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_WARNING_DISTANCE = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.warning.distance.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_BUFFER = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.damage.buffer.failed"));
    private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_AMOUNT = new SimpleCommandExceptionType((Message)Component.translatable("commands.worldborder.damage.amount.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("worldborder").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("add").then(((RequiredArgumentBuilder)Commands.argument("distance", DoubleArgumentType.doubleArg((double)-5.9999968E7, (double)5.9999968E7)).executes(c -> WorldBorderCommand.setSize((CommandSourceStack)c.getSource(), ((CommandSourceStack)c.getSource()).getLevel().getWorldBorder().getSize() + DoubleArgumentType.getDouble((CommandContext)c, (String)"distance"), 0L))).then(Commands.argument("time", TimeArgument.time(0)).executes(c -> WorldBorderCommand.setSize((CommandSourceStack)c.getSource(), ((CommandSourceStack)c.getSource()).getLevel().getWorldBorder().getSize() + DoubleArgumentType.getDouble((CommandContext)c, (String)"distance"), ((CommandSourceStack)c.getSource()).getLevel().getWorldBorder().getLerpTime() + (long)IntegerArgumentType.getInteger((CommandContext)c, (String)"time"))))))).then(Commands.literal("set").then(((RequiredArgumentBuilder)Commands.argument("distance", DoubleArgumentType.doubleArg((double)-5.9999968E7, (double)5.9999968E7)).executes(c -> WorldBorderCommand.setSize((CommandSourceStack)c.getSource(), DoubleArgumentType.getDouble((CommandContext)c, (String)"distance"), 0L))).then(Commands.argument("time", TimeArgument.time(0)).executes(c -> WorldBorderCommand.setSize((CommandSourceStack)c.getSource(), DoubleArgumentType.getDouble((CommandContext)c, (String)"distance"), IntegerArgumentType.getInteger((CommandContext)c, (String)"time"))))))).then(Commands.literal("center").then(Commands.argument("pos", Vec2Argument.vec2()).executes(c -> WorldBorderCommand.setCenter((CommandSourceStack)c.getSource(), Vec2Argument.getVec2((CommandContext<CommandSourceStack>)c, "pos")))))).then(((LiteralArgumentBuilder)Commands.literal("damage").then(Commands.literal("amount").then(Commands.argument("damagePerBlock", FloatArgumentType.floatArg((float)0.0f)).executes(c -> WorldBorderCommand.setDamageAmount((CommandSourceStack)c.getSource(), FloatArgumentType.getFloat((CommandContext)c, (String)"damagePerBlock")))))).then(Commands.literal("buffer").then(Commands.argument("distance", FloatArgumentType.floatArg((float)0.0f)).executes(c -> WorldBorderCommand.setDamageBuffer((CommandSourceStack)c.getSource(), FloatArgumentType.getFloat((CommandContext)c, (String)"distance"))))))).then(Commands.literal("get").executes(c -> WorldBorderCommand.getSize((CommandSourceStack)c.getSource())))).then(((LiteralArgumentBuilder)Commands.literal("warning").then(Commands.literal("distance").then(Commands.argument("distance", IntegerArgumentType.integer((int)0)).executes(c -> WorldBorderCommand.setWarningDistance((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"distance")))))).then(Commands.literal("time").then(Commands.argument("time", TimeArgument.time(0)).executes(c -> WorldBorderCommand.setWarningTime((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"time")))))));
    }

    private static int setDamageBuffer(CommandSourceStack source, float distance) throws CommandSyntaxException {
        WorldBorder border = source.getLevel().getWorldBorder();
        if (border.getSafeZone() == (double)distance) {
            throw ERROR_SAME_DAMAGE_BUFFER.create();
        }
        border.setSafeZone(distance);
        source.sendSuccess(() -> Component.translatable("commands.worldborder.damage.buffer.success", String.format(Locale.ROOT, "%.2f", Float.valueOf(distance))), true);
        return (int)distance;
    }

    private static int setDamageAmount(CommandSourceStack source, float damagePerBlock) throws CommandSyntaxException {
        WorldBorder border = source.getLevel().getWorldBorder();
        if (border.getDamagePerBlock() == (double)damagePerBlock) {
            throw ERROR_SAME_DAMAGE_AMOUNT.create();
        }
        border.setDamagePerBlock(damagePerBlock);
        source.sendSuccess(() -> Component.translatable("commands.worldborder.damage.amount.success", String.format(Locale.ROOT, "%.2f", Float.valueOf(damagePerBlock))), true);
        return (int)damagePerBlock;
    }

    private static int setWarningTime(CommandSourceStack source, int ticks) throws CommandSyntaxException {
        WorldBorder border = source.getLevel().getWorldBorder();
        if (border.getWarningTime() == ticks) {
            throw ERROR_SAME_WARNING_TIME.create();
        }
        border.setWarningTime(ticks);
        source.sendSuccess(() -> Component.translatable("commands.worldborder.warning.time.success", WorldBorderCommand.formatTicksToSeconds(ticks)), true);
        return ticks;
    }

    private static int setWarningDistance(CommandSourceStack source, int distance) throws CommandSyntaxException {
        WorldBorder border = source.getLevel().getWorldBorder();
        if (border.getWarningBlocks() == distance) {
            throw ERROR_SAME_WARNING_DISTANCE.create();
        }
        border.setWarningBlocks(distance);
        source.sendSuccess(() -> Component.translatable("commands.worldborder.warning.distance.success", distance), true);
        return distance;
    }

    private static int getSize(CommandSourceStack source) {
        double size = source.getLevel().getWorldBorder().getSize();
        source.sendSuccess(() -> Component.translatable("commands.worldborder.get", String.format(Locale.ROOT, "%.0f", size)), false);
        return Mth.floor(size + 0.5);
    }

    private static int setCenter(CommandSourceStack source, Vec2 center) throws CommandSyntaxException {
        WorldBorder border = source.getLevel().getWorldBorder();
        if (border.getCenterX() == (double)center.x && border.getCenterZ() == (double)center.y) {
            throw ERROR_SAME_CENTER.create();
        }
        if ((double)Math.abs(center.x) > 2.9999984E7 || (double)Math.abs(center.y) > 2.9999984E7) {
            throw ERROR_TOO_FAR_OUT.create();
        }
        border.setCenter(center.x, center.y);
        source.sendSuccess(() -> Component.translatable("commands.worldborder.center.success", String.format(Locale.ROOT, "%.2f", Float.valueOf(center.x)), String.format(Locale.ROOT, "%.2f", Float.valueOf(center.y))), true);
        return 0;
    }

    private static int setSize(CommandSourceStack source, double distance, long ticks) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        WorldBorder border = level.getWorldBorder();
        double current = border.getSize();
        if (current == distance) {
            throw ERROR_SAME_SIZE.create();
        }
        if (distance < 1.0) {
            throw ERROR_TOO_SMALL.create();
        }
        if (distance > 5.9999968E7) {
            throw ERROR_TOO_BIG.create();
        }
        String formattedDistance = String.format(Locale.ROOT, "%.1f", distance);
        if (ticks > 0L) {
            border.lerpSizeBetween(current, distance, ticks, level.getGameTime());
            if (distance > current) {
                source.sendSuccess(() -> Component.translatable("commands.worldborder.set.grow", formattedDistance, WorldBorderCommand.formatTicksToSeconds(ticks)), true);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.worldborder.set.shrink", formattedDistance, WorldBorderCommand.formatTicksToSeconds(ticks)), true);
            }
        } else {
            border.setSize(distance);
            source.sendSuccess(() -> Component.translatable("commands.worldborder.set.immediate", formattedDistance), true);
        }
        return (int)(distance - current);
    }

    private static String formatTicksToSeconds(long ticks) {
        return String.format(Locale.ROOT, "%.2f", (double)ticks / 20.0);
    }
}

