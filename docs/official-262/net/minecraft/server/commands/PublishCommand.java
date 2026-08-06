/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.BoolArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public class PublishCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.publish.failed"));
    private static final DynamicCommandExceptionType ERROR_ALREADY_PUBLISHED_LAN = new DynamicCommandExceptionType(port -> Component.translatableEscape("commands.publish.alreadyPublished.lan", port));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("publish").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))).executes(c -> PublishCommand.publish((CommandSourceStack)c.getSource(), HttpUtil.getAvailablePort(), false, null))).then(((RequiredArgumentBuilder)Commands.argument("allowCommands", BoolArgumentType.bool()).executes(c -> PublishCommand.publish((CommandSourceStack)c.getSource(), HttpUtil.getAvailablePort(), BoolArgumentType.getBool((CommandContext)c, (String)"allowCommands"), null))).then(((RequiredArgumentBuilder)Commands.argument("gamemode", GameModeArgument.gameMode()).executes(c -> PublishCommand.publish((CommandSourceStack)c.getSource(), HttpUtil.getAvailablePort(), BoolArgumentType.getBool((CommandContext)c, (String)"allowCommands"), GameModeArgument.getGameMode((CommandContext<CommandSourceStack>)c, "gamemode")))).then(Commands.argument("port", IntegerArgumentType.integer((int)0, (int)65535)).executes(c -> PublishCommand.publish((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"port"), BoolArgumentType.getBool((CommandContext)c, (String)"allowCommands"), GameModeArgument.getGameMode((CommandContext<CommandSourceStack>)c, "gamemode")))))));
    }

    private static int publish(CommandSourceStack source, int port, boolean allowCommands, @Nullable GameType type) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        if (server.isPublished() && server.getPort() > -1) {
            throw ERROR_ALREADY_PUBLISHED_LAN.create((Object)server.getPort());
        }
        if (!server.publishServer(MinecraftServer.MultiplayerScope.LAN, type, allowCommands, port)) {
            throw ERROR_FAILED.create();
        }
        source.sendSuccess(() -> PublishCommand.getSuccessMessage(port), true);
        return port;
    }

    public static MutableComponent getSuccessMessage(int port) {
        MutableComponent portText = ComponentUtils.copyOnClickText(String.valueOf(port));
        return Component.translatable("commands.publish.started.lan", portText);
    }
}

