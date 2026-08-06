/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.Hashing
 *  com.google.common.hash.HashingOutputStream
 *  com.google.gson.JsonElement
 *  com.google.gson.stream.JsonWriter
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.slf4j.Logger
 */
package net.minecraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public interface DataProvider {
    public static final ToIntFunction<String> FIXED_ORDER_FIELDS = (ToIntFunction)Util.make(new Object2IntOpenHashMap(), m -> {
        m.put((Object)"type", 0);
        m.put((Object)"parent", 1);
        m.defaultReturnValue(2);
    });
    public static final Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(e -> e);
    public static final Logger LOGGER = LogUtils.getLogger();

    public CompletableFuture<?> run(CachedOutput var1);

    public String getName();

    public static <T> CompletableFuture<?> saveAll(CachedOutput cache, Codec<T> codec, PackOutput.PathProvider pathProvider, Map<Identifier, T> entries) {
        return DataProvider.saveAll(cache, codec, pathProvider::json, entries);
    }

    public static <T, E> CompletableFuture<?> saveAll(CachedOutput cache, Codec<E> codec, Function<T, Path> pathGetter, Map<T, E> contents) {
        return DataProvider.saveAll(cache, (E e) -> (JsonElement)codec.encodeStart((DynamicOps)JsonOps.INSTANCE, e).getOrThrow(), pathGetter, contents);
    }

    public static <T, E> CompletableFuture<?> saveAll(CachedOutput cache, Function<E, JsonElement> serializer, Function<T, Path> pathGetter, Map<T, E> contents) {
        return CompletableFuture.allOf((CompletableFuture[])contents.entrySet().stream().map(entry -> {
            Path path = (Path)pathGetter.apply(entry.getKey());
            JsonElement json = (JsonElement)serializer.apply(entry.getValue());
            return DataProvider.saveStable(cache, json, path);
        }).toArray(CompletableFuture[]::new));
    }

    public static <T> CompletableFuture<?> saveStable(CachedOutput cache, HolderLookup.Provider registries, Codec<T> codec, T value, Path path) {
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        return DataProvider.saveStable(cache, ops, codec, value, path);
    }

    public static <T> CompletableFuture<?> saveStable(CachedOutput cache, Codec<T> codec, T value, Path path) {
        return DataProvider.saveStable(cache, (DynamicOps<JsonElement>)JsonOps.INSTANCE, codec, value, path);
    }

    private static <T> CompletableFuture<?> saveStable(CachedOutput cache, DynamicOps<JsonElement> ops, Codec<T> codec, T value, Path path) {
        JsonElement json = (JsonElement)codec.encodeStart(ops, value).getOrThrow();
        return DataProvider.saveStable(cache, json, path);
    }

    public static CompletableFuture<?> saveStable(CachedOutput cache, JsonElement root, Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha1(), (OutputStream)bytes);
                try (JsonWriter jsonWriter = new JsonWriter((Writer)new OutputStreamWriter((OutputStream)hashedBytes, StandardCharsets.UTF_8));){
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");
                    GsonHelper.writeValue(jsonWriter, root, KEY_COMPARATOR);
                }
                cache.writeIfNeeded(path, bytes.toByteArray(), hashedBytes.hash());
            }
            catch (IOException e) {
                LOGGER.error("Failed to save file to {}", (Object)path, (Object)e);
            }
        }, Util.backgroundExecutor().forName("saveStable"));
    }

    @FunctionalInterface
    public static interface Factory<T extends DataProvider> {
        public T create(PackOutput var1);
    }
}

