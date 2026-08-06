/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

public class StopSoundCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder target = (RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players()).executes(c -> StopSoundCommand.stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), null, null))).then(Commands.literal("*").then(Commands.argument("sound", IdentifierArgument.id()).suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS)).executes(c -> StopSoundCommand.stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), null, IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound")))));
        for (SoundSource source : SoundSource.values()) {
            target.then(((LiteralArgumentBuilder)Commands.literal(source.getName()).executes(c -> StopSoundCommand.stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), source, null))).then(Commands.argument("sound", IdentifierArgument.id()).suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS)).executes(c -> StopSoundCommand.stopSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), source, IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound")))));
        }
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stopsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then((ArgumentBuilder)target));
    }

    private static int stopSound(CommandSourceStack source, Collection<ServerPlayer> targets, @Nullable SoundSource soundSource, @Nullable Identifier sound) {
        ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(sound, soundSource);
        for (ServerPlayer player : targets) {
            player.connection.send(packet);
        }
        if (soundSource != null) {
            if (sound != null) {
                source.sendSuccess(() -> Component.translatable("commands.stopsound.success.source.sound", Component.translationArg(sound), soundSource.getName()), true);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.stopsound.success.source.any", soundSource.getName()), true);
            }
        } else if (sound != null) {
            source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.sound", Component.translationArg(sound)), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.any"), true);
        }
        return targets.size();
    }
}

