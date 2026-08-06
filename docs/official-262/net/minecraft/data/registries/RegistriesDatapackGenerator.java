/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Encoder
 *  com.mojang.serialization.JsonOps
 */
package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public class RegistriesDatapackGenerator
implements DataProvider {
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public RegistriesDatapackGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.registries = registries;
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.registries.thenCompose(access -> {
            RegistryOps registryOps = access.createSerializationContext(JsonOps.INSTANCE);
            return CompletableFuture.allOf((CompletableFuture[])RegistryDataLoader.WORLDGEN_REGISTRIES.stream().flatMap(v -> this.dumpRegistryCap(cache, (HolderLookup.Provider)access, registryOps, (RegistryDataLoader.RegistryData)v).stream()).toArray(CompletableFuture[]::new));
        });
    }

    private <T> Optional<CompletableFuture<?>> dumpRegistryCap(CachedOutput cache, HolderLookup.Provider registries, DynamicOps<JsonElement> writeOps, RegistryDataLoader.RegistryData<T> v) {
        ResourceKey registryKey = v.key();
        return registries.lookup(registryKey).map(registry -> {
            PackOutput.PathProvider pathProvider = this.output.createRegistryElementsPathProvider(registryKey);
            return CompletableFuture.allOf((CompletableFuture[])registry.listElements().map(e -> RegistriesDatapackGenerator.dumpValue(pathProvider.json(e.key().identifier()), cache, writeOps, v.elementCodec(), e.value())).toArray(CompletableFuture[]::new));
        });
    }

    private static <E> CompletableFuture<?> dumpValue(Path path, CachedOutput cache, DynamicOps<JsonElement> ops, Encoder<E> codec, E value) {
        return (CompletableFuture)codec.encodeStart(ops, value).mapOrElse(result -> DataProvider.saveStable(cache, result, path), error -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + String.valueOf(path) + "': " + error.message())));
    }

    @Override
    public final String getName() {
        return "Registries";
    }
}

