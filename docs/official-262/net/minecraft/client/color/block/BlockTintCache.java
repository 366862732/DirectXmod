/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.color.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ToIntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BlockTintCache {
    private static final int MAX_CACHE_ENTRIES = 256;
    private final ThreadLocal<LatestCacheInfo> latestChunkOnThread = ThreadLocal.withInitial(LatestCacheInfo::new);
    private final Long2ObjectLinkedOpenHashMap<CacheData> cache = new Long2ObjectLinkedOpenHashMap(256, 0.25f);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ToIntFunction<BlockPos> source;

    public BlockTintCache(ToIntFunction<BlockPos> source) {
        this.source = source;
    }

    public int getColor(BlockPos pos) {
        int calculated;
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        LatestCacheInfo chunkInfo = this.latestChunkOnThread.get();
        if (chunkInfo.x != chunkX || chunkInfo.z != chunkZ || chunkInfo.cache == null || chunkInfo.cache.isInvalidated()) {
            chunkInfo.x = chunkX;
            chunkInfo.z = chunkZ;
            chunkInfo.cache = this.findOrCreateChunkCache(chunkX, chunkZ);
        }
        int[] layer = chunkInfo.cache.getLayer(pos.getY());
        int x = pos.getX() & 0xF;
        int z = pos.getZ() & 0xF;
        int index = z << 4 | x;
        int cached = layer[index];
        if (cached != -1) {
            return cached;
        }
        layer[index] = calculated = this.source.applyAsInt(pos);
        return calculated;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void invalidateForChunk(int chunkX, int chunkZ) {
        try {
            this.lock.writeLock().lock();
            for (int offsetX = -1; offsetX <= 1; ++offsetX) {
                for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                    long key = ChunkPos.pack(chunkX + offsetX, chunkZ + offsetZ);
                    CacheData removed = (CacheData)this.cache.remove(key);
                    if (removed == null) continue;
                    removed.invalidate();
                }
            }
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public void invalidateAll() {
        try {
            this.lock.writeLock().lock();
            this.cache.values().forEach(CacheData::invalidate);
            this.cache.clear();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private CacheData findOrCreateChunkCache(int x, int z) {
        long key = ChunkPos.pack(x, z);
        this.lock.readLock().lock();
        try {
            CacheData existing = (CacheData)this.cache.get(key);
            if (existing != null) {
                CacheData cacheData = existing;
                return cacheData;
            }
        }
        finally {
            this.lock.readLock().unlock();
        }
        this.lock.writeLock().lock();
        try {
            CacheData cacheData;
            CacheData existingNow = (CacheData)this.cache.get(key);
            if (existingNow != null) {
                CacheData cacheData2 = existingNow;
                return cacheData2;
            }
            CacheData newCache = new CacheData();
            if (this.cache.size() >= 256 && (cacheData = (CacheData)this.cache.removeFirst()) != null) {
                cacheData.invalidate();
            }
            this.cache.put(key, (Object)newCache);
            CacheData cacheData3 = newCache;
            return cacheData3;
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class LatestCacheInfo {
        private int x = Integer.MIN_VALUE;
        private int z = Integer.MIN_VALUE;
        private @Nullable CacheData cache;

        private LatestCacheInfo() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class CacheData {
        private final Int2ObjectArrayMap<int[]> cache = new Int2ObjectArrayMap(16);
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private static final int BLOCKS_PER_LAYER = Mth.square(16);
        private volatile boolean invalidated;

        private CacheData() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public int[] getLayer(int y) {
            this.lock.readLock().lock();
            try {
                int[] existing = (int[])this.cache.get(y);
                if (existing != null) {
                    int[] nArray = existing;
                    return nArray;
                }
            }
            finally {
                this.lock.readLock().unlock();
            }
            this.lock.writeLock().lock();
            try {
                int[] nArray = (int[])this.cache.computeIfAbsent(y, n -> this.allocateLayer());
                return nArray;
            }
            finally {
                this.lock.writeLock().unlock();
            }
        }

        private int[] allocateLayer() {
            int[] newCache = new int[BLOCKS_PER_LAYER];
            Arrays.fill(newCache, -1);
            return newCache;
        }

        public boolean isInvalidated() {
            return this.invalidated;
        }

        public void invalidate() {
            this.invalidated = true;
        }
    }
}

