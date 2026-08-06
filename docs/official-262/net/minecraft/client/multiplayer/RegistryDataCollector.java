/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RegistryDataCollector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private @Nullable ContentsCollector contentsCollector;
    private @Nullable TagCollector tagCollector;

    public void appendContents(ResourceKey<? extends Registry<?>> registry, List<RegistrySynchronization.PackedRegistryEntry> elementData) {
        if (this.contentsCollector == null) {
            this.contentsCollector = new ContentsCollector();
        }
        this.contentsCollector.append(registry, elementData);
    }

    public void appendTags(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> data) {
        if (this.tagCollector == null) {
            this.tagCollector = new TagCollector();
        }
        data.forEach(this.tagCollector::append);
    }

    private static <T> Registry.PendingTags<T> resolveRegistryTags(RegistryAccess.Frozen context, ResourceKey<? extends Registry<? extends T>> registryKey, TagNetworkSerialization.NetworkPayload tags) {
        HolderLookup.RegistryLookup staticRegistry = context.lookupOrThrow((ResourceKey)registryKey);
        return staticRegistry.prepareTagReload(tags.resolve(staticRegistry));
    }

    private RegistryAccess loadNewElementsAndTags(ResourceProvider knownDataSource, ContentsCollector contentsCollector, boolean tagsForSynchronizedRegistriesOnly) {
        RegistryAccess.Frozen receivedRegistries;
        LayeredRegistryAccess<ClientRegistryLayer> base = ClientRegistryLayer.createRegistryAccess();
        RegistryAccess.Frozen loadingContext = base.getAccessForLoading(ClientRegistryLayer.REMOTE);
        HashMap entriesToLoad = new HashMap();
        contentsCollector.elements.forEach((registryKey, elements) -> entriesToLoad.put((ResourceKey<? extends Registry<?>>)registryKey, new RegistryDataLoader.NetworkedRegistryData((List<RegistrySynchronization.PackedRegistryEntry>)elements, TagNetworkSerialization.NetworkPayload.EMPTY)));
        ArrayList pendingStaticTags = new ArrayList();
        if (this.tagCollector != null) {
            this.tagCollector.forEach((registryKey, tags) -> {
                if (tags.isEmpty()) {
                    return;
                }
                if (RegistrySynchronization.isNetworkable(registryKey)) {
                    entriesToLoad.compute((ResourceKey<? extends Registry<?>>)registryKey, (key, previousData) -> {
                        List<RegistrySynchronization.PackedRegistryEntry> elements = previousData != null ? previousData.elements() : List.of();
                        return new RegistryDataLoader.NetworkedRegistryData(elements, (TagNetworkSerialization.NetworkPayload)tags);
                    });
                } else if (!tagsForSynchronizedRegistriesOnly) {
                    pendingStaticTags.add(RegistryDataCollector.resolveRegistryTags(loadingContext, registryKey, tags));
                }
            });
        }
        List<HolderLookup.RegistryLookup<?>> contextRegistriesWithTags = TagLoader.buildUpdatedLookups(loadingContext, pendingStaticTags);
        try {
            long start = Util.getMillis();
            receivedRegistries = RegistryDataLoader.load(entriesToLoad, knownDataSource, contextRegistriesWithTags, RegistryDataLoader.SYNCHRONIZED_REGISTRIES, Util.backgroundExecutor()).join();
            long end = Util.getMillis();
            LOGGER.debug("Loading network data took {} ms", (Object)(end - start));
        }
        catch (Exception e) {
            CrashReport report = CrashReport.forThrowable(e, "Network Registry Load");
            RegistryDataCollector.addCrashDetails(report, entriesToLoad, pendingStaticTags);
            throw new ReportedException(report);
        }
        RegistryAccess.Frozen registries = base.replaceFrom(ClientRegistryLayer.REMOTE, receivedRegistries).compositeAccess();
        pendingStaticTags.forEach(Registry.PendingTags::apply);
        return registries;
    }

    private static void addCrashDetails(CrashReport report, Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> dynamicRegistries, List<Registry.PendingTags<?>> staticRegistries) {
        CrashReportCategory details = report.addCategory("Received Elements and Tags");
        details.setDetail("Dynamic Registries", () -> dynamicRegistries.entrySet().stream().sorted(Comparator.comparing(entry -> ((ResourceKey)entry.getKey()).identifier())).map(entry -> String.format(Locale.ROOT, "\n\t\t%s: elements=%d tags=%d", ((ResourceKey)entry.getKey()).identifier(), ((RegistryDataLoader.NetworkedRegistryData)entry.getValue()).elements().size(), ((RegistryDataLoader.NetworkedRegistryData)entry.getValue()).tags().size())).collect(Collectors.joining()));
        details.setDetail("Static Registries", () -> staticRegistries.stream().sorted(Comparator.comparing(entry -> entry.key().identifier())).map(entry -> String.format(Locale.ROOT, "\n\t\t%s: tags=%d", entry.key().identifier(), entry.size())).collect(Collectors.joining()));
    }

    private static void loadOnlyTags(TagCollector tagCollector, RegistryAccess.Frozen originalRegistries, boolean includeSharedRegistries) {
        tagCollector.forEach((registryKey, tags) -> {
            if (includeSharedRegistries || RegistrySynchronization.isNetworkable(registryKey)) {
                RegistryDataCollector.resolveRegistryTags(originalRegistries, registryKey, tags).apply();
            }
        });
    }

    private static void updateComponents(RegistryAccess.Frozen frozenRegistries, boolean includeSharedRegistries) {
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(frozenRegistries).forEach(pendingComponents -> {
            if (includeSharedRegistries || RegistrySynchronization.isNetworkable(pendingComponents.key())) {
                pendingComponents.apply();
            }
        });
    }

    public RegistryAccess.Frozen collectGameRegistries(ResourceProvider knownDataSource, RegistryAccess.Frozen originalRegistries, boolean tagsAndComponentsForSynchronizedRegistriesOnly) {
        RegistryAccess registries;
        if (this.contentsCollector != null) {
            registries = this.loadNewElementsAndTags(knownDataSource, this.contentsCollector, tagsAndComponentsForSynchronizedRegistriesOnly);
        } else {
            if (this.tagCollector != null) {
                RegistryDataCollector.loadOnlyTags(this.tagCollector, originalRegistries, !tagsAndComponentsForSynchronizedRegistriesOnly);
            }
            registries = originalRegistries;
        }
        RegistryAccess.Frozen frozenRegistries = registries.freeze();
        RegistryDataCollector.updateComponents(frozenRegistries, !tagsAndComponentsForSynchronizedRegistriesOnly);
        return frozenRegistries;
    }

    @Environment(value=EnvType.CLIENT)
    private static class ContentsCollector {
        private final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> elements = new HashMap();

        private ContentsCollector() {
        }

        public void append(ResourceKey<? extends Registry<?>> registry, List<RegistrySynchronization.PackedRegistryEntry> elementData) {
            this.elements.computeIfAbsent(registry, ignore -> new ArrayList()).addAll(elementData);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class TagCollector {
        private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap();

        private TagCollector() {
        }

        public void append(ResourceKey<? extends Registry<?>> registry, TagNetworkSerialization.NetworkPayload tagData) {
            this.tags.put(registry, tagData);
        }

        public void forEach(BiConsumer<? super ResourceKey<? extends Registry<?>>, ? super TagNetworkSerialization.NetworkPayload> action) {
            this.tags.forEach(action);
        }
    }
}

