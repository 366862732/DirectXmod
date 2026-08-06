/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.TeamColorArgument;
import net.minecraft.commands.arguments.WaypointArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("waypoint").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("list").executes(c -> WaypointCommand.listWaypoints((CommandSourceStack)c.getSource())))).then(Commands.literal("modify").then(((RequiredArgumentBuilder)Commands.argument("waypoint", EntityArgument.entity()).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("color").then(Commands.argument("color", TeamColorArgument.teamColor()).executes(c -> WaypointCommand.setWaypointColor((CommandSourceStack)c.getSource(), WaypointArgument.getWaypoint((CommandContext<CommandSourceStack>)c, "waypoint"), TeamColorArgument.getTeamColor((CommandContext<CommandSourceStack>)c, "color"))))).then(Commands.literal("hex").then(Commands.argument("color", HexColorArgument.hexColor()).executes(c -> WaypointCommand.setWaypointColor((CommandSourceStack)c.getSource(), WaypointArgument.getWaypoint((CommandContext<CommandSourceStack>)c, "waypoint"), HexColorArgument.getHexColor((CommandContext<CommandSourceStack>)c, "color")))))).then(Commands.literal("reset").executes(c -> WaypointCommand.resetWaypointColor((CommandSourceStack)c.getSource(), WaypointArgument.getWaypoint((CommandContext<CommandSourceStack>)c, "waypoint")))))).then(((LiteralArgumentBuilder)Commands.literal("style").then(Commands.literal("reset").executes(c -> WaypointCommand.setWaypointStyle((CommandSourceStack)c.getSource(), WaypointArgument.getWaypoint((CommandContext<CommandSourceStack>)c, "waypoint"), WaypointStyleAssets.DEFAULT)))).then(Commands.literal("set").then(Commands.argument("style", IdentifierArgument.id()).executes(c -> WaypointCommand.setWaypointStyle((CommandSourceStack)c.getSource(), WaypointArgument.getWaypoint((CommandContext<CommandSourceStack>)c, "waypoint"), ResourceKey.create(WaypointStyleAssets.ROOT_ID, IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "style"))))))))));
    }

    private static int setWaypointStyle(CommandSourceStack source, WaypointTransmitter waypoint, ResourceKey<WaypointStyleAsset> style) {
        WaypointCommand.mutateIcon(source, waypoint, icon -> {
            icon.style = style;
        });
        source.sendSuccess(() -> Component.translatable("commands.waypoint.modify.style"), false);
        return 0;
    }

    private static int setWaypointColor(CommandSourceStack source, WaypointTransmitter waypoint, TeamColor color) {
        WaypointCommand.mutateIcon(source, waypoint, icon -> {
            icon.color = Optional.of(color.rgb());
        });
        source.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color", Component.literal(color.getSerializedName()).withColor(color.textColor())), false);
        return 0;
    }

    private static int setWaypointColor(CommandSourceStack source, WaypointTransmitter waypoint, Integer color) {
        WaypointCommand.mutateIcon(source, waypoint, icon -> {
            icon.color = Optional.of(color);
        });
        source.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color", Component.literal(HexFormat.of().withUpperCase().toHexDigits(ARGB.color(0, (int)color), 6)).withColor(color)), false);
        return 0;
    }

    private static int resetWaypointColor(CommandSourceStack source, WaypointTransmitter waypoint) {
        WaypointCommand.mutateIcon(source, waypoint, icon -> {
            icon.color = Optional.empty();
        });
        source.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color.reset"), false);
        return 0;
    }

    private static int listWaypoints(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Set<WaypointTransmitter> waypoints = level.getWaypointManager().transmitters();
        String dimension = level.dimension().identifier().toString();
        if (waypoints.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.waypoint.list.empty", dimension), false);
            return 0;
        }
        Component waypointNames = ComponentUtils.formatList(waypoints.stream().map(transmitter -> {
            if (transmitter instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)transmitter;
                BlockPos pos = livingEntity.blockPosition();
                return livingEntity.getFeedbackDisplayName().copy().withStyle(s -> s.withClickEvent(new ClickEvent.SuggestCommand("/execute in " + dimension + " run tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ())).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))).withColor(transmitter.waypointIcon().color.orElse(-1)));
            }
            return Component.literal(transmitter.toString());
        }).toList(), Function.identity());
        source.sendSuccess(() -> Component.translatable("commands.waypoint.list.success", waypoints.size(), dimension, waypointNames), false);
        return waypoints.size();
    }

    private static void mutateIcon(CommandSourceStack source, WaypointTransmitter waypoint, Consumer<Waypoint.Icon> iconConsumer) {
        ServerLevel level = source.getLevel();
        level.getWaypointManager().untrackWaypoint(waypoint);
        iconConsumer.accept(waypoint.waypointIcon());
        level.getWaypointManager().trackWaypoint(waypoint);
    }
}

