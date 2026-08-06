/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class UnpublishCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("unpublish").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))).executes(c -> UnpublishCommand.unpublish((CommandSourceStack)c.getSource())));
    }

    private static int unpublish(CommandSourceStack source) {
        if (source.getServer().unpublishServer()) {
            source.sendSuccess(() -> Component.translatable("commands.unpublish.success"), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.unpublish.notPublished"));
        return 0;
    }
}

