/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Iterators
 *  com.mojang.serialization.Lifecycle
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectList
 *  it.unimi.dsi.fastutil.objects.Reference2IntMap
 *  it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.component.DataComponentLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class MappedRegistry<T>
implements WritableRegistry<T> {
    private final ResourceKey<? extends Registry<T>> key;
    private final ObjectList<Holder.Reference<T>> byId = new ObjectArrayList(256);
    private final Reference2IntMap<T> toId = (Reference2IntMap)Util.make(new Reference2IntOpenHashMap(), t -> t.defaultReturnValue(-1));
    private final Map<Identifier, Holder.Reference<T>> byLocation = new HashMap<Identifier, Holder.Reference<T>>();
    private final Map<ResourceKey<T>, Holder.Reference<T>> byKey = new HashMap<ResourceKey<T>, Holder.Reference<T>>();
    private final Map<T, Holder.Reference<T>> byValue = new IdentityHashMap<T, Holder.Reference<T>>();
    private final Map<ResourceKey<T>, RegistrationInfo> registrationInfos = new IdentityHashMap<ResourceKey<T>, RegistrationInfo>();
    private Lifecycle registryLifecycle;
    private final Map<TagKey<T>, HolderSet.Named<T>> frozenTags = new IdentityHashMap<TagKey<T>, HolderSet.Named<T>>();
    private TagSet<T> allTags = TagSet.unbound();
    private @Nullable DataComponentLookup<T> componentLookup;
    private boolean frozen;
    private @Nullable Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;

    @Override
    public Stream<HolderSet.Named<T>> listTags() {
        return this.getTags();
    }

    public MappedRegistry(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle) {
        this(key, lifecycle, false);
    }

    public MappedRegistry(ResourceKey<? extends Registry<T>> key, Lifecycle initialLifecycle, boolean intrusiveHolders) {
        this.key = key;
        this.registryLifecycle = initialLifecycle;
        if (intrusiveHolders) {
            this.unregisteredIntrusiveHolders = new IdentityHashMap<T, Holder.Reference<T>>();
        }
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    public String toString() {
        return "Registry[" + String.valueOf(this.key) + " (" + String.valueOf(this.registryLifecycle) + ")]";
    }

    private void validateWrite() {
        if (this.frozen) {
            throw new IllegalStateException("Registry is already frozen");
        }
    }

    private void validateWrite(ResourceKey<T> key) {
        if (this.frozen) {
            throw new IllegalStateException("Registry is already frozen (trying to add key " + String.valueOf(key) + ")");
        }
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo registrationInfo) {
        Holder.Reference holder;
        this.validateWrite(key);
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (this.byLocation.containsKey(key.identifier())) {
            throw Util.pauseInIde(new IllegalStateException("Adding duplicate key '" + String.valueOf(key) + "' to registry"));
        }
        if (this.byValue.containsKey(value)) {
            throw Util.pauseInIde(new IllegalStateException("Adding duplicate value '" + String.valueOf(value) + "' to registry"));
        }
        if (this.unregisteredIntrusiveHolders != null) {
            holder = this.unregisteredIntrusiveHolders.remove(value);
            if (holder == null) {
                throw new AssertionError((Object)("Missing intrusive holder for " + String.valueOf(key) + ":" + String.valueOf(value)));
            }
            holder.bindKey(key);
        } else {
            holder = this.byKey.computeIfAbsent(key, k -> Holder.Reference.createStandAlone(this, k));
        }
        this.byKey.put(key, holder);
        this.byLocation.put(key.identifier(), holder);
        this.byValue.put(value, holder);
        int newId = this.byId.size();
        this.byId.add((Object)holder);
        this.toId.put(value, newId);
        this.registrationInfos.put(key, registrationInfo);
        this.registryLifecycle = this.registryLifecycle.add(registrationInfo.lifecycle());
        return holder;
    }

    @Override
    public @Nullable Identifier getKey(T thing) {
        Holder.Reference<T> holder = this.byValue.get(thing);
        return holder != null ? holder.key().identifier() : null;
    }

    @Override
    public Optional<ResourceKey<T>> getResourceKey(T thing) {
        return Optional.ofNullable(this.byValue.get(thing)).map(Holder.Reference::key);
    }

    @Override
    public int getId(@Nullable T thing) {
        return this.toId.getInt(thing);
    }

    @Override
    public @Nullable T getValue(@Nullable ResourceKey<T> key) {
        return MappedRegistry.getValueFromNullable(this.byKey.get(key));
    }

    @Override
    public @Nullable T byId(int id) {
        if (id < 0 || id >= this.byId.size()) {
            return null;
        }
        return ((Holder.Reference)this.byId.get(id)).value();
    }

    @Override
    public Optional<Holder.Reference<T>> get(int id) {
        if (id < 0 || id >= this.byId.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable((Holder.Reference)this.byId.get(id));
    }

    @Override
    public Optional<Holder.Reference<T>> get(Identifier id) {
        return Optional.ofNullable(this.byLocation.get(id));
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
        return Optional.ofNullable(this.byKey.get(id));
    }

    @Override
    public Optional<Holder.Reference<T>> getAny() {
        return this.byId.isEmpty() ? Optional.empty() : Optional.of((Holder.Reference)this.byId.getFirst());
    }

    @Override
    public Holder<T> wrapAsHolder(T value) {
        Holder.Reference<T> existingHolder = this.byValue.get(value);
        return existingHolder != null ? existingHolder : Holder.direct(value);
    }

    private Holder.Reference<T> getOrCreateHolderOrThrow(ResourceKey<T> key) {
        return this.byKey.computeIfAbsent(key, id -> {
            if (this.unregisteredIntrusiveHolders != null) {
                throw new IllegalStateException("This registry can't create new holders without value");
            }
            this.validateWrite((ResourceKey<T>)id);
            return Holder.Reference.createStandAlone(this, id);
        });
    }

    @Override
    public int size() {
        return this.byKey.size();
    }

    @Override
    public Optional<RegistrationInfo> registrationInfo(ResourceKey<T> element) {
        return Optional.ofNullable(this.registrationInfos.get(element));
    }

    @Override
    public Lifecycle registryLifecycle() {
        return this.registryLifecycle;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.transform((Iterator)this.byId.iterator(), Holder::value);
    }

    @Override
    public @Nullable T getValue(@Nullable Identifier key) {
        Holder.Reference<T> result = this.byLocation.get(key);
        return MappedRegistry.getValueFromNullable(result);
    }

    private static <T> @Nullable T getValueFromNullable(@Nullable Holder.Reference<T> result) {
        return result != null ? (T)result.value() : null;
    }

    @Override
    public Set<Identifier> keySet() {
        return Collections.unmodifiableSet(this.byLocation.keySet());
    }

    @Override
    public Set<ResourceKey<T>> registryKeySet() {
        return Collections.unmodifiableSet(this.byKey.keySet());
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return Collections.unmodifiableSet(Util.mapValuesLazy(this.byKey, Holder::value).entrySet());
    }

    @Override
    public Stream<Holder.Reference<T>> listElements() {
        return this.byId.stream();
    }

    @Override
    public Stream<HolderSet.Named<T>> getTags() {
        return this.allTags.getTags();
    }

    private HolderSet.Named<T> getOrCreateTagForRegistration(TagKey<T> tag) {
        return this.frozenTags.computeIfAbsent(tag, this::createTag);
    }

    private HolderSet.Named<T> createTag(TagKey<T> tag) {
        return new HolderSet.Named<T>(this, tag);
    }

    @Override
    public boolean isEmpty() {
        return this.byKey.isEmpty();
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource random) {
        return Util.getRandomSafe(this.byId, random);
    }

    @Override
    public boolean containsKey(Identifier key) {
        return this.byLocation.containsKey(key);
    }

    @Override
    public boolean containsKey(ResourceKey<T> key) {
        return this.byKey.containsKey(key);
    }

    @Override
    public DataComponentLookup<T> componentLookup() {
        return Objects.requireNonNull(this.componentLookup, "Registry not frozen yet");
    }

    @Override
    public Registry<T> freeze() {
        if (this.frozen) {
            return this;
        }
        this.frozen = true;
        this.byValue.forEach((? super K value, ? super V holder) -> holder.bindValue(value));
        List<Identifier> unboundEntries = this.byKey.entrySet().stream().filter(e -> !((Holder.Reference)e.getValue()).isBound()).map(e -> ((ResourceKey)e.getKey()).identifier()).sorted().toList();
        if (!unboundEntries.isEmpty()) {
            throw new IllegalStateException("Unbound values in registry " + String.valueOf(this.key()) + ": " + String.valueOf(unboundEntries));
        }
        if (this.unregisteredIntrusiveHolders != null) {
            if (!this.unregisteredIntrusiveHolders.isEmpty()) {
                throw new IllegalStateException("Some intrusive holders were not registered: " + String.valueOf(this.unregisteredIntrusiveHolders.values()));
            }
            this.unregisteredIntrusiveHolders = null;
        }
        if (this.allTags.isBound()) {
            throw new IllegalStateException("Tags already present before freezing");
        }
        List<Identifier> unboundTags = this.frozenTags.entrySet().stream().filter(e -> !((HolderSet.Named)e.getValue()).isBound()).map(e -> ((TagKey)e.getKey()).location()).sorted().toList();
        if (!unboundTags.isEmpty()) {
            throw new IllegalStateException("Unbound tags in registry " + String.valueOf(this.key()) + ": " + String.valueOf(unboundTags));
        }
        this.componentLookup = new DataComponentLookup<T>(this.byId);
        this.allTags = TagSet.fromMap(this.frozenTags);
        this.refreshTagsInHolders();
        return this;
    }

    @Override
    public Holder.Reference<T> createIntrusiveHolder(T value) {
        if (this.unregisteredIntrusiveHolders == null) {
            throw new IllegalStateException("This registry can't create intrusive holders");
        }
        this.validateWrite();
        return this.unregisteredIntrusiveHolders.computeIfAbsent(value, v -> Holder.Reference.createIntrusive(this, v));
    }

    @Override
    public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
        return this.allTags.get(id);
    }

    private Holder.Reference<T> validateAndUnwrapTagElement(TagKey<T> id, Holder<T> value) {
        if (!value.canSerializeIn(this)) {
            throw new IllegalStateException("Can't create named set " + String.valueOf(id) + " containing value " + String.valueOf(value) + " from outside registry " + String.valueOf(this));
        }
        if (value instanceof Holder.Reference) {
            Holder.Reference reference = (Holder.Reference)value;
            return reference;
        }
        throw new IllegalStateException("Found direct holder " + String.valueOf(value) + " value in tag " + String.valueOf(id));
    }

    @Override
    public void bindTags(Map<TagKey<T>, List<Holder<T>>> pendingTags) {
        this.validateWrite();
        pendingTags.forEach((? super K id, ? super V values) -> this.getOrCreateTagForRegistration((TagKey<T>)id).bind((List<Holder<T>>)values));
    }

    private void refreshTagsInHolders() {
        IdentityHashMap<Holder.Reference, List> tagsForElement = new IdentityHashMap<Holder.Reference, List>();
        this.byKey.values().forEach(h -> tagsForElement.put((Holder.Reference)h, new ArrayList()));
        this.allTags.forEach((? super TagKey<T> id, ? super HolderSet.Named<T> values) -> {
            for (Holder value : values) {
                Holder.Reference reference = this.validateAndUnwrapTagElement((TagKey<T>)id, value);
                ((List)tagsForElement.get(reference)).add(id);
            }
        });
        tagsForElement.forEach(Holder.Reference::bindTags);
    }

    public void bindAllTagsToEmpty() {
        this.validateWrite();
        this.frozenTags.values().forEach(e -> e.bind(List.of()));
    }

    @Override
    public HolderGetter<T> createRegistrationLookup() {
        this.validateWrite();
        return new HolderGetter<T>(this){
            final /* synthetic */ MappedRegistry this$0;
            {
                MappedRegistry mappedRegistry = this$0;
                Objects.requireNonNull(mappedRegistry);
                this.this$0 = mappedRegistry;
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                return Optional.of(this.getOrThrow(id));
            }

            @Override
            public Holder.Reference<T> getOrThrow(ResourceKey<T> id) {
                return this.this$0.getOrCreateHolderOrThrow(id);
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                return Optional.of(this.getOrThrow(id));
            }

            @Override
            public HolderSet.Named<T> getOrThrow(TagKey<T> id) {
                return this.this$0.getOrCreateTagForRegistration(id);
            }
        };
    }

    @Override
    public Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> tags) {
        if (!this.frozen) {
            throw new IllegalStateException("Invalid method used for tag loading");
        }
        ImmutableMap.Builder pendingTagsBuilder = ImmutableMap.builder();
        final HashMap pendingContents = new HashMap();
        tags.tags().forEach((? super K id, ? super V contents) -> {
            HolderSet.Named<T> tagToAdd = this.frozenTags.get(id);
            if (tagToAdd == null) {
                tagToAdd = this.createTag((TagKey<T>)id);
            }
            pendingTagsBuilder.put(id, tagToAdd);
            pendingContents.put(id, List.copyOf(contents));
        });
        final ImmutableMap pendingTags = pendingTagsBuilder.build();
        final HolderLookup.RegistryLookup.Delegate patchedHolder = new HolderLookup.RegistryLookup.Delegate<T>(this){
            final /* synthetic */ MappedRegistry this$0;
            {
                MappedRegistry mappedRegistry = this$0;
                Objects.requireNonNull(mappedRegistry);
                this.this$0 = mappedRegistry;
            }

            @Override
            public HolderLookup.RegistryLookup<T> parent() {
                return this.this$0;
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                return Optional.ofNullable((HolderSet.Named)pendingTags.get(id));
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return pendingTags.values().stream();
            }
        };
        return new Registry.PendingTags<T>(this){
            final /* synthetic */ MappedRegistry this$0;
            {
                MappedRegistry mappedRegistry = this$0;
                Objects.requireNonNull(mappedRegistry);
                this.this$0 = mappedRegistry;
            }

            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return this.this$0.key();
            }

            @Override
            public int size() {
                return pendingContents.size();
            }

            @Override
            public HolderLookup.RegistryLookup<T> lookup() {
                return patchedHolder;
            }

            @Override
            public void apply() {
                pendingTags.forEach((id, tag) -> {
                    List values = pendingContents.getOrDefault(id, List.of());
                    tag.bind(values);
                });
                this.this$0.allTags = TagSet.fromMap(pendingTags);
                this.this$0.refreshTagsInHolders();
            }
        };
    }

    private static interface TagSet<T> {
        public static <T> TagSet<T> unbound() {
            return new TagSet<T>(){

                @Override
                public boolean isBound() {
                    return false;
                }

                @Override
                public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                    throw new IllegalStateException("Tags not bound, trying to access " + String.valueOf(id));
                }

                @Override
                public void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> action) {
                    throw new IllegalStateException("Tags not bound");
                }

                @Override
                public Stream<HolderSet.Named<T>> getTags() {
                    throw new IllegalStateException("Tags not bound");
                }
            };
        }

        public static <T> TagSet<T> fromMap(final Map<TagKey<T>, HolderSet.Named<T>> tags) {
            return new TagSet<T>(){

                @Override
                public boolean isBound() {
                    return true;
                }

                @Override
                public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                    return Optional.ofNullable((HolderSet.Named)tags.get(id));
                }

                @Override
                public void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> action) {
                    tags.forEach(action);
                }

                @Override
                public Stream<HolderSet.Named<T>> getTags() {
                    return tags.values().stream();
                }
            };
        }

        public boolean isBound();

        public Optional<HolderSet.Named<T>> get(TagKey<T> var1);

        public void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> var1);

        public Stream<HolderSet.Named<T>> getTags();
    }
}

