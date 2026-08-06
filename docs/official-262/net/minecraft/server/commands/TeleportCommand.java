/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.tree.CommandNode
 *  com.mojang.brigadier.tree.LiteralCommandNode
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LookAt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TeleportCommand {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType((Message)Component.translatable("commands.teleport.invalidPosition"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode teleport = dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("teleport").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("location", Vec3Argument.vec3()).executes(c -> TeleportCommand.teleportToPos((CommandSourceStack)c.getSource(), Collections.singleton(((CommandSourceStack)c.getSource()).getEntityOrException()), ((CommandSourceStack)c.getSource()).getLevel(), Vec3Argument.getCoordinates((CommandContext<CommandSourceStack>)c, "location"), null, null)))).then(Commands.argument("destination", EntityArgument.entity()).executes(c -> TeleportCommand.teleportToEntity((CommandSourceStack)c.getSource(), Collections.singleton(((CommandSourceStack)c.getSource()).getEntityOrException()), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "destination"))))).then(((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("location", Vec3Argument.vec3()).executes(c -> TeleportCommand.teleportToPos((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ((CommandSourceStack)c.getSource()).getLevel(), Vec3Argument.getCoordinates((CommandContext<CommandSourceStack>)c, "location"), null, null))).then(Commands.argument("rotation", RotationArgument.rotation()).executes(c -> TeleportCommand.teleportToPos((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ((CommandSourceStack)c.getSource()).getLevel(), Vec3Argument.getCoordinates((CommandContext<CommandSourceStack>)c, "location"), RotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rotation"), null)))).then(((LiteralArgumentBuilder)Commands.literal("facing").then(Commands.literal("entity").then(((RequiredArgumentBuilder)Commands.argument("facingEntity", EntityArgument.entity()).executes(c -> TeleportCommand.teleportToPos((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ((CommandSourceStack)c.getSource()).getLevel(), Vec3Argument.getCoordinates((CommandContext<CommandSourceStack>)c, "location"), null, new LookAt.LookAtEntity(EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "facingEntity"), EntityAnchorArgument.Anchor.FEET)))).then(Commands.argument("facingAnchor", EntityAnchorArgument.anchor()).executes(c -> TeleportCommand.teleportToPos((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ((CommandSourceStack)c.getSource()).getLevel(), Vec3Argument.getCoordinates((CommandContext<CommandSourceStack>)c, "location"), null, new LookAt.LookAtEntity(EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "facingEntity"), EntityAnchorArgument.getAnchor((CommandContext<CommandSourceStack>)c, "facingAnchor")))))))).then(Commands.argument("facingLocation", Vec3Argument.vec3()).executes(c -> TeleportCommand.teleportToPos((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), ((CommandSourceStack)c.getSource()).getLevel(), Vec3Argument.getCoordinates((CommandContext<CommandSourceStack>)c, "location"), null, new LookAt.LookAtPosition(Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "facingLocation")))))))).then(Commands.argument("destination", EntityArgument.entity()).executes(c -> TeleportCommand.teleportToEntity((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "destination"))))));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).redirect((CommandNode)teleport));
    }

    private static int teleportToEntity(CommandSourceStack source, Collection<? extends Entity> entities, Entity destination) throws CommandSyntaxException {
        for (Entity entity : entities) {
            TeleportCommand.performTeleport(source, entity, (ServerLevel)destination.level(), destination.getX(), destination.getY(), destination.getZ(), EnumSet.noneOf(Relative.class), destination.getYRot(), destination.getXRot(), null);
        }
        if (entities.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.single", ((Entity)entities.iterator().next()).getDisplayName(), destination.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.multiple", entities.size(), destination.getDisplayName()), true);
        }
        return entities.size();
    }

    private static int teleportToPos(CommandSourceStack source, Collection<? extends Entity> entities, ServerLevel level, Coordinates destination, @Nullable Coordinates rotation, @Nullable LookAt lookAt) throws CommandSyntaxException {
        Vec3 pos = destination.getPosition(source);
        Vec2 rot = rotation == null ? null : rotation.getRotation(source);
        for (Entity entity : entities) {
            Set<Relative> relatives = TeleportCommand.getRelatives(destination, rotation, entity.level().dimension() == level.dimension());
            if (rot == null) {
                TeleportCommand.performTeleport(source, entity, level, pos.x, pos.y, pos.z, relatives, entity.getYRot(), entity.getXRot(), lookAt);
                continue;
            }
            TeleportCommand.performTeleport(source, entity, level, pos.x, pos.y, pos.z, relatives, rot.y, rot.x, lookAt);
        }
        if (entities.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.teleport.success.location.single", ((Entity)entities.iterator().next()).getDisplayName(), TeleportCommand.formatDouble(pos.x), TeleportCommand.formatDouble(pos.y), TeleportCommand.formatDouble(pos.z)), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.teleport.success.location.multiple", entities.size(), TeleportCommand.formatDouble(pos.x), TeleportCommand.formatDouble(pos.y), TeleportCommand.formatDouble(pos.z)), true);
        }
        return entities.size();
    }

    private static Set<Relative> getRelatives(Coordinates destination, @Nullable Coordinates rotation, boolean sameDimension) {
        Set<Relative> dir = Relative.direction(destination.isXRelative(), destination.isYRelative(), destination.isZRelative());
        Set pos = sameDimension ? Relative.position(destination.isXRelative(), destination.isYRelative(), destination.isZRelative()) : Set.of();
        Set<Relative> rot = rotation == null ? Relative.ROTATION : Relative.rotation(rotation.isYRelative(), rotation.isXRelative());
        return Relative.union(dir, pos, rot);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%f", value);
    }

    private static void performTeleport(CommandSourceStack source, Entity victim, ServerLevel level, double x, double y, double z, Set<Relative> relatives, float yRot, float xRot, @Nullable LookAt lookAt) throws CommandSyntaxException {
        LivingEntity living;
        float newXRot;
        BlockPos blockPos = BlockPos.containing(x, y, z);
        if (!Level.isInSpawnableBounds(blockPos)) {
            throw INVALID_POSITION.create();
        }
        double relativeOrAbsoluteX = relatives.contains((Object)Relative.X) ? x - victim.getX() : x;
        double relativeOrAbsoluteY = relatives.contains((Object)Relative.Y) ? y - victim.getY() : y;
        double relativeOrAbsoluteZ = relatives.contains((Object)Relative.Z) ? z - victim.getZ() : z;
        float relativeOrAbsoluteYRot = relatives.contains((Object)Relative.Y_ROT) ? yRot - victim.getYRot() : yRot;
        float relativeOrAbsoluteXRot = relatives.contains((Object)Relative.X_ROT) ? xRot - victim.getXRot() : xRot;
        float newYRot = Mth.wrapDegrees(relativeOrAbsoluteYRot);
        if (!victim.teleportTo(level, relativeOrAbsoluteX, relativeOrAbsoluteY, relativeOrAbsoluteZ, relatives, newYRot, newXRot = Mth.wrapDegrees(relativeOrAbsoluteXRot), true)) {
            return;
        }
        if (lookAt != null) {
            lookAt.perform(source, victim);
        }
        if (!(victim instanceof LivingEntity) || !(living = (LivingEntity)victim).isFallFlying()) {
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            victim.setOnGround(true);
        }
        if (victim instanceof PathfinderMob) {
            PathfinderMob mob = (PathfinderMob)victim;
            mob.getNavigation().stop();
        }
    }
}

