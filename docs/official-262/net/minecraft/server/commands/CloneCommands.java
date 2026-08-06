/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.InCommandFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.ticks.LevelTicks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CloneCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType((Message)Component.translatable("commands.clone.overlap"));
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((max, count) -> Component.translatableEscape("commands.clone.toobig", max, count));
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.clone.failed"));
    public static final Predicate<BlockInWorld> FILTER_AIR = b -> !b.getState().isAir();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(CloneCommands.beginEndDestinationAndModeSuffix(context, c -> ((CommandSourceStack)c.getSource()).getLevel()))).then(Commands.literal("from").then(Commands.argument("sourceDimension", DimensionArgument.dimension()).then(CloneCommands.beginEndDestinationAndModeSuffix(context, c -> DimensionArgument.getDimension((CommandContext<CommandSourceStack>)c, "sourceDimension"))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(CommandBuildContext context, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> fromDimension) {
        return Commands.argument("begin", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos()).then(CloneCommands.destinationAndStrictSuffix(context, fromDimension, c -> ((CommandSourceStack)c.getSource()).getLevel()))).then(Commands.literal("to").then(Commands.argument("targetDimension", DimensionArgument.dimension()).then(CloneCommands.destinationAndStrictSuffix(context, fromDimension, c -> DimensionArgument.getDimension((CommandContext<CommandSourceStack>)c, "targetDimension"))))));
    }

    private static DimensionAndPosition getLoadedDimensionAndPosition(CommandContext<CommandSourceStack> context, ServerLevel level, String positionArgument) throws CommandSyntaxException {
        BlockPos blockPos = BlockPosArgument.getLoadedBlockPos(context, level, positionArgument);
        return new DimensionAndPosition(level, blockPos);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> destinationAndStrictSuffix(CommandBuildContext context, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> fromDimension, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> toDimension) {
        InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> beginPos = c -> CloneCommands.getLoadedDimensionAndPosition((CommandContext<CommandSourceStack>)c, (ServerLevel)fromDimension.apply((CommandContext<CommandSourceStack>)c), "begin");
        InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> endPos = c -> CloneCommands.getLoadedDimensionAndPosition((CommandContext<CommandSourceStack>)c, (ServerLevel)fromDimension.apply((CommandContext<CommandSourceStack>)c), "end");
        InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> destinationPos = c -> CloneCommands.getLoadedDimensionAndPosition((CommandContext<CommandSourceStack>)c, (ServerLevel)toDimension.apply((CommandContext<CommandSourceStack>)c), "destination");
        return CloneCommands.modeSuffix(context, beginPos, endPos, destinationPos, false, Commands.argument("destination", BlockPosArgument.blockPos())).then(CloneCommands.modeSuffix(context, beginPos, endPos, destinationPos, true, Commands.literal("strict")));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> modeSuffix(CommandBuildContext context, InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> beginPos, InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> endPos, InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> destinationPos, boolean strict, ArgumentBuilder<CommandSourceStack, ?> builder) {
        return builder.executes(c -> CloneCommands.clone((CommandSourceStack)c.getSource(), (DimensionAndPosition)beginPos.apply(c), (DimensionAndPosition)endPos.apply(c), (DimensionAndPosition)destinationPos.apply(c), b -> true, Mode.NORMAL, strict)).then(CloneCommands.wrapWithCloneMode(beginPos, endPos, destinationPos, c -> b -> true, strict, Commands.literal("replace"))).then(CloneCommands.wrapWithCloneMode(beginPos, endPos, destinationPos, c -> FILTER_AIR, strict, Commands.literal("masked"))).then(Commands.literal("filtered").then(CloneCommands.wrapWithCloneMode(beginPos, endPos, destinationPos, c -> BlockPredicateArgument.getBlockPredicate((CommandContext<CommandSourceStack>)c, "filter"), strict, Commands.argument("filter", BlockPredicateArgument.blockPredicate(context)))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> beginPos, InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> endPos, InCommandFunction<CommandContext<CommandSourceStack>, DimensionAndPosition> destinationPos, InCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> filter, boolean strict, ArgumentBuilder<CommandSourceStack, ?> builder) {
        return builder.executes(c -> CloneCommands.clone((CommandSourceStack)c.getSource(), (DimensionAndPosition)beginPos.apply(c), (DimensionAndPosition)endPos.apply(c), (DimensionAndPosition)destinationPos.apply(c), (Predicate)filter.apply(c), Mode.NORMAL, strict)).then(Commands.literal("force").executes(c -> CloneCommands.clone((CommandSourceStack)c.getSource(), (DimensionAndPosition)beginPos.apply(c), (DimensionAndPosition)endPos.apply(c), (DimensionAndPosition)destinationPos.apply(c), (Predicate)filter.apply(c), Mode.FORCE, strict))).then(Commands.literal("move").executes(c -> CloneCommands.clone((CommandSourceStack)c.getSource(), (DimensionAndPosition)beginPos.apply(c), (DimensionAndPosition)endPos.apply(c), (DimensionAndPosition)destinationPos.apply(c), (Predicate)filter.apply(c), Mode.MOVE, strict))).then(Commands.literal("normal").executes(c -> CloneCommands.clone((CommandSourceStack)c.getSource(), (DimensionAndPosition)beginPos.apply(c), (DimensionAndPosition)endPos.apply(c), (DimensionAndPosition)destinationPos.apply(c), (Predicate)filter.apply(c), Mode.NORMAL, strict)));
    }

    private static int clone(CommandSourceStack source, DimensionAndPosition startPosAndDimension, DimensionAndPosition endPosAndDimension, DimensionAndPosition destPosAndDimension, Predicate<BlockInWorld> predicate, Mode mode, boolean strict) throws CommandSyntaxException {
        int limit;
        BlockPos startPos = startPosAndDimension.position();
        BlockPos endPos = endPosAndDimension.position();
        BoundingBox from = BoundingBox.fromCorners(startPos, endPos);
        BlockPos destPos = destPosAndDimension.position();
        BlockPos destEndPos = destPos.offset(from.getLength());
        BoundingBox destination = BoundingBox.fromCorners(destPos, destEndPos);
        ServerLevel fromDimension = startPosAndDimension.dimension();
        ServerLevel toDimension = destPosAndDimension.dimension();
        if (!mode.canOverlap() && fromDimension == toDimension && destination.intersects(from)) {
            throw ERROR_OVERLAP.create();
        }
        long area = (long)from.getXSpan() * (long)from.getYSpan() * (long)from.getZSpan();
        if (area > (long)(limit = source.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS).intValue())) {
            throw ERROR_AREA_TOO_LARGE.create((Object)limit, (Object)area);
        }
        if (!fromDimension.hasChunksAt(startPos, endPos) || !toDimension.hasChunksAt(destPos, destEndPos)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }
        if (toDimension.isDebug()) {
            throw ERROR_FAILED.create();
        }
        ArrayList solidList = Lists.newArrayList();
        ArrayList blockEntitiesList = Lists.newArrayList();
        ArrayList otherBlocksList = Lists.newArrayList();
        LinkedList clearBlocksList = Lists.newLinkedList();
        int count = 0;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
            BlockPos offset = new BlockPos(destination.minX() - from.minX(), destination.minY() - from.minY(), destination.minZ() - from.minZ());
            for (int z = from.minZ(); z <= from.maxZ(); ++z) {
                for (int y = from.minY(); y <= from.maxY(); ++y) {
                    for (int x = from.minX(); x <= from.maxX(); ++x) {
                        BlockPos sourcePos = new BlockPos(x, y, z);
                        BlockPos destinationPos = sourcePos.offset(offset);
                        BlockInWorld block = new BlockInWorld(fromDimension, sourcePos, false);
                        BlockState blockState = block.getState();
                        if (!predicate.test(block)) continue;
                        BlockEntity blockEntity = fromDimension.getBlockEntity(sourcePos);
                        if (blockEntity != null) {
                            TagValueOutput output = TagValueOutput.createWithContext(reporter.forChild(blockEntity.problemPath()), source.registryAccess());
                            blockEntity.saveCustomOnly(output);
                            CloneBlockEntityInfo blockEntityInfo = new CloneBlockEntityInfo(output.buildResult(), blockEntity.components());
                            blockEntitiesList.add(new CloneBlockInfo(destinationPos, blockState, blockEntityInfo, toDimension.getBlockState(destinationPos)));
                            clearBlocksList.addLast(sourcePos);
                            continue;
                        }
                        if (blockState.isSolidRender() || blockState.isCollisionShapeFullBlock(fromDimension, sourcePos)) {
                            solidList.add(new CloneBlockInfo(destinationPos, blockState, null, toDimension.getBlockState(destinationPos)));
                            clearBlocksList.addLast(sourcePos);
                            continue;
                        }
                        otherBlocksList.add(new CloneBlockInfo(destinationPos, blockState, null, toDimension.getBlockState(destinationPos)));
                        clearBlocksList.addFirst(sourcePos);
                    }
                }
            }
            int defaultUpdateFlags = 2 | (strict ? 816 : 0);
            if (mode == Mode.MOVE) {
                for (BlockPos pos : clearBlocksList) {
                    fromDimension.setBlock(pos, Blocks.BARRIER.defaultBlockState(), defaultUpdateFlags | 0x330);
                }
                int standardUpdateFlags = strict ? defaultUpdateFlags : 3;
                for (BlockPos pos : clearBlocksList) {
                    fromDimension.setBlock(pos, Blocks.AIR.defaultBlockState(), standardUpdateFlags);
                }
            }
            ArrayList blockInfoList = Lists.newArrayList();
            blockInfoList.addAll(solidList);
            blockInfoList.addAll(blockEntitiesList);
            blockInfoList.addAll(otherBlocksList);
            List reverse = Lists.reverse((List)blockInfoList);
            for (CloneBlockInfo cloneInfo : reverse) {
                toDimension.setBlock(cloneInfo.pos, Blocks.BARRIER.defaultBlockState(), defaultUpdateFlags | 0x330);
            }
            for (CloneBlockInfo cloneInfo : blockInfoList) {
                if (!toDimension.setBlock(cloneInfo.pos, cloneInfo.state, defaultUpdateFlags)) continue;
                ++count;
            }
            for (CloneBlockInfo cloneInfo : blockEntitiesList) {
                BlockEntity newBlockEntity = toDimension.getBlockEntity(cloneInfo.pos);
                if (cloneInfo.blockEntityInfo != null && newBlockEntity != null) {
                    newBlockEntity.loadCustomOnly(TagValueInput.create(reporter.forChild(newBlockEntity.problemPath()), (HolderLookup.Provider)toDimension.registryAccess(), cloneInfo.blockEntityInfo.tag));
                    newBlockEntity.setComponents(cloneInfo.blockEntityInfo.components);
                    newBlockEntity.setChanged();
                }
                toDimension.setBlock(cloneInfo.pos, cloneInfo.state, defaultUpdateFlags);
            }
            if (!strict) {
                for (CloneBlockInfo cloneInfo : reverse) {
                    toDimension.updateNeighboursOnBlockSet(cloneInfo.pos, cloneInfo.previousStateAtDestination);
                }
            }
            ((LevelTicks)toDimension.getBlockTicks()).copyAreaFrom(fromDimension.getBlockTicks(), from, offset);
        }
        if (count == 0) {
            throw ERROR_FAILED.create();
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.translatable("commands.clone.success", finalCount), true);
        return count;
    }

    private record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
    }

    private static enum Mode {
        FORCE(true),
        MOVE(true),
        NORMAL(false);

        private final boolean canOverlap;

        private Mode(boolean canOverlap) {
            this.canOverlap = canOverlap;
        }

        public boolean canOverlap() {
            return this.canOverlap;
        }
    }

    private record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
    }

    private record CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CloneBlockEntityInfo blockEntityInfo, BlockState previousStateAtDestination) {
    }
}

