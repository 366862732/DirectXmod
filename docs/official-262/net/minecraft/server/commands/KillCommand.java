/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class KillCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("kill").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(c -> KillCommand.kill((CommandSourceStack)c.getSource(), (Collection<? extends Entity>)ImmutableList.of((Object)((CommandSourceStack)c.getSource()).getEntityOrException())))).then(Commands.argument("targets", EntityArgument.entities()).executes(c -> KillCommand.kill((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets")))));
    }

    private static int kill(CommandSourceStack source, Collection<? extends Entity> victims) {
        for (Entity entity : victims) {
            entity.kill(source.getLevel());
        }
        if (victims.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.kill.success.single", ((Entity)victims.iterator().next()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.kill.success.multiple", victims.size()), true);
        }
        return victims.size();
    }
}

