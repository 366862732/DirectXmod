/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.shorts.ShortOpenHashSet
 *  it.unimi.dsi.fastutil.shorts.ShortSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ChunkHolder
extends GenerationChunkHolder {
    public static final ChunkResult<LevelChunk> UNLOADED_LEVEL_CHUNK = ChunkResult.error("Unloaded level chunk");
    private static final CompletableFuture<ChunkResult<LevelChunk>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
    private final LevelHeightAccessor levelHeightAccessor;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private int oldTicketLevel;
    private int ticketLevel;
    private int queueLevel;
    private boolean hasChangedSections;
    private final @Nullable ShortSet[] changedBlocksPerSection;
    private final BitSet blockChangedLightSectionFilter = new BitSet();
    private final BitSet skyChangedLightSectionFilter = new BitSet();
    private final LevelLightEngine lightEngine;
    private final LevelChangeListener onLevelChange;
    private final PlayerProvider playerProvider;
    private boolean wasAccessibleSinceLastSave;
    private CompletableFuture<?> pendingFullStateConfirmation = CompletableFuture.completedFuture(null);
    private CompletableFuture<?> sendSync = CompletableFuture.completedFuture(null);
    private CompletableFuture<?> saveSync = CompletableFuture.completedFuture(null);

    public ChunkHolder(ChunkPos pos, int ticketLevel, LevelHeightAccessor levelHeightAccessor, LevelLightEngine lightEngine, LevelChangeListener onLevelChange, PlayerProvider playerProvider) {
        super(pos);
        this.levelHeightAccessor = levelHeightAccessor;
        this.lightEngine = lightEngine;
        this.onLevelChange = onLevelChange;
        this.playerProvider = playerProvider;
        this.ticketLevel = this.oldTicketLevel = ChunkLevel.MAX_LEVEL + 1;
        this.queueLevel = this.oldTicketLevel;
        this.setTicketLevel(ticketLevel);
        this.changedBlocksPerSection = new ShortSet[levelHeightAccessor.getSectionsCount()];
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getTickingChunkFuture() {
        return this.tickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getEntityTickingChunkFuture() {
        return this.entityTickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture() {
        return this.fullChunkFuture;
    }

    public @Nullable LevelChunk getTickingChunk() {
        return this.getTickingChunkFuture().getNow(UNLOADED_LEVEL_CHUNK).orElse(null);
    }

    public @Nullable LevelChunk getChunkToSend() {
        if (!this.sendSync.isDone()) {
            return null;
        }
        return this.getTickingChunk();
    }

    public CompletableFuture<?> getSendSyncFuture() {
        return this.sendSync;
    }

    public void addSendDependency(CompletableFuture<?> sync) {
        this.sendSync = this.sendSync.isDone() ? sync : this.sendSync.thenCombine(sync, (a, b) -> null);
    }

    public CompletableFuture<?> getSaveSyncFuture() {
        return this.saveSync;
    }

    public boolean isReadyForSaving() {
        return this.saveSync.isDone();
    }

    @Override
    protected void addSaveDependency(CompletableFuture<?> sync) {
        this.saveSync = this.saveSync.isDone() ? sync : this.saveSync.thenCombine(sync, (a, b) -> null);
    }

    public boolean blockChanged(BlockPos pos) {
        LevelChunk chunk = this.getTickingChunk();
        if (chunk == null) {
            return false;
        }
        boolean hadChangedSections = this.hasChangedSections;
        int sectionIndex = this.levelHeightAccessor.getSectionIndex(pos.getY());
        ShortSet changedBlocksInSection = this.changedBlocksPerSection[sectionIndex];
        if (changedBlocksInSection == null) {
            this.hasChangedSections = true;
            this.changedBlocksPerSection[sectionIndex] = changedBlocksInSection = new ShortOpenHashSet();
        }
        changedBlocksInSection.add(SectionPos.sectionRelativePos(pos));
        return !hadChangedSections;
    }

    public boolean sectionLightChanged(LightLayer layer, int chunkY) {
        int index;
        ChunkAccess chunk = this.getChunkIfPresent(ChunkStatus.INITIALIZE_LIGHT);
        if (chunk == null) {
            return false;
        }
        chunk.markUnsaved();
        LevelChunk tickingChunk = this.getTickingChunk();
        if (tickingChunk == null) {
            return false;
        }
        int minLightSection = this.lightEngine.getMinLightSection();
        int maxLightSection = this.lightEngine.getMaxLightSection();
        if (chunkY < minLightSection || chunkY > maxLightSection) {
            return false;
        }
        BitSet filter = layer == LightLayer.SKY ? this.skyChangedLightSectionFilter : this.blockChangedLightSectionFilter;
        if (!filter.get(index = chunkY - minLightSection)) {
            filter.set(index);
            return true;
        }
        return false;
    }

    public boolean hasChangesToBroadcast() {
        return this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty();
    }

    public void broadcastChanges(LevelChunk chunk) {
        if (!this.hasChangesToBroadcast()) {
            return;
        }
        Level level = chunk.getLevel();
        if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            List<ServerPlayer> borderPlayers = this.playerProvider.getPlayers(this.pos, true);
            if (!borderPlayers.isEmpty()) {
                ClientboundLightUpdatePacket lightPacket = new ClientboundLightUpdatePacket(chunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter);
                this.broadcast(borderPlayers, lightPacket);
            }
            this.skyChangedLightSectionFilter.clear();
            this.blockChangedLightSectionFilter.clear();
        }
        if (!this.hasChangedSections) {
            return;
        }
        List<ServerPlayer> players = this.playerProvider.getPlayers(this.pos, false);
        for (int sectionIndex = 0; sectionIndex < this.changedBlocksPerSection.length; ++sectionIndex) {
            ShortSet changedBlocks = this.changedBlocksPerSection[sectionIndex];
            if (changedBlocks == null) continue;
            this.changedBlocksPerSection[sectionIndex] = null;
            if (players.isEmpty()) continue;
            int sectionY = this.levelHeightAccessor.getSectionYFromSectionIndex(sectionIndex);
            SectionPos sectionPos = SectionPos.of(chunk.getPos(), sectionY);
            if (changedBlocks.size() == 1) {
                BlockPos pos2 = sectionPos.relativeToBlockPos(changedBlocks.iterator().nextShort());
                BlockState state2 = level.getBlockState(pos2);
                this.broadcast(players, new ClientboundBlockUpdatePacket(pos2, state2));
                this.broadcastBlockEntityIfNeeded(players, level, pos2, state2);
                continue;
            }
            LevelChunkSection section = chunk.getSection(sectionIndex);
            ClientboundSectionBlocksUpdatePacket packet = new ClientboundSectionBlocksUpdatePacket(sectionPos, changedBlocks, section);
            this.broadcast(players, packet);
            packet.runUpdates((pos, state) -> this.broadcastBlockEntityIfNeeded(players, level, (BlockPos)pos, (BlockState)state));
        }
        this.hasChangedSections = false;
    }

    private void broadcastBlockEntityIfNeeded(List<ServerPlayer> players, Level level, BlockPos pos, BlockState state) {
        if (state.hasBlockEntity()) {
            this.broadcastBlockEntity(players, level, pos);
        }
    }

    private void broadcastBlockEntity(List<ServerPlayer> players, Level level, BlockPos blockPos) {
        Packet<ClientGamePacketListener> packet;
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity != null && (packet = blockEntity.getUpdatePacket()) != null) {
            this.broadcast(players, packet);
        }
    }

    private void broadcast(List<ServerPlayer> players, Packet<?> packet) {
        players.forEach(player -> player.connection.send(packet));
    }

    @Override
    public int getTicketLevel() {
        return this.ticketLevel;
    }

    @Override
    public int getQueueLevel() {
        return this.queueLevel;
    }

    private void setQueueLevel(int queueLevel) {
        this.queueLevel = queueLevel;
    }

    public void setTicketLevel(int ticketLevel) {
        this.ticketLevel = ticketLevel;
    }

    private void scheduleFullChunkPromotion(ChunkMap scheduler, CompletableFuture<ChunkResult<LevelChunk>> task, Executor mainThreadExecutor, FullChunkStatus status) {
        this.pendingFullStateConfirmation.cancel(false);
        CompletableFuture confirmation = new CompletableFuture();
        confirmation.thenRunAsync(() -> scheduler.onFullChunkStatusChange(this.pos, status), mainThreadExecutor);
        this.pendingFullStateConfirmation = confirmation;
        task.thenAccept(r -> r.ifSuccess(l -> confirmation.complete(null)));
    }

    private void demoteFullChunk(ChunkMap scheduler, FullChunkStatus status) {
        this.pendingFullStateConfirmation.cancel(false);
        scheduler.onFullChunkStatusChange(this.pos, status);
    }

    protected void updateFutures(ChunkMap scheduler, Executor mainThreadExecutor) {
        FullChunkStatus oldFullStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus newFullStatus = ChunkLevel.fullStatus(this.ticketLevel);
        boolean wasAccessible = oldFullStatus.isOrAfter(FullChunkStatus.FULL);
        boolean isAccessible = newFullStatus.isOrAfter(FullChunkStatus.FULL);
        this.wasAccessibleSinceLastSave |= isAccessible;
        if (!wasAccessible && isAccessible) {
            this.fullChunkFuture = scheduler.prepareAccessibleChunk(this);
            this.scheduleFullChunkPromotion(scheduler, this.fullChunkFuture, mainThreadExecutor, FullChunkStatus.FULL);
            this.addSaveDependency(this.fullChunkFuture);
        }
        if (wasAccessible && !isAccessible) {
            this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }
        boolean wasTicking = oldFullStatus.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        boolean isTicking = newFullStatus.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        if (!wasTicking && isTicking) {
            this.tickingChunkFuture = scheduler.prepareTickingChunk(this);
            this.scheduleFullChunkPromotion(scheduler, this.tickingChunkFuture, mainThreadExecutor, FullChunkStatus.BLOCK_TICKING);
            this.addSaveDependency(this.tickingChunkFuture);
        }
        if (wasTicking && !isTicking) {
            this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }
        boolean wasEntityTicking = oldFullStatus.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        boolean isEntityTicking = newFullStatus.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        if (!wasEntityTicking && isEntityTicking) {
            if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
                throw Util.pauseInIde(new IllegalStateException());
            }
            this.entityTickingChunkFuture = scheduler.prepareEntityTickingChunk(this);
            this.scheduleFullChunkPromotion(scheduler, this.entityTickingChunkFuture, mainThreadExecutor, FullChunkStatus.ENTITY_TICKING);
            this.addSaveDependency(this.entityTickingChunkFuture);
        }
        if (wasEntityTicking && !isEntityTicking) {
            this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }
        if (!newFullStatus.isOrAfter(oldFullStatus)) {
            this.demoteFullChunk(scheduler, newFullStatus);
        }
        this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
        this.oldTicketLevel = this.ticketLevel;
    }

    public boolean wasAccessibleSinceLastSave() {
        return this.wasAccessibleSinceLastSave;
    }

    public void refreshAccessibility() {
        this.wasAccessibleSinceLastSave = ChunkLevel.fullStatus(this.ticketLevel).isOrAfter(FullChunkStatus.FULL);
    }

    @FunctionalInterface
    public static interface LevelChangeListener {
        public void onLevelChange(ChunkPos var1, IntSupplier var2, int var3, IntConsumer var4);
    }

    public static interface PlayerProvider {
        public List<ServerPlayer> getPlayers(ChunkPos var1, boolean var2);
    }
}

