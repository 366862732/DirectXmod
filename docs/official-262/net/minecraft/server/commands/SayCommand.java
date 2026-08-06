/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.players.PlayerList;

public class SayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("say").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("message", MessageArgument.message()).executes(c -> {
            MessageArgument.resolveChatMessage((CommandContext<CommandSourceStack>)c, "message", message -> {
                CommandSourceStack source = (CommandSourceStack)c.getSource();
                PlayerList playerList = source.getServer().getPlayerList();
                playerList.broadcastChatMessage((PlayerChatMessage)message, source, ChatType.bind(ChatType.SAY_COMMAND, source));
            });
            return 1;
        })));
    }
}

