/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.entity.player.Player;

public class WhitelistCommand {
    private static final SimpleCommandExceptionType ERROR_ALREADY_ENABLED = new SimpleCommandExceptionType((Message)Component.translatable("commands.whitelist.alreadyOn"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_DISABLED = new SimpleCommandExceptionType((Message)Component.translatable("commands.whitelist.alreadyOff"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_WHITELISTED = new SimpleCommandExceptionType((Message)Component.translatable("commands.whitelist.add.failed"));
    private static final SimpleCommandExceptionType ERROR_NOT_WHITELISTED = new SimpleCommandExceptionType((Message)Component.translatable("commands.whitelist.remove.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("whitelist").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))).then(Commands.literal("on").executes(c -> WhitelistCommand.enableWhitelist((CommandSourceStack)c.getSource())))).then(Commands.literal("off").executes(c -> WhitelistCommand.disableWhitelist((CommandSourceStack)c.getSource())))).then(Commands.literal("list").executes(c -> WhitelistCommand.showList((CommandSourceStack)c.getSource())))).then(Commands.literal("add").then(Commands.argument("targets", GameProfileArgument.gameProfile()).suggests((c, p) -> {
            PlayerList list = ((CommandSourceStack)c.getSource()).getServer().getPlayerList();
            return SharedSuggestionProvider.suggest(list.getPlayers().stream().map(Player::nameAndId).filter(nameAndId -> !list.getWhiteList().isWhiteListed((NameAndId)nameAndId)).map(NameAndId::name), p);
        }).executes(c -> WhitelistCommand.addPlayers((CommandSourceStack)c.getSource(), GameProfileArgument.getGameProfiles((CommandContext<CommandSourceStack>)c, "targets")))))).then(Commands.literal("remove").then(Commands.argument("targets", GameProfileArgument.gameProfile()).suggests((c, p) -> SharedSuggestionProvider.suggest(((CommandSourceStack)c.getSource()).getServer().getPlayerList().getWhiteListNames(), p)).executes(c -> WhitelistCommand.removePlayers((CommandSourceStack)c.getSource(), GameProfileArgument.getGameProfiles((CommandContext<CommandSourceStack>)c, "targets")))))).then(Commands.literal("reload").executes(c -> WhitelistCommand.reload((CommandSourceStack)c.getSource()))));
    }

    private static int reload(CommandSourceStack source) {
        source.getServer().getPlayerList().reloadWhiteList();
        source.sendSuccess(() -> Component.translatable("commands.whitelist.reloaded"), true);
        source.getServer().kickUnlistedPlayers();
        return 1;
    }

    private static int addPlayers(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        UserWhiteList list = source.getServer().getPlayerList().getWhiteList();
        int success = 0;
        for (NameAndId target : targets) {
            if (list.isWhiteListed(target)) continue;
            UserWhiteListEntry entry = new UserWhiteListEntry(target);
            list.add(entry);
            source.sendSuccess(() -> Component.translatable("commands.whitelist.add.success", Component.literal(target.name())), true);
            ++success;
        }
        if (success == 0) {
            throw ERROR_ALREADY_WHITELISTED.create();
        }
        return success;
    }

    private static int removePlayers(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        UserWhiteList list = source.getServer().getPlayerList().getWhiteList();
        int success = 0;
        for (NameAndId target : targets) {
            if (!list.isWhiteListed(target)) continue;
            UserWhiteListEntry entry = new UserWhiteListEntry(target);
            list.remove(entry);
            source.sendSuccess(() -> Component.translatable("commands.whitelist.remove.success", Component.literal(target.name())), true);
            ++success;
        }
        if (success == 0) {
            throw ERROR_NOT_WHITELISTED.create();
        }
        source.getServer().kickUnlistedPlayers();
        return success;
    }

    private static int enableWhitelist(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getServer().isUsingWhitelist()) {
            throw ERROR_ALREADY_ENABLED.create();
        }
        source.getServer().setUsingWhitelist(true);
        source.sendSuccess(() -> Component.translatable("commands.whitelist.enabled"), true);
        source.getServer().kickUnlistedPlayers();
        return 1;
    }

    private static int disableWhitelist(CommandSourceStack source) throws CommandSyntaxException {
        if (!source.getServer().isUsingWhitelist()) {
            throw ERROR_ALREADY_DISABLED.create();
        }
        source.getServer().setUsingWhitelist(false);
        source.sendSuccess(() -> Component.translatable("commands.whitelist.disabled"), true);
        return 1;
    }

    private static int showList(CommandSourceStack source) {
        String[] list = source.getServer().getPlayerList().getWhiteListNames();
        if (list.length == 0) {
            source.sendSuccess(() -> Component.translatable("commands.whitelist.none"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.whitelist.list", list.length, String.join((CharSequence)", ", list)), false);
        }
        return list.length;
    }
}

