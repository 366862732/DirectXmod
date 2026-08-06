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
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

public class GameModeCommand {
    public static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("gamemode").requires(Commands.hasPermission(PERMISSION_CHECK))).then(((RequiredArgumentBuilder)Commands.argument("gamemode", GameModeArgument.gameMode()).executes(c -> GameModeCommand.setMode((CommandContext<CommandSourceStack>)c, Collections.singleton(((CommandSourceStack)c.getSource()).getPlayerOrException()), GameModeArgument.getGameMode((CommandContext<CommandSourceStack>)c, "gamemode")))).then(Commands.argument("target", EntityArgument.players()).executes(c -> GameModeCommand.setMode((CommandContext<CommandSourceStack>)c, EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "target"), GameModeArgument.getGameMode((CommandContext<CommandSourceStack>)c, "gamemode"))))));
    }

    private static void logGamemodeChange(CommandSourceStack source, ServerPlayer target, GameType newType) {
        MutableComponent mode = Component.translatable("gameMode." + newType.getName());
        if (source.getEntity() == target) {
            source.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", mode), true);
        } else {
            if (source.getLevel().getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK).booleanValue()) {
                target.sendSystemMessage(Component.translatable("gameMode.changed", mode));
            }
            source.sendSuccess(() -> Component.translatable("commands.gamemode.success.other", target.getDisplayName(), mode), true);
        }
    }

    private static int setMode(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, GameType type) {
        int count = 0;
        MinecraftServer server = ((CommandSourceStack)context.getSource()).getServer();
        for (ServerPlayer player : players) {
            if (server.isSingleplayerOwner(player.nameAndId())) {
                server.setDefaultGameType(type);
            }
            if (!GameModeCommand.setGameMode((CommandSourceStack)context.getSource(), player, type)) continue;
            ++count;
        }
        return count;
    }

    public static void setGameMode(ServerPlayer player, GameType type) {
        GameModeCommand.setGameMode(player.createCommandSourceStack(), player, type);
    }

    private static boolean setGameMode(CommandSourceStack source, ServerPlayer player, GameType type) {
        if (player.setGameMode(type)) {
            MinecraftServer server = source.getServer();
            if (server.isSingleplayerOwner(player.nameAndId())) {
                server.setDefaultGameType(type);
            }
            GameModeCommand.logGamemodeChange(source, player, type);
            return true;
        }
        return false;
    }
}

