/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Lifecycle
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Cloner;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class RegistrySetBuilder {
    private final List<RegistryStub<?>> entries = new ArrayList();

    private static <T> HolderGetter<T> wrapContextLookup(final HolderLookup.RegistryLookup<T> original) {
        return new EmptyTagLookup<T>(original){

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                return original.get(id);
            }
        };
    }

    private static <T> HolderLookup.RegistryLookup<T> lookupFromMap(final ResourceKey<? extends Registry<? extends T>> key, final Lifecycle lifecycle, HolderOwner<T> owner, final Map<ResourceKey<T>, Holder.Reference<T>> entries) {
        return new EmptyTagRegistryLookup<T>(owner){

            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return key;
            }

            @Override
            public Lifecycle registryLifecycle() {
                return lifecycle;
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                return Optional.ofNullable((Holder.Reference)entries.get(id));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return entries.values().stream();
            }
        };
    }

    public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, RegistryBootstrap<T> bootstrap) {
        this.entries.add(new RegistryStub<T>(key, lifecycle, bootstrap));
        return this;
    }

    public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> key, RegistryBootstrap<T> bootstrap) {
        return this.add(key, Lifecycle.stable(), bootstrap);
    }

    private BuildState createState(RegistryAccess context) {
        BuildState state = BuildState.create(context, this.entries.stream().map(RegistryStub::key));
        this.entries.forEach(e -> e.apply(state));
        return state;
    }

    private static HolderLookup.Provider buildProviderWithContext(UniversalOwner owner, RegistryAccess context, Stream<HolderLookup.RegistryLookup<?>> newRegistries) {
        record Entry<T>(HolderLookup.RegistryLookup<T> lookup, RegistryOps.RegistryInfo<T> opsInfo) {
            public static <T> Entry<T> createForContextRegistry(HolderLookup.RegistryLookup<T> registryLookup) {
                return new Entry<T>(new EmptyTagLookupWrapper<T>(registryLookup, registryLookup), RegistryOps.RegistryInfo.fromRegistryLookup(registryLookup));
            }

            public static <T> Entry<T> createForNewRegistry(UniversalOwner owner, HolderLookup.RegistryLookup<T> registryLookup) {
                return new Entry(new EmptyTagLookupWrapper(owner.cast(), registryLookup), new RegistryOps.RegistryInfo(owner.cast(), registryLookup, registryLookup.registryLifecycle()));
            }
        }
        final HashMap lookups = new HashMap();
        context.registries().forEach(contextRegistry -> lookups.put(contextRegistry.key(), Entry.createForContextRegistry(contextRegistry.value())));
        newRegistries.forEach(newRegistry -> lookups.put(newRegistry.key(), Entry.createForNewRegistry(owner, newRegistry)));
        return new HolderLookup.Provider(){

            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return lookups.keySet().stream();
            }

            private <T> Optional<Entry<T>> getEntry(ResourceKey<? extends Registry<? extends T>> key) {
                return Optional.ofNullable((Entry)lookups.get(key));
            }

            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                return this.getEntry(key).map(Entry::lookup);
            }

            @Override
            public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> parent) {
                return RegistryOps.create(parent, new RegistryOps.RegistryInfoLookup(this){
                    final /* synthetic */ 3 this$0;
                    {
                        3 v0 = this$0;
                        Objects.requireNonNull(v0);
                        this.this$0 = v0;
                    }

                    @Override
                    public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                        return this.this$0.getEntry(registryKey).map(Entry::opsInfo);
                    }
                });
            }
        };
    }

    public HolderLookup.Provider build(RegistryAccess context) {
        BuildState state = this.createState(context);
        Stream<HolderLookup.RegistryLookup<?>> newRegistries = this.entries.stream().map(stub -> stub.collectRegisteredValues(state).buildAsLookup(state.owner));
        HolderLookup.Provider result = RegistrySetBuilder.buildProviderWithContext(state.owner, context, newRegistries);
        state.reportNotCollectedHolders();
        state.reportUnclaimedRegisteredValues();
        state.throwOnError();
        return result;
    }

    private HolderLookup.Provider createLazyFullPatchedRegistries(RegistryAccess context, HolderLookup.Provider fallbackProvider, Cloner.Factory clonerFactory, Map<ResourceKey<? extends Registry<?>>, RegistryContents<?>> newRegistries, HolderLookup.Provider patchOnlyRegistries) {
        UniversalOwner fullPatchedOwner = new UniversalOwner();
        MutableObject resultReference = new MutableObject();
        List lazyFullRegistries = newRegistries.keySet().stream().map(registryKey -> this.createLazyFullPatchedRegistries(fullPatchedOwner, clonerFactory, (ResourceKey)registryKey, patchOnlyRegistries, fallbackProvider, (MutableObject<HolderLookup.Provider>)resultReference)).collect(Collectors.toUnmodifiableList());
        HolderLookup.Provider result = RegistrySetBuilder.buildProviderWithContext(fullPatchedOwner, context, lazyFullRegistries.stream());
        resultReference.setValue((Object)result);
        return result;
    }

    private <T> HolderLookup.RegistryLookup<T> createLazyFullPatchedRegistries(HolderOwner<T> owner, Cloner.Factory clonerFactory, ResourceKey<? extends Registry<? extends T>> registryKey, HolderLookup.Provider patchProvider, HolderLookup.Provider fallbackProvider, MutableObject<HolderLookup.Provider> targetProvider) {
        Cloner cloner = clonerFactory.cloner(registryKey);
        if (cloner == null) {
            throw new NullPointerException("No cloner for " + String.valueOf(registryKey.identifier()));
        }
        HashMap entries = new HashMap();
        HolderGetter patchContents = patchProvider.lookupOrThrow(registryKey);
        patchContents.listElements().forEach(elementHolder -> {
            ResourceKey elementKey = elementHolder.key();
            LazyHolder holder = new LazyHolder(owner, elementKey);
            holder.supplier = () -> cloner.clone(elementHolder.value(), patchProvider, (HolderLookup.Provider)targetProvider.get());
            entries.put(elementKey, holder);
        });
        HolderGetter fallbackContents = fallbackProvider.lookupOrThrow(registryKey);
        fallbackContents.listElements().forEach(elementHolder -> {
            ResourceKey elementKey = elementHolder.key();
            entries.computeIfAbsent(elementKey, key -> {
                LazyHolder holder = new LazyHolder(owner, elementKey);
                holder.supplier = () -> cloner.clone(elementHolder.value(), fallbackProvider, (HolderLookup.Provider)targetProvider.get());
                return holder;
            });
        });
        Lifecycle lifecycle = patchContents.registryLifecycle().add(fallbackContents.registryLifecycle());
        return RegistrySetBuilder.lookupFromMap(registryKey, lifecycle, owner, entries);
    }

    public PatchedRegistries buildPatch(RegistryAccess context, HolderLookup.Provider fallbackProvider, Cloner.Factory clonerFactory) {
        BuildState state = this.createState(context);
        HashMap newRegistries = new HashMap();
        this.entries.stream().map(stub -> stub.collectRegisteredValues(state)).forEach(e -> newRegistries.put((ResourceKey<Registry<?>>)e.key, (RegistryContents<?>)e));
        Set contextRegistries = context.listRegistryKeys().collect(Collectors.toUnmodifiableSet());
        fallbackProvider.listRegistryKeys().filter(k -> !contextRegistries.contains(k)).forEach(resourceKey -> newRegistries.putIfAbsent((ResourceKey<Registry<?>>)resourceKey, new RegistryContents(resourceKey, Lifecycle.stable(), Map.of())));
        Stream<HolderLookup.RegistryLookup<?>> dynamicRegistries = newRegistries.values().stream().map(registryContents -> registryContents.buildAsLookup(state.owner));
        HolderLookup.Provider patchOnlyRegistries = RegistrySetBuilder.buildProviderWithContext(state.owner, context, dynamicRegistries);
        state.reportUnclaimedRegisteredValues();
        state.throwOnError();
        HolderLookup.Provider fullPatchedRegistries = this.createLazyFullPatchedRegistries(context, fallbackProvider, clonerFactory, newRegistries, patchOnlyRegistries);
        return new PatchedRegistries(fullPatchedRegistries, patchOnlyRegistries);
    }

    private record RegistryStub<T>(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, RegistryBootstrap<T> bootstrap) {
        private void apply(BuildState state) {
            this.bootstrap.run(state.bootstrapContext());
        }

        public RegistryContents<T> collectRegisteredValues(BuildState state) {
            HashMap result = new HashMap();
            Iterator<Map.Entry<ResourceKey<?>, RegisteredValue<?>>> iterator = state.registeredValues.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ResourceKey<?>, RegisteredValue<?>> entry = iterator.next();
                ResourceKey<?> key = entry.getKey();
                if (!key.isFor(this.key)) continue;
                ResourceKey<?> castKey = key;
                RegisteredValue<?> value = entry.getValue();
                Holder.Reference<Object> holder = state.lookup.holders.remove(key);
                result.put(castKey, new ValueAndHolder(value, Optional.ofNullable(holder)));
                iterator.remove();
            }
            return new RegistryContents(this.key, this.lifecycle, result);
        }
    }

    @FunctionalInterface
    public static interface RegistryBootstrap<T> {
        public void run(BootstrapContext<T> var1);
    }

    private record BuildState(UniversalOwner owner, UniversalLookup lookup, Map<Identifier, HolderGetter<?>> registries, Map<ResourceKey<?>, RegisteredValue<?>> registeredValues, List<RuntimeException> errors) {
        public static BuildState create(RegistryAccess context, Stream<ResourceKey<? extends Registry<?>>> newRegistries) {
            UniversalOwner owner = new UniversalOwner();
            ArrayList<RuntimeException> errors = new ArrayList<RuntimeException>();
            UniversalLookup lookup = new UniversalLookup(owner);
            ImmutableMap.Builder registries = ImmutableMap.builder();
            context.registries().forEach(contextRegistry -> registries.put((Object)contextRegistry.key().identifier(), RegistrySetBuilder.wrapContextLookup(contextRegistry.value())));
            newRegistries.forEach(newRegistry -> registries.put((Object)newRegistry.identifier(), (Object)lookup));
            return new BuildState(owner, lookup, (Map<Identifier, HolderGetter<?>>)registries.build(), new HashMap(), (List<RuntimeException>)errors);
        }

        public <T> BootstrapContext<T> bootstrapContext() {
            return new BootstrapContext<T>(this){
                final /* synthetic */ BuildState this$0;
                {
                    BuildState buildState = this$0;
                    Objects.requireNonNull(buildState);
                    this.this$0 = buildState;
                }

                @Override
                public Holder.Reference<T> register(ResourceKey<T> key, T value, Lifecycle lifecycle) {
                    RegisteredValue previousValue = this.this$0.registeredValues.put(key, new RegisteredValue(value, lifecycle));
                    if (previousValue != null) {
                        this.this$0.errors.add(new IllegalStateException("Duplicate registration for " + String.valueOf(key) + ", new=" + String.valueOf(value) + ", old=" + String.valueOf(previousValue.value)));
                    }
                    return this.this$0.lookup.getOrCreate(key);
                }

                @Override
                public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
                    return this.this$0.registries.getOrDefault(key.identifier(), this.this$0.lookup);
                }
            };
        }

        public void reportUnclaimedRegisteredValues() {
            this.registeredValues.forEach((key, registeredValue) -> this.errors.add(new IllegalStateException("Orpaned value " + String.valueOf(registeredValue.value) + " for key " + String.valueOf(key))));
        }

        public void reportNotCollectedHolders() {
            for (ResourceKey<Object> key : this.lookup.holders.keySet()) {
                this.errors.add(new IllegalStateException("Unreferenced key: " + String.valueOf(key)));
            }
        }

        public void throwOnError() {
            if (!this.errors.isEmpty()) {
                IllegalStateException result = new IllegalStateException("Errors during registry creation");
                for (RuntimeException error : this.errors) {
                    result.addSuppressed(error);
                }
                throw result;
            }
        }
    }

    private static class UniversalOwner
    implements HolderOwner<Object> {
        private UniversalOwner() {
        }

        public <T> HolderOwner<T> cast() {
            return this;
        }
    }

    public record PatchedRegistries(HolderLookup.Provider full, HolderLookup.Provider patches) {
    }

    private record RegistryContents<T>(ResourceKey<? extends Registry<? extends T>> key, Lifecycle lifecycle, Map<ResourceKey<T>, ValueAndHolder<T>> values) {
        public HolderLookup.RegistryLookup<T> buildAsLookup(UniversalOwner owner) {
            Map entries = this.values.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> {
                ValueAndHolder entry = (ValueAndHolder)e.getValue();
                Holder.Reference holder = entry.holder().orElseGet(() -> Holder.Reference.createStandAlone(owner.cast(), (ResourceKey)e.getKey()));
                holder.bindValue(entry.value().value());
                return holder;
            }));
            return RegistrySetBuilder.lookupFromMap(this.key, this.lifecycle, owner.cast(), entries);
        }
    }

    private static class LazyHolder<T>
    extends Holder.Reference<T> {
        private @Nullable Supplier<T> supplier;

        protected LazyHolder(HolderOwner<T> owner, @Nullable ResourceKey<T> key) {
            super(Holder.Reference.Type.STAND_ALONE, owner, key, null);
        }

        @Override
        protected void bindValue(T value) {
            super.bindValue(value);
            this.supplier = null;
        }

        @Override
        public T value() {
            if (this.supplier != null) {
                this.bindValue(this.supplier.get());
            }
            return super.value();
        }
    }

    private record ValueAndHolder<T>(RegisteredValue<T> value, Optional<Holder.Reference<T>> holder) {
    }

    private record RegisteredValue<T>(T value, Lifecycle lifecycle) {
    }

    private static class UniversalLookup
    extends EmptyTagLookup<Object> {
        private final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<ResourceKey<Object>, Holder.Reference<Object>>();

        public UniversalLookup(HolderOwner<Object> owner) {
            super(owner);
        }

        @Override
        public Optional<Holder.Reference<Object>> get(ResourceKey<Object> id) {
            return Optional.of(this.getOrCreate(id));
        }

        private <T> Holder.Reference<T> getOrCreate(ResourceKey<T> id) {
            return this.holders.computeIfAbsent(id, k -> Holder.Reference.createStandAlone(this.owner, k));
        }
    }

    private static class EmptyTagLookupWrapper<T>
    extends EmptyTagRegistryLookup<T>
    implements HolderLookup.RegistryLookup.Delegate<T> {
        private final HolderLookup.RegistryLookup<T> parent;

        private EmptyTagLookupWrapper(HolderOwner<T> owner, HolderLookup.RegistryLookup<T> parent) {
            super(owner);
            this.parent = parent;
        }

        @Override
        public HolderLookup.RegistryLookup<T> parent() {
            return this.parent;
        }
    }

    private static abstract class EmptyTagRegistryLookup<T>
    extends EmptyTagLookup<T>
    implements HolderLookup.RegistryLookup<T> {
        protected EmptyTagRegistryLookup(HolderOwner<T> owner) {
            super(owner);
        }

        @Override
        public Stream<HolderSet.Named<T>> listTags() {
            throw new UnsupportedOperationException("Tags are not available in datagen");
        }
    }

    private static abstract class EmptyTagLookup<T>
    implements HolderGetter<T> {
        protected final HolderOwner<T> owner;

        protected EmptyTagLookup(HolderOwner<T> owner) {
            this.owner = owner;
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
            return Optional.of(HolderSet.emptyNamed(this.owner, id));
        }
    }
}

