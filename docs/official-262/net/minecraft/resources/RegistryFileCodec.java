/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Lifecycle
 */
package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public final class RegistryFileCodec<E>
implements Codec<Holder<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;
    private final Codec<E> elementCodec;
    private final boolean allowInline;

    public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec) {
        return RegistryFileCodec.create(registryKey, elementCodec, true);
    }

    public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec, boolean allowInline) {
        return new RegistryFileCodec<E>(registryKey, elementCodec, allowInline);
    }

    private RegistryFileCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<E> elementCodec, boolean allowInline) {
        this.registryKey = registryKey;
        this.elementCodec = elementCodec;
        this.allowInline = allowInline;
    }

    public <T> DataResult<T> encode(Holder<E> input, DynamicOps<T> ops, T prefix) {
        RegistryOps registryOps;
        Optional maybeOwner;
        if (ops instanceof RegistryOps && (maybeOwner = (registryOps = (RegistryOps)ops).owner(this.registryKey)).isPresent()) {
            if (!input.canSerializeIn(maybeOwner.get())) {
                return DataResult.error(() -> "Element " + String.valueOf(input) + " is not valid in current registry set");
            }
            return (DataResult)input.unwrap().map(id -> Identifier.CODEC.encode((Object)id.identifier(), ops, prefix), value -> this.elementCodec.encode(value, ops, prefix));
        }
        return this.elementCodec.encode(input.value(), ops, prefix);
    }

    public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> ops, T input) {
        if (ops instanceof RegistryOps) {
            RegistryOps registryOps = (RegistryOps)ops;
            Optional maybeLookup = registryOps.getter(this.registryKey);
            if (maybeLookup.isEmpty()) {
                return DataResult.error(() -> "Registry does not exist: " + String.valueOf(this.registryKey));
            }
            HolderGetter lookup = maybeLookup.get();
            DataResult decoded = Identifier.CODEC.decode(ops, input);
            if (decoded.result().isEmpty()) {
                if (!this.allowInline) {
                    return DataResult.error(() -> "Inline definitions not allowed here");
                }
                return this.elementCodec.decode(ops, input).map(p -> p.mapFirst(Holder::direct));
            }
            Pair pair = (Pair)decoded.result().get();
            ResourceKey elementKey = ResourceKey.create(this.registryKey, (Identifier)pair.getFirst());
            return lookup.get(elementKey).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Failed to get element " + String.valueOf(elementKey))).map(h -> Pair.of((Object)h, (Object)pair.getSecond())).setLifecycle(Lifecycle.stable());
        }
        return this.elementCodec.decode(ops, input).map(p -> p.mapFirst(Holder::direct));
    }

    public String toString() {
        return "RegistryFileCodec[" + String.valueOf(this.registryKey) + " " + String.valueOf(this.elementCodec) + "]";
    }
}

