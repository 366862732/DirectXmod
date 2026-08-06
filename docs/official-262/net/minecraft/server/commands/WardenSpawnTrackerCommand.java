/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.Player;

public class WardenSpawnTrackerCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("warden_spawn_tracker").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("clear").executes(c -> WardenSpawnTrackerCommand.resetTracker((CommandSourceStack)c.getSource(), (Collection<? extends Player>)ImmutableList.of((Object)((CommandSourceStack)c.getSource()).getPlayerOrException()))))).then(Commands.literal("set").then(Commands.argument("warning_level", IntegerArgumentType.integer((int)0, (int)4)).executes(c -> WardenSpawnTrackerCommand.setWarningLevel((CommandSourceStack)c.getSource(), (Collection<? extends Player>)ImmutableList.of((Object)((CommandSourceStack)c.getSource()).getPlayerOrException()), IntegerArgumentType.getInteger((CommandContext)c, (String)"warning_level"))))));
    }

    private static int setWarningLevel(CommandSourceStack source, Collection<? extends Player> players, int warningLevel) {
        for (Player player : players) {
            player.getWardenSpawnTracker().ifPresent(wardenSpawnTracker -> wardenSpawnTracker.setWarningLevel(warningLevel));
        }
        if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.set.success.single", ((Player)players.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.set.success.multiple", players.size()), true);
        }
        return players.size();
    }

    private static int resetTracker(CommandSourceStack source, Collection<? extends Player> players) {
        for (Player player : players) {
            player.getWardenSpawnTracker().ifPresent(WardenSpawnTracker::reset);
        }
        if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.clear.success.single", ((Player)players.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.clear.success.multiple", players.size()), true);
        }
        return players.size();
    }
}

