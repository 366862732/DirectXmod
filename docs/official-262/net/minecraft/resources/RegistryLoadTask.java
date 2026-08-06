/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Decoder
 *  com.mojang.serialization.Lifecycle
 */
package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.ConcurrentHolderGetter;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StrictJsonParser;

public abstract class RegistryLoadTask<T> {
    private final Object registryWriteLock = new Object();
    protected final RegistryDataLoader.RegistryData<T> data;
    private final WritableRegistry<T> registry;
    protected final ConcurrentHolderGetter<T> concurrentRegistrationGetter;
    protected final Map<ResourceKey<?>, Exception> loadingErrors;
    private volatile boolean elementsRegistered;

    protected RegistryLoadTask(RegistryDataLoader.RegistryData<T> data, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> loadingErrors) {
        this.data = data;
        this.registry = new MappedRegistry<T>(data.key(), lifecycle);
        this.loadingErrors = loadingErrors;
        this.concurrentRegistrationGetter = new ConcurrentHolderGetter<T>(this.registryWriteLock, this.registry.createRegistrationLookup());
    }

    protected ResourceKey<? extends Registry<T>> registryKey() {
        return this.registry.key();
    }

    protected Registry<T> readOnlyRegistry() {
        if (!this.elementsRegistered) {
            throw new IllegalStateException("Elements not registered");
        }
        return this.registry;
    }

    public abstract CompletableFuture<?> load(RegistryOps.RegistryInfoLookup var1, Executor var2);

    public RegistryOps.RegistryInfo<?> createRegistryInfo() {
        return new RegistryOps.RegistryInfo<T>(this.registry, this.concurrentRegistrationGetter, this.registry.registryLifecycle());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void registerElements(Stream<PendingRegistration<T>> elements) {
        Object object = this.registryWriteLock;
        synchronized (object) {
            elements.forEach(element -> element.value.ifLeft(value -> this.registry.register(element.key, value, element.registrationInfo)).ifRight(error -> this.loadingErrors.put(element.key, (Exception)error)));
            this.elementsRegistered = true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void registerTags(Map<TagKey<T>, List<Holder<T>>> pendingTags) {
        Object object = this.registryWriteLock;
        synchronized (object) {
            this.registry.bindTags(pendingTags);
        }
    }

    public boolean freezeRegistry(Map<ResourceKey<?>, Exception> loadingErrors) {
        try {
            this.registry.freeze();
            return true;
        }
        catch (Exception e) {
            loadingErrors.put(this.registry.key(), e);
            return false;
        }
    }

    public Optional<Registry<T>> validateRegistry(Map<ResourceKey<?>, Exception> loadingErrors) {
        HashMap registryErrors = new HashMap();
        this.data.validator().validate(this.registry, registryErrors);
        if (registryErrors.isEmpty()) {
            return Optional.of(this.registry);
        }
        loadingErrors.putAll(registryErrors);
        return Optional.empty();
    }

    protected record PendingRegistration<T>(ResourceKey<T> key, Either<T, Exception> value, RegistrationInfo registrationInfo) {
        public static <T> Either<T, Exception> loadFromResource(Decoder<T> elementDecoder, RegistryOps<JsonElement> ops, ResourceKey<T> elementKey, Resource thunk) {
            Either either;
            block8: {
                BufferedReader reader = thunk.openAsReader();
                try {
                    JsonElement json = StrictJsonParser.parse(reader);
                    either = Either.left((Object)elementDecoder.parse(ops, (Object)json).getOrThrow());
                    if (reader == null) break block8;
                }
                catch (Throwable throwable) {
                    try {
                        if (reader != null) {
                            try {
                                ((Reader)reader).close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    catch (Exception e) {
                        return Either.right((Object)new IllegalStateException(String.format(Locale.ROOT, "Failed to parse %s from pack %s", elementKey.identifier(), thunk.sourcePackId()), e));
                    }
                }
                ((Reader)reader).close();
            }
            return either;
        }

        public static <T> Either<T, Exception> findAndLoadFromResource(Decoder<T> elementDecoder, RegistryOps<JsonElement> ops, ResourceKey<T> elementKey, FileToIdConverter converter, ResourceProvider resourceProvider) {
            Identifier resourceId = converter.idToFile(elementKey.identifier());
            return resourceProvider.getResource(resourceId).map(resource -> PendingRegistration.loadFromResource(elementDecoder, ops, elementKey, resource)).orElseGet(() -> Either.right((Object)new IllegalStateException(String.format(Locale.ROOT, "Failed to find resource %s for element %s", resourceId, elementKey.identifier()))));
        }

        public static <T> Either<T, Exception> loadFromNetwork(Decoder<T> elementDecoder, RegistryOps<Tag> ops, ResourceKey<T> elementKey, Tag contents) {
            try {
                DataResult parseResult = elementDecoder.parse(ops, (Object)contents);
                return Either.left((Object)parseResult.getOrThrow());
            }
            catch (Exception e) {
                return Either.right((Object)new IllegalStateException(String.format(Locale.ROOT, "Failed to parse value %s for key %s from server", contents, elementKey.identifier()), e));
            }
        }
    }
}

