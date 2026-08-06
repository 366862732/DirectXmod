/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs.resources;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceFilterSection;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MultiPackResourceManager
implements CloseableResourceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, FallbackResourceManager> namespacedManagers;
    private final List<PackResources> packs;

    public MultiPackResourceManager(PackType type, List<PackResources> packs) {
        this.packs = List.copyOf(packs);
        HashMap<String, FallbackResourceManager> namespacedManagers = new HashMap<String, FallbackResourceManager>();
        List namespaces = packs.stream().flatMap(p -> p.getNamespaces(type).stream()).distinct().toList();
        for (PackResources pack : packs) {
            ResourceFilterSection filterSection = this.getPackFilterSection(pack);
            Set<String> providedNamespaces = pack.getNamespaces(type);
            Predicate<Identifier> pathFilter = filterSection != null ? location -> filterSection.isPathFiltered(location.getPath()) : null;
            for (String namespace : namespaces) {
                boolean filterMatchesNamespace;
                boolean packContainsNamespace = providedNamespaces.contains(namespace);
                boolean bl = filterMatchesNamespace = filterSection != null && filterSection.isNamespaceFiltered(namespace);
                if (!packContainsNamespace && !filterMatchesNamespace) continue;
                FallbackResourceManager fallbackResourceManager = (FallbackResourceManager)namespacedManagers.get(namespace);
                if (fallbackResourceManager == null) {
                    fallbackResourceManager = new FallbackResourceManager(type, namespace);
                    namespacedManagers.put(namespace, fallbackResourceManager);
                }
                if (packContainsNamespace && filterMatchesNamespace) {
                    fallbackResourceManager.push(pack, pathFilter);
                    continue;
                }
                if (packContainsNamespace) {
                    fallbackResourceManager.push(pack);
                    continue;
                }
                fallbackResourceManager.pushFilterOnly(pack.packId(), pathFilter);
            }
        }
        this.namespacedManagers = namespacedManagers;
    }

    private @Nullable ResourceFilterSection getPackFilterSection(PackResources pack) {
        try {
            return pack.getMetadataSection(ResourceFilterSection.TYPE);
        }
        catch (Exception e) {
            LOGGER.error("Failed to get filter section from pack {}", (Object)pack.packId());
            return null;
        }
    }

    @Override
    public Set<String> getNamespaces() {
        return this.namespacedManagers.keySet();
    }

    @Override
    public Optional<Resource> getResource(Identifier location) {
        ResourceManager pack = this.namespacedManagers.get(location.getNamespace());
        if (pack != null) {
            return pack.getResource(location);
        }
        return Optional.empty();
    }

    @Override
    public List<Resource> getResourceStack(Identifier location) {
        ResourceManager pack = this.namespacedManagers.get(location.getNamespace());
        if (pack != null) {
            return pack.getResourceStack(location);
        }
        return List.of();
    }

    @Override
    public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
        MultiPackResourceManager.checkTrailingDirectoryPath(directory);
        TreeMap<Identifier, Resource> result = new TreeMap<Identifier, Resource>();
        for (FallbackResourceManager manager : this.namespacedManagers.values()) {
            result.putAll(manager.listResources(directory, filter));
        }
        return result;
    }

    @Override
    public Map<Identifier, List<Resource>> listResourceStacks(String directory, Predicate<Identifier> filter) {
        MultiPackResourceManager.checkTrailingDirectoryPath(directory);
        TreeMap<Identifier, List<Resource>> result = new TreeMap<Identifier, List<Resource>>();
        for (FallbackResourceManager manager : this.namespacedManagers.values()) {
            result.putAll(manager.listResourceStacks(directory, filter));
        }
        return result;
    }

    private static void checkTrailingDirectoryPath(String directory) {
        if (directory.endsWith("/")) {
            throw new IllegalArgumentException("Trailing slash in path " + directory);
        }
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.packs.stream();
    }

    @Override
    public void close() {
        this.packs.forEach(PackResources::close);
    }
}

