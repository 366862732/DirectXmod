/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

public class DialogCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("dialog").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("show").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("dialog", ResourceOrIdArgument.dialog(context)).executes(c -> DialogCommand.showDialog((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"), ResourceOrIdArgument.getDialog((CommandContext<CommandSourceStack>)c, "dialog"))))))).then(Commands.literal("clear").then(Commands.argument("targets", EntityArgument.players()).executes(c -> DialogCommand.clearDialog((CommandSourceStack)c.getSource(), EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "targets"))))));
    }

    private static int showDialog(CommandSourceStack sender, Collection<ServerPlayer> targets, Holder<Dialog> dialog) {
        for (ServerPlayer target : targets) {
            target.openDialog(dialog);
        }
        if (targets.size() == 1) {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.show.single", ((ServerPlayer)targets.iterator().next()).getDisplayName()), true);
        } else {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.show.multiple", targets.size()), true);
        }
        return targets.size();
    }

    private static int clearDialog(CommandSourceStack sender, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            target.connection.send(ClientboundClearDialogPacket.INSTANCE);
        }
        if (targets.size() == 1) {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.clear.single", ((ServerPlayer)targets.iterator().next()).getDisplayName()), true);
        } else {
            sender.sendSuccess(() -> Component.translatable("commands.dialog.clear.multiple", targets.size()), true);
        }
        return targets.size();
    }
}

