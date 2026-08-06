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
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jspecify.annotations.Nullable;

public class SetBlockCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.setblock.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        Predicate<BlockInWorld> filter = b -> b.getLevel().isEmptyBlock(b.getPos());
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setblock").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("pos", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("block", BlockStateArgument.block(context)).executes(c -> SetBlockCommand.setBlock((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), Mode.REPLACE, null, false))).then(Commands.literal("destroy").executes(c -> SetBlockCommand.setBlock((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), Mode.DESTROY, null, false)))).then(Commands.literal("keep").executes(c -> SetBlockCommand.setBlock((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), Mode.REPLACE, filter, false)))).then(Commands.literal("replace").executes(c -> SetBlockCommand.setBlock((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), Mode.REPLACE, null, false)))).then(Commands.literal("strict").executes(c -> SetBlockCommand.setBlock((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), Mode.REPLACE, null, true))))));
    }

    private static int setBlock(CommandSourceStack source, BlockPos pos, BlockInput block, Mode mode, @Nullable Predicate<BlockInWorld> predicate, boolean strict) throws CommandSyntaxException {
        boolean placeNeeded;
        ServerLevel level = source.getLevel();
        if (level.isDebug()) {
            throw ERROR_FAILED.create();
        }
        if (predicate != null && !predicate.test(new BlockInWorld(level, pos, true))) {
            throw ERROR_FAILED.create();
        }
        if (mode == Mode.DESTROY) {
            level.destroyBlock(pos, true);
            placeNeeded = !block.getState().isAir() || !level.getBlockState(pos).isAir();
        } else {
            placeNeeded = true;
        }
        BlockState oldState = level.getBlockState(pos);
        if (placeNeeded && !block.place(level, pos, 2 | (strict ? 816 : 256))) {
            throw ERROR_FAILED.create();
        }
        if (!strict) {
            level.updateNeighboursOnBlockSet(pos, oldState);
        }
        source.sendSuccess(() -> Component.translatable("commands.setblock.success", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    public static enum Mode {
        REPLACE,
        DESTROY;

    }
}

