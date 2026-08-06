/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 */
package net.minecraft.world.level;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;

public class LocalMobCapCalculator {
    private final Long2ObjectMap<List<ServerPlayer>> playersNearChunk = new Long2ObjectOpenHashMap();
    private final Map<ServerPlayer, MobCounts> playerMobCounts = Maps.newHashMap();
    private final ChunkMap chunkMap;

    public LocalMobCapCalculator(ChunkMap chunkMap) {
        this.chunkMap = chunkMap;
    }

    private List<ServerPlayer> getPlayersNear(ChunkPos pos) {
        return (List)this.playersNearChunk.computeIfAbsent(pos.pack(), key -> this.chunkMap.getPlayersCloseForSpawning(pos));
    }

    public void addMob(ChunkPos pos, MobCategory category) {
        for (ServerPlayer player : this.getPlayersNear(pos)) {
            this.playerMobCounts.computeIfAbsent(player, key -> new MobCounts()).add(category);
        }
    }

    public boolean canSpawn(MobCategory mobCategory, ChunkPos pos) {
        for (ServerPlayer serverPlayer : this.getPlayersNear(pos)) {
            MobCounts mobCounts = this.playerMobCounts.get(serverPlayer);
            if (mobCounts != null && !mobCounts.canSpawn(mobCategory)) continue;
            return true;
        }
        return false;
    }

    private static class MobCounts {
        private final Object2IntMap<MobCategory> counts = new Object2IntOpenHashMap(MobCategory.values().length);

        private MobCounts() {
        }

        public void add(MobCategory category) {
            this.counts.computeInt((Object)category, (k, count) -> count == null ? 1 : count + 1);
        }

        public boolean canSpawn(MobCategory category) {
            return this.counts.getOrDefault((Object)category, 0) < category.getMaxInstancesPerChunk();
        }
    }
}

