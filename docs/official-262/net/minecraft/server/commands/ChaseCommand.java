/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.ImmutableBiMap
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.commands;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.chase.ChaseClient;
import net.minecraft.server.chase.ChaseServer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChaseCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_CONNECT_HOST = "localhost";
    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 10000;
    private static final int BROADCAST_INTERVAL_MS = 100;
    public static final BiMap<String, ResourceKey<Level>> DIMENSION_NAMES = ImmutableBiMap.of((Object)"o", Level.OVERWORLD, (Object)"n", Level.NETHER, (Object)"e", Level.END);
    private static @Nullable ChaseServer chaseServer;
    private static @Nullable ChaseClient chaseClient;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("chase").then(((LiteralArgumentBuilder)Commands.literal("follow").then(((RequiredArgumentBuilder)Commands.argument("host", StringArgumentType.string()).executes(c -> ChaseCommand.follow((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"host"), 10000))).then(Commands.argument("port", IntegerArgumentType.integer((int)1, (int)65535)).executes(c -> ChaseCommand.follow((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"host"), IntegerArgumentType.getInteger((CommandContext)c, (String)"port")))))).executes(c -> ChaseCommand.follow((CommandSourceStack)c.getSource(), DEFAULT_CONNECT_HOST, 10000)))).then(((LiteralArgumentBuilder)Commands.literal("lead").then(((RequiredArgumentBuilder)Commands.argument("bind_address", StringArgumentType.string()).executes(c -> ChaseCommand.lead((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"bind_address"), 10000))).then(Commands.argument("port", IntegerArgumentType.integer((int)1024, (int)65535)).executes(c -> ChaseCommand.lead((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"bind_address"), IntegerArgumentType.getInteger((CommandContext)c, (String)"port")))))).executes(c -> ChaseCommand.lead((CommandSourceStack)c.getSource(), DEFAULT_BIND_ADDRESS, 10000)))).then(Commands.literal("stop").executes(c -> ChaseCommand.stop((CommandSourceStack)c.getSource()))));
    }

    private static int stop(CommandSourceStack source) {
        if (chaseClient != null) {
            chaseClient.stop();
            source.sendSuccess(() -> Component.literal("You have now stopped chasing"), false);
            chaseClient = null;
        }
        if (chaseServer != null) {
            chaseServer.stop();
            source.sendSuccess(() -> Component.literal("You are no longer being chased"), false);
            chaseServer = null;
        }
        return 0;
    }

    private static boolean alreadyRunning(CommandSourceStack source) {
        if (chaseServer != null) {
            source.sendFailure(Component.literal("Chase server is already running. Stop it using /chase stop"));
            return true;
        }
        if (chaseClient != null) {
            source.sendFailure(Component.literal("You are already chasing someone. Stop it using /chase stop"));
            return true;
        }
        return false;
    }

    private static int lead(CommandSourceStack source, String serverBindAddress, int port) {
        if (ChaseCommand.alreadyRunning(source)) {
            return 0;
        }
        chaseServer = new ChaseServer(serverBindAddress, port, source.getServer().getPlayerList(), 100);
        try {
            chaseServer.start();
            source.sendSuccess(() -> Component.literal("Chase server is now running on port " + port + ". Clients can follow you using /chase follow <ip> <port>"), false);
        }
        catch (IOException e) {
            LOGGER.error("Failed to start chase server", (Throwable)e);
            source.sendFailure(Component.literal("Failed to start chase server on port " + port));
            chaseServer = null;
        }
        return 0;
    }

    private static int follow(CommandSourceStack source, String host, int port) {
        if (ChaseCommand.alreadyRunning(source)) {
            return 0;
        }
        chaseClient = new ChaseClient(host, port, source.getServer());
        chaseClient.start();
        source.sendSuccess(() -> Component.literal("You are now chasing " + host + ":" + port + ". If that server does '/chase lead' then you will automatically go to the same position. Use '/chase stop' to stop chasing."), false);
        return 0;
    }
}

