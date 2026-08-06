/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class ChunkTracker
extends DynamicGraphMinFixedPoint {
    protected ChunkTracker(int levelCount, int minQueueSize, int minMapSize) {
        super(levelCount, minQueueSize, minMapSize);
    }

    @Override
    protected boolean isSource(long node) {
        return node == ChunkPos.INVALID_CHUNK_POS;
    }

    @Override
    protected void checkNeighborsAfterUpdate(long node, int level, boolean onlyDecrease) {
        if (onlyDecrease && level >= this.levelCount - 2) {
            return;
        }
        ChunkPos pos = ChunkPos.unpack(node);
        int x = pos.x();
        int z = pos.z();
        for (int offsetX = -1; offsetX <= 1; ++offsetX) {
            for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                long neighbor = ChunkPos.pack(x + offsetX, z + offsetZ);
                if (neighbor == node) continue;
                this.checkNeighbor(node, neighbor, level, onlyDecrease);
            }
        }
    }

    @Override
    protected int getComputedLevel(long node, long knownParent, int knownLevelFromParent) {
        int computedLevel = knownLevelFromParent;
        ChunkPos pos = ChunkPos.unpack(node);
        int x = pos.x();
        int z = pos.z();
        for (int offsetX = -1; offsetX <= 1; ++offsetX) {
            for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                long neighbor = ChunkPos.pack(x + offsetX, z + offsetZ);
                if (neighbor == node) {
                    neighbor = ChunkPos.INVALID_CHUNK_POS;
                }
                if (neighbor == knownParent) continue;
                int costFromNeighbor = this.computeLevelFromNeighbor(neighbor, node, this.getLevel(neighbor));
                if (computedLevel > costFromNeighbor) {
                    computedLevel = costFromNeighbor;
                }
                if (computedLevel != 0) continue;
                return computedLevel;
            }
        }
        return computedLevel;
    }

    @Override
    protected int computeLevelFromNeighbor(long from, long to, int fromLevel) {
        if (from == ChunkPos.INVALID_CHUNK_POS) {
            return this.getLevelFromSource(to);
        }
        return fromLevel + 1;
    }

    protected abstract int getLevelFromSource(long var1);

    public void update(long node, int newLevelFrom, boolean onlyDecreased) {
        this.checkEdge(ChunkPos.INVALID_CHUNK_POS, node, newLevelFrom, onlyDecreased);
    }
}

