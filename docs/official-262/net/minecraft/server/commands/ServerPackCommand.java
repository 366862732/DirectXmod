/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

public class ServerPackCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("serverpack").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("push").then(((RequiredArgumentBuilder)Commands.argument("url", StringArgumentType.string()).then(((RequiredArgumentBuilder)Commands.argument("uuid", UuidArgument.uuid()).then(Commands.argument("hash", StringArgumentType.word()).executes(c -> ServerPackCommand.pushPack((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"url"), Optional.of(UuidArgument.getUuid((CommandContext<CommandSourceStack>)c, "uuid")), Optional.of(StringArgumentType.getString((CommandContext)c, (String)"hash")))))).executes(c -> ServerPackCommand.pushPack((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"url"), Optional.of(UuidArgument.getUuid((CommandContext<CommandSourceStack>)c, "uuid")), Optional.empty())))).executes(c -> ServerPackCommand.pushPack((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"url"), Optional.empty(), Optional.empty()))))).then(Commands.literal("pop").then(Commands.argument("uuid", UuidArgument.uuid()).executes(c -> ServerPackCommand.popPack((CommandSourceStack)c.getSource(), UuidArgument.getUuid((CommandContext<CommandSourceStack>)c, "uuid"))))));
    }

    private static void sendToAllConnections(CommandSourceStack source, Packet<?> packet) {
        source.getServer().getConnection().getConnections().forEach(connection -> connection.send(packet));
    }

    private static int pushPack(CommandSourceStack source, String url, Optional<UUID> maybeId, Optional<String> maybeHash) {
        UUID id = maybeId.orElseGet(() -> UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)));
        String hash = maybeHash.orElse("");
        ClientboundResourcePackPushPacket packet = new ClientboundResourcePackPushPacket(id, url, hash, false, null);
        ServerPackCommand.sendToAllConnections(source, packet);
        return 0;
    }

    private static int popPack(CommandSourceStack source, UUID uuid) {
        ClientboundResourcePackPopPacket packet = new ClientboundResourcePackPopPacket(Optional.of(uuid));
        ServerPackCommand.sendToAllConnections(source, packet);
        return 0;
    }
}

