/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

public class DebugPathCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_MOB = new SimpleCommandExceptionType((Message)Component.literal("Source is not a mob"));
    private static final SimpleCommandExceptionType ERROR_NO_PATH = new SimpleCommandExceptionType((Message)Component.literal("Path not found"));
    private static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType((Message)Component.literal("Target not reached"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debugpath").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("to", BlockPosArgument.blockPos()).executes(c -> DebugPathCommand.fillBlocks((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "to")))));
    }

    private static int fillBlocks(CommandSourceStack source, BlockPos target) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        if (!(entity instanceof Mob)) {
            throw ERROR_NOT_MOB.create();
        }
        Mob mob = (Mob)entity;
        GroundPathNavigation pathNavigation = new GroundPathNavigation(mob, source.getLevel());
        Path path = ((PathNavigation)pathNavigation).createPath(target, 0);
        if (path == null) {
            throw ERROR_NO_PATH.create();
        }
        if (!path.canReach()) {
            throw ERROR_NOT_COMPLETE.create();
        }
        source.sendSuccess(() -> Component.literal("Made path"), true);
        return 1;
    }
}

