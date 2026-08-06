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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

public class DebugMobSpawningCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder base = (LiteralArgumentBuilder)Commands.literal("debugmobspawning").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (MobCategory mobCategory : MobCategory.values()) {
            base.then(Commands.literal(mobCategory.getName()).then(Commands.argument("at", BlockPosArgument.blockPos()).executes(c -> DebugMobSpawningCommand.spawnMobs((CommandSourceStack)c.getSource(), mobCategory, BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "at")))));
        }
        dispatcher.register(base);
    }

    private static int spawnMobs(CommandSourceStack source, MobCategory mobCategory, BlockPos at) {
        NaturalSpawner.spawnCategoryForPosition(mobCategory, source.getLevel(), at);
        return 1;
    }
}

