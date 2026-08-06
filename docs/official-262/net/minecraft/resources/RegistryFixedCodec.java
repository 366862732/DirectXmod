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

public final class RegistryFixedCodec<E>
implements Codec<Holder<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;

    public static <E> RegistryFixedCodec<E> create(ResourceKey<? extends Registry<E>> registryKey) {
        return new RegistryFixedCodec<E>(registryKey);
    }

    private RegistryFixedCodec(ResourceKey<? extends Registry<E>> registryKey) {
        this.registryKey = registryKey;
    }

    public <T> DataResult<T> encode(Holder<E> input, DynamicOps<T> ops, T prefix) {
        RegistryOps registryOps;
        Optional maybeOwner;
        if (ops instanceof RegistryOps && (maybeOwner = (registryOps = (RegistryOps)ops).owner(this.registryKey)).isPresent()) {
            if (!input.canSerializeIn(maybeOwner.get())) {
                return DataResult.error(() -> "Element " + String.valueOf(input) + " is not valid in current registry set");
            }
            return (DataResult)input.unwrap().map(id -> Identifier.CODEC.encode((Object)id.identifier(), ops, prefix), value -> DataResult.error(() -> "Elements from registry " + String.valueOf(this.registryKey) + " can't be serialized to a value"));
        }
        return DataResult.error(() -> "Can't access registry " + String.valueOf(this.registryKey));
    }

    public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> ops, T input) {
        RegistryOps registryOps;
        Optional lookup;
        if (ops instanceof RegistryOps && (lookup = (registryOps = (RegistryOps)ops).getter(this.registryKey)).isPresent()) {
            return Identifier.CODEC.decode(ops, input).flatMap(pair -> {
                Identifier id = (Identifier)pair.getFirst();
                return ((HolderGetter)lookup.get()).get(ResourceKey.create(this.registryKey, id)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Failed to get element " + String.valueOf(id))).map(h -> Pair.of((Object)h, (Object)pair.getSecond())).setLifecycle(Lifecycle.stable());
            });
        }
        return DataResult.error(() -> "Can't access registry " + String.valueOf(this.registryKey));
    }

    public String toString() {
        return "RegistryFixedCodec[" + String.valueOf(this.registryKey) + "]";
    }
}

