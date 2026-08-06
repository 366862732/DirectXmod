/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
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
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlaySoundCommand {
    private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType((Message)Component.translatable("commands.playsound.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder name = (RequiredArgumentBuilder)Commands.argument("sound", IdentifierArgument.id()).suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS)).executes(c -> PlaySoundCommand.playSound((CommandSourceStack)c.getSource(), PlaySoundCommand.getCallingPlayerAsCollection(((CommandSourceStack)c.getSource()).getPlayer()), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound"), SoundSource.MASTER, ((CommandSourceStack)c.getSource()).getPosition(), 1.0f, 1.0f, 0.0f));
        for (SoundSource source : SoundSource.values()) {
            name.then(PlaySoundCommand.source(source));
        }
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("playsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then((ArgumentBuilder)name));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> source(SoundSource source) {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(source.getName()).executes(c -> PlaySoundCommand.playSound((CommandSourceStack)c.getSource(), PlaySoundCommand.getCallingPlayerAsCollection(((CommandSourceStack)c.getSource()).getPlayer()), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound"), source, ((CommandSourceStack)c.getSource()).getPosition(), 1.0f, 1.0f, 0.0f))).then(((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players()).executes(c -> PlaySoundCommand.playSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound"), source, ((CommandSourceStack)c.getSource()).getPosition(), 1.0f, 1.0f, 0.0f))).then(((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3()).executes(c -> PlaySoundCommand.playSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound"), source, Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), 1.0f, 1.0f, 0.0f))).then(((RequiredArgumentBuilder)Commands.argument("volume", FloatArgumentType.floatArg((float)0.0f)).executes(c -> PlaySoundCommand.playSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound"), source, Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), ((Float)c.getArgument("volume", Float.class)).floatValue(), 1.0f, 0.0f))).then(((RequiredArgumentBuilder)Commands.argument("pitch", FloatArgumentType.floatArg((float)0.0f, (float)2.0f)).executes(c -> PlaySoundCommand.playSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound"), source, Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), ((Float)c.getArgument("volume", Float.class)).floatValue(), ((Float)c.getArgument("pitch", Float.class)).floatValue(), 0.0f))).then(Commands.argument("minVolume", FloatArgumentType.floatArg((float)0.0f, (float)1.0f)).executes(c -> PlaySoundCommand.playSound((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "sound"), source, Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), ((Float)c.getArgument("volume", Float.class)).floatValue(), ((Float)c.getArgument("pitch", Float.class)).floatValue(), ((Float)c.getArgument("minVolume", Float.class)).floatValue())))))));
    }

    private static Collection<ServerPlayer> getCallingPlayerAsCollection(@Nullable ServerPlayer player) {
        return player != null ? List.of(player) : List.of();
    }

    private static int playSound(CommandSourceStack source, Collection<ServerPlayer> players, Identifier sound, SoundSource soundSource, Vec3 position, float volume, float pitch, float minVolume) throws CommandSyntaxException {
        Holder<SoundEvent> soundHolder = Holder.direct(SoundEvent.createVariableRangeEvent(sound));
        double maxDistSqr = Mth.square(soundHolder.value().getRange(volume));
        ServerLevel level = source.getLevel();
        long seed = level.getRandom().nextLong();
        ArrayList<ServerPlayer> playedFor = new ArrayList<ServerPlayer>();
        for (ServerPlayer player : players) {
            if (player.level() != level) continue;
            double deltaX = position.x - player.getX();
            double deltaY = position.y - player.getY();
            double deltaZ = position.z - player.getZ();
            double distSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
            Vec3 localPosition = position;
            float localVolume = volume;
            if (distSqr > maxDistSqr) {
                if (minVolume <= 0.0f) continue;
                double distance = Math.sqrt(distSqr);
                localPosition = new Vec3(player.getX() + deltaX / distance * 2.0, player.getY() + deltaY / distance * 2.0, player.getZ() + deltaZ / distance * 2.0);
                localVolume = minVolume;
            }
            player.connection.send(new ClientboundSoundPacket(soundHolder, soundSource, localPosition.x(), localPosition.y(), localPosition.z(), localVolume, pitch, seed));
            playedFor.add(player);
        }
        int count = playedFor.size();
        if (count == 0) {
            throw ERROR_TOO_FAR.create();
        }
        if (count == 1) {
            source.sendSuccess(() -> Component.translatable("commands.playsound.success.single", Component.translationArg(sound), ((ServerPlayer)playedFor.getFirst()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.playsound.success.multiple", Component.translationArg(sound), count), true);
        }
        return count;
    }
}

