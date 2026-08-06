/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FallbackResourceManager
implements ResourceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final List<PackEntry> fallbacks = Lists.newArrayList();
    private final PackType type;
    private final String namespace;

    public FallbackResourceManager(PackType type, String namespace) {
        this.type = type;
        this.namespace = namespace;
    }

    public void push(PackResources pack) {
        this.pushInternal(pack.packId(), pack, null);
    }

    public void push(PackResources pack, Predicate<Identifier> filter) {
        this.pushInternal(pack.packId(), pack, filter);
    }

    public void pushFilterOnly(String name, Predicate<Identifier> filter) {
        this.pushInternal(name, null, filter);
    }

    private void pushInternal(String name, @Nullable PackResources pack, @Nullable Predicate<Identifier> contentFilter) {
        this.fallbacks.add(new PackEntry(name, pack, contentFilter));
    }

    @Override
    public Set<String> getNamespaces() {
        return ImmutableSet.of((Object)this.namespace);
    }

    @Override
    public Optional<Resource> getResource(Identifier location) {
        for (int i = this.fallbacks.size() - 1; i >= 0; --i) {
            IoSupplier<InputStream> resource;
            PackEntry entry = this.fallbacks.get(i);
            PackResources fallback = entry.resources;
            if (fallback != null && (resource = fallback.getResource(this.type, location)) != null) {
                IoSupplier<ResourceMetadata> metadataGetter = this.createStackMetadataFinder(location, i);
                return Optional.of(FallbackResourceManager.createResource(fallback, location, resource, metadataGetter));
            }
            if (!entry.isFiltered(location)) continue;
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", (Object)location, (Object)entry.name);
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Resource createResource(PackResources source, Identifier location, IoSupplier<InputStream> resource, IoSupplier<ResourceMetadata> metadata) {
        return new Resource(source, FallbackResourceManager.wrapForDebug(location, source, resource), metadata);
    }

    private static IoSupplier<InputStream> wrapForDebug(Identifier location, PackResources source, IoSupplier<InputStream> resource) {
        if (LOGGER.isDebugEnabled()) {
            return () -> new LeakedResourceWarningInputStream((InputStream)resource.get(), location, source.packId());
        }
        return resource;
    }

    @Override
    public List<Resource> getResourceStack(Identifier location) {
        Identifier metadataLocation = FallbackResourceManager.getMetadataLocation(location);
        ArrayList<Resource> result = new ArrayList<Resource>();
        boolean filterMeta = false;
        String lastFilterName = null;
        for (int i = this.fallbacks.size() - 1; i >= 0; --i) {
            IoSupplier<InputStream> resource;
            PackEntry entry = this.fallbacks.get(i);
            PackResources fileSource = entry.resources;
            if (fileSource != null && (resource = fileSource.getResource(this.type, location)) != null) {
                IoSupplier<ResourceMetadata> metadataGetter = filterMeta ? ResourceMetadata.EMPTY_SUPPLIER : () -> {
                    IoSupplier<InputStream> metaResource = fileSource.getResource(this.type, metadataLocation);
                    return metaResource != null ? FallbackResourceManager.parseMetadata(metaResource) : ResourceMetadata.EMPTY;
                };
                result.add(new Resource(fileSource, resource, metadataGetter));
            }
            if (entry.isFiltered(location)) {
                lastFilterName = entry.name;
                break;
            }
            if (!entry.isFiltered(metadataLocation)) continue;
            filterMeta = true;
        }
        if (result.isEmpty() && lastFilterName != null) {
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", (Object)location, lastFilterName);
        }
        return Lists.reverse(result);
    }

    private static boolean isMetadata(Identifier location) {
        return location.getPath().endsWith(".mcmeta");
    }

    private static Identifier getIdentifierFromMetadata(Identifier identifier) {
        String newPath = identifier.getPath().substring(0, identifier.getPath().length() - ".mcmeta".length());
        return identifier.withPath(newPath);
    }

    private static Identifier getMetadataLocation(Identifier identifier) {
        return identifier.withPath(identifier.getPath() + ".mcmeta");
    }

    @Override
    public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
        record ResourceWithSourceAndIndex(PackResources packResources, IoSupplier<InputStream> resource, int packIndex) {
        }
        HashMap<Identifier, ResourceWithSourceAndIndex> topResourceForFileLocation = new HashMap<Identifier, ResourceWithSourceAndIndex>();
        HashMap topResourceForMetaLocation = new HashMap();
        int packCount = this.fallbacks.size();
        for (int i = 0; i < packCount; ++i) {
            PackEntry entry = this.fallbacks.get(i);
            entry.filterAll(topResourceForFileLocation.keySet());
            entry.filterAll(topResourceForMetaLocation.keySet());
            PackResources packResources = entry.resources;
            if (packResources == null) continue;
            int packIndex = i;
            packResources.listResources(this.type, this.namespace, directory, (resource, streamSupplier) -> {
                if (FallbackResourceManager.isMetadata(resource)) {
                    if (filter.test(FallbackResourceManager.getIdentifierFromMetadata(resource))) {
                        topResourceForMetaLocation.put(resource, new ResourceWithSourceAndIndex(packResources, (IoSupplier<InputStream>)streamSupplier, packIndex));
                    }
                } else if (filter.test((Identifier)resource)) {
                    topResourceForFileLocation.put((Identifier)resource, new ResourceWithSourceAndIndex(packResources, (IoSupplier<InputStream>)streamSupplier, packIndex));
                }
            });
        }
        TreeMap result = Maps.newTreeMap();
        topResourceForFileLocation.forEach((location, resource) -> {
            Identifier metadataLocation = FallbackResourceManager.getMetadataLocation(location);
            ResourceWithSourceAndIndex metaResource = (ResourceWithSourceAndIndex)topResourceForMetaLocation.get(metadataLocation);
            IoSupplier<ResourceMetadata> metaGetter = metaResource != null && metaResource.packIndex >= resource.packIndex ? FallbackResourceManager.convertToMetadata(metaResource.resource) : ResourceMetadata.EMPTY_SUPPLIER;
            result.put(location, FallbackResourceManager.createResource(resource.packResources, location, resource.resource, metaGetter));
        });
        return result;
    }

    private IoSupplier<ResourceMetadata> createStackMetadataFinder(Identifier location, int finalPackIndex) {
        return () -> {
            Identifier metadataLocation = FallbackResourceManager.getMetadataLocation(location);
            for (int i = this.fallbacks.size() - 1; i >= finalPackIndex; --i) {
                IoSupplier<InputStream> resource;
                PackEntry entry = this.fallbacks.get(i);
                PackResources metadataPackCandidate = entry.resources;
                if (metadataPackCandidate != null && (resource = metadataPackCandidate.getResource(this.type, metadataLocation)) != null) {
                    return FallbackResourceManager.parseMetadata(resource);
                }
                if (entry.isFiltered(metadataLocation)) break;
            }
            return ResourceMetadata.EMPTY;
        };
    }

    private static IoSupplier<ResourceMetadata> convertToMetadata(IoSupplier<InputStream> input) {
        return () -> FallbackResourceManager.parseMetadata(input);
    }

    private static ResourceMetadata parseMetadata(IoSupplier<InputStream> input) throws IOException {
        try (InputStream metadata = input.get();){
            ResourceMetadata resourceMetadata = ResourceMetadata.fromJsonStream(metadata);
            return resourceMetadata;
        }
    }

    private static void applyPackFiltersToExistingResources(PackEntry entry, Map<Identifier, EntryStack> foundResources) {
        for (EntryStack e : foundResources.values()) {
            if (entry.isFiltered(e.fileLocation)) {
                e.fileSources.clear();
                continue;
            }
            if (!entry.isFiltered(e.metadataLocation())) continue;
            e.metaSources.clear();
        }
    }

    private void listPackResources(PackEntry entry, String directory, Predicate<Identifier> filter, Map<Identifier, EntryStack> foundResources) {
        PackResources pack = entry.resources;
        if (pack == null) {
            return;
        }
        pack.listResources(this.type, this.namespace, directory, (id, resource) -> {
            if (FallbackResourceManager.isMetadata(id)) {
                Identifier actualId = FallbackResourceManager.getIdentifierFromMetadata(id);
                if (!filter.test(actualId)) {
                    return;
                }
                foundResources.computeIfAbsent(actualId, (Function<Identifier, EntryStack>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, <init>(net.minecraft.resources.Identifier ), (Lnet/minecraft/resources/Identifier;)Lnet/minecraft/server/packs/resources/FallbackResourceManager$EntryStack;)()).metaSources.put(pack, (IoSupplier<InputStream>)resource);
            } else {
                if (!filter.test((Identifier)id)) {
                    return;
                }
                foundResources.computeIfAbsent(id, (Function<Identifier, EntryStack>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, <init>(net.minecraft.resources.Identifier ), (Lnet/minecraft/resources/Identifier;)Lnet/minecraft/server/packs/resources/FallbackResourceManager$EntryStack;)()).fileSources.add(new ResourceWithSource(pack, (IoSupplier<InputStream>)resource));
            }
        });
    }

    @Override
    public Map<Identifier, List<Resource>> listResourceStacks(String directory, Predicate<Identifier> filter) {
        HashMap foundResources = Maps.newHashMap();
        for (PackEntry entry : this.fallbacks) {
            FallbackResourceManager.applyPackFiltersToExistingResources(entry, foundResources);
            this.listPackResources(entry, directory, filter, foundResources);
        }
        TreeMap result = Maps.newTreeMap();
        for (EntryStack entry : foundResources.values()) {
            if (entry.fileSources.isEmpty()) continue;
            ArrayList<Resource> resources = new ArrayList<Resource>();
            for (ResourceWithSource stackEntry : entry.fileSources) {
                PackResources source = stackEntry.source;
                IoSupplier<InputStream> metaSource = entry.metaSources.get(source);
                IoSupplier<ResourceMetadata> metaGetter = metaSource != null ? FallbackResourceManager.convertToMetadata(metaSource) : ResourceMetadata.EMPTY_SUPPLIER;
                resources.add(FallbackResourceManager.createResource(source, entry.fileLocation, stackEntry.resource, metaGetter));
            }
            result.put(entry.fileLocation, resources);
        }
        return result;
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.fallbacks.stream().map(p -> p.resources).filter(Objects::nonNull);
    }

    private record PackEntry(String name, @Nullable PackResources resources, @Nullable Predicate<Identifier> filter) {
        public void filterAll(Collection<Identifier> collection) {
            if (this.filter != null) {
                collection.removeIf(this.filter);
            }
        }

        public boolean isFiltered(Identifier location) {
            return this.filter != null && this.filter.test(location);
        }
    }

    private record EntryStack(Identifier fileLocation, Identifier metadataLocation, List<ResourceWithSource> fileSources, Map<PackResources, IoSupplier<InputStream>> metaSources) {
        public EntryStack(Identifier fileLocation) {
            this(fileLocation, FallbackResourceManager.getMetadataLocation(fileLocation), new ArrayList<ResourceWithSource>(), (Map<PackResources, IoSupplier<InputStream>>)new Object2ObjectArrayMap());
        }
    }

    private record ResourceWithSource(PackResources source, IoSupplier<InputStream> resource) {
    }

    private static class LeakedResourceWarningInputStream
    extends FilterInputStream {
        private final Supplier<String> message;
        private boolean closed;

        public LeakedResourceWarningInputStream(InputStream wrapped, Identifier location, String name) {
            super(wrapped);
            Exception exception = new Exception("Stacktrace");
            this.message = () -> {
                StringWriter data = new StringWriter();
                exception.printStackTrace(new PrintWriter(data));
                return "Leaked resource: '" + String.valueOf(location) + "' loaded from pack: '" + name + "'\n" + String.valueOf(data);
            };
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.closed = true;
        }

        protected void finalize() throws Throwable {
            if (!this.closed) {
                LOGGER.warn("{}", (Object)this.message.get());
            }
            super.finalize();
        }
    }
}

