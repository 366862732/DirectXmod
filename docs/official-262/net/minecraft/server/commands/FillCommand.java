/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.InCommandFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public class FillCommand {
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((max, count) -> Component.translatableEscape("commands.fill.toobig", max, count));
    private static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.fill.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fill").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).then(FillCommand.wrapWithMode(context, Commands.argument("block", BlockStateArgument.block(context)), c -> BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "from"), c -> BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "to"), c -> BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), c -> null).then(((LiteralArgumentBuilder)Commands.literal("replace").executes(c -> FillCommand.fillBlocks((CommandSourceStack)c.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "from"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "to")), BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), Mode.REPLACE, null, false))).then(FillCommand.wrapWithMode(context, Commands.argument("filter", BlockPredicateArgument.blockPredicate(context)), c -> BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "from"), c -> BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "to"), c -> BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), c -> BlockPredicateArgument.getBlockPredicate((CommandContext<CommandSourceStack>)c, "filter")))).then(Commands.literal("keep").executes(c -> FillCommand.fillBlocks((CommandSourceStack)c.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "from"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "to")), BlockStateArgument.getBlock((CommandContext<CommandSourceStack>)c, "block"), Mode.REPLACE, b -> b.getLevel().isEmptyBlock(b.getPos()), false)))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapWithMode(CommandBuildContext context, ArgumentBuilder<CommandSourceStack, ?> builder, InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> from, InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> to, InCommandFunction<CommandContext<CommandSourceStack>, BlockInput> block, NullableCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> filter) {
        return builder.executes(c -> FillCommand.fillBlocks((CommandSourceStack)c.getSource(), BoundingBox.fromCorners((Vec3i)from.apply(c), (Vec3i)to.apply(c)), (BlockInput)block.apply(c), Mode.REPLACE, (Predicate)filter.apply(c), false)).then(Commands.literal("outline").executes(c -> FillCommand.fillBlocks((CommandSourceStack)c.getSource(), BoundingBox.fromCorners((Vec3i)from.apply(c), (Vec3i)to.apply(c)), (BlockInput)block.apply(c), Mode.OUTLINE, (Predicate)filter.apply(c), false))).then(Commands.literal("hollow").executes(c -> FillCommand.fillBlocks((CommandSourceStack)c.getSource(), BoundingBox.fromCorners((Vec3i)from.apply(c), (Vec3i)to.apply(c)), (BlockInput)block.apply(c), Mode.HOLLOW, (Predicate)filter.apply(c), false))).then(Commands.literal("destroy").executes(c -> FillCommand.fillBlocks((CommandSourceStack)c.getSource(), BoundingBox.fromCorners((Vec3i)from.apply(c), (Vec3i)to.apply(c)), (BlockInput)block.apply(c), Mode.DESTROY, (Predicate)filter.apply(c), false))).then(Commands.literal("strict").executes(c -> FillCommand.fillBlocks((CommandSourceStack)c.getSource(), BoundingBox.fromCorners((Vec3i)from.apply(c), (Vec3i)to.apply(c)), (BlockInput)block.apply(c), Mode.REPLACE, (Predicate)filter.apply(c), true)));
    }

    private static int fillBlocks(CommandSourceStack source, BoundingBox region, BlockInput target, Mode mode, @Nullable Predicate<BlockInWorld> predicate, boolean strict) throws CommandSyntaxException {
        record UpdatedPosition(BlockPos pos, BlockState oldState) {
        }
        int limit;
        long area = (long)region.getXSpan() * (long)region.getYSpan() * (long)region.getZSpan();
        if (area > (long)(limit = source.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS).intValue())) {
            throw ERROR_AREA_TOO_LARGE.create((Object)limit, (Object)area);
        }
        ArrayList updatePositions = Lists.newArrayList();
        ServerLevel level = source.getLevel();
        if (level.isDebug()) {
            throw ERROR_FAILED.create();
        }
        int count = 0;
        for (BlockPos blockPos : BlockPos.betweenClosed(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ())) {
            BlockInput block;
            if (predicate != null && !predicate.test(new BlockInWorld(level, blockPos, true))) continue;
            BlockState oldState = level.getBlockState(blockPos);
            boolean affected = false;
            if (mode.affector.affect(level, blockPos)) {
                affected = true;
            }
            if ((block = mode.filter.filter(region, blockPos, target, level)) == null) {
                if (!affected) continue;
                ++count;
                continue;
            }
            if (!block.place(level, blockPos, 2 | (strict ? 816 : 256))) {
                if (!affected) continue;
                ++count;
                continue;
            }
            if (!strict) {
                updatePositions.add(new UpdatedPosition(blockPos.immutable(), oldState));
            }
            ++count;
        }
        for (UpdatedPosition updatedPosition : updatePositions) {
            level.updateNeighboursOnBlockSet(updatedPosition.pos, updatedPosition.oldState);
        }
        if (count == 0) {
            throw ERROR_FAILED.create();
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.translatable("commands.fill.success", finalCount), true);
        return count;
    }

    @FunctionalInterface
    private static interface NullableCommandFunction<T, R> {
        public @Nullable R apply(T var1) throws CommandSyntaxException;
    }

    private static enum Mode {
        REPLACE(Affector.NOOP, Filter.NOOP),
        OUTLINE(Affector.NOOP, (r, p, b, l) -> {
            if (p.getX() == r.minX() || p.getX() == r.maxX() || p.getY() == r.minY() || p.getY() == r.maxY() || p.getZ() == r.minZ() || p.getZ() == r.maxZ()) {
                return b;
            }
            return null;
        }),
        HOLLOW(Affector.NOOP, (r, p, b, l) -> {
            if (p.getX() == r.minX() || p.getX() == r.maxX() || p.getY() == r.minY() || p.getY() == r.maxY() || p.getZ() == r.minZ() || p.getZ() == r.maxZ()) {
                return b;
            }
            return HOLLOW_CORE;
        }),
        DESTROY((l, p) -> l.destroyBlock(p, true), Filter.NOOP);

        public final Filter filter;
        public final Affector affector;

        private Mode(Affector affector, Filter filter) {
            this.affector = affector;
            this.filter = filter;
        }
    }

    @FunctionalInterface
    public static interface Affector {
        public static final Affector NOOP = (l, p) -> false;

        public boolean affect(ServerLevel var1, BlockPos var2);
    }

    @FunctionalInterface
    public static interface Filter {
        public static final Filter NOOP = (r, p, b, l) -> b;

        public @Nullable BlockInput filter(BoundingBox var1, BlockPos var2, BlockInput var3, ServerLevel var4);
    }
}

