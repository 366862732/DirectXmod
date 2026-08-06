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
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class SwingCommand {
    private static final SimpleCommandExceptionType ERROR_NO_LIVING_ENTITY = new SimpleCommandExceptionType((Message)Component.translatable("commands.swing.failed.notliving"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("swing").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(c -> SwingCommand.swing((CommandSourceStack)c.getSource(), List.of(((CommandSourceStack)c.getSource()).getEntityOrException()), InteractionHand.MAIN_HAND))).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities()).executes(c -> SwingCommand.swing((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), InteractionHand.MAIN_HAND))).then(Commands.literal("mainhand").executes(c -> SwingCommand.swing((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), InteractionHand.MAIN_HAND)))).then(Commands.literal("offhand").executes(c -> SwingCommand.swing((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), InteractionHand.OFF_HAND)))));
    }

    private static int swing(CommandSourceStack source, Collection<? extends Entity> targets, InteractionHand hand) throws CommandSyntaxException {
        int livingEntitiesCount = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity livingEntity = (LivingEntity)entity;
            livingEntity.swing(hand, true);
            ++livingEntitiesCount;
        }
        if (livingEntitiesCount == 0) {
            throw ERROR_NO_LIVING_ENTITY.create();
        }
        if (livingEntitiesCount == 1) {
            source.sendSuccess(() -> Component.translatable("commands.swing.success.single", ((Entity)targets.iterator().next()).getDisplayName()), true);
        } else {
            int count = livingEntitiesCount;
            source.sendSuccess(() -> Component.translatable("commands.swing.success.multiple", count), true);
        }
        return livingEntitiesCount;
    }
}

