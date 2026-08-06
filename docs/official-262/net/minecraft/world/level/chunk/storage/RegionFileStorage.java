/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.jspecify.annotations.Nullable;

public final class RegionFileStorage
implements AutoCloseable {
    public static final String ANVIL_EXTENSION = ".mca";
    private static final int MAX_CACHE_SIZE = 256;
    private final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap();
    private final RegionStorageInfo info;
    private final Path folder;
    private final boolean sync;

    public RegionFileStorage(RegionStorageInfo info, Path folder, boolean sync) {
        this.folder = folder;
        this.sync = sync;
        this.info = info;
    }

    private RegionFile getRegionFile(ChunkPos pos) throws IOException {
        long key = ChunkPos.pack(pos.getRegionX(), pos.getRegionZ());
        RegionFile region = (RegionFile)this.regionCache.getAndMoveToFirst(key);
        if (region != null) {
            return region;
        }
        if (this.regionCache.size() >= 256) {
            ((RegionFile)this.regionCache.removeLast()).close();
        }
        FileUtil.createDirectoriesSafe(this.folder);
        Path file = this.folder.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ANVIL_EXTENSION);
        RegionFile newRegion = new RegionFile(this.info, file, this.folder, this.sync);
        this.regionCache.putAndMoveToFirst(key, (Object)newRegion);
        return newRegion;
    }

    public @Nullable CompoundTag read(ChunkPos pos) throws IOException {
        RegionFile region = this.getRegionFile(pos);
        try (DataInputStream regionChunkInputStream = region.getChunkDataInputStream(pos);){
            if (regionChunkInputStream == null) {
                CompoundTag compoundTag = null;
                return compoundTag;
            }
            CompoundTag compoundTag = NbtIo.read(regionChunkInputStream);
            return compoundTag;
        }
    }

    public void scanChunk(ChunkPos pos, StreamTagVisitor scanner) throws IOException {
        RegionFile region = this.getRegionFile(pos);
        try (DataInputStream regionChunkInputStream = region.getChunkDataInputStream(pos);){
            if (regionChunkInputStream != null) {
                NbtIo.parse(regionChunkInputStream, scanner, NbtAccounter.unlimitedHeap());
            }
        }
    }

    public void write(ChunkPos pos, @Nullable CompoundTag value) throws IOException {
        if (SharedConstants.DEBUG_DONT_SAVE_WORLD) {
            return;
        }
        RegionFile region = this.getRegionFile(pos);
        if (value == null) {
            region.clear(pos);
        } else {
            try (DataOutputStream output = region.getChunkDataOutputStream(pos);){
                NbtIo.write(value, output);
            }
        }
    }

    @Override
    public void close() throws IOException {
        ExceptionCollector<IOException> exception = new ExceptionCollector<IOException>();
        for (RegionFile regionFile : this.regionCache.values()) {
            try {
                regionFile.close();
            }
            catch (IOException e) {
                exception.add(e);
            }
        }
        exception.throwIfPresent();
    }

    public void flush() throws IOException {
        for (RegionFile regionFile : this.regionCache.values()) {
            regionFile.flush();
        }
    }

    public RegionStorageInfo info() {
        return this.info;
    }
}

