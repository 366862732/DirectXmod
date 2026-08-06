/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonParseException
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class BlockStateModelLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public static CompletableFuture<LoadedModels> loadBlockStates(ResourceManager manager, Executor executor) {
        Function<Identifier, @Nullable StateDefinition<Block, BlockState>> definitionToBlockState = BlockStateDefinitions.definitionLocationToBlockStateMapper();
        return CompletableFuture.supplyAsync(() -> BLOCKSTATE_LISTER.listMatchingResourceStacks(manager), executor).thenCompose(resources -> {
            ArrayList<CompletableFuture<@Nullable LoadedModels>> result = new ArrayList<CompletableFuture<LoadedModels>>(resources.size());
            for (Map.Entry resourceStack : resources.entrySet()) {
                result.add(CompletableFuture.supplyAsync(() -> {
                    @Nullable Identifier stateDefinitionId = BLOCKSTATE_LISTER.fileToId((Identifier)resourceStack.getKey());
                    @Nullable StateDefinition stateDefinition = (StateDefinition)definitionToBlockState.apply(stateDefinitionId);
                    if (stateDefinition == null) {
                        LOGGER.debug("Discovered unknown block state definition {}, ignoring", (Object)stateDefinitionId);
                        return null;
                    }
                    List stack = (List)resourceStack.getValue();
                    ArrayList<LoadedBlockStateModelDispatcher> loadedStack = new ArrayList<LoadedBlockStateModelDispatcher>(stack.size());
                    for (Resource resource : stack) {
                        try {
                            BufferedReader reader = resource.openAsReader();
                            try {
                                JsonElement element = StrictJsonParser.parse(reader);
                                BlockStateModelDispatcher definition = (BlockStateModelDispatcher)BlockStateModelDispatcher.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)element).getOrThrow(JsonParseException::new);
                                loadedStack.add(new LoadedBlockStateModelDispatcher(resource.sourcePackId(), definition));
                            }
                            finally {
                                if (reader == null) continue;
                                ((Reader)reader).close();
                            }
                        }
                        catch (Exception e) {
                            LOGGER.error("Failed to load blockstate definition {} from pack {}", new Object[]{stateDefinitionId, resource.sourcePackId(), e});
                        }
                    }
                    try {
                        return BlockStateModelLoader.loadBlockStateDefinitionStack(stateDefinitionId, stateDefinition, loadedStack);
                    }
                    catch (Exception e) {
                        LOGGER.error("Failed to load blockstate definition {}", (Object)stateDefinitionId, (Object)e);
                        return null;
                    }
                }, executor));
            }
            return Util.sequence(result).thenApply(partialMaps -> {
                IdentityHashMap<BlockState, BlockStateModel.UnbakedRoot> fullMap = new IdentityHashMap<BlockState, BlockStateModel.UnbakedRoot>();
                for (LoadedModels partialMap : partialMaps) {
                    if (partialMap == null) continue;
                    fullMap.putAll(partialMap.models());
                }
                return new LoadedModels(fullMap);
            });
        });
    }

    private static LoadedModels loadBlockStateDefinitionStack(Identifier stateDefinitionId, StateDefinition<Block, BlockState> stateDefinition, List<LoadedBlockStateModelDispatcher> definitionStack) {
        IdentityHashMap<BlockState, BlockStateModel.UnbakedRoot> result = new IdentityHashMap<BlockState, BlockStateModel.UnbakedRoot>();
        for (LoadedBlockStateModelDispatcher definition : definitionStack) {
            result.putAll(definition.contents.instantiate(stateDefinition, () -> String.valueOf(stateDefinitionId) + "/" + definition.source));
        }
        return new LoadedModels(result);
    }

    @Environment(value=EnvType.CLIENT)
    private record LoadedBlockStateModelDispatcher(String source, BlockStateModelDispatcher contents) {
    }

    @Environment(value=EnvType.CLIENT)
    public record LoadedModels(Map<BlockState, BlockStateModel.UnbakedRoot> models) {
    }
}

