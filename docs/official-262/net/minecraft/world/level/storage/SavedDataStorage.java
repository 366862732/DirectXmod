/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SavedDataStorage
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<SavedDataType<?>, Optional<SavedData>> cache = new HashMap();
    private final DataFixer fixerUpper;
    private final HolderLookup.Provider registries;
    private final Path dataFolder;
    private CompletableFuture<?> pendingWriteFuture = CompletableFuture.completedFuture(null);
    private boolean closed;

    public SavedDataStorage(Path dataFolder, DataFixer fixerUpper, HolderLookup.Provider registries) {
        this.fixerUpper = fixerUpper;
        this.dataFolder = dataFolder;
        this.registries = registries;
    }

    private Path getDataFile(Identifier id) {
        Path path = id.withSuffix(".dat").resolveAgainst(this.dataFolder);
        if (!path.toAbsolutePath().startsWith(this.dataFolder.toAbsolutePath())) {
            throw new IllegalArgumentException("SavedDataStorage attempted file access outside of data directory: {}" + String.valueOf(path));
        }
        return path;
    }

    public <T extends SavedData> T computeIfAbsent(SavedDataType<T> type) {
        T data = this.get(type);
        if (data != null) {
            return data;
        }
        SavedData newData = (SavedData)type.constructor().get();
        this.set(type, newData);
        return (T)newData;
    }

    public <T extends SavedData> @Nullable T get(SavedDataType<T> type) {
        Optional<SavedData> data = this.cache.get(type);
        if (data == null) {
            data = Optional.ofNullable(this.readSavedData(type));
            this.cache.put(type, data);
        }
        return (T)((SavedData)data.orElse(null));
    }

    private <T extends SavedData> @Nullable T readSavedData(SavedDataType<T> type) {
        try {
            Path file = this.getDataFile(type.id());
            if (Files.exists(file, new LinkOption[0])) {
                CompoundTag tag = this.readTagFromDisk(file, type.dataFixType(), SharedConstants.getCurrentVersion().dataVersion().version());
                RegistryOps<Tag> ops = this.registries.createSerializationContext(NbtOps.INSTANCE);
                return (T)((SavedData)type.codec().parse(ops, (Object)tag.get("data")).resultOrPartial(error -> LOGGER.error("Failed to parse saved data for '{}': {}", (Object)type, error)).orElse(null));
            }
        }
        catch (Exception e) {
            LOGGER.error("Error loading saved data: {}", type, (Object)e);
        }
        return null;
    }

    public <T extends SavedData> void set(SavedDataType<T> type, T data) {
        this.cache.put(type, Optional.of(data));
        data.setDirty();
    }

    public CompoundTag readTagFromDisk(Path dataFile, DataFixTypes type, int newVersion) throws IOException {
        try (InputStream in = Files.newInputStream(dataFile, new OpenOption[0]);){
            CompoundTag compoundTag;
            try (PushbackInputStream inputStream = new PushbackInputStream(new FastBufferedInputStream(in), 2);){
                CompoundTag tag;
                if (this.isGzip(inputStream)) {
                    tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
                } else {
                    try (DataInputStream dis = new DataInputStream(inputStream);){
                        tag = NbtIo.read(dis);
                    }
                }
                int version = NbtUtils.getDataVersion(tag, 1343);
                compoundTag = type.update(this.fixerUpper, tag, version, newVersion);
            }
            return compoundTag;
        }
    }

    private boolean isGzip(PushbackInputStream inputStream) throws IOException {
        int fullHeader;
        byte[] header = new byte[2];
        boolean gzip = false;
        int read = inputStream.read(header, 0, 2);
        if (read == 2 && (fullHeader = (header[1] & 0xFF) << 8 | header[0] & 0xFF) == 35615) {
            gzip = true;
        }
        if (read != 0) {
            inputStream.unread(header, 0, read);
        }
        return gzip;
    }

    public CompletableFuture<?> scheduleSave() {
        if (this.closed) {
            throw new IllegalStateException("Trying to schedule save when SavedDataStorage is already closed");
        }
        Map<SavedDataType<?>, CompoundTag> tagsToSave = this.collectDirtyTagsToSave();
        if (tagsToSave.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        int threads = Util.maxAllowedExecutorThreads();
        int taskCount = tagsToSave.size();
        this.pendingWriteFuture = taskCount > threads ? this.pendingWriteFuture.thenCompose(ignored -> {
            ArrayList<CompletableFuture<Void>> tasks = new ArrayList<CompletableFuture<Void>>(threads);
            int bucketSize = Mth.positiveCeilDiv(taskCount, threads);
            for (List entries : Iterables.partition(tagsToSave.entrySet(), (int)bucketSize)) {
                tasks.add(CompletableFuture.runAsync(() -> {
                    for (Map.Entry entry : entries) {
                        this.tryWrite((SavedDataType)entry.getKey(), (CompoundTag)entry.getValue());
                    }
                }, Util.ioPool()));
            }
            return CompletableFuture.allOf((CompletableFuture[])tasks.toArray(CompletableFuture[]::new));
        }) : this.pendingWriteFuture.thenCompose(ignored -> CompletableFuture.allOf((CompletableFuture[])tagsToSave.entrySet().stream().map(entry -> CompletableFuture.runAsync(() -> this.tryWrite((SavedDataType)entry.getKey(), (CompoundTag)entry.getValue()), Util.ioPool())).toArray(CompletableFuture[]::new)));
        return this.pendingWriteFuture;
    }

    private Map<SavedDataType<?>, CompoundTag> collectDirtyTagsToSave() {
        Object2ObjectArrayMap tagsToSave = new Object2ObjectArrayMap();
        RegistryOps<Tag> ops = this.registries.createSerializationContext(NbtOps.INSTANCE);
        this.cache.forEach((arg_0, arg_1) -> this.lambda$collectDirtyTagsToSave$0((Map)tagsToSave, ops, arg_0, arg_1));
        return tagsToSave;
    }

    private <T extends SavedData> CompoundTag encodeUnchecked(SavedDataType<T> type, SavedData data, RegistryOps<Tag> ops) {
        Codec<T> codec = type.codec();
        CompoundTag tag = new CompoundTag();
        tag.put("data", (Tag)codec.encodeStart(ops, (Object)data).getOrThrow());
        NbtUtils.addCurrentDataVersion(tag);
        return tag;
    }

    private void tryWrite(SavedDataType<?> type, CompoundTag tag) {
        Path path = this.getDataFile(type.id());
        try {
            FileUtil.createDirectoriesSafe(path.getParent());
            NbtIo.writeCompressed(tag, path);
        }
        catch (IOException e) {
            LOGGER.error("Could not save data to {}", (Object)path.getFileName(), (Object)e);
        }
    }

    public void saveAndJoin() {
        this.scheduleSave().join();
    }

    @Override
    public void close() {
        if (this.closed) {
            throw new IllegalStateException("Trying to close SavedDataStorage when it is already closed");
        }
        this.saveAndJoin();
        this.closed = true;
    }

    private /* synthetic */ void lambda$collectDirtyTagsToSave$0(Map tagsToSave, RegistryOps ops, SavedDataType type, Optional optional) {
        optional.filter(SavedData::isDirty).ifPresent(data -> {
            tagsToSave.put(type, this.encodeUnchecked(type, (SavedData)data, ops));
            data.setDirty(false);
        });
    }
}

