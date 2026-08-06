/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.Lifecycle
 */
package net.minecraft.resources;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ParallelMapTransform;

public class ResourceManagerRegistryLoadTask<T>
extends RegistryLoadTask<T> {
    private static final Function<Optional<KnownPack>, RegistrationInfo> REGISTRATION_INFO_CACHE = Util.memoize(knownPack -> {
        Lifecycle lifecycle = knownPack.map(KnownPack::isVanilla).map(info -> Lifecycle.stable()).orElse(Lifecycle.experimental());
        return new RegistrationInfo((Optional<KnownPack>)knownPack, lifecycle);
    });
    private final ResourceManager resourceManager;

    public ResourceManagerRegistryLoadTask(RegistryDataLoader.RegistryData<T> data, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> loadingErrors, ResourceManager resourceManager) {
        super(data, lifecycle, loadingErrors);
        this.resourceManager = resourceManager;
    }

    @Override
    public CompletableFuture<?> load(RegistryOps.RegistryInfoLookup context, Executor executor) {
        FileToIdConverter lister = FileToIdConverter.registry(this.registryKey());
        return ((CompletableFuture)CompletableFuture.supplyAsync(() -> lister.listMatchingResources(this.resourceManager), executor).thenCompose(registryResources -> {
            RegistryOps ops = RegistryOps.create(JsonOps.INSTANCE, context);
            return ParallelMapTransform.schedule(registryResources, (resourceId, thunk) -> {
                ResourceKey elementKey = ResourceKey.create(this.registryKey(), lister.fileToId((Identifier)resourceId));
                RegistrationInfo registrationInfo = REGISTRATION_INFO_CACHE.apply(thunk.knownPackInfo());
                return new RegistryLoadTask.PendingRegistration(elementKey, RegistryLoadTask.PendingRegistration.loadFromResource(this.data.elementCodec(), ops, elementKey, thunk), registrationInfo);
            }, executor);
        })).thenAcceptAsync(loadedEntries -> {
            this.registerElements(loadedEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue));
            TagLoader.ElementLookup tagElementLookup = TagLoader.ElementLookup.fromGetters(this.registryKey(), this.concurrentRegistrationGetter, this.readOnlyRegistry());
            Map pendingTags = TagLoader.loadTagsForRegistry(this.resourceManager, this.registryKey(), tagElementLookup);
            this.registerTags(pendingTags);
        }, executor);
    }
}

