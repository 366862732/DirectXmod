/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.particle.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("particle").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((RequiredArgumentBuilder)Commands.argument("name", ParticleArgument.particle(context)).executes(c -> ParticleCommand.sendParticles((CommandSourceStack)c.getSource(), ParticleArgument.getParticle((CommandContext<CommandSourceStack>)c, "name"), ((CommandSourceStack)c.getSource()).getPosition(), Vec3.ZERO, 0.0f, 0, false, ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()))).then(((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3()).executes(c -> ParticleCommand.sendParticles((CommandSourceStack)c.getSource(), ParticleArgument.getParticle((CommandContext<CommandSourceStack>)c, "name"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), Vec3.ZERO, 0.0f, 0, false, ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()))).then(Commands.argument("delta", Vec3Argument.vec3(false)).then(Commands.argument("speed", FloatArgumentType.floatArg((float)0.0f)).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("count", IntegerArgumentType.integer((int)0)).executes(c -> ParticleCommand.sendParticles((CommandSourceStack)c.getSource(), ParticleArgument.getParticle((CommandContext<CommandSourceStack>)c, "name"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "delta"), FloatArgumentType.getFloat((CommandContext)c, (String)"speed"), IntegerArgumentType.getInteger((CommandContext)c, (String)"count"), false, ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()))).then(((LiteralArgumentBuilder)Commands.literal("force").executes(c -> ParticleCommand.sendParticles((CommandSourceStack)c.getSource(), ParticleArgument.getParticle((CommandContext<CommandSourceStack>)c, "name"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "delta"), FloatArgumentType.getFloat((CommandContext)c, (String)"speed"), IntegerArgumentType.getInteger((CommandContext)c, (String)"count"), true, ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()))).then(Commands.argument("viewers", EntityArgument.players()).executes(c -> ParticleCommand.sendParticles((CommandSourceStack)c.getSource(), ParticleArgument.getParticle((CommandContext<CommandSourceStack>)c, "name"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "delta"), FloatArgumentType.getFloat((CommandContext)c, (String)"speed"), IntegerArgumentType.getInteger((CommandContext)c, (String)"count"), true, EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "viewers")))))).then(((LiteralArgumentBuilder)Commands.literal("normal").executes(c -> ParticleCommand.sendParticles((CommandSourceStack)c.getSource(), ParticleArgument.getParticle((CommandContext<CommandSourceStack>)c, "name"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "delta"), FloatArgumentType.getFloat((CommandContext)c, (String)"speed"), IntegerArgumentType.getInteger((CommandContext)c, (String)"count"), false, ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()))).then(Commands.argument("viewers", EntityArgument.players()).executes(c -> ParticleCommand.sendParticles((CommandSourceStack)c.getSource(), ParticleArgument.getParticle((CommandContext<CommandSourceStack>)c, "name"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos"), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "delta"), FloatArgumentType.getFloat((CommandContext)c, (String)"speed"), IntegerArgumentType.getInteger((CommandContext)c, (String)"count"), false, EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "viewers")))))))))));
    }

    private static int sendParticles(CommandSourceStack source, ParticleOptions particle, Vec3 pos, Vec3 delta, float speed, int count, boolean force, Collection<ServerPlayer> players) throws CommandSyntaxException {
        int result = 0;
        for (ServerPlayer player : players) {
            if (!source.getLevel().sendParticles(player, particle, force, false, pos.x, pos.y, pos.z, count, delta.x, delta.y, delta.z, speed)) continue;
            ++result;
        }
        if (result == 0) {
            throw ERROR_FAILED.create();
        }
        source.sendSuccess(() -> Component.translatable("commands.particle.success", BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType()).toString()), true);
        return result;
    }
}

