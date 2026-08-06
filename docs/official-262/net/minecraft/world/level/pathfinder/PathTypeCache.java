/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.HashCommon
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jspecify.annotations.Nullable;

public class PathTypeCache {
    private static final int SIZE = 4096;
    private static final int MASK = 4095;
    private final long[] positions = new long[4096];
    private final PathType[] pathTypes = new PathType[4096];

    public PathType getOrCompute(BlockGetter level, BlockPos pos) {
        long key = pos.asLong();
        int index = PathTypeCache.index(key);
        PathType cachedPathType = this.get(index, key);
        if (cachedPathType != null) {
            return cachedPathType;
        }
        return this.compute(level, pos, index, key);
    }

    private @Nullable PathType get(int index, long key) {
        if (this.positions[index] == key) {
            return this.pathTypes[index];
        }
        return null;
    }

    private PathType compute(BlockGetter level, BlockPos pos, int index, long key) {
        PathType pathType = WalkNodeEvaluator.getPathTypeFromState(level, pos);
        this.positions[index] = key;
        this.pathTypes[index] = pathType;
        return pathType;
    }

    public void invalidate(BlockPos pos) {
        long key = pos.asLong();
        int index = PathTypeCache.index(key);
        if (this.positions[index] == key) {
            this.pathTypes[index] = null;
        }
    }

    private static int index(long pos) {
        return (int)HashCommon.mix((long)pos) & 0xFFF;
    }
}

