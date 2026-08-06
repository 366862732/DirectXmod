/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.JsonOps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.PlaceholderLookupProvider;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientItemInfoLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("items");

    public static CompletableFuture<LoadedClientInfos> scheduleLoad(ResourceManager manager, Executor executor) {
        RegistryAccess.Frozen staticRegistries = ClientRegistryLayer.createRegistryAccess().compositeAccess();
        return CompletableFuture.supplyAsync(() -> LISTER.listMatchingResources(manager), executor).thenCompose(resources -> {
            ArrayList pendingLoads = new ArrayList(resources.size());
            resources.forEach((resourceId, resource) -> pendingLoads.add(CompletableFuture.supplyAsync(() -> {
                PendingLoad pendingLoad;
                block8: {
                    Identifier modelId = LISTER.fileToId((Identifier)resourceId);
                    BufferedReader reader = resource.openAsReader();
                    try {
                        PlaceholderLookupProvider lookup = new PlaceholderLookupProvider(staticRegistries);
                        RegistryOps ops = lookup.createSerializationContext(JsonOps.INSTANCE);
                        ClientItem parsedInfo = ClientItem.CODEC.parse(ops, (Object)StrictJsonParser.parse(reader)).ifError(error -> LOGGER.error("Couldn't parse item model '{}' from pack '{}': {}", new Object[]{modelId, resource.sourcePackId(), error.message()})).result().map(clientItem -> {
                            if (lookup.hasRegisteredPlaceholders()) {
                                return clientItem.withRegistrySwapper(lookup.createSwapper());
                            }
                            return clientItem;
                        }).orElse(null);
                        pendingLoad = new PendingLoad(modelId, parsedInfo);
                        if (reader == null) break block8;
                    }
                    catch (Throwable t$) {
                        try {
                            if (reader != null) {
                                try {
                                    ((Reader)reader).close();
                                }
                                catch (Throwable x2) {
                                    t$.addSuppressed(x2);
                                }
                            }
                            throw t$;
                        }
                        catch (Exception e) {
                            LOGGER.error("Failed to open item model {} from pack '{}'", new Object[]{resourceId, resource.sourcePackId(), e});
                            return new PendingLoad(modelId, null);
                        }
                    }
                    ((Reader)reader).close();
                }
                return pendingLoad;
            }, executor)));
            return Util.sequence(pendingLoads).thenApply(loads -> {
                HashMap<Identifier, ClientItem> resultMap = new HashMap<Identifier, ClientItem>();
                for (PendingLoad load : loads) {
                    if (load.clientItemInfo == null) continue;
                    resultMap.put(load.id, load.clientItemInfo);
                }
                return new LoadedClientInfos(resultMap);
            });
        });
    }

    @Environment(value=EnvType.CLIENT)
    private record PendingLoad(Identifier id, @Nullable ClientItem clientItemInfo) {
    }

    @Environment(value=EnvType.CLIENT)
    public record LoadedClientInfos(Map<Identifier, ClientItem> contents) {
    }
}

