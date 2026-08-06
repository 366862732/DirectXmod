/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

public class HolderSetCodec<E>
implements Codec<HolderSet<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;
    private final Codec<Holder<E>> elementCodec;
    private final Codec<List<Holder<E>>> homogenousListCodec;
    private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

    private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> elementCodec, boolean alwaysUseList) {
        Codec listCodec = elementCodec.listOf().validate(ExtraCodecs.ensureHomogenous(Holder::kind));
        if (alwaysUseList) {
            return listCodec;
        }
        return ExtraCodecs.compactListCodec(elementCodec, listCodec);
    }

    public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> registryKey, Codec<Holder<E>> elementCodec, boolean alwaysUseList) {
        return new HolderSetCodec<E>(registryKey, elementCodec, alwaysUseList);
    }

    private HolderSetCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<Holder<E>> elementCodec, boolean alwaysUseList) {
        this.registryKey = registryKey;
        this.elementCodec = elementCodec;
        this.homogenousListCodec = HolderSetCodec.homogenousList(elementCodec, alwaysUseList);
        this.registryAwareCodec = Codec.either(TagKey.hashedCodec(registryKey), this.homogenousListCodec);
    }

    public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> ops, T input) {
        RegistryOps registryOps;
        Optional registryOptional;
        if (ops instanceof RegistryOps && (registryOptional = (registryOps = (RegistryOps)ops).getter(this.registryKey)).isPresent()) {
            HolderGetter registry = registryOptional.get();
            return this.registryAwareCodec.decode(ops, input).flatMap(p -> {
                DataResult result = (DataResult)((Either)p.getFirst()).map(tag -> HolderSetCodec.lookupTag(registry, tag), values -> DataResult.success(HolderSet.direct(values)));
                return result.map(holders -> Pair.of((Object)holders, (Object)p.getSecond()));
            });
        }
        return this.decodeWithoutRegistry(ops, input);
    }

    private static <E> DataResult<HolderSet<E>> lookupTag(HolderGetter<E> registry, TagKey<E> key) {
        return registry.get(key).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Missing tag: '" + String.valueOf(key.location()) + "' in '" + String.valueOf(key.registry().identifier()) + "'"));
    }

    public <T> DataResult<T> encode(HolderSet<E> input, DynamicOps<T> ops, T prefix) {
        RegistryOps registryOps;
        Optional maybeOwner;
        if (ops instanceof RegistryOps && (maybeOwner = (registryOps = (RegistryOps)ops).owner(this.registryKey)).isPresent()) {
            if (!input.canSerializeIn(maybeOwner.get())) {
                return DataResult.error(() -> "HolderSet " + String.valueOf(input) + " is not valid in current registry set");
            }
            return this.registryAwareCodec.encode((Object)input.unwrap().mapRight(List::copyOf), ops, prefix);
        }
        return this.encodeWithoutRegistry(input, ops, prefix);
    }

    private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> ops, T input) {
        return this.elementCodec.listOf().decode(ops, input).flatMap(p -> {
            ArrayList<Holder.Direct> directHolders = new ArrayList<Holder.Direct>();
            for (Holder holder : (List)p.getFirst()) {
                if (holder instanceof Holder.Direct) {
                    Holder.Direct direct = (Holder.Direct)holder;
                    directHolders.add(direct);
                    continue;
                }
                return DataResult.error(() -> "Can't decode element " + String.valueOf(holder) + " without registry");
            }
            return DataResult.success((Object)new Pair(HolderSet.direct(directHolders), p.getSecond()));
        });
    }

    private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> input, DynamicOps<T> ops, T prefix) {
        return this.homogenousListCodec.encode(input.stream().toList(), ops, prefix);
    }
}

