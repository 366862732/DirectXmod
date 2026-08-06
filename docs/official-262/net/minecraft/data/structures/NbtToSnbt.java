/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.Hashing
 *  com.google.common.hash.HashingOutputStream
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.data.structures;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class NbtToSnbt
implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Iterable<Path> inputFolders;
    private final PackOutput output;

    public NbtToSnbt(PackOutput output, Collection<Path> inputFolders) {
        this.inputFolders = inputFolders;
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path output = this.output.getOutputFolder();
        ArrayList<CompletionStage> tasks = new ArrayList<CompletionStage>();
        for (Path input : this.inputFolders) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                CompletableFuture<Void> completableFuture;
                block8: {
                    Stream<Path> walk = Files.walk(input, new FileVisitOption[0]);
                    try {
                        completableFuture = CompletableFuture.allOf((CompletableFuture[])walk.filter(path -> path.toString().endsWith(".nbt")).map(path -> CompletableFuture.runAsync(() -> NbtToSnbt.convertStructure(cache, path, NbtToSnbt.getName(input, path), output), Util.ioPool())).toArray(CompletableFuture[]::new));
                        if (walk == null) break block8;
                    }
                    catch (Throwable t$) {
                        try {
                            if (walk != null) {
                                try {
                                    walk.close();
                                }
                                catch (Throwable x2) {
                                    t$.addSuppressed(x2);
                                }
                            }
                            throw t$;
                        }
                        catch (IOException e) {
                            LOGGER.error("Failed to read structure input directory", (Throwable)e);
                            return CompletableFuture.completedFuture(null);
                        }
                    }
                    walk.close();
                }
                return completableFuture;
            }, Util.backgroundExecutor().forName("NbtToSnbt")).thenCompose(v -> v));
        }
        return CompletableFuture.allOf((CompletableFuture[])tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public final String getName() {
        return "NBT -> SNBT";
    }

    private static String getName(Path root, Path path) {
        String name = root.relativize(path).toString().replaceAll("\\\\", "/");
        return name.substring(0, name.length() - ".nbt".length());
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static @Nullable Path convertStructure(CachedOutput cache, Path path, String name, Path output) {
        try (InputStream rawInput = Files.newInputStream(path, new OpenOption[0]);){
            Path path2;
            try (FastBufferedInputStream input = new FastBufferedInputStream(rawInput);){
                Path resultPath = output.resolve(name + ".snbt");
                NbtToSnbt.writeSnbt(cache, resultPath, NbtUtils.structureToSnbt(NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap())));
                LOGGER.info("Converted {} from NBT to SNBT", (Object)name);
                path2 = resultPath;
            }
            return path2;
        }
        catch (IOException e) {
            LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", new Object[]{name, path, e});
            return null;
        }
    }

    public static void writeSnbt(CachedOutput cache, Path destination, String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha1(), (OutputStream)bytes);
        hashedBytes.write(text.getBytes(StandardCharsets.UTF_8));
        hashedBytes.write(10);
        cache.writeIfNeeded(destination, bytes.toByteArray(), hashedBytes.hash());
    }
}

