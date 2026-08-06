/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

public class DifficultyCommand {
    private static final DynamicCommandExceptionType ERROR_ALREADY_SAME_DIFFICULTY = new DynamicCommandExceptionType(difficulty -> Component.translatableEscape("commands.difficulty.failure", difficulty));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("difficulty");
        for (Difficulty difficulty : Difficulty.values()) {
            command.then(Commands.literal(difficulty.getSerializedName()).executes(c -> DifficultyCommand.setDifficulty((CommandSourceStack)c.getSource(), difficulty)));
        }
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)command.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(c -> {
            Difficulty difficulty = ((CommandSourceStack)c.getSource()).getLevel().getDifficulty();
            ((CommandSourceStack)c.getSource()).sendSuccess(() -> Component.translatable("commands.difficulty.query", difficulty.getDisplayName()), false);
            return difficulty.getId();
        }));
    }

    public static int setDifficulty(CommandSourceStack source, Difficulty difficulty) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        if (server.getWorldData().getDifficulty() == difficulty) {
            throw ERROR_ALREADY_SAME_DIFFICULTY.create((Object)difficulty.getDisplayName());
        }
        server.setDifficulty(difficulty, true);
        source.sendSuccess(() -> Component.translatable("commands.difficulty.success", difficulty.getDisplayName()), true);
        return 0;
    }
}

