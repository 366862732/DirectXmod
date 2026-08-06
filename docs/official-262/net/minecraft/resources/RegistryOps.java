/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

public class RegistryOps<T>
extends DelegatingOps<T> {
    private final RegistryInfoLookup lookupProvider;

    public static <T> RegistryOps<T> create(DynamicOps<T> parent, HolderLookup.Provider lookupProvider) {
        return RegistryOps.create(parent, new HolderLookupAdapter(lookupProvider));
    }

    public static <T> RegistryOps<T> create(DynamicOps<T> parent, RegistryInfoLookup lookupProvider) {
        return new RegistryOps<T>(parent, lookupProvider);
    }

    public static <T> Dynamic<T> injectRegistryContext(Dynamic<T> dynamic, HolderLookup.Provider lookupProvider) {
        return new Dynamic(lookupProvider.createSerializationContext(dynamic.getOps()), dynamic.getValue());
    }

    private RegistryOps(DynamicOps<T> parent, RegistryInfoLookup lookupProvider) {
        super(parent);
        this.lookupProvider = lookupProvider;
    }

    public <U> RegistryOps<U> withParent(DynamicOps<U> parent) {
        if (parent == this.delegate) {
            return this;
        }
        return new RegistryOps<U>(parent, this.lookupProvider);
    }

    public <E> Optional<HolderOwner<E>> owner(ResourceKey<? extends Registry<? extends E>> registryKey) {
        return this.lookupProvider.lookup(registryKey).map(RegistryInfo::owner);
    }

    public <E> Optional<HolderGetter<E>> getter(ResourceKey<? extends Registry<? extends E>> registryKey) {
        return this.lookupProvider.lookup(registryKey).map(RegistryInfo::getter);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        RegistryOps ops = (RegistryOps)obj;
        return this.delegate.equals((Object)ops.delegate) && this.lookupProvider.equals(ops.lookupProvider);
    }

    public int hashCode() {
        return this.delegate.hashCode() * 31 + this.lookupProvider.hashCode();
    }

    public static <E, O> RecordCodecBuilder<O, HolderGetter<E>> retrieveGetter(ResourceKey<? extends Registry<? extends E>> registryKey) {
        return ExtraCodecs.retrieveContext(ops -> {
            if (ops instanceof RegistryOps) {
                RegistryOps registryOps = (RegistryOps)ops;
                return registryOps.lookupProvider.lookup(registryKey).map(r -> DataResult.success(r.getter(), (Lifecycle)r.elementsLifecycle())).orElseGet(() -> DataResult.error(() -> "Unknown registry: " + String.valueOf(registryKey)));
            }
            return DataResult.error(() -> "Not a registry ops");
        }).forGetter(e -> null);
    }

    public static <E, O> RecordCodecBuilder<O, Holder.Reference<E>> retrieveElement(ResourceKey<E> key) {
        ResourceKey registryKey = ResourceKey.createRegistryKey(key.registry());
        return ExtraCodecs.retrieveContext(ops -> {
            if (ops instanceof RegistryOps) {
                RegistryOps registryOps = (RegistryOps)ops;
                return registryOps.lookupProvider.lookup(registryKey).flatMap(r -> r.getter().get(key)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Can't find value: " + String.valueOf(key)));
            }
            return DataResult.error(() -> "Not a registry ops");
        }).forGetter(e -> null);
    }

    private static final class HolderLookupAdapter
    implements RegistryInfoLookup {
        private final HolderLookup.Provider lookupProvider;
        private final Map<ResourceKey<? extends Registry<?>>, Optional<? extends RegistryInfo<?>>> lookups = new ConcurrentHashMap();

        public HolderLookupAdapter(HolderLookup.Provider lookupProvider) {
            this.lookupProvider = lookupProvider;
        }

        public <E> Optional<RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
            return this.lookups.computeIfAbsent(registryKey, this::createLookup);
        }

        private Optional<RegistryInfo<Object>> createLookup(ResourceKey<? extends Registry<?>> key) {
            return this.lookupProvider.lookup(key).map(RegistryInfo::fromRegistryLookup);
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof HolderLookupAdapter)) return false;
            HolderLookupAdapter adapter = (HolderLookupAdapter)obj;
            if (!this.lookupProvider.equals(adapter.lookupProvider)) return false;
            return true;
        }

        public int hashCode() {
            return this.lookupProvider.hashCode();
        }
    }

    public static interface RegistryInfoLookup {
        public <T> Optional<RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);
    }

    public record RegistryInfo<T>(HolderOwner<T> owner, HolderGetter<T> getter, Lifecycle elementsLifecycle) {
        public static <T> RegistryInfo<T> fromRegistryLookup(HolderLookup.RegistryLookup<T> registry) {
            return new RegistryInfo<T>(registry, registry, registry.registryLifecycle());
        }
    }
}

