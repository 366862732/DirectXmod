/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Keyable
 *  com.mojang.serialization.Lifecycle
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.component.DataComponentLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public interface Registry<T>
extends IdMap<T>,
Keyable,
HolderLookup.RegistryLookup<T> {
    @Override
    public ResourceKey<? extends Registry<T>> key();

    default public Codec<T> byNameCodec() {
        return this.referenceHolderWithLifecycle().flatComapMap(Holder.Reference::value, value -> this.safeCastToReference(this.wrapAsHolder(value)));
    }

    default public Codec<Holder<T>> holderByNameCodec() {
        return this.referenceHolderWithLifecycle().flatComapMap(holder -> holder, this::safeCastToReference);
    }

    private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
        Codec referenceCodec = Identifier.CODEC.comapFlatMap(name -> this.get((Identifier)name).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + String.valueOf(this.key()) + ": " + String.valueOf(name))), holder -> holder.key().identifier());
        return ExtraCodecs.overrideLifecycle(referenceCodec, e -> this.registrationInfo(e.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental()));
    }

    private DataResult<Holder.Reference<T>> safeCastToReference(Holder<T> holder) {
        DataResult dataResult;
        if (holder instanceof Holder.Reference) {
            Holder.Reference reference = (Holder.Reference)holder;
            dataResult = DataResult.success((Object)reference);
        } else {
            dataResult = DataResult.error(() -> "Unregistered holder in " + String.valueOf(this.key()) + ": " + String.valueOf(holder));
        }
        return dataResult;
    }

    default public <U> Stream<U> keys(DynamicOps<U> ops) {
        return this.keySet().stream().map(k -> ops.createString(k.toString()));
    }

    public @Nullable Identifier getKey(T var1);

    public Optional<ResourceKey<T>> getResourceKey(T var1);

    @Override
    public int getId(@Nullable T var1);

    public @Nullable T getValue(@Nullable ResourceKey<T> var1);

    public @Nullable T getValue(@Nullable Identifier var1);

    public Optional<RegistrationInfo> registrationInfo(ResourceKey<T> var1);

    default public Optional<T> getOptional(@Nullable Identifier key) {
        return Optional.ofNullable(this.getValue(key));
    }

    default public Optional<T> getOptional(@Nullable ResourceKey<T> key) {
        return Optional.ofNullable(this.getValue(key));
    }

    public Optional<Holder.Reference<T>> getAny();

    default public T getValueOrThrow(ResourceKey<T> key) {
        T value = this.getValue(key);
        if (value == null) {
            throw new IllegalStateException("Missing key in " + String.valueOf(this.key()) + ": " + String.valueOf(key));
        }
        return value;
    }

    public Set<Identifier> keySet();

    public Set<Map.Entry<ResourceKey<T>, T>> entrySet();

    public Set<ResourceKey<T>> registryKeySet();

    public Optional<Holder.Reference<T>> getRandom(RandomSource var1);

    default public Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    public boolean containsKey(Identifier var1);

    public boolean containsKey(ResourceKey<T> var1);

    public static <T> T register(Registry<? super T> registry, String name, T value) {
        return Registry.register(registry, Identifier.parse(name), value);
    }

    public static <V, T extends V> T register(Registry<V> registry, Identifier location, T value) {
        return Registry.register(registry, ResourceKey.create(registry.key(), location), value);
    }

    public static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> key, T value) {
        ((WritableRegistry)registry).register(key, value, RegistrationInfo.BUILT_IN);
        return value;
    }

    public static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, ResourceKey<R> key, T value) {
        return ((WritableRegistry)registry).register(key, value, RegistrationInfo.BUILT_IN);
    }

    public static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, Identifier location, T value) {
        return Registry.registerForHolder(registry, ResourceKey.create(registry.key(), location), value);
    }

    public Registry<T> freeze();

    public Holder.Reference<T> createIntrusiveHolder(T var1);

    public Optional<Holder.Reference<T>> get(int var1);

    public Optional<Holder.Reference<T>> get(Identifier var1);

    public Holder<T> wrapAsHolder(T var1);

    default public Iterable<Holder<T>> getTagOrEmpty(TagKey<T> id) {
        return (Iterable)DataFixUtils.orElse((Optional)this.get(id), List.of());
    }

    public Stream<HolderSet.Named<T>> getTags();

    default public IdMap<Holder<T>> asHolderIdMap() {
        return new IdMap<Holder<T>>(this){
            final /* synthetic */ Registry this$0;
            {
                Registry registry = this$0;
                Objects.requireNonNull(registry);
                this.this$0 = registry;
            }

            @Override
            public int getId(Holder<T> thing) {
                return this.this$0.getId(thing.value());
            }

            @Override
            public @Nullable Holder<T> byId(int id) {
                return this.this$0.get(id).orElse(null);
            }

            @Override
            public int size() {
                return this.this$0.size();
            }

            @Override
            public Iterator<Holder<T>> iterator() {
                return this.this$0.listElements().map(e -> e).iterator();
            }
        };
    }

    public PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> var1);

    public DataComponentLookup<T> componentLookup();

    public static interface PendingTags<T> {
        public ResourceKey<? extends Registry<? extends T>> key();

        public HolderLookup.RegistryLookup<T> lookup();

        public void apply();

        public int size();
    }
}

