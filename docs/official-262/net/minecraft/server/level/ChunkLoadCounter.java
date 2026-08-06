/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 */
package net.minecraft.server.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ChunkLoadCounter {
    private final List<ChunkHolder> pendingChunks = new ArrayList<ChunkHolder>();
    private int totalChunks;

    public void track(ServerLevel level, Runnable scheduler) {
        ServerChunkCache chunkSource = level.getChunkSource();
        LongOpenHashSet alreadyLoadedChunks = new LongOpenHashSet();
        chunkSource.runDistanceManagerUpdates();
        chunkSource.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(arg_0 -> ChunkLoadCounter.lambda$track$0((LongSet)alreadyLoadedChunks, arg_0));
        scheduler.run();
        chunkSource.runDistanceManagerUpdates();
        chunkSource.chunkMap.allChunksWithAtLeastStatus(ChunkStatus.FULL).forEach(arg_0 -> this.lambda$track$1((LongSet)alreadyLoadedChunks, arg_0));
    }

    public int readyChunks() {
        return this.totalChunks - this.pendingChunks();
    }

    public int pendingChunks() {
        this.pendingChunks.removeIf(chunkHolder -> chunkHolder.getLatestStatus() == ChunkStatus.FULL);
        return this.pendingChunks.size();
    }

    public int totalChunks() {
        return this.totalChunks;
    }

    private /* synthetic */ void lambda$track$1(LongSet alreadyLoadedChunks, ChunkHolder chunkHolder) {
        if (!alreadyLoadedChunks.contains(chunkHolder.getPos().pack())) {
            this.pendingChunks.add(chunkHolder);
            ++this.totalChunks;
        }
    }

    private static /* synthetic */ void lambda$track$0(LongSet alreadyLoadedChunks, ChunkHolder chunkHolder) {
        alreadyLoadedChunks.add(chunkHolder.getPos().pack());
    }
}

