/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientChunkCache
extends ChunkSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final LevelChunk emptyChunk;
    private final LevelLightEngine lightEngine;
    private volatile Storage storage;
    private final ClientLevel level;

    public ClientChunkCache(ClientLevel level, int serverChunkRadius) {
        this.level = level;
        this.emptyChunk = new EmptyLevelChunk(level, new ChunkPos(0, 0), level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
        this.lightEngine = new LevelLightEngine(this, true, level.dimensionType().hasSkyLight());
        this.storage = new Storage(this, ClientChunkCache.calculateStorageRange(serverChunkRadius));
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    private static boolean isValidChunk(@Nullable LevelChunk chunk, int x, int z) {
        if (chunk == null) {
            return false;
        }
        ChunkPos pos = chunk.getPos();
        return pos.x() == x && pos.z() == z;
    }

    public void drop(ChunkPos pos) {
        if (!this.storage.inRange(pos.x(), pos.z())) {
            return;
        }
        int index = this.storage.getIndex(pos.x(), pos.z());
        LevelChunk currentChunk = this.storage.getChunk(index);
        if (ClientChunkCache.isValidChunk(currentChunk, pos.x(), pos.z())) {
            this.storage.drop(index, currentChunk);
        }
    }

    @Override
    public @Nullable LevelChunk getChunk(int x, int z, ChunkStatus targetStatus, boolean loadOrGenerate) {
        LevelChunk chunk;
        if (this.storage.inRange(x, z) && ClientChunkCache.isValidChunk(chunk = this.storage.getChunk(this.storage.getIndex(x, z)), x, z)) {
            return chunk;
        }
        if (loadOrGenerate) {
            return this.emptyChunk;
        }
        return null;
    }

    @Override
    public BlockGetter getLevel() {
        return this.level;
    }

    public void replaceBiomes(int chunkX, int chunkZ, FriendlyByteBuf readBuffer) {
        if (!this.storage.inRange(chunkX, chunkZ)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", (Object)chunkX, (Object)chunkZ);
            return;
        }
        int index = this.storage.getIndex(chunkX, chunkZ);
        LevelChunk chunk = this.storage.chunks.get(index);
        if (!ClientChunkCache.isValidChunk(chunk, chunkX, chunkZ)) {
            LOGGER.warn("Ignoring chunk since it's not present: {}, {}", (Object)chunkX, (Object)chunkZ);
        } else {
            chunk.replaceBiomes(readBuffer);
        }
    }

    public @Nullable LevelChunk replaceWithPacketData(int chunkX, int chunkZ, FriendlyByteBuf readBuffer, Map<Heightmap.Types, long[]> heightmaps, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntities) {
        if (!this.storage.inRange(chunkX, chunkZ)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", (Object)chunkX, (Object)chunkZ);
            return null;
        }
        int index = this.storage.getIndex(chunkX, chunkZ);
        LevelChunk chunk = this.storage.chunks.get(index);
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        if (!ClientChunkCache.isValidChunk(chunk, chunkX, chunkZ)) {
            chunk = new LevelChunk(this.level, pos);
            chunk.replaceWithPacketData(readBuffer, heightmaps, blockEntities);
            this.storage.replace(index, chunk);
        } else {
            chunk.replaceWithPacketData(readBuffer, heightmaps, blockEntities);
            this.storage.refreshEmptySections(chunk);
        }
        this.level.onChunkLoaded(pos);
        return chunk;
    }

    @Override
    public void tick(BooleanSupplier haveTime, boolean tickChunks) {
    }

    public void updateViewCenter(int x, int z) {
        this.storage.viewCenterX = x;
        this.storage.viewCenterZ = z;
    }

    public void updateViewRadius(int viewRange) {
        int chunkRadius = this.storage.chunkRadius;
        int newChunkRadius = ClientChunkCache.calculateStorageRange(viewRange);
        if (chunkRadius != newChunkRadius) {
            Storage newStorage = new Storage(this, newChunkRadius);
            newStorage.viewCenterX = this.storage.viewCenterX;
            newStorage.viewCenterZ = this.storage.viewCenterZ;
            for (int i = 0; i < this.storage.chunks.length(); ++i) {
                ChunkPos pos;
                LevelChunk chunk = this.storage.chunks.get(i);
                if (chunk == null || !newStorage.inRange((pos = chunk.getPos()).x(), pos.z())) continue;
                newStorage.replace(newStorage.getIndex(pos.x(), pos.z()), chunk);
            }
            this.storage = newStorage;
        }
    }

    private static int calculateStorageRange(int viewRange) {
        return Math.max(2, viewRange) + 3;
    }

    @Override
    public String gatherStats() {
        return this.storage.chunks.length() + ", " + this.getLoadedChunksCount();
    }

    @Override
    public int getLoadedChunksCount() {
        return this.storage.chunkCount;
    }

    @Override
    public void onLightUpdate(LightLayer layer, SectionPos pos) {
        Minecraft.getInstance().levelExtractor.setSectionDirty(pos.x(), pos.y(), pos.z());
    }

    public LongOpenHashSet addedEmptySections() {
        return this.storage.addedEmptySections[this.storage.updatingSetsIndex];
    }

    public LongOpenHashSet removedEmptySections() {
        return this.storage.removedEmptySections[this.storage.updatingSetsIndex];
    }

    public LongOpenHashSet addedLoadedChunks() {
        return this.storage.addedLoadedChunks[this.storage.updatingSetsIndex];
    }

    public LongOpenHashSet removedLoadedChunks() {
        return this.storage.removedLoadedChunks[this.storage.updatingSetsIndex];
    }

    public void flipUpdateTrackingSets() {
        this.storage.updatingSetsIndex = (this.storage.updatingSetsIndex + 1) % 2;
        this.storage.addedEmptySections[this.storage.updatingSetsIndex].clear();
        this.storage.removedEmptySections[this.storage.updatingSetsIndex].clear();
        this.storage.addedLoadedChunks[this.storage.updatingSetsIndex].clear();
        this.storage.removedLoadedChunks[this.storage.updatingSetsIndex].clear();
    }

    @Override
    public void onSectionEmptinessChanged(int sectionX, int sectionY, int sectionZ, boolean empty) {
        this.storage.onSectionEmptinessChanged(sectionX, sectionY, sectionZ, empty);
    }

    @Environment(value=EnvType.CLIENT)
    private final class Storage {
        private static final int UPDATE_TRACKING_BUFFERS = 2;
        private final AtomicReferenceArray<@Nullable LevelChunk> chunks;
        private final LongOpenHashSet[] addedEmptySections;
        private final LongOpenHashSet[] removedEmptySections;
        private final LongOpenHashSet[] addedLoadedChunks;
        private final LongOpenHashSet[] removedLoadedChunks;
        private int updatingSetsIndex;
        private final int chunkRadius;
        private final int viewRange;
        private volatile int viewCenterX;
        private volatile int viewCenterZ;
        private int chunkCount;
        final /* synthetic */ ClientChunkCache this$0;

        private Storage(ClientChunkCache clientChunkCache, int chunkRadius) {
            ClientChunkCache clientChunkCache2 = clientChunkCache;
            Objects.requireNonNull(clientChunkCache2);
            this.this$0 = clientChunkCache2;
            this.addedEmptySections = new LongOpenHashSet[2];
            this.removedEmptySections = new LongOpenHashSet[2];
            this.addedLoadedChunks = new LongOpenHashSet[2];
            this.removedLoadedChunks = new LongOpenHashSet[2];
            this.chunkRadius = chunkRadius;
            this.viewRange = chunkRadius * 2 + 1;
            this.chunks = new AtomicReferenceArray(this.viewRange * this.viewRange);
            for (int i = 0; i < 2; ++i) {
                this.addedEmptySections[i] = new LongOpenHashSet();
                this.removedEmptySections[i] = new LongOpenHashSet();
                this.addedLoadedChunks[i] = new LongOpenHashSet();
                this.removedLoadedChunks[i] = new LongOpenHashSet();
            }
        }

        private int getIndex(int chunkX, int chunkZ) {
            return Math.floorMod(chunkZ, this.viewRange) * this.viewRange + Math.floorMod(chunkX, this.viewRange);
        }

        private void replace(int index, @Nullable LevelChunk newChunk) {
            LevelChunk removedChunk = this.chunks.getAndSet(index, newChunk);
            if (removedChunk != null) {
                --this.chunkCount;
                this.onChunkRemoved(removedChunk);
                this.this$0.level.unload(removedChunk);
            }
            if (newChunk != null) {
                ++this.chunkCount;
                this.onChunkAdded(newChunk);
            }
        }

        private void drop(int index, LevelChunk oldChunk) {
            if (this.chunks.compareAndSet(index, oldChunk, null)) {
                --this.chunkCount;
                this.onChunkRemoved(oldChunk);
            }
            this.this$0.level.unload(oldChunk);
        }

        public void onSectionEmptinessChanged(int sectionX, int sectionY, int sectionZ, boolean empty) {
            if (!this.inRange(sectionX, sectionZ)) {
                return;
            }
            long sectionNode = SectionPos.asLong(sectionX, sectionY, sectionZ);
            if (empty) {
                this.addedEmptySections[this.updatingSetsIndex].add(sectionNode);
            } else {
                this.removedEmptySections[this.updatingSetsIndex].add(sectionNode);
            }
        }

        private void onChunkRemoved(LevelChunk chunk) {
            ChunkPos chunkPos = chunk.getPos();
            this.removedLoadedChunks[this.updatingSetsIndex].add(chunkPos.pack());
            LevelChunkSection[] sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < sections.length; ++sectionIndex) {
                this.removedEmptySections[this.updatingSetsIndex].add(SectionPos.asLong(chunkPos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), chunkPos.z()));
            }
        }

        private void onChunkAdded(LevelChunk chunk) {
            ChunkPos chunkPos = chunk.getPos();
            this.addedLoadedChunks[this.updatingSetsIndex].add(chunkPos.pack());
            LevelChunkSection[] sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < sections.length; ++sectionIndex) {
                LevelChunkSection section = sections[sectionIndex];
                if (!section.hasOnlyAir()) continue;
                this.addedEmptySections[this.updatingSetsIndex].add(SectionPos.asLong(chunkPos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), chunkPos.z()));
            }
        }

        private void refreshEmptySections(LevelChunk chunk) {
            ChunkPos chunkPos = chunk.getPos();
            LevelChunkSection[] sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < sections.length; ++sectionIndex) {
                LevelChunkSection section = sections[sectionIndex];
                long sectionNode = SectionPos.asLong(chunkPos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), chunkPos.z());
                if (section.hasOnlyAir()) {
                    this.addedEmptySections[this.updatingSetsIndex].add(sectionNode);
                    continue;
                }
                this.removedEmptySections[this.updatingSetsIndex].add(sectionNode);
            }
        }

        private boolean inRange(int chunkX, int chunkZ) {
            return Math.abs(chunkX - this.viewCenterX) <= this.chunkRadius && Math.abs(chunkZ - this.viewCenterZ) <= this.chunkRadius;
        }

        public @Nullable LevelChunk getChunk(int index) {
            return this.chunks.get(index);
        }

        private void dumpChunks(String file) {
            try (FileOutputStream stream = new FileOutputStream(file);){
                int chunkRadius = this.this$0.storage.chunkRadius;
                for (int z = this.viewCenterZ - chunkRadius; z <= this.viewCenterZ + chunkRadius; ++z) {
                    for (int x = this.viewCenterX - chunkRadius; x <= this.viewCenterX + chunkRadius; ++x) {
                        LevelChunk chunk = this.this$0.storage.chunks.get(this.this$0.storage.getIndex(x, z));
                        if (chunk == null) continue;
                        ChunkPos pos = chunk.getPos();
                        stream.write((pos.x() + "\t" + pos.z() + "\t" + chunk.isEmpty() + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            catch (IOException e) {
                LOGGER.error("Failed to dump chunks to file {}", (Object)file, (Object)e);
            }
        }
    }
}

