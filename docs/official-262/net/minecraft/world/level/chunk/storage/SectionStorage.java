/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.OptionalDynamic
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMaps
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongListIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SectionStorage<R, P>
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTIONS_TAG = "Sections";
    private final SimpleRegionStorage simpleRegionStorage;
    private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap();
    private final LongLinkedOpenHashSet dirtyChunks = new LongLinkedOpenHashSet();
    private final Codec<P> codec;
    private final Function<R, P> packer;
    private final BiFunction<P, Runnable, R> unpacker;
    private final Function<Runnable, R> factory;
    private final RegistryAccess registryAccess;
    private final ChunkIOErrorReporter errorReporter;
    protected final LevelHeightAccessor levelHeightAccessor;
    private final LongSet loadedChunks = new LongOpenHashSet();
    private final Long2ObjectMap<CompletableFuture<Optional<PackedChunk<P>>>> pendingLoads = new Long2ObjectOpenHashMap();
    private final Object loadLock = new Object();

    public SectionStorage(SimpleRegionStorage simpleRegionStorage, Codec<P> codec, Function<R, P> packer, BiFunction<P, Runnable, R> unpacker, Function<Runnable, R> factory, RegistryAccess registryAccess, ChunkIOErrorReporter errorReporter, LevelHeightAccessor levelHeightAccessor) {
        this.simpleRegionStorage = simpleRegionStorage;
        this.codec = codec;
        this.packer = packer;
        this.unpacker = unpacker;
        this.factory = factory;
        this.registryAccess = registryAccess;
        this.errorReporter = errorReporter;
        this.levelHeightAccessor = levelHeightAccessor;
    }

    protected void tick(BooleanSupplier haveTime) {
        LongListIterator iterator = this.dirtyChunks.iterator();
        while (iterator.hasNext() && haveTime.getAsBoolean()) {
            ChunkPos chunkPos = ChunkPos.unpack(iterator.nextLong());
            iterator.remove();
            this.writeChunk(chunkPos);
        }
        this.unpackPendingLoads();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void unpackPendingLoads() {
        Object object = this.loadLock;
        synchronized (object) {
            ObjectIterator iterator = Long2ObjectMaps.fastIterator(this.pendingLoads);
            while (iterator.hasNext()) {
                Long2ObjectMap.Entry entry = (Long2ObjectMap.Entry)iterator.next();
                Optional chunk = ((CompletableFuture)entry.getValue()).getNow(null);
                if (chunk == null) continue;
                long chunkKey = entry.getLongKey();
                this.unpackChunk(ChunkPos.unpack(chunkKey), chunk.orElse(null));
                iterator.remove();
                this.loadedChunks.add(chunkKey);
            }
        }
    }

    public void flushAll() {
        if (!this.dirtyChunks.isEmpty()) {
            this.dirtyChunks.forEach(pos -> this.writeChunk(ChunkPos.unpack(pos)));
            this.dirtyChunks.clear();
        }
    }

    public boolean hasWork() {
        return !this.dirtyChunks.isEmpty();
    }

    protected @Nullable Optional<R> get(long sectionPos) {
        return (Optional)this.storage.get(sectionPos);
    }

    protected Optional<R> getOrLoad(long sectionPos) {
        if (this.outsideStoredRange(sectionPos)) {
            return Optional.empty();
        }
        Optional<R> r = this.get(sectionPos);
        if (r != null) {
            return r;
        }
        this.unpackChunk(SectionPos.of(sectionPos).chunk());
        r = this.get(sectionPos);
        if (r == null) {
            throw Util.pauseInIde(new IllegalStateException());
        }
        return r;
    }

    protected boolean outsideStoredRange(long sectionPos) {
        int y = SectionPos.sectionToBlockCoord(SectionPos.y(sectionPos));
        return this.levelHeightAccessor.isOutsideBuildHeight(y);
    }

    protected R getOrCreate(long sectionPos) {
        if (this.outsideStoredRange(sectionPos)) {
            throw Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
        }
        Optional<R> r = this.getOrLoad(sectionPos);
        if (r.isPresent()) {
            return r.get();
        }
        R newR = this.factory.apply(() -> this.setDirty(sectionPos));
        this.storage.put(sectionPos, Optional.of(newR));
        return newR;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public CompletableFuture<?> prefetch(ChunkPos chunkPos) {
        Object object = this.loadLock;
        synchronized (object) {
            long chunkKey = chunkPos.pack();
            if (this.loadedChunks.contains(chunkKey)) {
                return CompletableFuture.completedFuture(null);
            }
            return (CompletableFuture)this.pendingLoads.computeIfAbsent(chunkKey, k -> this.tryRead(chunkPos));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void unpackChunk(ChunkPos chunkPos) {
        CompletableFuture future;
        long chunkKey = chunkPos.pack();
        Object object = this.loadLock;
        synchronized (object) {
            if (!this.loadedChunks.add(chunkKey)) {
                return;
            }
            future = (CompletableFuture)this.pendingLoads.computeIfAbsent(chunkKey, k -> this.tryRead(chunkPos));
        }
        this.unpackChunk(chunkPos, ((Optional)future.join()).orElse(null));
        object = this.loadLock;
        synchronized (object) {
            this.pendingLoads.remove(chunkKey);
        }
    }

    private CompletableFuture<Optional<PackedChunk<P>>> tryRead(ChunkPos chunkPos) {
        RegistryOps<Tag> registryOps = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        return ((CompletableFuture)this.simpleRegionStorage.read(chunkPos).thenApplyAsync(result -> result.map(tag -> PackedChunk.parse(this.codec, registryOps, tag, this.simpleRegionStorage, this.levelHeightAccessor)), Util.backgroundExecutor().forName("parseSection"))).exceptionally(throwable -> {
            if (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }
            if (throwable instanceof IOException) {
                IOException e = (IOException)throwable;
                LOGGER.error("Error reading chunk {} data from disk", (Object)chunkPos, (Object)e);
                this.errorReporter.reportChunkLoadFailure(e, this.simpleRegionStorage.storageInfo(), chunkPos);
                return Optional.empty();
            }
            throw new CompletionException((Throwable)throwable);
        });
    }

    private void unpackChunk(ChunkPos pos, @Nullable PackedChunk<P> packedChunk) {
        if (packedChunk == null) {
            for (int sectionY = this.levelHeightAccessor.getMinSectionY(); sectionY <= this.levelHeightAccessor.getMaxSectionY(); ++sectionY) {
                this.storage.put(SectionStorage.getKey(pos, sectionY), Optional.empty());
            }
        } else {
            boolean versionChanged = packedChunk.versionChanged();
            for (int sectionY = this.levelHeightAccessor.getMinSectionY(); sectionY <= this.levelHeightAccessor.getMaxSectionY(); ++sectionY) {
                long key = SectionStorage.getKey(pos, sectionY);
                Optional<Object> section = Optional.ofNullable(packedChunk.sectionsByY.get(sectionY)).map(packed -> this.unpacker.apply(packed, () -> this.setDirty(key)));
                this.storage.put(key, section);
                section.ifPresent(s -> {
                    this.onSectionLoad(key);
                    if (versionChanged) {
                        this.setDirty(key);
                    }
                });
            }
        }
    }

    private void writeChunk(ChunkPos chunkPos) {
        RegistryOps<Tag> registryOps = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        Dynamic<Tag> tag = this.writeChunk(chunkPos, registryOps);
        Tag value = (Tag)tag.getValue();
        if (value instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag)value;
            this.simpleRegionStorage.write(chunkPos, compoundTag).exceptionally(throwable -> {
                this.errorReporter.reportChunkSaveFailure((Throwable)throwable, this.simpleRegionStorage.storageInfo(), chunkPos);
                return null;
            });
        } else {
            LOGGER.error("Expected compound tag, got {}", (Object)value);
        }
    }

    private <T> Dynamic<T> writeChunk(ChunkPos chunkPos, DynamicOps<T> ops) {
        HashMap sections = Maps.newHashMap();
        for (int sectionY = this.levelHeightAccessor.getMinSectionY(); sectionY <= this.levelHeightAccessor.getMaxSectionY(); ++sectionY) {
            long key = SectionStorage.getKey(chunkPos, sectionY);
            Optional r = (Optional)this.storage.get(key);
            if (r == null || r.isEmpty()) continue;
            DataResult serializedSection = this.codec.encodeStart(ops, this.packer.apply(r.get()));
            String yName = Integer.toString(sectionY);
            serializedSection.resultOrPartial(arg_0 -> ((Logger)LOGGER).error(arg_0)).ifPresent(s -> sections.put(ops.createString(yName), s));
        }
        return new Dynamic(ops, ops.createMap((Map)ImmutableMap.of((Object)ops.createString(SECTIONS_TAG), (Object)ops.createMap((Map)sections), (Object)ops.createString("DataVersion"), (Object)ops.createInt(SharedConstants.getCurrentVersion().dataVersion().version()))));
    }

    private static long getKey(ChunkPos chunkPos, int sectionY) {
        return SectionPos.asLong(chunkPos.x(), sectionY, chunkPos.z());
    }

    protected void onSectionLoad(long sectionPos) {
    }

    protected void setDirty(long sectionPos) {
        Optional r = (Optional)this.storage.get(sectionPos);
        if (r == null || r.isEmpty()) {
            LOGGER.warn("No data for position: {}", (Object)SectionPos.of(sectionPos));
            return;
        }
        this.dirtyChunks.add(ChunkPos.pack(SectionPos.x(sectionPos), SectionPos.z(sectionPos)));
    }

    public void flush(ChunkPos chunkPos) {
        if (this.dirtyChunks.remove(chunkPos.pack())) {
            this.writeChunk(chunkPos);
        }
    }

    @Override
    public void close() throws IOException {
        this.simpleRegionStorage.close();
    }

    private record PackedChunk<T>(Int2ObjectMap<T> sectionsByY, boolean versionChanged) {
        public static <T> PackedChunk<T> parse(Codec<T> codec, DynamicOps<Tag> ops, Tag tag, SimpleRegionStorage simpleRegionStorage, LevelHeightAccessor levelHeightAccessor) {
            Dynamic originalTag = new Dynamic(ops, (Object)tag);
            Dynamic<Tag> fixedTag = simpleRegionStorage.upgradeChunkTag((Dynamic<Tag>)originalTag, 1945);
            boolean versionChanged = originalTag != fixedTag;
            OptionalDynamic sections = fixedTag.get(SectionStorage.SECTIONS_TAG);
            Int2ObjectOpenHashMap sectionsByY = new Int2ObjectOpenHashMap();
            for (int sectionY = levelHeightAccessor.getMinSectionY(); sectionY <= levelHeightAccessor.getMaxSectionY(); ++sectionY) {
                Optional section = sections.get(Integer.toString(sectionY)).result().flatMap(sectionData -> codec.parse(sectionData).resultOrPartial(arg_0 -> ((Logger)LOGGER).error(arg_0)));
                if (!section.isPresent()) continue;
                sectionsByY.put(sectionY, section.get());
            }
            return new PackedChunk<T>(sectionsByY, versionChanged);
        }
    }
}

