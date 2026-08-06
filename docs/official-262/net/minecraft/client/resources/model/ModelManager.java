/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMaps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.BuiltInBlockModels;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.block.LoadedBlockModels;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelGroupCollector;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ModelManager
implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private Map<Identifier, ItemModel> bakedItemStackModels = Map.of();
    private Map<Identifier, ClientItem.Properties> itemProperties = Map.of();
    private final AtlasManager atlasManager;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final BlockColors blockColors;
    private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
    private ModelBakery.MissingModels missingModels;
    private @Nullable BlockStateModelSet blockStateModelSet;
    private @Nullable BlockModelSet blockModelSet;
    private @Nullable FluidStateModelSet fluidStateModelSet;
    private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();

    public ModelManager(BlockColors blockColors, AtlasManager atlasManager, PlayerSkinRenderCache playerSkinRenderCache) {
        this.blockColors = blockColors;
        this.atlasManager = atlasManager;
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    public ItemModel getItemModel(Identifier id) {
        return this.bakedItemStackModels.getOrDefault(id, this.missingModels.item());
    }

    public ClientItem.Properties getItemProperties(Identifier id) {
        return this.itemProperties.getOrDefault(id, ClientItem.Properties.DEFAULT);
    }

    public BlockStateModelSet getBlockStateModelSet() {
        return Objects.requireNonNull(this.blockStateModelSet, "Block models not yet initialized");
    }

    public BlockModelSet getBlockModelSet() {
        return Objects.requireNonNull(this.blockModelSet, "Block models not yet initialized");
    }

    public FluidStateModelSet getFluidStateModelSet() {
        return Objects.requireNonNull(this.fluidStateModelSet, "Fluid models not yet initialized");
    }

    @Override
    public final CompletableFuture<Void> reload(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        ResourceManager manager = currentReload.resourceManager();
        CompletableFuture<EntityModelSet> entityModelSet = CompletableFuture.supplyAsync(EntityModelSet::vanilla, taskExecutor);
        CompletableFuture<Map<Identifier, UnbakedModel>> modelCache = ModelManager.loadBlockModels(manager, taskExecutor);
        CompletableFuture<BlockStateModelLoader.LoadedModels> blockStateModels = BlockStateModelLoader.loadBlockStates(manager, taskExecutor);
        CompletableFuture<Map> blockModelContents = CompletableFuture.supplyAsync(() -> BuiltInBlockModels.createBlockModels(this.blockColors), taskExecutor);
        CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> itemStackModels = ClientItemInfoLoader.scheduleLoad(manager, taskExecutor);
        CompletionStage modelDiscovery = CompletableFuture.allOf(modelCache, blockStateModels, itemStackModels).thenApplyAsync(void_ -> ModelManager.discoverModelDependencies((Map)modelCache.join(), (BlockStateModelLoader.LoadedModels)blockStateModels.join(), (ClientItemInfoLoader.LoadedClientInfos)itemStackModels.join()), taskExecutor);
        CompletionStage modelGroups = blockStateModels.thenApplyAsync(models -> ModelManager.buildModelGroups(this.blockColors, models), taskExecutor);
        AtlasManager.PendingStitchResults pendingStitches = currentReload.get(AtlasManager.PENDING_STITCH);
        CompletableFuture<SpriteLoader.Preparations> pendingBlockAtlasSprites = pendingStitches.get(AtlasIds.BLOCKS);
        CompletableFuture<SpriteLoader.Preparations> pendingItemAtlasSprites = pendingStitches.get(AtlasIds.ITEMS);
        CompletionStage blockModels = CompletableFuture.allOf(blockModelContents, entityModelSet).thenApply(void_ -> new LoadedBlockModels((Map)blockModelContents.join(), (EntityModelSet)entityModelSet.join(), this.atlasManager, this.playerSkinRenderCache));
        return ((CompletableFuture)((CompletableFuture)CompletableFuture.allOf(new CompletableFuture[]{pendingBlockAtlasSprites, pendingItemAtlasSprites, modelDiscovery, modelGroups, blockStateModels, itemStackModels, entityModelSet, blockModels, modelCache}).thenComposeAsync(arg_0 -> this.lambda$reload$4(pendingBlockAtlasSprites, pendingItemAtlasSprites, (CompletableFuture)modelDiscovery, (CompletableFuture)modelGroups, modelCache, entityModelSet, blockStateModels, itemStackModels, (CompletableFuture)blockModels, taskExecutor, arg_0), taskExecutor)).thenCompose(preparationBarrier::wait)).thenAcceptAsync(this::apply, reloadExecutor);
    }

    private static CompletableFuture<Map<Identifier, UnbakedModel>> loadBlockModels(ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> MODEL_LISTER.listMatchingResources(manager), executor).thenCompose(resources -> {
            ArrayList<CompletableFuture<@Nullable Pair>> result = new ArrayList<CompletableFuture<Pair>>(resources.size());
            for (Map.Entry resource : resources.entrySet()) {
                result.add(CompletableFuture.supplyAsync(() -> {
                    Pair pair;
                    block8: {
                        Identifier modelId = MODEL_LISTER.fileToId((Identifier)resource.getKey());
                        @Nullable BufferedReader reader = ((Resource)resource.getValue()).openAsReader();
                        try {
                            pair = Pair.of((Object)modelId, (Object)CuboidModel.fromStream(reader));
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
                                LOGGER.error("Failed to load model {}", resource.getKey(), (Object)e);
                                return null;
                            }
                        }
                        ((Reader)reader).close();
                    }
                    return pair;
                }, executor));
            }
            return Util.sequence(result).thenApply(pairs -> pairs.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
        });
    }

    private static ResolvedModels discoverModelDependencies(Map<Identifier, UnbakedModel> allModels, BlockStateModelLoader.LoadedModels blockStateModels, ClientItemInfoLoader.LoadedClientInfos itemInfos) {
        try (Zone ignored = Profiler.get().zone("dependencies");){
            ModelDiscovery result = new ModelDiscovery(allModels, MissingCuboidModel.missingModel());
            result.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
            blockStateModels.models().values().forEach(result::addRoot);
            itemInfos.contents().values().forEach(info -> result.addRoot(info.model()));
            ResolvedModels resolvedModels = new ResolvedModels(result.missingModel(), result.resolve());
            return resolvedModels;
        }
    }

    private static CompletableFuture<ReloadState> loadModels(SpriteLoader.Preparations blockAtlas, SpriteLoader.Preparations itemAtlas, ModelBakery bakery, LoadedBlockModels blockModels, Object2IntMap<BlockState> modelGroups, EntityModelSet entityModelSet, Executor taskExecutor) {
        CombinedBlockItemMaterialBaker blockItemMaterialBaker = new CombinedBlockItemMaterialBaker(blockAtlas, itemAtlas);
        BlockOnlyMaterialBaker blockOnlyMaterialBaker = new BlockOnlyMaterialBaker(blockAtlas);
        CompletableFuture<ModelBakery.BakingResult> bakedStateResults = bakery.bakeModels(blockItemMaterialBaker, taskExecutor);
        CompletionStage bakedModelsFuture = bakedStateResults.thenCompose(bakingResult -> blockModels.bake(bakingResult::getBlockStateModel, bakingResult.missingModels().block(), taskExecutor));
        return bakedStateResults.thenCombine(bakedModelsFuture, (bakingResult, bakedModels) -> {
            blockItemMaterialBaker.logMissingTextures();
            Map<Fluid, FluidModel> fluidModels = FluidStateModelSet.bake(blockOnlyMaterialBaker);
            blockOnlyMaterialBaker.logMissingTextures();
            Map<BlockState, BlockStateModel> modelByStateCache = ModelManager.createBlockStateToModelDispatch(bakingResult.blockStateModels(), bakingResult.missingModels().block());
            return new ReloadState((ModelBakery.BakingResult)bakingResult, modelGroups, modelByStateCache, (Map<BlockState, BlockModel>)bakedModels, fluidModels, entityModelSet);
        });
    }

    private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(Map<BlockState, BlockStateModel> bakedModels, BlockStateModel missingModel) {
        try (Zone ignored = Profiler.get().zone("block state dispatch");){
            IdentityHashMap<BlockState, BlockStateModel> modelByStateCache = new IdentityHashMap<BlockState, BlockStateModel>(bakedModels);
            for (Block block : BuiltInRegistries.BLOCK) {
                block.getStateDefinition().getPossibleStates().forEach(state -> {
                    if (bakedModels.putIfAbsent((BlockState)state, missingModel) == null) {
                        LOGGER.warn("Missing model for variant: '{}'", state);
                    }
                });
            }
            IdentityHashMap<BlockState, BlockStateModel> identityHashMap = modelByStateCache;
            return identityHashMap;
        }
    }

    private static Object2IntMap<BlockState> buildModelGroups(BlockColors blockColors, BlockStateModelLoader.LoadedModels blockStateModels) {
        try (Zone ignored = Profiler.get().zone("block groups");){
            Object2IntMap<BlockState> object2IntMap = ModelGroupCollector.build(blockColors, blockStateModels);
            return object2IntMap;
        }
    }

    private void apply(ReloadState preparations) {
        ModelBakery.BakingResult bakedModels = preparations.bakedModels;
        this.bakedItemStackModels = bakedModels.itemStackModels();
        this.itemProperties = bakedModels.itemProperties();
        this.modelGroups = preparations.modelGroups;
        this.missingModels = bakedModels.missingModels();
        this.blockStateModelSet = new BlockStateModelSet(preparations.blockStateModels, this.missingModels.block());
        this.blockModelSet = new BlockModelSet(this.blockStateModelSet, preparations.blockModels, this.blockColors);
        this.fluidStateModelSet = new FluidStateModelSet(preparations.fluidModels, this.missingModels.fluid());
        this.entityModelSet = preparations.entityModelSet;
    }

    public boolean requiresRender(BlockState oldState, BlockState newState) {
        int newModelGroup;
        if (oldState == newState) {
            return false;
        }
        int oldModelGroup = this.modelGroups.getInt((Object)oldState);
        if (oldModelGroup != -1 && oldModelGroup == (newModelGroup = this.modelGroups.getInt((Object)newState))) {
            FluidState newFluidState;
            FluidState oldFluidState = oldState.getFluidState();
            return oldFluidState != (newFluidState = newState.getFluidState());
        }
        return true;
    }

    public Supplier<EntityModelSet> entityModels() {
        return () -> this.entityModelSet;
    }

    private /* synthetic */ CompletionStage lambda$reload$4(CompletableFuture pendingBlockAtlasSprites, CompletableFuture pendingItemAtlasSprites, CompletableFuture modelDiscovery, CompletableFuture modelGroups, CompletableFuture modelCache, CompletableFuture entityModelSet, CompletableFuture blockStateModels, CompletableFuture itemStackModels, CompletableFuture blockModels, Executor taskExecutor, Void void_) {
        SpriteLoader.Preparations blockAtlasSprites = (SpriteLoader.Preparations)pendingBlockAtlasSprites.join();
        SpriteLoader.Preparations itemAtlasSprites = (SpriteLoader.Preparations)pendingItemAtlasSprites.join();
        ResolvedModels resolvedModels = (ResolvedModels)modelDiscovery.join();
        Object2IntMap groups = (Object2IntMap)modelGroups.join();
        Sets.SetView unreferencedModels = Sets.difference(((Map)modelCache.join()).keySet(), resolvedModels.models.keySet());
        if (!unreferencedModels.isEmpty()) {
            LOGGER.debug("Unreferenced models: \n{}", (Object)unreferencedModels.stream().sorted().map(modelId -> "\t" + String.valueOf(modelId) + "\n").collect(Collectors.joining()));
        }
        ModelBakery bakery = new ModelBakery((EntityModelSet)entityModelSet.join(), this.atlasManager, this.playerSkinRenderCache, ((BlockStateModelLoader.LoadedModels)blockStateModels.join()).models(), ((ClientItemInfoLoader.LoadedClientInfos)itemStackModels.join()).contents(), resolvedModels.models(), resolvedModels.missing());
        return ModelManager.loadModels(blockAtlasSprites, itemAtlasSprites, bakery, (LoadedBlockModels)blockModels.join(), (Object2IntMap<BlockState>)groups, (EntityModelSet)entityModelSet.join(), taskExecutor);
    }

    @Environment(value=EnvType.CLIENT)
    private record ResolvedModels(ResolvedModel missing, Map<Identifier, ResolvedModel> models) {
    }

    @Environment(value=EnvType.CLIENT)
    private static class CombinedBlockItemMaterialBaker
    extends MaterialBaker {
        private final SpriteLoader.Preparations blockAtlas;
        private final SpriteLoader.Preparations itemAtlas;

        public CombinedBlockItemMaterialBaker(SpriteLoader.Preparations blockAtlas, SpriteLoader.Preparations itemAtlas) {
            super(blockAtlas.missing());
            this.blockAtlas = blockAtlas;
            this.itemAtlas = itemAtlas;
        }

        @Override
        protected @Nullable Material.Baked bake(Material material) {
            Material.Baked itemMaterial = CombinedBlockItemMaterialBaker.bakeForAtlas(material, this.itemAtlas);
            if (itemMaterial != null) {
                return itemMaterial;
            }
            return CombinedBlockItemMaterialBaker.bakeForAtlas(material, this.blockAtlas);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class BlockOnlyMaterialBaker
    extends MaterialBaker {
        private final SpriteLoader.Preparations blockAtlas;

        public BlockOnlyMaterialBaker(SpriteLoader.Preparations blockAtlas) {
            super(blockAtlas.missing());
            this.blockAtlas = blockAtlas;
        }

        @Override
        protected @Nullable Material.Baked bake(Material material) {
            return BlockOnlyMaterialBaker.bakeForAtlas(material, this.blockAtlas);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record ReloadState(ModelBakery.BakingResult bakedModels, Object2IntMap<BlockState> modelGroups, Map<BlockState, BlockStateModel> blockStateModels, Map<BlockState, BlockModel> blockModels, Map<Fluid, FluidModel> fluidModels, EntityModelSet entityModelSet) {
    }
}

