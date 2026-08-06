/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Maps
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagLoader;
import org.slf4j.Logger;

public class ServerFunctionLibrary
implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Registry<CommandFunction<CommandSourceStack>>> TYPE_KEY = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("function"));
    private static final FileToIdConverter LISTER = new FileToIdConverter(Registries.elementsDirPath(TYPE_KEY), ".mcfunction");
    private volatile Map<Identifier, CommandFunction<CommandSourceStack>> functions = ImmutableMap.of();
    private final TagLoader<CommandFunction<CommandSourceStack>> tagsLoader = new TagLoader((id, required) -> this.getFunction(id), Registries.tagsDirPath(TYPE_KEY));
    private volatile Map<Identifier, List<CommandFunction<CommandSourceStack>>> tags = Map.of();
    private final PermissionSet functionCompilationPermissions;
    private final CommandDispatcher<CommandSourceStack> dispatcher;

    public Optional<CommandFunction<CommandSourceStack>> getFunction(Identifier id) {
        return Optional.ofNullable(this.functions.get(id));
    }

    public Map<Identifier, CommandFunction<CommandSourceStack>> getFunctions() {
        return this.functions;
    }

    public List<CommandFunction<CommandSourceStack>> getTag(Identifier tag) {
        return this.tags.getOrDefault(tag, List.of());
    }

    public Iterable<Identifier> getAvailableTags() {
        return this.tags.keySet();
    }

    public ServerFunctionLibrary(PermissionSet functionCompilationPermissions, CommandDispatcher<CommandSourceStack> dispatcher) {
        this.functionCompilationPermissions = functionCompilationPermissions;
        this.dispatcher = dispatcher;
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        ResourceManager manager = currentReload.resourceManager();
        CompletableFuture<Map> tags = CompletableFuture.supplyAsync(() -> this.tagsLoader.load(manager), taskExecutor);
        CompletionStage functions = CompletableFuture.supplyAsync(() -> LISTER.listMatchingResources(manager), taskExecutor).thenCompose(functionsToLoad -> {
            HashMap result = Maps.newHashMap();
            CommandSourceStack compilationContext = Commands.createCompilationContext(this.functionCompilationPermissions);
            for (Map.Entry entry : functionsToLoad.entrySet()) {
                Identifier resourceId = (Identifier)entry.getKey();
                Identifier id = LISTER.fileToId(resourceId);
                result.put(id, CompletableFuture.supplyAsync(() -> {
                    List<String> lines = ServerFunctionLibrary.readLines((Resource)entry.getValue());
                    return CommandFunction.fromLines(id, this.dispatcher, compilationContext, lines);
                }, taskExecutor));
            }
            CompletableFuture[] futuresToCollect = result.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(futuresToCollect).handle((ignore, throwable) -> result);
        });
        return ((CompletableFuture)((CompletableFuture)tags.thenCombine(functions, Pair::of)).thenCompose(preparationBarrier::wait)).thenAcceptAsync(data -> {
            Map functionFutures = (Map)data.getSecond();
            ImmutableMap.Builder newFunctions = ImmutableMap.builder();
            functionFutures.forEach((id, functionFuture) -> ((CompletableFuture)functionFuture.handle((function, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Failed to load function {}", id, throwable);
                } else {
                    newFunctions.put(id, function);
                }
                return null;
            })).join());
            this.functions = newFunctions.build();
            this.tags = this.tagsLoader.build((Map)data.getFirst());
        }, reloadExecutor);
    }

    private static List<String> readLines(Resource resource) {
        List<String> list;
        block8: {
            BufferedReader reader = resource.openAsReader();
            try {
                list = reader.lines().toList();
                if (reader == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            reader.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException ex) {
                    throw new CompletionException(ex);
                }
            }
            reader.close();
        }
        return list;
    }
}

