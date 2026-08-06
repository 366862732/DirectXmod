/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.MapMaker
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.resources;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public class ResourceKey<T> {
    private static final ConcurrentMap<InternKey, ResourceKey<?>> VALUES = new MapMaker().weakValues().makeMap();
    private final Identifier registryName;
    private final Identifier identifier;

    public static <T> Codec<ResourceKey<T>> codec(ResourceKey<? extends Registry<T>> registryName) {
        return Identifier.CODEC.xmap(name -> ResourceKey.create(registryName, name), ResourceKey::identifier);
    }

    public static <T> StreamCodec<ByteBuf, ResourceKey<T>> streamCodec(ResourceKey<? extends Registry<T>> registryName) {
        return Identifier.STREAM_CODEC.map(name -> ResourceKey.create(registryName, name), ResourceKey::identifier);
    }

    public static <T> ResourceKey<T> create(ResourceKey<? extends Registry<T>> registryName, Identifier location) {
        return ResourceKey.create(registryName.identifier, location);
    }

    public static <T> ResourceKey<Registry<T>> createRegistryKey(Identifier identifier) {
        return ResourceKey.create(Registries.ROOT_REGISTRY_NAME, identifier);
    }

    private static <T> ResourceKey<T> create(Identifier registryName, Identifier identifier) {
        return VALUES.computeIfAbsent(new InternKey(registryName, identifier), k -> new ResourceKey(k.registry, k.identifier));
    }

    private ResourceKey(Identifier registryName, Identifier identifier) {
        this.registryName = registryName;
        this.identifier = identifier;
    }

    public String toString() {
        return "ResourceKey[" + String.valueOf(this.registryName) + " / " + String.valueOf(this.identifier) + "]";
    }

    public boolean isFor(ResourceKey<? extends Registry<?>> registry) {
        return this.registryName.equals(registry.identifier());
    }

    public <E> Optional<ResourceKey<E>> cast(ResourceKey<? extends Registry<E>> registry) {
        return this.isFor(registry) ? Optional.of(this) : Optional.empty();
    }

    public <E> ResourceKey<E> dependent(ResourceKey<? extends Registry<E>> registryKey, String suffix) {
        return ResourceKey.create(registryKey, this.identifier.withSuffix(suffix));
    }

    public <E> ResourceKey<E> dependent(ResourceKey<? extends Registry<E>> registryKey, UnaryOperator<String> decoration) {
        return ResourceKey.create(registryKey, this.identifier.withPath(decoration));
    }

    public Identifier identifier() {
        return this.identifier;
    }

    public Identifier registry() {
        return this.registryName;
    }

    public ResourceKey<Registry<T>> registryKey() {
        return ResourceKey.createRegistryKey(this.registryName);
    }

    private record InternKey(Identifier registry, Identifier identifier) {
    }
}

