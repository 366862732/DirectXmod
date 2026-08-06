/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.TicketStorage;

public class LoadingChunkTracker
extends ChunkTracker {
    private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final DistanceManager distanceManager;
    private final TicketStorage ticketStorage;

    public LoadingChunkTracker(DistanceManager distanceManager, TicketStorage ticketStorage) {
        super(MAX_LEVEL + 1, 16, 256);
        this.distanceManager = distanceManager;
        this.ticketStorage = ticketStorage;
        ticketStorage.setLoadingChunkUpdatedListener(this::update);
    }

    @Override
    protected int getLevelFromSource(long to) {
        return this.ticketStorage.getTicketLevelAt(to, false);
    }

    @Override
    protected int getLevel(long node) {
        ChunkHolder chunk;
        if (!this.distanceManager.isChunkToRemove(node) && (chunk = this.distanceManager.getChunk(node)) != null) {
            return chunk.getTicketLevel();
        }
        return MAX_LEVEL;
    }

    @Override
    protected void setLevel(long node, int level) {
        int oldLevel;
        ChunkHolder chunk = this.distanceManager.getChunk(node);
        int n = oldLevel = chunk == null ? MAX_LEVEL : chunk.getTicketLevel();
        if (oldLevel == level) {
            return;
        }
        if ((chunk = this.distanceManager.updateChunkScheduling(node, level, chunk, oldLevel)) != null) {
            this.distanceManager.chunksToUpdateFutures.add(chunk);
        }
    }

    public int runDistanceUpdates(int count) {
        return this.runUpdates(count);
    }
}

