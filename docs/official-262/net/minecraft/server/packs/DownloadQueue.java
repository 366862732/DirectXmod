/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.HashCode
 *  com.google.common.hash.HashFunction
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.packs.DownloadCacheCleaner;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FileUtil;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Util;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DownloadQueue
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_KEPT_PACKS = 20;
    private final Path cacheDir;
    private final JsonEventLog<LogEntry> eventLog;
    private final ConsecutiveExecutor tasks = new ConsecutiveExecutor(Util.nonCriticalIoPool(), "download-queue");

    public DownloadQueue(Path cacheDir) throws IOException {
        this.cacheDir = cacheDir;
        FileUtil.createDirectoriesSafe(cacheDir);
        this.eventLog = JsonEventLog.open(LogEntry.CODEC, cacheDir.resolve("log.json"));
        DownloadCacheCleaner.vacuumCacheDir(cacheDir, 20);
    }

    private BatchResult runDownload(BatchConfig config, Map<UUID, DownloadRequest> requests) {
        BatchResult result = new BatchResult();
        requests.forEach((id, request) -> {
            Path targetDir = this.cacheDir.resolve(id.toString());
            Path downloadedFile = null;
            try {
                downloadedFile = HttpUtil.downloadFile(targetDir, request.url, config.headers, config.hashFunction, request.hash, config.maxSize, config.proxy, config.listener);
                result.downloaded.put((UUID)id, downloadedFile);
            }
            catch (Exception e) {
                LOGGER.error("Failed to download {}", (Object)request.url, (Object)e);
                result.failed.add((UUID)id);
            }
            try {
                this.eventLog.write(new LogEntry((UUID)id, request.url.toString(), Instant.now(), Optional.ofNullable(request.hash).map(HashCode::toString), downloadedFile != null ? this.getFileInfo(downloadedFile) : Either.left((Object)"download_failed")));
            }
            catch (Exception e) {
                LOGGER.error("Failed to log download of {}", (Object)request.url, (Object)e);
            }
        });
        return result;
    }

    private Either<String, FileInfoEntry> getFileInfo(Path downloadedFile) {
        try {
            long size = Files.size(downloadedFile);
            Path relativePath = this.cacheDir.relativize(downloadedFile);
            return Either.right((Object)new FileInfoEntry(relativePath.toString(), size));
        }
        catch (IOException e) {
            LOGGER.error("Failed to get file size of {}", (Object)downloadedFile, (Object)e);
            return Either.left((Object)"no_access");
        }
    }

    public CompletableFuture<BatchResult> downloadBatch(BatchConfig config, Map<UUID, DownloadRequest> requests) {
        return CompletableFuture.supplyAsync(() -> this.runDownload(config, requests), this.tasks::schedule);
    }

    @Override
    public void close() throws IOException {
        this.tasks.close();
        this.eventLog.close();
    }

    private record LogEntry(UUID id, String url, Instant time, Optional<String> hash, Either<String, FileInfoEntry> errorOrFileInfo) {
        public static final Codec<LogEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(LogEntry::id), (App)Codec.STRING.fieldOf("url").forGetter(LogEntry::url), (App)ExtraCodecs.INSTANT_ISO8601.fieldOf("time").forGetter(LogEntry::time), (App)Codec.STRING.optionalFieldOf("hash").forGetter(LogEntry::hash), (App)Codec.mapEither((MapCodec)Codec.STRING.fieldOf("error"), (MapCodec)FileInfoEntry.CODEC.fieldOf("file")).forGetter(LogEntry::errorOrFileInfo)).apply((Applicative)i, LogEntry::new));
    }

    public record BatchResult(Map<UUID, Path> downloaded, Set<UUID> failed) {
        public BatchResult() {
            this(new HashMap<UUID, Path>(), new HashSet<UUID>());
        }
    }

    public record BatchConfig(HashFunction hashFunction, int maxSize, Map<String, String> headers, Proxy proxy, HttpUtil.DownloadProgressListener listener) {
    }

    private record FileInfoEntry(String name, long size) {
        public static final Codec<FileInfoEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.fieldOf("name").forGetter(FileInfoEntry::name), (App)Codec.LONG.fieldOf("size").forGetter(FileInfoEntry::size)).apply((Applicative)i, FileInfoEntry::new));
    }

    public record DownloadRequest(URL url, @Nullable HashCode hash) {
    }
}

