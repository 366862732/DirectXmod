/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  it.unimi.dsi.fastutil.longs.LongSet
 */
package net.minecraft.server.commands;

import com.google.common.base.Joiner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ForceLoadCommand {
    private static final int MAX_CHUNK_LIMIT = 256;
    private static final Dynamic2CommandExceptionType ERROR_TOO_MANY_CHUNKS = new Dynamic2CommandExceptionType((max, amount) -> Component.translatableEscape("commands.forceload.toobig", max, amount));
    private static final Dynamic2CommandExceptionType ERROR_NOT_TICKING = new Dynamic2CommandExceptionType((pos, dimension) -> Component.translatableEscape("commands.forceload.query.failure", pos, dimension));
    private static final SimpleCommandExceptionType ERROR_ALL_ADDED = new SimpleCommandExceptionType((Message)Component.translatable("commands.forceload.added.failure"));
    private static final SimpleCommandExceptionType ERROR_NONE_REMOVED = new SimpleCommandExceptionType((Message)Component.translatable("commands.forceload.removed.failure"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("forceload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("add").then(((RequiredArgumentBuilder)Commands.argument("from", ColumnPosArgument.columnPos()).executes(c -> ForceLoadCommand.changeForceLoad((CommandSourceStack)c.getSource(), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "from"), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "from"), true))).then(Commands.argument("to", ColumnPosArgument.columnPos()).executes(c -> ForceLoadCommand.changeForceLoad((CommandSourceStack)c.getSource(), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "from"), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "to"), true)))))).then(((LiteralArgumentBuilder)Commands.literal("remove").then(((RequiredArgumentBuilder)Commands.argument("from", ColumnPosArgument.columnPos()).executes(c -> ForceLoadCommand.changeForceLoad((CommandSourceStack)c.getSource(), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "from"), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "from"), false))).then(Commands.argument("to", ColumnPosArgument.columnPos()).executes(c -> ForceLoadCommand.changeForceLoad((CommandSourceStack)c.getSource(), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "from"), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "to"), false))))).then(Commands.literal("all").executes(c -> ForceLoadCommand.removeAll((CommandSourceStack)c.getSource()))))).then(((LiteralArgumentBuilder)Commands.literal("query").executes(c -> ForceLoadCommand.listForceLoad((CommandSourceStack)c.getSource()))).then(Commands.argument("pos", ColumnPosArgument.columnPos()).executes(c -> ForceLoadCommand.queryForceLoad((CommandSourceStack)c.getSource(), ColumnPosArgument.getColumnPos((CommandContext<CommandSourceStack>)c, "pos"))))));
    }

    private static int queryForceLoad(CommandSourceStack source, ColumnPos pos) throws CommandSyntaxException {
        ChunkPos chunkPos = pos.toChunkPos();
        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        boolean result = level.getForceLoadedChunks().contains(chunkPos.pack());
        if (result) {
            source.sendSuccess(() -> Component.translatable("commands.forceload.query.success", Component.translationArg(chunkPos), Component.translationArg(dimension.identifier())), false);
            return 1;
        }
        throw ERROR_NOT_TICKING.create((Object)chunkPos, (Object)dimension.identifier());
    }

    private static int listForceLoad(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        LongSet forcedChunks = level.getForceLoadedChunks();
        int chunkCount = forcedChunks.size();
        if (chunkCount > 0) {
            String chunkList = Joiner.on((String)", ").join(forcedChunks.stream().sorted().map(ChunkPos::unpack).map(ChunkPos::toString).iterator());
            if (chunkCount == 1) {
                source.sendSuccess(() -> Component.translatable("commands.forceload.list.single", Component.translationArg(dimension.identifier()), chunkList), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.forceload.list.multiple", chunkCount, Component.translationArg(dimension.identifier()), chunkList), false);
            }
        } else {
            source.sendFailure(Component.translatable("commands.forceload.added.none", Component.translationArg(dimension.identifier())));
        }
        return chunkCount;
    }

    private static int removeAll(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        LongSet forcedChunks = level.getForceLoadedChunks();
        forcedChunks.forEach(chunk -> level.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false));
        source.sendSuccess(() -> Component.translatable("commands.forceload.removed.all", Component.translationArg(dimension.identifier())), true);
        return 0;
    }

    private static int changeForceLoad(CommandSourceStack source, ColumnPos from, ColumnPos to, boolean add) throws CommandSyntaxException {
        int maxChunkZ;
        int minX = Math.min(from.x(), to.x());
        int minZ = Math.min(from.z(), to.z());
        int maxX = Math.max(from.x(), to.x());
        int maxZ = Math.max(from.z(), to.z());
        if (minX < -30000000 || minZ < -30000000 || maxX >= 30000000 || maxZ >= 30000000) {
            throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
        }
        int minChunkX = SectionPos.blockToSectionCoord(minX);
        int minChunkZ = SectionPos.blockToSectionCoord(minZ);
        int maxChunkX = SectionPos.blockToSectionCoord(maxX);
        long chunkCount = ((long)(maxChunkX - minChunkX) + 1L) * ((long)((maxChunkZ = SectionPos.blockToSectionCoord(maxZ)) - minChunkZ) + 1L);
        if (chunkCount > 256L) {
            throw ERROR_TOO_MANY_CHUNKS.create((Object)256, (Object)chunkCount);
        }
        ServerLevel level = source.getLevel();
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos firstChanged = null;
        int changedCount = 0;
        for (int x = minChunkX; x <= maxChunkX; ++x) {
            for (int z = minChunkZ; z <= maxChunkZ; ++z) {
                boolean changed = level.setChunkForced(x, z, add);
                if (!changed) continue;
                ++changedCount;
                if (firstChanged != null) continue;
                firstChanged = new ChunkPos(x, z);
            }
        }
        ChunkPos finalFirstChanged = firstChanged;
        int changedChunks = changedCount;
        if (changedChunks == 0) {
            throw (add ? ERROR_ALL_ADDED : ERROR_NONE_REMOVED).create();
        }
        if (changedChunks == 1) {
            source.sendSuccess(() -> Component.translatable("commands.forceload." + (add ? "added" : "removed") + ".single", Component.translationArg(finalFirstChanged), Component.translationArg(dimension.identifier())), true);
        } else {
            ChunkPos min = new ChunkPos(minChunkX, minChunkZ);
            ChunkPos max = new ChunkPos(maxChunkX, maxChunkZ);
            source.sendSuccess(() -> Component.translatable("commands.forceload." + (add ? "added" : "removed") + ".multiple", changedChunks, Component.translationArg(dimension.identifier()), Component.translationArg(min), Component.translationArg(max)), true);
        }
        return changedChunks;
    }
}

