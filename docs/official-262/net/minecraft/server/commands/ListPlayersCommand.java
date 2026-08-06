/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class ListPlayersCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("list").executes(c -> ListPlayersCommand.listPlayers((CommandSourceStack)c.getSource()))).then(Commands.literal("uuids").executes(c -> ListPlayersCommand.listPlayersWithUuids((CommandSourceStack)c.getSource()))));
    }

    private static int listPlayers(CommandSourceStack source) {
        return ListPlayersCommand.format(source, Player::getDisplayName);
    }

    private static int listPlayersWithUuids(CommandSourceStack source) {
        return ListPlayersCommand.format(source, player -> Component.translatable("commands.list.nameAndId", player.getName(), Component.translationArg(player.getGameProfile().id())));
    }

    private static int format(CommandSourceStack source, Function<ServerPlayer, Component> formatter) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<ServerPlayer> players = playerList.getPlayers();
        Component listComponent = ComponentUtils.formatList(players, formatter);
        source.sendSuccess(() -> Component.translatable("commands.list.players", players.size(), playerList.getMaxPlayers(), listComponent), false);
        return players.size();
    }
}

