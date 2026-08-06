/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Comparators
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.slf4j.Logger
 */
package net.minecraft.server.network;

import com.google.common.collect.Comparators;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.lang.invoke.LambdaMetafactory;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

public class PlayerChunkSender {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float MIN_CHUNKS_PER_TICK = 0.01f;
    public static final float MAX_CHUNKS_PER_TICK = 64.0f;
    private static final float START_CHUNKS_PER_TICK = 9.0f;
    private static final int MAX_UNACKNOWLEDGED_BATCHES = 10;
    private final LongSet pendingChunks = new LongOpenHashSet();
    private final boolean memoryConnection;
    private float desiredChunksPerTick = 9.0f;
    private float batchQuota;
    private int unacknowledgedBatches;
    private int maxUnacknowledgedBatches = 1;

    public PlayerChunkSender(boolean memoryConnection) {
        this.memoryConnection = memoryConnection;
    }

    public void markChunkPendingToSend(LevelChunk chunk) {
        this.pendingChunks.add(chunk.getPos().pack());
    }

    public void dropChunk(ServerPlayer player, ChunkPos pos) {
        if (!this.pendingChunks.remove(pos.pack()) && player.isAlive()) {
            player.connection.send(new ClientboundForgetLevelChunkPacket(pos));
        }
    }

    public void sendNextChunks(ServerPlayer player) {
        if (this.unacknowledgedBatches >= this.maxUnacknowledgedBatches) {
            return;
        }
        float maxBatchSize = Math.max(1.0f, this.desiredChunksPerTick);
        this.batchQuota = Math.min(this.batchQuota + this.desiredChunksPerTick, maxBatchSize);
        if (this.batchQuota < 1.0f) {
            return;
        }
        if (this.pendingChunks.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        List<LevelChunk> chunksToSend = this.collectChunksToSend(chunkMap, player.chunkPosition());
        if (chunksToSend.isEmpty()) {
            return;
        }
        ServerGamePacketListenerImpl connection = player.connection;
        ++this.unacknowledgedBatches;
        connection.send(ClientboundChunkBatchStartPacket.INSTANCE);
        for (LevelChunk chunk : chunksToSend) {
            PlayerChunkSender.sendChunk(connection, level, chunk);
        }
        connection.send(new ClientboundChunkBatchFinishedPacket(chunksToSend.size()));
        this.batchQuota -= (float)chunksToSend.size();
    }

    private static void sendChunk(ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk chunk) {
        connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
        ChunkPos pos = chunk.getPos();
        if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("SEN {}", (Object)pos);
        }
        level.debugSynchronizers().startTrackingChunk(connection.player, chunk.getPos());
    }

    /*
     * Unable to fully structure code
     */
    private List<LevelChunk> collectChunksToSend(ChunkMap chunkMap, ChunkPos playerPos) {
        maxBatchSize = Mth.floor(this.batchQuota);
        if (this.memoryConnection) ** GOTO lbl7
        if (this.pendingChunks.size() <= maxBatchSize) {
lbl7:
            // 2 sources

            chunks = this.pendingChunks.longStream().mapToObj((LongFunction<LevelChunk>)LambdaMetafactory.metafactory(null, null, null, (J)Ljava/lang/Object;, getChunkToSend(long ), (J)Lnet/minecraft/world/level/chunk/LevelChunk;)((ChunkMap)chunkMap)).filter((Predicate<LevelChunk>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, nonNull(java.lang.Object ), (Lnet/minecraft/world/level/chunk/LevelChunk;)Z)()).sorted(Comparator.comparingInt((ToIntFunction<LevelChunk>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)I, lambda$collectChunksToSend$0(net.minecraft.world.level.ChunkPos net.minecraft.world.level.chunk.LevelChunk ), (Lnet/minecraft/world/level/chunk/LevelChunk;)I)((ChunkPos)playerPos))).toList();
        } else {
            chunks = ((List)this.pendingChunks.stream().collect(Comparators.least((int)maxBatchSize, Comparator.comparingInt((ToIntFunction<Long>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)I, distanceSquared(long ), (Ljava/lang/Long;)I)((ChunkPos)playerPos))))).stream().mapToLong((ToLongFunction<Long>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)J, longValue(), (Ljava/lang/Long;)J)()).mapToObj((LongFunction<LevelChunk>)LambdaMetafactory.metafactory(null, null, null, (J)Ljava/lang/Object;, getChunkToSend(long ), (J)Lnet/minecraft/world/level/chunk/LevelChunk;)((ChunkMap)chunkMap)).filter((Predicate<LevelChunk>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, nonNull(java.lang.Object ), (Lnet/minecraft/world/level/chunk/LevelChunk;)Z)()).toList();
        }
        for (LevelChunk chunk : chunks) {
            this.pendingChunks.remove(chunk.getPos().pack());
        }
        return chunks;
    }

    public void onChunkBatchReceivedByClient(float desiredChunksPerTick) {
        --this.unacknowledgedBatches;
        if (this.unacknowledgedBatches < 0) {
            this.unacknowledgedBatches = 0;
        }
        float f = this.desiredChunksPerTick = Double.isNaN(desiredChunksPerTick) ? 0.01f : Mth.clamp(desiredChunksPerTick, 0.01f, 64.0f);
        if (this.unacknowledgedBatches == 0) {
            this.batchQuota = 1.0f;
        }
        this.maxUnacknowledgedBatches = 10;
    }

    public boolean isPending(long pos) {
        return this.pendingChunks.contains(pos);
    }

    private static /* synthetic */ int lambda$collectChunksToSend$0(ChunkPos playerPos, LevelChunk chunk) {
        return playerPos.distanceSquared(chunk.getPos());
    }
}

