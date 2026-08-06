/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.base.MoreObjects
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.state.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jspecify.annotations.Nullable;

public class BlockPattern {
    private final Predicate<BlockInWorld>[][][] pattern;
    private final int depth;
    private final int height;
    private final int width;

    public BlockPattern(Predicate<BlockInWorld>[][][] pattern) {
        this.pattern = pattern;
        this.depth = pattern.length;
        if (this.depth > 0) {
            this.height = pattern[0].length;
            this.width = this.height > 0 ? pattern[0][0].length : 0;
        } else {
            this.height = 0;
            this.width = 0;
        }
    }

    public int getDepth() {
        return this.depth;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    @VisibleForTesting
    public Predicate<BlockInWorld>[][][] getPattern() {
        return this.pattern;
    }

    @VisibleForTesting
    public @Nullable BlockPatternMatch matches(LevelReader level, BlockPos origin, Direction forwards, Direction up) {
        LoadingCache<BlockPos, BlockInWorld> cache = BlockPattern.createLevelCache(level, false);
        return this.matches(origin, forwards, up, cache);
    }

    private @Nullable BlockPatternMatch matches(BlockPos origin, Direction forwards, Direction up, LoadingCache<BlockPos, BlockInWorld> cache) {
        for (int x = 0; x < this.width; ++x) {
            for (int y = 0; y < this.height; ++y) {
                for (int z = 0; z < this.depth; ++z) {
                    if (this.pattern[z][y][x].test((BlockInWorld)cache.getUnchecked((Object)BlockPattern.translateAndRotate(origin, forwards, up, x, y, z)))) continue;
                    return null;
                }
            }
        }
        return new BlockPatternMatch(origin, forwards, up, cache, this.width, this.height, this.depth);
    }

    public @Nullable BlockPatternMatch find(LevelReader level, BlockPos origin) {
        LoadingCache<BlockPos, BlockInWorld> cache = BlockPattern.createLevelCache(level, false);
        int dist = Math.max(Math.max(this.width, this.height), this.depth);
        for (BlockPos testPos : BlockPos.betweenClosed(origin, origin.offset(dist - 1, dist - 1, dist - 1))) {
            for (Direction forwards : Direction.values()) {
                for (Direction up : Direction.values()) {
                    BlockPatternMatch match;
                    if (up == forwards || up == forwards.getOpposite() || (match = this.matches(testPos, forwards, up, cache)) == null) continue;
                    return match;
                }
            }
        }
        return null;
    }

    public static LoadingCache<BlockPos, BlockInWorld> createLevelCache(LevelReader level, boolean loadChunks) {
        return CacheBuilder.newBuilder().build((CacheLoader)new BlockCacheLoader(level, loadChunks));
    }

    protected static BlockPos translateAndRotate(BlockPos origin, Direction forwardsDirection, Direction upDirection, int right, int down, int forwards) {
        if (forwardsDirection == upDirection || forwardsDirection == upDirection.getOpposite()) {
            throw new IllegalArgumentException("Invalid forwards & up combination");
        }
        Vec3i forwardsVector = new Vec3i(forwardsDirection.getStepX(), forwardsDirection.getStepY(), forwardsDirection.getStepZ());
        Vec3i upVector = new Vec3i(upDirection.getStepX(), upDirection.getStepY(), upDirection.getStepZ());
        Vec3i rightVector = forwardsVector.cross(upVector);
        return origin.offset(upVector.getX() * -down + rightVector.getX() * right + forwardsVector.getX() * forwards, upVector.getY() * -down + rightVector.getY() * right + forwardsVector.getY() * forwards, upVector.getZ() * -down + rightVector.getZ() * right + forwardsVector.getZ() * forwards);
    }

    public static class BlockPatternMatch {
        private final BlockPos frontTopLeft;
        private final Direction forwards;
        private final Direction up;
        private final LoadingCache<BlockPos, BlockInWorld> cache;
        private final int width;
        private final int height;
        private final int depth;

        public BlockPatternMatch(BlockPos frontTopLeft, Direction forwards, Direction up, LoadingCache<BlockPos, BlockInWorld> cache, int width, int height, int depth) {
            this.frontTopLeft = frontTopLeft;
            this.forwards = forwards;
            this.up = up;
            this.cache = cache;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public BlockPos getFrontTopLeft() {
            return this.frontTopLeft;
        }

        public Direction getForwards() {
            return this.forwards;
        }

        public Direction getUp() {
            return this.up;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public int getDepth() {
            return this.depth;
        }

        public BlockInWorld getBlock(int right, int down, int forwards) {
            return (BlockInWorld)this.cache.getUnchecked((Object)BlockPattern.translateAndRotate(this.frontTopLeft, this.getForwards(), this.getUp(), right, down, forwards));
        }

        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("up", (Object)this.up).add("forwards", (Object)this.forwards).add("frontTopLeft", (Object)this.frontTopLeft).toString();
        }
    }

    private static class BlockCacheLoader
    extends CacheLoader<BlockPos, BlockInWorld> {
        private final LevelReader level;
        private final boolean loadChunks;

        public BlockCacheLoader(LevelReader level, boolean loadChunks) {
            this.level = level;
            this.loadChunks = loadChunks;
        }

        public BlockInWorld load(BlockPos key) {
            return new BlockInWorld(this.level, key, this.loadChunks);
        }
    }
}

