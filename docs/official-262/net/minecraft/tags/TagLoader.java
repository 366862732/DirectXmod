/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.tags;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TagLoader<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ElementLookup<T> elementLookup;
    private final String directory;

    public TagLoader(ElementLookup<T> elementLookup, String directory) {
        this.elementLookup = elementLookup;
        this.directory = directory;
    }

    public Map<Identifier, List<EntryWithSource>> load(ResourceManager resourceManager) {
        HashMap<Identifier, List<EntryWithSource>> builders = new HashMap<Identifier, List<EntryWithSource>>();
        FileToIdConverter lister = FileToIdConverter.json(this.directory);
        for (Map.Entry<Identifier, List<Resource>> entry : lister.listMatchingResourceStacks(resourceManager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier id = lister.fileToId(location);
            for (Resource resource : entry.getValue()) {
                try {
                    BufferedReader reader = resource.openAsReader();
                    try {
                        JsonElement element = StrictJsonParser.parse(reader);
                        List tagContents = builders.computeIfAbsent(id, key -> new ArrayList());
                        TagFile parsedContents = (TagFile)TagFile.CODEC.parse(new Dynamic((DynamicOps)JsonOps.INSTANCE, (Object)element)).getOrThrow();
                        if (parsedContents.replace()) {
                            tagContents.clear();
                        }
                        String sourceId = resource.sourcePackId();
                        parsedContents.entries().forEach(e -> tagContents.add(new EntryWithSource((TagEntry)e, sourceId)));
                    }
                    finally {
                        if (reader == null) continue;
                        ((Reader)reader).close();
                    }
                }
                catch (Exception e2) {
                    LOGGER.error("Couldn't read tag list {} from {} in data pack {}", new Object[]{id, location, resource.sourcePackId(), e2});
                }
            }
        }
        return builders;
    }

    private Either<List<EntryWithSource>, List<T>> tryBuildTag(TagEntry.Lookup<T> lookup, List<EntryWithSource> entries) {
        LinkedHashSet values = new LinkedHashSet();
        ArrayList<EntryWithSource> missingElements = new ArrayList<EntryWithSource>();
        for (EntryWithSource entry : entries) {
            if (entry.entry().build(lookup, values::add)) continue;
            missingElements.add(entry);
        }
        return missingElements.isEmpty() ? Either.right(List.copyOf(values)) : Either.left(missingElements);
    }

    public Map<Identifier, List<T>> build(Map<Identifier, List<EntryWithSource>> builders) {
        final HashMap newTags = new HashMap();
        TagEntry.Lookup lookup = new TagEntry.Lookup<T>(this){
            final /* synthetic */ TagLoader this$0;
            {
                TagLoader tagLoader = this$0;
                Objects.requireNonNull(tagLoader);
                this.this$0 = tagLoader;
            }

            @Override
            public @Nullable T element(Identifier key, boolean required) {
                return this.this$0.elementLookup.get(key, required).orElse(null);
            }

            @Override
            public @Nullable Collection<T> tag(Identifier key) {
                return (Collection)newTags.get(key);
            }
        };
        DependencySorter<Identifier, SortingEntry> sorter = new DependencySorter<Identifier, SortingEntry>();
        builders.forEach((id, entry) -> sorter.addEntry((Identifier)id, new SortingEntry((List<EntryWithSource>)entry)));
        sorter.orderByDependencies((id, contents) -> this.tryBuildTag(lookup, contents.entries).ifLeft(missing -> LOGGER.error("Couldn't load tag {} as it is missing following references: {}", id, (Object)missing.stream().map(Objects::toString).collect(Collectors.joining(", ")))).ifRight(tag -> newTags.put((Identifier)id, (List)tag)));
        return newTags;
    }

    public static <T> Map<TagKey<T>, List<Holder<T>>> loadTagsFromNetwork(TagNetworkSerialization.NetworkPayload tags, Registry<T> registry) {
        return tags.resolve(registry).tags;
    }

    public static List<Registry.PendingTags<?>> loadTagsForExistingRegistries(ResourceManager manager, RegistryAccess layer) {
        return layer.registries().map(entry -> TagLoader.loadPendingTags(manager, entry.value())).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
    }

    public static <T> void loadTagsForRegistry(ResourceManager manager, WritableRegistry<T> registry) {
        TagLoader.loadTagsForRegistry(manager, registry.key(), ElementLookup.fromWritableRegistry(registry));
    }

    public static <T> Map<TagKey<T>, List<Holder<T>>> loadTagsForRegistry(ResourceManager manager, ResourceKey<? extends Registry<T>> registryKey, ElementLookup<Holder<T>> lookup) {
        TagLoader<Holder<T>> loader = new TagLoader<Holder<T>>(lookup, Registries.tagsDirPath(registryKey));
        return TagLoader.wrapTags(registryKey, loader.build(loader.load(manager)));
    }

    private static <T> Map<TagKey<T>, List<Holder<T>>> wrapTags(ResourceKey<? extends Registry<T>> registryKey, Map<Identifier, List<Holder<T>>> tags) {
        return tags.entrySet().stream().collect(Collectors.toUnmodifiableMap(e -> TagKey.create(registryKey, (Identifier)e.getKey()), Map.Entry::getValue));
    }

    private static <T> Optional<Registry.PendingTags<T>> loadPendingTags(ResourceManager manager, Registry<T> registry) {
        ResourceKey<Registry<T>> key = registry.key();
        TagLoader<Holder<T>> loader = new TagLoader<Holder<T>>(ElementLookup.fromFrozenRegistry(registry), Registries.tagsDirPath(key));
        LoadResult<T> tags = new LoadResult<T>(key, TagLoader.wrapTags(registry.key(), loader.build(loader.load(manager))));
        return tags.tags().isEmpty() ? Optional.empty() : Optional.of(registry.prepareTagReload(tags));
    }

    public static List<HolderLookup.RegistryLookup<?>> buildUpdatedLookups(RegistryAccess.Frozen registries, List<Registry.PendingTags<?>> tags) {
        ArrayList result = new ArrayList();
        registries.registries().forEach(lookup -> {
            Registry.PendingTags foundTags = TagLoader.findTagsForRegistry(tags, lookup.key());
            result.add(foundTags != null ? foundTags.lookup() : lookup.value());
        });
        return result;
    }

    private static @Nullable Registry.PendingTags<?> findTagsForRegistry(List<Registry.PendingTags<?>> tags, ResourceKey<? extends Registry<?>> registryKey) {
        for (Registry.PendingTags<?> tag : tags) {
            if (tag.key() != registryKey) continue;
            return tag;
        }
        return null;
    }

    public static interface ElementLookup<T> {
        public Optional<? extends T> get(Identifier var1, boolean var2);

        public static <T> ElementLookup<? extends Holder<T>> fromFrozenRegistry(Registry<T> registry) {
            return (id, required) -> registry.get(id);
        }

        public static <T> ElementLookup<Holder<T>> fromWritableRegistry(WritableRegistry<T> registry) {
            return ElementLookup.fromGetters(registry.key(), registry.createRegistrationLookup(), registry);
        }

        public static <T> ElementLookup<Holder<T>> fromGetters(ResourceKey<? extends Registry<T>> registryKey, HolderGetter<T> writable, HolderGetter<T> immutable) {
            return (id, required) -> (required ? writable : immutable).get(ResourceKey.create(registryKey, id));
        }
    }

    public record EntryWithSource(TagEntry entry, String source) {
        @Override
        public String toString() {
            return String.valueOf(this.entry) + " (from " + this.source + ")";
        }
    }

    public record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<TagKey<T>, List<Holder<T>>> tags) {
    }

    private record SortingEntry(List<EntryWithSource> entries) implements DependencySorter.Entry<Identifier>
    {
        @Override
        public void visitRequiredDependencies(Consumer<Identifier> output) {
            this.entries.forEach(e -> e.entry.visitRequiredDependencies(output));
        }

        @Override
        public void visitOptionalDependencies(Consumer<Identifier> output) {
            this.entries.forEach(e -> e.entry.visitOptionalDependencies(output));
        }
    }
}

