/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Interner
 *  com.google.common.collect.Interners
 *  com.google.common.collect.Multimap
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3fc
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.thread.ParallelMapTransform;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ModelBakery {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final SpriteId FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_0");
    public static final SpriteId FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_1");
    public static final int DESTROY_STAGE_COUNT = 10;
    public static final List<Identifier> DESTROY_STAGES = IntStream.range(0, 10).mapToObj(i -> Identifier.withDefaultNamespace("block/destroy_stage_" + i)).collect(Collectors.toList());
    public static final List<Identifier> BREAKING_LOCATIONS = DESTROY_STAGES.stream().map(location -> location.withPath(path -> "textures/" + path + ".png")).collect(Collectors.toList());
    public static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(RenderTypes::crumbling).collect(Collectors.toList());
    private static final Matrix4fc IDENTITY = new Matrix4f();
    private final EntityModelSet entityModelSet;
    private final SpriteGetter sprites;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final Map<BlockState, BlockStateModel.UnbakedRoot> unbakedBlockStateModels;
    private final Map<Identifier, ClientItem> clientInfos;
    private final Map<Identifier, ResolvedModel> resolvedModels;
    private final ResolvedModel missingModel;

    public ModelBakery(EntityModelSet entityModelSet, SpriteGetter sprites, PlayerSkinRenderCache playerSkinRenderCache, Map<BlockState, BlockStateModel.UnbakedRoot> unbakedBlockStateModels, Map<Identifier, ClientItem> clientInfos, Map<Identifier, ResolvedModel> resolvedModels, ResolvedModel missingModel) {
        this.entityModelSet = entityModelSet;
        this.sprites = sprites;
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.unbakedBlockStateModels = unbakedBlockStateModels;
        this.clientInfos = clientInfos;
        this.resolvedModels = resolvedModels;
        this.missingModel = missingModel;
    }

    public CompletableFuture<BakingResult> bakeModels(MaterialBaker materials, Executor taskExecutor) {
        InternerImpl interner = new InternerImpl();
        MissingModels missingModels = MissingModels.bake(this.missingModel, materials, interner);
        ModelBakerImpl baker = new ModelBakerImpl(this, materials, interner, missingModels);
        CompletableFuture<Map<BlockState, BlockStateModel>> bakedBlockStateModelFuture = ParallelMapTransform.schedule(this.unbakedBlockStateModels, (blockState, model) -> {
            try {
                return model.bake((BlockState)blockState, baker);
            }
            catch (Exception e) {
                LOGGER.warn("Unable to bake model: '{}': {}", blockState, (Object)e);
                return null;
            }
        }, taskExecutor);
        CompletableFuture<Map<Identifier, ItemModel>> bakedItemStackModelFuture = ParallelMapTransform.schedule(this.clientInfos, (location, clientInfo) -> {
            try {
                return clientInfo.model().bake(new ItemModel.BakingContext(baker, this.entityModelSet, this.sprites, this.playerSkinRenderCache, missingModels.item, clientInfo.registrySwapper()), IDENTITY);
            }
            catch (Exception e) {
                LOGGER.warn("Unable to bake item model: '{}'", location, (Object)e);
                return null;
            }
        }, taskExecutor);
        HashMap itemStackModelProperties = new HashMap(this.clientInfos.size());
        this.clientInfos.forEach((id, clientInfo) -> {
            ClientItem.Properties properties = clientInfo.properties();
            if (!properties.equals(ClientItem.Properties.DEFAULT)) {
                itemStackModelProperties.put(id, properties);
            }
        });
        return bakedBlockStateModelFuture.thenCombine(bakedItemStackModelFuture, (bakedBlockStateModels, bakedItemStateModels) -> new BakingResult(missingModels, (Map<BlockState, BlockStateModel>)bakedBlockStateModels, (Map<Identifier, ItemModel>)bakedItemStateModels, itemStackModelProperties));
    }

    @Environment(value=EnvType.CLIENT)
    private static class InternerImpl
    implements ModelBaker.Interner {
        private final Interner<Vector3fc> vectors = Interners.newStrongInterner();
        private final Interner<BakedQuad.MaterialInfo> materialInfos = Interners.newStrongInterner();

        private InternerImpl() {
        }

        @Override
        public Vector3fc vector(Vector3fc v) {
            return (Vector3fc)this.vectors.intern((Object)v);
        }

        @Override
        public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
            return (BakedQuad.MaterialInfo)this.materialInfos.intern((Object)material);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record MissingModels(BlockStateModelPart blockPart, BlockStateModel block, MissingItemModel item, FluidModel fluid) {
        public static MissingModels bake(ResolvedModel unbaked, final MaterialBaker materials, final ModelBaker.Interner interner) {
            ModelBaker missingModelBakery = new ModelBaker(){

                @Override
                public ResolvedModel getModel(Identifier location) {
                    throw new IllegalStateException("Missing model can't have dependencies, but asked for " + String.valueOf(location));
                }

                @Override
                public BlockStateModelPart missingBlockModelPart() {
                    throw new IllegalStateException();
                }

                @Override
                public <T> T compute(ModelBaker.SharedOperationKey<T> key) {
                    return key.compute(this);
                }

                @Override
                public MaterialBaker materials() {
                    return materials;
                }

                @Override
                public ModelBaker.Interner interner() {
                    return interner;
                }
            };
            TextureSlots textureSlots = unbaked.getTopTextureSlots();
            boolean hasAmbientOcclusion = unbaked.getTopAmbientOcclusion();
            boolean usesBlockLight = unbaked.getTopGuiLight().lightLikeBlock();
            ItemTransforms transforms = unbaked.getTopTransforms();
            QuadCollection geometry = unbaked.bakeTopGeometry(textureSlots, missingModelBakery, BlockModelRotation.IDENTITY);
            Multimap<Identifier, Identifier> forbiddenSprites = SimpleModelWrapper.findNonBlockSprites(geometry);
            if (forbiddenSprites != null) {
                throw new IllegalStateException("Missing block contains sprites from outside of block atlas: " + String.valueOf(forbiddenSprites));
            }
            Material.Baked particleMaterial = unbaked.resolveParticleMaterial(textureSlots, missingModelBakery);
            SimpleModelWrapper missingModelPart = new SimpleModelWrapper(geometry, hasAmbientOcclusion, particleMaterial);
            SingleVariant bakedBlockModel = new SingleVariant(missingModelPart);
            MissingItemModel bakedItemModel = new MissingItemModel(geometry.getAll(), new ModelRenderProperties(usesBlockLight, particleMaterial, transforms));
            FluidModel bakedFluidModel = new FluidModel(ChunkSectionLayer.SOLID, particleMaterial, particleMaterial, null, null);
            return new MissingModels(missingModelPart, bakedBlockModel, bakedItemModel, bakedFluidModel);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class ModelBakerImpl
    implements ModelBaker {
        private final MaterialBaker materials;
        private final ModelBaker.Interner interner;
        private final MissingModels missingModels;
        private final Map<ModelBaker.SharedOperationKey<Object>, Object> operationCache;
        private final Function<ModelBaker.SharedOperationKey<Object>, Object> cacheComputeFunction;
        final /* synthetic */ ModelBakery this$0;

        private ModelBakerImpl(ModelBakery modelBakery, MaterialBaker materials, ModelBaker.Interner interner, MissingModels missingModels) {
            ModelBakery modelBakery2 = modelBakery;
            Objects.requireNonNull(modelBakery2);
            this.this$0 = modelBakery2;
            this.operationCache = new ConcurrentHashMap<ModelBaker.SharedOperationKey<Object>, Object>();
            this.cacheComputeFunction = k -> k.compute(this);
            this.materials = materials;
            this.interner = interner;
            this.missingModels = missingModels;
        }

        @Override
        public BlockStateModelPart missingBlockModelPart() {
            return this.missingModels.blockPart;
        }

        @Override
        public MaterialBaker materials() {
            return this.materials;
        }

        @Override
        public ModelBaker.Interner interner() {
            return this.interner;
        }

        @Override
        public ResolvedModel getModel(Identifier location) {
            ResolvedModel result = this.this$0.resolvedModels.get(location);
            if (result == null) {
                LOGGER.warn("Requested a model that was not discovered previously: {}", (Object)location);
                return this.this$0.missingModel;
            }
            return result;
        }

        @Override
        public <T> T compute(ModelBaker.SharedOperationKey<T> key) {
            return (T)this.operationCache.computeIfAbsent(key, this.cacheComputeFunction);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record BakingResult(MissingModels missingModels, Map<BlockState, BlockStateModel> blockStateModels, Map<Identifier, ItemModel> itemStackModels, Map<Identifier, ClientItem.Properties> itemProperties) {
        public BlockStateModel getBlockStateModel(BlockState blockState) {
            return this.blockStateModels.getOrDefault(blockState, this.missingModels.block);
        }
    }
}

