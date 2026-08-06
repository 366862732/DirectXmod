/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class SectionTracker
extends DynamicGraphMinFixedPoint {
    protected SectionTracker(int levelCount, int minQueueSize, int minMapSize) {
        super(levelCount, minQueueSize, minMapSize);
    }

    @Override
    protected void checkNeighborsAfterUpdate(long node, int level, boolean onlyDecrease) {
        if (onlyDecrease && level >= this.levelCount - 2) {
            return;
        }
        for (int offsetX = -1; offsetX <= 1; ++offsetX) {
            for (int offsetY = -1; offsetY <= 1; ++offsetY) {
                for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                    long neighbor = SectionPos.offset(node, offsetX, offsetY, offsetZ);
                    if (neighbor == node) continue;
                    this.checkNeighbor(node, neighbor, level, onlyDecrease);
                }
            }
        }
    }

    @Override
    protected int getComputedLevel(long node, long knownParent, int knownLevelFromParent) {
        int computedLevel = knownLevelFromParent;
        for (int offsetX = -1; offsetX <= 1; ++offsetX) {
            for (int offsetY = -1; offsetY <= 1; ++offsetY) {
                for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                    long neighbor = SectionPos.offset(node, offsetX, offsetY, offsetZ);
                    if (neighbor == node) {
                        neighbor = Long.MAX_VALUE;
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
        }
        return computedLevel;
    }

    @Override
    protected int computeLevelFromNeighbor(long from, long to, int fromLevel) {
        if (this.isSource(from)) {
            return this.getLevelFromSource(to);
        }
        return fromLevel + 1;
    }

    protected abstract int getLevelFromSource(long var1);

    public void update(long node, int newLevelFrom, boolean onlyDecreased) {
        this.checkEdge(Long.MAX_VALUE, node, newLevelFrom, onlyDecreased);
    }
}

