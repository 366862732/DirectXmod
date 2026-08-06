/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.jspecify.annotations.Nullable;

public class BanPlayerCommands {
    private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType((Message)Component.translatable("commands.ban.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ban").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))).then(((RequiredArgumentBuilder)Commands.argument("targets", GameProfileArgument.gameProfile()).executes(c -> BanPlayerCommands.banPlayers((CommandSourceStack)c.getSource(), GameProfileArgument.getGameProfiles((CommandContext<CommandSourceStack>)c, "targets"), null))).then(Commands.argument("reason", MessageArgument.message()).executes(c -> BanPlayerCommands.banPlayers((CommandSourceStack)c.getSource(), GameProfileArgument.getGameProfiles((CommandContext<CommandSourceStack>)c, "targets"), MessageArgument.getMessage((CommandContext<CommandSourceStack>)c, "reason"))))));
    }

    private static int banPlayers(CommandSourceStack source, Collection<NameAndId> players, @Nullable Component reason) throws CommandSyntaxException {
        UserBanList list = source.getServer().getPlayerList().getBans();
        int count = 0;
        for (NameAndId player : players) {
            if (list.isBanned(player)) continue;
            UserBanListEntry entry = new UserBanListEntry(player, null, source.getTextName(), null, reason == null ? null : reason.getString());
            list.add(entry);
            ++count;
            source.sendSuccess(() -> Component.translatable("commands.ban.success", Component.literal(player.name()), entry.getReasonMessage()), true);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(player.id());
            if (online == null) continue;
            online.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
        }
        if (count == 0) {
            throw ERROR_ALREADY_BANNED.create();
        }
        return count;
    }
}

