/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.access;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;

public class ChunkNbt
implements AutoCloseable {
    private final SimpleRegionStorage storage;
    private final int targetVersion;

    public ChunkNbt(RegionStorageInfo info, Path path, DataFixTypes type, int targetVersion) {
        this.targetVersion = targetVersion;
        this.storage = new SimpleRegionStorage(info, path, DataFixers.getDataFixer(), false, type);
    }

    public CompletableFuture<?> updateChunk(ChunkPos pos, CompoundTag dataFixContext, UnaryOperator<CompoundTag> fixer) {
        return this.storage.read(pos).thenComposeAsync(maybeTag -> {
            if (maybeTag.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            CompoundTag tag = (CompoundTag)maybeTag.get();
            tag = this.storage.upgradeChunkTag(tag, -1, dataFixContext, this.targetVersion);
            tag = (CompoundTag)fixer.apply(tag);
            return this.storage.write(pos, tag);
        }, (Executor)Util.backgroundExecutor());
    }

    @Override
    public void close() throws IOException {
        this.storage.close();
    }
}

