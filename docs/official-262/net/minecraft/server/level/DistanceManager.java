/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ByteMaps
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2IntMap
 *  it.unimi.dsi.fastutil.longs.Long2IntMaps
 *  it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongConsumer
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  it.unimi.dsi.fastutil.objects.ObjectSet
 *  it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.LoadingChunkTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.SimulationChunkTracker;
import net.minecraft.server.level.ThrottlingChunkTaskDispatcher;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.TriState;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class DistanceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
    private final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap();
    private final LoadingChunkTracker loadingChunkTracker;
    private final SimulationChunkTracker simulationChunkTracker;
    private final TicketStorage ticketStorage;
    private final FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new FixedPlayerDistanceChunkTracker(this, 8);
    private final PlayerTicketTracker playerTicketManager = new PlayerTicketTracker(this, 32);
    protected final Set<ChunkHolder> chunksToUpdateFutures = new ReferenceOpenHashSet();
    private final ThrottlingChunkTaskDispatcher ticketDispatcher;
    private final LongSet ticketsToRelease = new LongOpenHashSet();
    private final Executor mainThreadExecutor;
    private int simulationDistance = 10;

    protected DistanceManager(TicketStorage ticketStorage, Executor executor, Executor mainThreadExecutor) {
        this.ticketStorage = ticketStorage;
        this.loadingChunkTracker = new LoadingChunkTracker(this, ticketStorage);
        this.simulationChunkTracker = new SimulationChunkTracker(ticketStorage);
        TaskScheduler<Runnable> mainThreadTaskScheduler = TaskScheduler.wrapExecutor("player ticket throttler", mainThreadExecutor);
        this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(mainThreadTaskScheduler, executor, 4);
        this.mainThreadExecutor = mainThreadExecutor;
    }

    protected abstract boolean isChunkToRemove(long var1);

    protected abstract @Nullable ChunkHolder getChunk(long var1);

    protected abstract @Nullable ChunkHolder updateChunkScheduling(long var1, int var3, @Nullable ChunkHolder var4, int var5);

    public boolean runAllUpdates(ChunkMap scheduler) {
        boolean updated;
        this.naturalSpawnChunkCounter.runAllUpdates();
        this.simulationChunkTracker.runAllUpdates();
        this.playerTicketManager.runAllUpdates();
        int updates = Integer.MAX_VALUE - this.loadingChunkTracker.runDistanceUpdates(Integer.MAX_VALUE);
        boolean bl = updated = updates != 0;
        if (updated && SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("DMU {}", (Object)updates);
        }
        if (!this.chunksToUpdateFutures.isEmpty()) {
            for (ChunkHolder chunksToUpdateFuture : this.chunksToUpdateFutures) {
                chunksToUpdateFuture.updateHighestAllowedStatus(scheduler);
            }
            for (ChunkHolder chunkHolder : this.chunksToUpdateFutures) {
                chunkHolder.updateFutures(scheduler, this.mainThreadExecutor);
            }
            this.chunksToUpdateFutures.clear();
            return true;
        }
        if (!this.ticketsToRelease.isEmpty()) {
            LongIterator iterator = this.ticketsToRelease.iterator();
            while (iterator.hasNext()) {
                long pos = iterator.nextLong();
                if (!this.ticketStorage.getTickets(pos).stream().anyMatch(t -> t.getType() == TicketType.PLAYER_LOADING)) continue;
                ChunkHolder chunk = scheduler.getUpdatingChunkIfPresent(pos);
                if (chunk == null) {
                    throw new IllegalStateException();
                }
                CompletableFuture<ChunkResult<LevelChunk>> future = chunk.getEntityTickingChunkFuture();
                future.thenAccept(c -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(pos, () -> {}, false)));
            }
            this.ticketsToRelease.clear();
        }
        return updated;
    }

    public void addPlayer(SectionPos pos, ServerPlayer player) {
        ChunkPos chunk = pos.chunk();
        long chunkPos = chunk.pack();
        ((ObjectSet)this.playersPerChunk.computeIfAbsent(chunkPos, k -> new ObjectOpenHashSet())).add((Object)player);
        this.naturalSpawnChunkCounter.update(chunkPos, 0, true);
        this.playerTicketManager.update(chunkPos, 0, true);
        this.ticketStorage.addTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunk);
    }

    public void removePlayer(SectionPos pos, ServerPlayer player) {
        ChunkPos chunk = pos.chunk();
        long chunkPos = chunk.pack();
        ObjectSet chunkPlayers = (ObjectSet)this.playersPerChunk.get(chunkPos);
        chunkPlayers.remove((Object)player);
        if (chunkPlayers.isEmpty()) {
            this.playersPerChunk.remove(chunkPos);
            this.naturalSpawnChunkCounter.update(chunkPos, Integer.MAX_VALUE, false);
            this.playerTicketManager.update(chunkPos, Integer.MAX_VALUE, false);
            this.ticketStorage.removeTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunk);
        }
    }

    private int getPlayerTicketLevel() {
        return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
    }

    public boolean inEntityTickingRange(long key) {
        return ChunkLevel.isEntityTicking(this.simulationChunkTracker.getLevel(key));
    }

    public boolean inBlockTickingRange(long key) {
        return ChunkLevel.isBlockTicking(this.simulationChunkTracker.getLevel(key));
    }

    public int getChunkLevel(long key, boolean simulation) {
        if (simulation) {
            return this.simulationChunkTracker.getLevel(key);
        }
        return this.loadingChunkTracker.getLevel(key);
    }

    protected void updatePlayerTickets(int viewDistance) {
        this.playerTicketManager.updateViewDistance(viewDistance);
    }

    public void updateSimulationDistance(int newDistance) {
        if (newDistance != this.simulationDistance) {
            this.simulationDistance = newDistance;
            this.ticketStorage.replaceTicketLevelOfType(this.getPlayerTicketLevel(), TicketType.PLAYER_SIMULATION);
        }
    }

    public int getNaturalSpawnChunkCount() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.size();
    }

    public TriState hasPlayersNearby(long pos) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        int distance = this.naturalSpawnChunkCounter.getLevel(pos);
        if (distance <= NaturalSpawner.INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK) {
            return TriState.TRUE;
        }
        if (distance > 8) {
            return TriState.FALSE;
        }
        return TriState.DEFAULT;
    }

    public void forEachEntityTickingChunk(LongConsumer consumer) {
        for (Long2ByteMap.Entry entry : Long2ByteMaps.fastIterable((Long2ByteMap)this.simulationChunkTracker.chunks)) {
            byte level = entry.getByteValue();
            long key = entry.getLongKey();
            if (!ChunkLevel.isEntityTicking(level)) continue;
            consumer.accept(key);
        }
    }

    public LongIterator getSpawnCandidateChunks() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.keySet().iterator();
    }

    public String getDebugStatus() {
        return this.ticketDispatcher.getDebugStatus();
    }

    public boolean hasTickets() {
        return this.ticketStorage.hasTickets();
    }

    private class FixedPlayerDistanceChunkTracker
    extends ChunkTracker {
        protected final Long2ByteMap chunks;
        protected final int maxDistance;
        final /* synthetic */ DistanceManager this$0;

        protected FixedPlayerDistanceChunkTracker(DistanceManager distanceManager, int maxDistance) {
            DistanceManager distanceManager2 = distanceManager;
            Objects.requireNonNull(distanceManager2);
            this.this$0 = distanceManager2;
            super(maxDistance + 2, 16, 256);
            this.chunks = new Long2ByteOpenHashMap();
            this.maxDistance = maxDistance;
            this.chunks.defaultReturnValue((byte)(maxDistance + 2));
        }

        @Override
        protected int getLevel(long node) {
            return this.chunks.get(node);
        }

        @Override
        protected void setLevel(long node, int level) {
            byte oldLevel = level > this.maxDistance ? this.chunks.remove(node) : this.chunks.put(node, (byte)level);
            this.onLevelChange(node, oldLevel, level);
        }

        protected void onLevelChange(long node, int oldLevel, int level) {
        }

        @Override
        protected int getLevelFromSource(long to) {
            return this.havePlayer(to) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(long chunkPos) {
            ObjectSet players = (ObjectSet)this.this$0.playersPerChunk.get(chunkPos);
            return players != null && !players.isEmpty();
        }

        public void runAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }
    }

    private class PlayerTicketTracker
    extends FixedPlayerDistanceChunkTracker {
        private int viewDistance;
        private final Long2IntMap queueLevels;
        private final LongSet toUpdate;
        final /* synthetic */ DistanceManager this$0;

        protected PlayerTicketTracker(DistanceManager distanceManager, int maxDistance) {
            DistanceManager distanceManager2 = distanceManager;
            Objects.requireNonNull(distanceManager2);
            this.this$0 = distanceManager2;
            super(distanceManager, maxDistance);
            this.queueLevels = Long2IntMaps.synchronize((Long2IntMap)new Long2IntOpenHashMap());
            this.toUpdate = new LongOpenHashSet();
            this.viewDistance = 0;
            this.queueLevels.defaultReturnValue(maxDistance + 2);
        }

        @Override
        protected void onLevelChange(long node, int oldLevel, int level) {
            this.toUpdate.add(node);
        }

        public void updateViewDistance(int viewDistance) {
            for (Long2ByteMap.Entry entry : this.chunks.long2ByteEntrySet()) {
                byte level = entry.getByteValue();
                long key = entry.getLongKey();
                this.onLevelChange(key, level, this.haveTicketFor(level), level <= viewDistance);
            }
            this.viewDistance = viewDistance;
        }

        private void onLevelChange(long key, int level, boolean saw, boolean sees) {
            if (saw != sees) {
                Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, PLAYER_TICKET_LEVEL);
                if (sees) {
                    this.this$0.ticketDispatcher.submit(() -> this.this$0.mainThreadExecutor.execute(() -> {
                        if (this.haveTicketFor(this.getLevel(key))) {
                            this.this$0.ticketStorage.addTicket(key, ticket);
                            this.this$0.ticketsToRelease.add(key);
                        } else {
                            this.this$0.ticketDispatcher.release(key, () -> {}, false);
                        }
                    }), key, () -> level);
                } else {
                    this.this$0.ticketDispatcher.release(key, () -> this.this$0.mainThreadExecutor.execute(() -> this.this$0.ticketStorage.removeTicket(key, ticket)), true);
                }
            }
        }

        @Override
        public void runAllUpdates() {
            super.runAllUpdates();
            if (!this.toUpdate.isEmpty()) {
                LongIterator iterator = this.toUpdate.iterator();
                while (iterator.hasNext()) {
                    int level;
                    long node = iterator.nextLong();
                    int oldLevel = this.queueLevels.get(node);
                    if (oldLevel == (level = this.getLevel(node))) continue;
                    this.this$0.ticketDispatcher.onLevelChange(ChunkPos.unpack(node), () -> this.queueLevels.get(node), level, l -> {
                        if (l >= this.queueLevels.defaultReturnValue()) {
                            this.queueLevels.remove(node);
                        } else {
                            this.queueLevels.put(node, l);
                        }
                    });
                    this.onLevelChange(node, level, this.haveTicketFor(oldLevel), this.haveTicketFor(level));
                }
                this.toUpdate.clear();
            }
        }

        private boolean haveTicketFor(int level) {
            return level <= this.viewDistance;
        }
    }
}

