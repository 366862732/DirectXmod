/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class SaveAllCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.save.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("save-all").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))).executes(c -> SaveAllCommand.saveAll((CommandSourceStack)c.getSource(), false))).then(Commands.literal("flush").executes(c -> SaveAllCommand.saveAll((CommandSourceStack)c.getSource(), true))));
    }

    private static int saveAll(CommandSourceStack source, boolean flush) throws CommandSyntaxException {
        source.sendSuccess(() -> Component.translatable("commands.save.saving"), false);
        MinecraftServer server = source.getServer();
        boolean success = server.saveEverything(true, flush, true);
        if (!success) {
            throw ERROR_FAILED.create();
        }
        source.sendSuccess(() -> Component.translatable("commands.save.success"), true);
        return 1;
    }
}

