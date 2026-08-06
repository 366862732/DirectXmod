/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.hash.HashCode
 *  com.google.common.hash.Hashing
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.lang3.mutable.MutableInt
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.WorldVersion;
import net.minecraft.data.CachedOutput;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class HashCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String HEADER_MARKER = "// ";
    private final Path rootDir;
    private final Path cacheDir;
    private final String versionId;
    private final Map<String, ProviderCache> caches;
    private final Set<String> cachesToWrite = new HashSet<String>();
    private final Set<Path> cachePaths = new HashSet<Path>();
    private final int initialCount;
    private int writes;

    private Path getProviderCachePath(String provider) {
        return this.cacheDir.resolve(Hashing.sha1().hashString((CharSequence)provider, StandardCharsets.UTF_8).toString());
    }

    public HashCache(Path rootDir, Collection<String> providerIds, WorldVersion version) throws IOException {
        this.versionId = version.id();
        this.rootDir = rootDir;
        this.cacheDir = rootDir.resolve(".cache");
        Files.createDirectories(this.cacheDir, new FileAttribute[0]);
        HashMap<String, ProviderCache> loadedCaches = new HashMap<String, ProviderCache>();
        int initialCount = 0;
        for (String providerId : providerIds) {
            Path providerCachePath = this.getProviderCachePath(providerId);
            this.cachePaths.add(providerCachePath);
            ProviderCache providerCache = HashCache.readCache(rootDir, providerCachePath);
            loadedCaches.put(providerId, providerCache);
            initialCount += providerCache.count();
        }
        this.caches = loadedCaches;
        this.initialCount = initialCount;
    }

    private static ProviderCache readCache(Path rootDir, Path providerCachePath) {
        if (Files.isReadable(providerCachePath)) {
            try {
                return ProviderCache.load(rootDir, providerCachePath);
            }
            catch (Exception e) {
                LOGGER.warn("Failed to parse cache {}, discarding", (Object)providerCachePath, (Object)e);
            }
        }
        return new ProviderCache("unknown", (ImmutableMap<Path, HashCode>)ImmutableMap.of());
    }

    public boolean shouldRunInThisVersion(String providerId) {
        ProviderCache result = this.caches.get(providerId);
        return result == null || !result.version.equals(this.versionId);
    }

    public CompletableFuture<UpdateResult> generateUpdate(String providerId, UpdateFunction function) {
        ProviderCache existingCache = this.caches.get(providerId);
        if (existingCache == null) {
            throw new IllegalStateException("Provider not registered: " + providerId);
        }
        CacheUpdater output = new CacheUpdater(providerId, this.versionId, existingCache);
        return function.update(output).thenApply(unused -> output.close());
    }

    public void applyUpdate(UpdateResult result) {
        this.caches.put(result.providerId(), result.cache());
        this.cachesToWrite.add(result.providerId());
        this.writes += result.writes();
    }

    public void purgeStaleAndWrite() throws IOException {
        final HashSet allowedFiles = new HashSet();
        this.caches.forEach((providerId, cache) -> {
            if (this.cachesToWrite.contains(providerId)) {
                Path cachePath = this.getProviderCachePath((String)providerId);
                cache.save(this.rootDir, cachePath, DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now()) + "\t" + providerId);
            }
            allowedFiles.addAll(cache.data().keySet());
        });
        final MutableInt found = new MutableInt();
        final MutableInt removed = new MutableInt();
        Files.walkFileTree(this.rootDir, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){
            final /* synthetic */ HashCache this$0;
            {
                HashCache hashCache = this$0;
                Objects.requireNonNull(hashCache);
                this.this$0 = hashCache;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (this.this$0.cachePaths.contains(file)) {
                    return FileVisitResult.CONTINUE;
                }
                found.increment();
                if (allowedFiles.contains(file)) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    Files.delete(file);
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to delete file {}", (Object)file, (Object)e);
                }
                removed.increment();
                return FileVisitResult.CONTINUE;
            }
        });
        LOGGER.info("Caching: total files: {}, old count: {}, new count: {}, removed stale: {}, written: {}", new Object[]{found, this.initialCount, allowedFiles.size(), removed, this.writes});
    }

    private record ProviderCache(String version, ImmutableMap<Path, HashCode> data) {
        public @Nullable HashCode get(Path path) {
            return (HashCode)this.data.get((Object)path);
        }

        public int count() {
            return this.data.size();
        }

        public static ProviderCache load(Path rootDir, Path cacheFile) throws IOException {
            try (BufferedReader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8);){
                String header = reader.readLine();
                if (!header.startsWith(HashCache.HEADER_MARKER)) {
                    throw new IllegalStateException("Missing cache file header");
                }
                String[] headerFields = header.substring(HashCache.HEADER_MARKER.length()).split("\t", 2);
                String savedVersionId = headerFields[0];
                ImmutableMap.Builder result = ImmutableMap.builder();
                reader.lines().forEach(s -> {
                    int i = s.indexOf(32);
                    result.put((Object)rootDir.resolve(s.substring(i + 1)), (Object)HashCode.fromString((String)s.substring(0, i)));
                });
                ProviderCache providerCache = new ProviderCache(savedVersionId, (ImmutableMap<Path, HashCode>)result.build());
                return providerCache;
            }
        }

        public void save(Path rootDir, Path cacheFile, String extraHeaderInfo) {
            try (BufferedWriter output = Files.newBufferedWriter(cacheFile, StandardCharsets.UTF_8, new OpenOption[0]);){
                output.write(HashCache.HEADER_MARKER);
                output.write(this.version);
                output.write(9);
                output.write(extraHeaderInfo);
                output.newLine();
                for (Map.Entry e : this.data.entrySet()) {
                    output.write(((HashCode)e.getValue()).toString());
                    output.write(32);
                    output.write(rootDir.relativize((Path)e.getKey()).toString());
                    output.newLine();
                }
            }
            catch (IOException e) {
                LOGGER.warn("Unable write cachefile {}: {}", (Object)cacheFile, (Object)e);
            }
        }
    }

    private static class CacheUpdater
    implements CachedOutput {
        private final String provider;
        private final ProviderCache oldCache;
        private final ProviderCacheBuilder newCache;
        private final AtomicInteger writes = new AtomicInteger();
        private volatile boolean closed;

        private CacheUpdater(String provider, String newVersionId, ProviderCache oldCache) {
            this.provider = provider;
            this.oldCache = oldCache;
            this.newCache = new ProviderCacheBuilder(newVersionId);
        }

        private boolean shouldWrite(Path path, HashCode hash) {
            return !Objects.equals(this.oldCache.get(path), hash) || !Files.exists(path, new LinkOption[0]);
        }

        @Override
        public void writeIfNeeded(Path path, byte[] input, HashCode hash) throws IOException {
            if (this.closed) {
                throw new IllegalStateException("Cannot write to cache as it has already been closed");
            }
            if (this.shouldWrite(path, hash)) {
                this.writes.incrementAndGet();
                Files.createDirectories(path.getParent(), new FileAttribute[0]);
                Files.write(path, input, new OpenOption[0]);
            }
            this.newCache.put(path, hash);
        }

        public UpdateResult close() {
            this.closed = true;
            return new UpdateResult(this.provider, this.newCache.build(), this.writes.get());
        }
    }

    @FunctionalInterface
    public static interface UpdateFunction {
        public CompletableFuture<?> update(CachedOutput var1);
    }

    public record UpdateResult(String providerId, ProviderCache cache, int writes) {
    }

    private record ProviderCacheBuilder(String version, ConcurrentMap<Path, HashCode> data) {
        public ProviderCacheBuilder(String version) {
            this(version, new ConcurrentHashMap<Path, HashCode>());
        }

        public void put(Path path, HashCode hash) {
            this.data.put(path, hash);
        }

        public ProviderCache build() {
            return new ProviderCache(this.version, (ImmutableMap<Path, HashCode>)ImmutableMap.copyOf(this.data));
        }
    }
}

