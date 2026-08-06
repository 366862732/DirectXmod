/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.hash.HashCode
 *  com.google.common.hash.Hashing
 *  com.google.common.hash.HashingOutputStream
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.io.IOUtils
 *  org.slf4j.Logger
 */
package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt
implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput output;
    private final Iterable<Path> inputFolders;
    private final List<Filter> filters = Lists.newArrayList();

    public SnbtToNbt(PackOutput output, Path inputFolder) {
        this(output, List.of(inputFolder));
    }

    public SnbtToNbt(PackOutput output, Iterable<Path> inputFolders) {
        this.output = output;
        this.inputFolders = inputFolders;
    }

    public SnbtToNbt addFilter(Filter filter) {
        this.filters.add(filter);
        return this;
    }

    private CompoundTag applyFilters(String name, CompoundTag input) {
        CompoundTag result = input;
        for (Filter filter : this.filters) {
            result = filter.apply(name, result);
        }
        return result;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path output = this.output.getOutputFolder();
        ArrayList tasks = Lists.newArrayList();
        for (Path input : this.inputFolders) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                CompletableFuture<Void> completableFuture;
                block8: {
                    Stream<Path> files = Files.walk(input, new FileVisitOption[0]);
                    try {
                        completableFuture = CompletableFuture.allOf((CompletableFuture[])files.filter(path -> path.toString().endsWith(".snbt")).map(path -> CompletableFuture.runAsync(() -> {
                            TaskResult structure = this.readStructure((Path)path, this.getName(input, (Path)path));
                            this.storeStructureIfChanged(cache, structure, output);
                        }, Util.backgroundExecutor().forName("SnbtToNbt"))).toArray(CompletableFuture[]::new));
                        if (files == null) break block8;
                    }
                    catch (Throwable t$) {
                        try {
                            if (files != null) {
                                try {
                                    files.close();
                                }
                                catch (Throwable x2) {
                                    t$.addSuppressed(x2);
                                }
                            }
                            throw t$;
                        }
                        catch (Exception e) {
                            throw new RuntimeException("Failed to read structure input directory, aborting", e);
                        }
                    }
                    files.close();
                }
                return completableFuture;
            }, Util.backgroundExecutor().forName("SnbtToNbt")).thenCompose(v -> v));
        }
        return Util.sequenceFailFast(tasks);
    }

    @Override
    public final String getName() {
        return "SNBT -> NBT";
    }

    private String getName(Path root, Path path) {
        String name = root.relativize(path).toString().replaceAll("\\\\", "/");
        return name.substring(0, name.length() - ".snbt".length());
    }

    private TaskResult readStructure(Path path, String name) {
        TaskResult taskResult;
        block8: {
            BufferedReader reader = Files.newBufferedReader(path);
            try {
                String input = IOUtils.toString((Reader)reader);
                CompoundTag updated = this.applyFilters(name, NbtUtils.snbtToStructure(input));
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                HashingOutputStream hos = new HashingOutputStream(Hashing.sha1(), (OutputStream)bos);
                NbtIo.writeCompressed(updated, (OutputStream)hos);
                byte[] bytes = bos.toByteArray();
                HashCode hash = hos.hash();
                taskResult = new TaskResult(name, bytes, hash);
                if (reader == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            reader.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Throwable t) {
                    throw new StructureConversionException(path, t);
                }
            }
            reader.close();
        }
        return taskResult;
    }

    private void storeStructureIfChanged(CachedOutput cache, TaskResult task, Path output) {
        Path destination = output.resolve(task.name + ".nbt");
        try {
            cache.writeIfNeeded(destination, task.payload, task.hash);
        }
        catch (IOException e) {
            LOGGER.error("Couldn't write structure {} at {}", new Object[]{task.name, destination, e});
        }
    }

    @FunctionalInterface
    public static interface Filter {
        public CompoundTag apply(String var1, CompoundTag var2);
    }

    private record TaskResult(String name, byte[] payload, HashCode hash) {
    }

    private static class StructureConversionException
    extends RuntimeException {
        public StructureConversionException(Path path, Throwable t) {
            super(path.toAbsolutePath().toString(), t);
        }
    }
}

