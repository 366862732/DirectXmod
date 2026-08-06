/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.serialization.Dynamic
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.jspecify.annotations.Nullable;

public class SimpleRegionStorage
implements AutoCloseable {
    private final IOWorker worker;
    private final DataFixer fixerUpper;
    private final DataFixTypes dataFixType;

    public SimpleRegionStorage(RegionStorageInfo info, Path folder, DataFixer fixerUpper, boolean syncWrites, DataFixTypes dataFixType) {
        this.fixerUpper = fixerUpper;
        this.dataFixType = dataFixType;
        this.worker = new IOWorker(info, folder, syncWrites);
    }

    public boolean isOldChunkAround(ChunkPos pos, int range) {
        return this.worker.isOldChunkAround(pos, range);
    }

    public CompletableFuture<Optional<CompoundTag>> read(ChunkPos pos) {
        return this.worker.loadAsync(pos);
    }

    public CompletableFuture<Void> write(ChunkPos pos, CompoundTag value) {
        return this.write(pos, () -> value);
    }

    public CompletableFuture<Void> write(ChunkPos pos, Supplier<CompoundTag> supplier) {
        return this.worker.store(pos, supplier);
    }

    public CompoundTag upgradeChunkTag(CompoundTag chunkTag, int defaultVersion, @Nullable CompoundTag dataFixContextTag, int targetVersion) {
        int version = NbtUtils.getDataVersion(chunkTag, defaultVersion);
        if (version >= targetVersion) {
            return chunkTag;
        }
        try {
            SimpleRegionStorage.injectDatafixingContext(chunkTag, dataFixContextTag);
            chunkTag = this.dataFixType.update(this.fixerUpper, chunkTag, version, targetVersion);
            SimpleRegionStorage.removeDatafixingContext(chunkTag);
            NbtUtils.addDataVersion(chunkTag, targetVersion);
            return chunkTag;
        }
        catch (Exception e) {
            CrashReport report = CrashReport.forThrowable(e, "Updated chunk");
            CrashReportCategory details = report.addCategory("Updated chunk details");
            details.setDetail("Data version", version);
            details.setDetail("Target version", targetVersion);
            throw new ReportedException(report);
        }
    }

    public CompoundTag upgradeChunkTag(CompoundTag chunkTag, int defaultVersion) {
        return this.upgradeChunkTag(chunkTag, defaultVersion, null, SharedConstants.getCurrentVersion().dataVersion().version());
    }

    public Dynamic<Tag> upgradeChunkTag(Dynamic<Tag> chunkTag, int defaultVersion) {
        return new Dynamic(chunkTag.getOps(), (Object)this.upgradeChunkTag((CompoundTag)chunkTag.getValue(), defaultVersion, null, SharedConstants.getCurrentVersion().dataVersion().version()));
    }

    public static void injectDatafixingContext(CompoundTag chunkTag, @Nullable CompoundTag contextTag) {
        if (contextTag != null) {
            chunkTag.put("__context", contextTag);
        }
    }

    private static void removeDatafixingContext(CompoundTag chunkTag) {
        chunkTag.remove("__context");
    }

    public CompletableFuture<Void> synchronize(boolean flush) {
        return this.worker.synchronize(flush);
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }

    public ChunkScanAccess chunkScanner() {
        return this.worker;
    }

    public RegionStorageInfo storageInfo() {
        return this.worker.storageInfo();
    }
}

