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
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;

public class DefaultGameModeCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("defaultgamemode").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("gamemode", GameModeArgument.gameMode()).executes(c -> DefaultGameModeCommands.setMode((CommandSourceStack)c.getSource(), GameModeArgument.getGameMode((CommandContext<CommandSourceStack>)c, "gamemode")))));
    }

    private static int setMode(CommandSourceStack source, GameType type) {
        MinecraftServer server = source.getServer();
        server.setDefaultGameType(type);
        int count = server.enforceGameTypeForPlayers(server.getForcedGameType());
        source.sendSuccess(() -> Component.translatable("commands.defaultgamemode.success", type.getLongDisplayName()), true);
        return count;
    }
}

