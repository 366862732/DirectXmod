/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.tree.CommandNode
 *  com.mojang.brigadier.tree.LiteralCommandNode
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class MsgCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode msg = dispatcher.register((LiteralArgumentBuilder)Commands.literal("msg").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes(c -> {
            Collection<ServerPlayer> players = EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets");
            if (!players.isEmpty()) {
                MessageArgument.resolveChatMessage((CommandContext<CommandSourceStack>)c, "message", message -> MsgCommand.sendMessage((CommandSourceStack)c.getSource(), players, message));
            }
            return players.size();
        }))));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal("tell").redirect((CommandNode)msg));
        dispatcher.register((LiteralArgumentBuilder)Commands.literal("w").redirect((CommandNode)msg));
    }

    private static void sendMessage(CommandSourceStack source, Collection<ServerPlayer> players, PlayerChatMessage message) {
        ChatType.Bound incomingChatType = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, source);
        OutgoingChatMessage tracked = OutgoingChatMessage.create(message);
        boolean wasFullyFiltered = false;
        for (ServerPlayer player : players) {
            ChatType.Bound outgoingChatType = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, source).withTargetName(player.getDisplayName());
            source.sendChatMessage(tracked, false, outgoingChatType);
            boolean filtered = source.shouldFilterMessageTo(player);
            player.sendChatMessage(tracked, filtered, incomingChatType);
            wasFullyFiltered |= filtered && message.isFullyFiltered();
        }
        if (wasFullyFiltered) {
            source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
        }
    }
}

