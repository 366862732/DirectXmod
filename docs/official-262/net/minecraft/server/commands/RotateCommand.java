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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LookAt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;

public class RotateCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("rotate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.entity()).then(Commands.argument("rotation", RotationArgument.rotation()).executes(c -> RotateCommand.rotate((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), RotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rotation"))))).then(((LiteralArgumentBuilder)Commands.literal("facing").then(Commands.literal("entity").then(((RequiredArgumentBuilder)Commands.argument("facingEntity", EntityArgument.entity()).executes(c -> RotateCommand.rotate((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), new LookAt.LookAtEntity(EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "facingEntity"), EntityAnchorArgument.Anchor.FEET)))).then(Commands.argument("facingAnchor", EntityAnchorArgument.anchor()).executes(c -> RotateCommand.rotate((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), new LookAt.LookAtEntity(EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "facingEntity"), EntityAnchorArgument.getAnchor((CommandContext<CommandSourceStack>)c, "facingAnchor")))))))).then(Commands.argument("facingLocation", Vec3Argument.vec3()).executes(c -> RotateCommand.rotate((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), new LookAt.LookAtPosition(Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "facingLocation"))))))));
    }

    private static int rotate(CommandSourceStack source, Entity entity, Coordinates rotation) {
        Vec2 rot = rotation.getRotation(source);
        float relativeOrAbsoluteYRot = rotation.isYRelative() ? rot.y - entity.getYRot() : rot.y;
        float relativeOrAbsoluteXRot = rotation.isXRelative() ? rot.x - entity.getXRot() : rot.x;
        entity.forceSetRotation(relativeOrAbsoluteYRot, rotation.isYRelative(), relativeOrAbsoluteXRot, rotation.isXRelative());
        source.sendSuccess(() -> Component.translatable("commands.rotate.success", entity.getDisplayName()), true);
        return 1;
    }

    private static int rotate(CommandSourceStack source, Entity entity, LookAt facing) {
        facing.perform(source, entity);
        source.sendSuccess(() -> Component.translatable("commands.rotate.success", entity.getDisplayName()), true);
        return 1;
    }
}

