/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectListIterator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fStack
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LevelRenderer
implements AutoCloseable {
    private static final Identifier TRANSPARENCY_POST_CHAIN_ID = Identifier.withDefaultNamespace("transparency");
    private static final Identifier ENTITY_OUTLINE_POST_CHAIN_ID = Identifier.withDefaultNamespace("entity_outline");
    private static final int MINIMUM_TRANSPARENT_SORT_COUNT = 15;
    private static final float CHUNK_VISIBILITY_THRESHOLD = 0.3f;
    private static final Vector4fc SCREEN_SIZE_TARGET_CLEAR_COLOR = new Vector4f(0.0f);
    private static final Vector4fc ENTITY_OUTLINE_CLEAR_COLOR = new Vector4f(0.0f);
    private final GameRenderer gameRenderer;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final RenderBuffers renderBuffers;
    private final FeatureRenderDispatcher featureRenderDispatcher;
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
    private final ModelManager modelManager;
    private final TextureManager textureManager;
    private final AtlasManager atlasManager;
    private final ShaderManager shaderManager;
    private final LevelRenderState levelRenderState;
    private final OptionsRenderState optionsRenderState;
    private @Nullable SkyRenderer skyRenderer;
    private final CloudRenderer cloudRenderer = new CloudRenderer();
    private final WorldBorderRenderer worldBorderRenderer = new WorldBorderRenderer();
    private final WeatherEffectRenderer weatherEffectRenderer = new WeatherEffectRenderer();
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList(50);
    private @Nullable ViewArea viewArea;
    private final RenderTarget entityOutlineTarget;
    private final LevelTargetBundle targets = new LevelTargetBundle();
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;
    private @Nullable BlockPos lastTranslucentSortBlockPos;
    private int translucencyResortIterationIndex;
    private @Nullable GpuSampler chunkLayerSampler;
    private final SimpleGizmoCollector renderThreadGizmos = new SimpleGizmoCollector();
    private FinalizedGizmos finalizedGizmos = new FinalizedGizmos(new DrawableGizmoPrimitives(), new DrawableGizmoPrimitives());

    public LevelRenderer(EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, ModelManager modelManager, TextureManager textureManager, AtlasManager atlasManager, ShaderManager shaderManager, GameRenderer gameRenderer, int width, int height) {
        this.gameRenderer = gameRenderer;
        this.entityRenderDispatcher = entityRenderDispatcher;
        this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
        this.renderBuffers = gameRenderer.renderBuffers();
        this.featureRenderDispatcher = gameRenderer.featureRenderDispatcher();
        this.modelManager = modelManager;
        this.textureManager = textureManager;
        this.atlasManager = atlasManager;
        this.shaderManager = shaderManager;
        this.levelRenderState = gameRenderer.gameRenderState().levelRenderState;
        this.optionsRenderState = gameRenderer.gameRenderState().optionsRenderState;
        this.entityOutlineTarget = new TextureTarget("Entity Outline", width, height, true, GpuFormat.RGBA8_UNORM);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky) {
        CloudStatus cloudStatus;
        float deltaPartialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        final ProfilerFiller profiler = Profiler.get();
        profiler.push("repositionCamera");
        this.repositionCamera(cameraState);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(modelViewMatrix);
        profiler.popPush("submitFeatures");
        this.submitFeatures(this.levelRenderState, this.submitNodeStorage, renderOutline);
        profiler.popPush("prepareFeatures");
        FeatureRenderDispatcher.PreparedFrame featureFrame = this.featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
        profiler.popPush("setupFrameGraph");
        FrameGraphBuilder frame = new FrameGraphBuilder();
        this.targets.main = frame.importExternal("main", this.gameRenderer.mainRenderTarget());
        int screenWidth = this.gameRenderer.mainRenderTarget().width;
        int screenHeight = this.gameRenderer.mainRenderTarget().height;
        RenderTargetDescriptor screenSizeTargetDescriptor = new RenderTargetDescriptor(screenWidth, screenHeight, true, SCREEN_SIZE_TARGET_CLEAR_COLOR, GpuFormat.RGBA8_UNORM);
        PostChain transparencyChain = this.getTransparencyChain();
        if (transparencyChain != null) {
            this.targets.translucent = frame.createInternal("translucent", screenSizeTargetDescriptor);
            this.targets.itemEntity = frame.createInternal("item_entity", screenSizeTargetDescriptor);
            this.targets.particles = frame.createInternal("particles", screenSizeTargetDescriptor);
            this.targets.weather = frame.createInternal("weather", screenSizeTargetDescriptor);
            this.targets.clouds = frame.createInternal("clouds", screenSizeTargetDescriptor);
        }
        this.targets.entityOutline = frame.importExternal("entity_outline", this.entityOutlineTarget);
        FramePass clearPass = frame.addPass("clear");
        this.targets.main = clearPass.readsAndWrites(this.targets.main);
        clearPass.executes(() -> {
            RenderTarget mainRenderTarget = this.gameRenderer.mainRenderTarget();
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(mainRenderTarget.getColorTexture(), (Vector4fc)new Vector4f(fogColor.x, fogColor.y, fogColor.z, 0.0f), mainRenderTarget.getDepthTexture(), 0.0);
        });
        if (shouldRenderSky) {
            this.addSkyPass(frame, cameraState, terrainFog);
        }
        ChunkSectionsToRender chunkSectionsToRender = this.prepareChunkRenders((Matrix4fc)this.levelRenderState.cameraRenderState.viewRotationMatrix);
        this.addMainPass(frame, featureFrame, terrainFog, this.levelRenderState, profiler, chunkSectionsToRender);
        PostChain entityOutlineChain = this.shaderManager.getPostChain(ENTITY_OUTLINE_POST_CHAIN_ID, LevelTargetBundle.OUTLINE_TARGETS);
        if (featureFrame.hasAnyOutline() && entityOutlineChain != null) {
            entityOutlineChain.addToFrame(frame, screenWidth, screenHeight, this.targets);
        }
        if ((cloudStatus = this.optionsRenderState.cloudStatus) != CloudStatus.OFF && ARGB.alpha(this.levelRenderState.cloudColor) > 0) {
            this.addCloudsPass(frame, cloudStatus, this.levelRenderState.cameraRenderState.pos, this.levelRenderState.gameTime, deltaPartialTick, this.levelRenderState.cloudColor, this.levelRenderState.cloudHeight, this.optionsRenderState.cloudRange);
        }
        this.addWeatherPass(frame, terrainFog);
        if (transparencyChain != null) {
            transparencyChain.addToFrame(frame, screenWidth, screenHeight, this.targets);
        }
        this.addAlwaysOnTopPass(frame, featureFrame, terrainFog);
        profiler.popPush("executeFrameGraph");
        frame.execute(resourceAllocator, new FrameGraphBuilder.Inspector(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public void beforeExecutePass(String name) {
                profiler.push(name);
            }

            @Override
            public void afterExecutePass(String name) {
                profiler.pop();
            }
        });
        profiler.pop();
        this.targets.clear();
        modelViewStack.popMatrix();
        featureFrame.close();
        profiler.push("compileSections");
        this.compileSections(cameraState);
        profiler.pop();
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.lock();
            profiler.push("uploadTerrainBuffers");
            try {
                this.sectionRenderDispatcher.uploadTerrainBuffersToGpu();
            }
            finally {
                this.sectionRenderDispatcher.unlock();
            }
            profiler.pop();
        }
        profiler.push("updateSectionOcclusion");
        this.sectionOcclusionGraph.update(cameraState, this.optionsRenderState.fov, this.levelRenderState.chunkLoadingRenderState);
        profiler.pop();
        Runnable playerCompiledSectionCallback = this.levelRenderState.playerCompiledSectionCallback;
        if (playerCompiledSectionCallback != null && this.isSectionCompiledAndVisible(this.levelRenderState.cameraRenderState.blockPos)) {
            playerCompiledSectionCallback.run();
        }
    }

    private void submitFeatures(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline) {
        PoseStack poseStack = new PoseStack();
        this.submitEntities(poseStack, levelRenderState, submitNodeCollector);
        levelRenderState.entityRenderStates.clear();
        this.submitBlockEntities(poseStack, levelRenderState, submitNodeCollector);
        levelRenderState.blockEntityRenderStates.clear();
        this.submitBlockDestroyAnimation(poseStack, submitNodeCollector, levelRenderState);
        levelRenderState.blockBreakingRenderStates.clear();
        levelRenderState.particlesRenderState.submit(submitNodeCollector, levelRenderState.cameraRenderState);
        if (renderOutline) {
            this.submitBlockOutline(poseStack, this.submitNodeStorage, levelRenderState);
        }
        this.finalizeGizmoCollection();
        this.finalizedGizmos.standardPrimitives().submit(submitNodeCollector, levelRenderState.cameraRenderState, false);
        this.finalizedGizmos.alwaysOnTopPrimitives().submit(submitNodeCollector, levelRenderState.cameraRenderState, true);
        if (!levelRenderState.shouldShowEntityOutlines) {
            for (SubmitNodeCollection collection : this.submitNodeStorage.getSubmitsPerOrder().values()) {
                collection.outline.clear();
            }
        }
        this.checkPoseStack(poseStack);
    }

    private void repositionCamera(CameraRenderState camera) {
        Vec3 cameraPos = camera.pos;
        SectionPos cameraSectionPos = SectionPos.of(cameraPos);
        if (this.viewArea.repositionCamera(cameraSectionPos)) {
            this.worldBorderRenderer.invalidate();
        }
        this.sectionRenderDispatcher.setCameraPosition(cameraPos);
    }

    private void addSkyPass(FrameGraphBuilder frame, CameraRenderState cameraState, GpuBufferSlice skyFog) {
        FogType fogType = cameraState.fogType;
        if (fogType == FogType.POWDER_SNOW || fogType == FogType.LAVA || cameraState.entityRenderState.doesMobEffectBlockSky) {
            return;
        }
        if (this.levelRenderState.shouldResetSkyRenderer || this.skyRenderer == null) {
            if (this.skyRenderer != null) {
                this.skyRenderer.close();
            }
            this.skyRenderer = new SkyRenderer(this.textureManager, this.atlasManager, this.gameRenderer.mainRenderTarget());
        }
        SkyRenderState state = this.levelRenderState.skyRenderState;
        if (state.skybox == DimensionType.Skybox.NONE) {
            return;
        }
        FramePass pass = frame.addPass("sky");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        pass.executes(() -> {
            RenderSystem.setShaderFog(skyFog);
            if (state.skybox == DimensionType.Skybox.END) {
                this.skyRenderer.renderEndSky();
                if (state.endFlashIntensity > 1.0E-5f) {
                    PoseStack poseStack = new PoseStack();
                    this.skyRenderer.renderEndFlash(poseStack, state.endFlashIntensity, state.endFlashXAngle, state.endFlashYAngle);
                }
                return;
            }
            PoseStack poseStack = new PoseStack();
            this.skyRenderer.renderSkyDisc(state.skyColor);
            this.skyRenderer.renderSunriseAndSunset(poseStack, state.sunAngle, state.sunriseAndSunsetColor);
            this.skyRenderer.renderSunMoonAndStars(poseStack, state.sunAngle, state.moonAngle, state.starAngle, state.moonPhase, state.rainBrightness, state.starBrightness);
            if (state.shouldRenderDarkDisc) {
                this.skyRenderer.renderDarkDisc();
            }
        });
    }

    private void addMainPass(FrameGraphBuilder frame, FeatureRenderDispatcher.PreparedFrame featureFrame, GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender) {
        FramePass pass = frame.addPass("main");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        if (this.targets.translucent != null) {
            this.targets.translucent = pass.readsAndWrites(this.targets.translucent);
        }
        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = pass.readsAndWrites(this.targets.itemEntity);
        }
        if (this.targets.weather != null) {
            this.targets.weather = pass.readsAndWrites(this.targets.weather);
        }
        if (this.targets.particles != null) {
            this.targets.particles = pass.readsAndWrites(this.targets.particles);
        }
        if (featureFrame.hasAnyOutline() && this.targets.entityOutline != null) {
            this.targets.entityOutline = pass.readsAndWrites(this.targets.entityOutline);
        }
        ResourceHandle<RenderTarget> mainTarget = this.targets.main;
        ResourceHandle<RenderTarget> translucentTarget = this.targets.translucent;
        ResourceHandle<RenderTarget> itemEntityTarget = this.targets.itemEntity;
        ResourceHandle<RenderTarget> entityOutlineTarget = this.targets.entityOutline;
        ResourceHandle<RenderTarget> particleTarget = this.targets.particles;
        pass.executes(() -> {
            RenderSystem.setShaderFog(terrainFog);
            if (levelRenderState.shouldResetChunkLayerSampler || this.chunkLayerSampler == null) {
                if (this.chunkLayerSampler != null) {
                    this.chunkLayerSampler.close();
                }
                int maxAnisotropy = this.optionsRenderState.textureFiltering == TextureFilteringMethod.ANISOTROPIC ? this.optionsRenderState.maxAnisotropyValue : 1;
                this.chunkLayerSampler = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, maxAnisotropy, OptionalDouble.empty());
            }
            profiler.push("solidTerrain");
            chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.OPAQUE, this.chunkLayerSampler);
            this.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
            if (levelRenderState.shouldShowEntityOutlines && entityOutlineTarget != null) {
                RenderTarget outlineTarget = (RenderTarget)entityOutlineTarget.get();
                RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(outlineTarget.getColorTexture(), ENTITY_OUTLINE_CLEAR_COLOR, outlineTarget.getDepthTexture(), 0.0);
            }
            profiler.popPush("renderSolidFeatures");
            featureFrame.executeSolid();
            profiler.pop();
            if (translucentTarget != null) {
                ((RenderTarget)translucentTarget.get()).copyDepthFrom((RenderTarget)mainTarget.get());
            }
            if (itemEntityTarget != null) {
                ((RenderTarget)itemEntityTarget.get()).copyDepthFrom((RenderTarget)mainTarget.get());
            }
            if (particleTarget != null) {
                ((RenderTarget)particleTarget.get()).copyDepthFrom((RenderTarget)mainTarget.get());
            }
            profiler.push("renderTranslucentFeatures");
            featureFrame.executeTranslucent();
            profiler.pop();
            featureFrame.executeOutline();
            profiler.push("translucentTerrain");
            chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT, this.chunkLayerSampler);
            profiler.pop();
            featureFrame.executeTranslucentAfterTerrain();
        });
    }

    private void addCloudsPass(FrameGraphBuilder frame, CloudStatus cloudStatus, Vec3 cameraPosition, long gameTime, float partialTicks, int cloudColor, float cloudHeight, int cloudRange) {
        FramePass pass = frame.addPass("clouds");
        if (this.targets.clouds != null) {
            this.targets.clouds = pass.readsAndWrites(this.targets.clouds);
        } else {
            this.targets.main = pass.readsAndWrites(this.targets.main);
        }
        pass.executes(() -> this.cloudRenderer.render(cloudColor, cloudStatus, cloudHeight, cloudRange, cameraPosition, gameTime, partialTicks));
    }

    private void addWeatherPass(FrameGraphBuilder frame, GpuBufferSlice fog) {
        int renderDistance = this.optionsRenderState.renderDistance * 16;
        FramePass pass = frame.addPass("weather");
        if (this.targets.weather != null) {
            this.targets.weather = pass.readsAndWrites(this.targets.weather);
        } else {
            this.targets.main = pass.readsAndWrites(this.targets.main);
        }
        pass.executes(() -> {
            RenderSystem.setShaderFog(fog);
            CameraRenderState cameraState = this.levelRenderState.cameraRenderState;
            this.weatherEffectRenderer.render(cameraState.pos, this.levelRenderState.weatherRenderState);
            this.worldBorderRenderer.render(this.levelRenderState.worldBorderRenderState, cameraState.pos, renderDistance, this.levelRenderState.cameraRenderState.depthFar);
        });
    }

    private void addAlwaysOnTopPass(FrameGraphBuilder frame, FeatureRenderDispatcher.PreparedFrame featureFrame, GpuBufferSlice fog) {
        if (!featureFrame.hasAnyAlwaysOnTop()) {
            return;
        }
        FramePass pass = frame.addPass("always_on_top");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = pass.readsAndWrites(this.targets.itemEntity);
        }
        ResourceHandle<RenderTarget> mainTarget = this.targets.main;
        pass.executes(() -> {
            RenderSystem.setShaderFog(fog);
            PoseStack poseStack = new PoseStack();
            RenderTarget mainRenderTarget = (RenderTarget)mainTarget.get();
            RenderSystem.outputColorTextureOverride = mainRenderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = mainRenderTarget.getDepthTextureView();
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainRenderTarget.getDepthTexture(), 0.0);
            featureFrame.executeAlwaysOnTop();
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            this.checkPoseStack(poseStack);
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ChunkSectionsToRender prepareChunkRenders(Matrix4fc modelViewMatrix) {
        ObjectListIterator iterator = this.visibleSections.listIterator(0);
        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = new EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>>(ChunkSectionLayer.class);
        int largestIndexCount = 0;
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            drawGroups.put(layer, new Int2ObjectOpenHashMap());
        }
        ArrayList<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ArrayList<DynamicUniforms.ChunkSectionInfo>();
        GpuTextureView blockAtlas = this.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int textureAtlasWidth = blockAtlas.getWidth(0);
        int textureAtlasHeight = blockAtlas.getHeight(0);
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.lock();
            try {
                while (iterator.hasNext()) {
                    SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection)iterator.next();
                    SectionMesh sectionMesh = section.getSectionMesh();
                    BlockPos renderOffset = section.getRenderOrigin();
                    long now = Util.getMillis();
                    int uboIndex = -1;
                    for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        IndexType indexType;
                        GpuBuffer indexBuffer;
                        SectionMesh.SectionDraw draw = sectionMesh.getSectionDraw(layer);
                        SectionRenderDispatcher.RenderSectionBufferSlice slice = this.sectionRenderDispatcher.getRenderSectionSlice(sectionMesh, layer);
                        if (slice == null || draw == null || draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) continue;
                        if (uboIndex == -1) {
                            uboIndex = sectionInfos.size();
                            sectionInfos.add(new DynamicUniforms.ChunkSectionInfo((Matrix4fc)new Matrix4f(modelViewMatrix), renderOffset.getX(), renderOffset.getY(), renderOffset.getZ(), section.getVisibility(now), textureAtlasWidth, textureAtlasHeight));
                        }
                        int combinedHash = 173;
                        VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
                        GpuBuffer vertexBuffer = slice.vertexBuffer();
                        if (layer != ChunkSectionLayer.TRANSLUCENT) {
                            combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                        }
                        int firstIndex = 0;
                        if (!draw.hasCustomIndexBuffer()) {
                            if (draw.indexCount() > largestIndexCount) {
                                largestIndexCount = draw.indexCount();
                            }
                            indexBuffer = null;
                            indexType = null;
                        } else {
                            indexBuffer = slice.indexBuffer();
                            indexType = draw.indexType();
                            if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                                combinedHash = 31 * combinedHash + indexType.hashCode();
                            }
                            firstIndex = (int)(slice.indexBufferOffset() / (long)indexType.bytes);
                        }
                        int finalUboIndex = uboIndex;
                        int baseVertex = (int)(slice.vertexBufferOffset() / (long)vertexFormat.getVertexSize());
                        List draws = (List)drawGroups.get((Object)layer).computeIfAbsent(combinedHash, n -> new ArrayList());
                        draws.add(new RenderPass.Draw<GpuBufferSlice[]>(0, vertexBuffer, indexBuffer, indexType, firstIndex, draw.indexCount(), baseVertex, (sectionUbos, uploader) -> uploader.upload("ChunkSection", sectionUbos[finalUboIndex])));
                    }
                }
            }
            finally {
                this.sectionRenderDispatcher.unlock();
            }
        }
        GpuBufferSlice[] chunkSectionInfos = RenderSystem.getDynamicUniforms().writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(blockAtlas, drawGroups, largestIndexCount, chunkSectionInfos);
    }

    private void compileSections(CameraRenderState camera) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("populateSectionsToCompile");
        BlockPos cameraPosition = camera.blockPos;
        long fadeDuration = Mth.floor(this.optionsRenderState.chunkSectionFadeInTime * 1000.0);
        for (SectionUpdateRenderState state : this.levelRenderState.sectionUpdateRenderStates) {
            BlockPos center = SectionPos.of(state.sectionNode()).center();
            double distSqr = center.distSqr(cameraPosition);
            boolean isNearby = distSqr < 768.0;
            boolean rebuildSync = false;
            if (this.optionsRenderState.prioritizeChunkUpdates == PrioritizeChunkUpdates.NEARBY) {
                rebuildSync = isNearby || state.playerChanged();
            } else if (this.optionsRenderState.prioritizeChunkUpdates == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                rebuildSync = state.playerChanged();
            }
            SectionRenderDispatcher.RenderSection section = this.viewArea.getRenderSection(state.sectionNode());
            if (isNearby || section.wasPreviouslyEmpty()) {
                section.setFadeDuration(0L);
            } else {
                section.setFadeDuration(fadeDuration);
            }
            section.setWasPreviouslyEmpty(false);
            if (rebuildSync) {
                profiler.push("compileSectionSynchronously");
                section.compileSync(state.region());
                profiler.pop();
                continue;
            }
            section.compileAsync(state.region());
        }
        profiler.popPush("scheduleTranslucentResort");
        this.scheduleTranslucentSectionResort(camera.pos);
        profiler.pop();
    }

    private void checkPoseStack(PoseStack poseStack) {
        if (!poseStack.isEmpty()) {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    private void submitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output) {
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        for (EntityRenderState state : levelRenderState.entityRenderStates) {
            this.entityRenderDispatcher.submit(state, levelRenderState.cameraRenderState, state.x - camX, state.y - camY, state.z - camZ, poseStack, output);
        }
    }

    private void submitBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector) {
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        for (BlockEntityRenderState renderState : levelRenderState.blockEntityRenderStates) {
            BlockPos blockPos = renderState.blockPos;
            poseStack.pushPose();
            poseStack.translate((double)blockPos.getX() - camX, (double)blockPos.getY() - camY, (double)blockPos.getZ() - camZ);
            this.blockEntityRenderDispatcher.submit(renderState, poseStack, submitNodeCollector, levelRenderState.cameraRenderState);
            poseStack.popPose();
        }
    }

    private void submitBlockDestroyAnimation(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState) {
        if (levelRenderState.blockBreakingRenderStates.isEmpty()) {
            return;
        }
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        ArrayList<BlockStateModelPart> parts = new ArrayList<BlockStateModelPart>();
        RandomSource random = RandomSource.createThreadLocalInstance();
        for (BlockBreakingRenderState state : levelRenderState.blockBreakingRenderStates) {
            if (state.blockState().getRenderShape() != RenderShape.MODEL) continue;
            BlockPos pos = state.blockPos();
            poseStack.pushPose();
            poseStack.translate((double)pos.getX() - camX, (double)pos.getY() - camY, (double)pos.getZ() - camZ);
            poseStack.translate(state.blockState().getOffset(pos));
            BlockStateModel model = this.modelManager.getBlockStateModelSet().get(state.blockState());
            random.setSeed(state.blockState().getSeed(pos));
            model.collectParts(random, parts);
            submitNodeCollector.submitBreakingBlockModel(poseStack, List.copyOf(parts), state.progress());
            parts.clear();
            poseStack.popPose();
        }
    }

    private void submitBlockOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState) {
        BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
        if (state == null) {
            return;
        }
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        BlockPos pos = state.pos();
        poseStack.pushPose();
        poseStack.translate((double)pos.getX() - cameraPos.x, (double)pos.getY() - cameraPos.y, (double)pos.getZ() - cameraPos.z);
        if (state.highContrast()) {
            this.submitHitOutline(poseStack, submitNodeCollector, RenderTypes.secondaryBlockOutline(), state, -16777216, 7.0f, state.isTranslucent());
        }
        int outlineColor = state.highContrast() ? -11010079 : ARGB.black(102);
        this.submitHitOutline(poseStack, submitNodeCollector, RenderTypes.lines(), state, outlineColor, this.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth, state.isTranslucent());
        poseStack.popPose();
    }

    private void submitHitOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, RenderType renderType, BlockOutlineRenderState state, int color, float width, boolean afterTerrain) {
        if (SharedConstants.DEBUG_SHAPES) {
            submitNodeCollector.submitShapeOutline(poseStack, state.shape(), renderType, -1, width, afterTerrain);
            if (state.collisionShape() != null) {
                submitNodeCollector.submitShapeOutline(poseStack, state.collisionShape(), renderType, ARGB.colorFromFloat(0.4f, 0.0f, 0.0f, 0.0f), width, afterTerrain);
            }
            if (state.occlusionShape() != null) {
                submitNodeCollector.submitShapeOutline(poseStack, state.occlusionShape(), renderType, ARGB.colorFromFloat(0.4f, 0.0f, 1.0f, 0.0f), width, afterTerrain);
            }
            if (state.interactionShape() != null) {
                submitNodeCollector.submitShapeOutline(poseStack, state.interactionShape(), renderType, ARGB.colorFromFloat(0.4f, 0.0f, 0.0f, 1.0f), width, afterTerrain);
            }
        } else {
            submitNodeCollector.submitShapeOutline(poseStack, state.shape(), renderType, color, width, afterTerrain);
        }
    }

    public void resize(int width, int height) {
        this.sectionOcclusionGraph.invalidate();
        this.entityOutlineTarget.resize(width, height);
    }

    public void endFrame() {
        this.cloudRenderer.endFrame();
    }

    @Override
    public void close() {
        this.resetLevelRenderData();
        this.entityOutlineTarget.destroyBuffers();
        if (this.skyRenderer != null) {
            this.skyRenderer.close();
        }
        if (this.chunkLayerSampler != null) {
            this.chunkLayerSampler.close();
        }
        this.worldBorderRenderer.close();
        this.cloudRenderer.close();
        this.weatherEffectRenderer.close();
    }

    public void doEntityOutline() {
        if (this.levelRenderState.shouldShowEntityOutlines) {
            this.entityOutlineTarget.blitAndBlendToTexture(this.gameRenderer.mainRenderTarget().getColorTextureView(), this.gameRenderer.mainRenderTarget().getDepthTextureView());
        }
    }

    public void invalidateCompiledGeometry(ClientLevel level, Options options, Camera camera, BlockColors blockColors) {
        SectionCompiler sectionCompiler = new SectionCompiler(options.ambientOcclusion().get(), options.cutoutLeaves().get(), this.modelManager.getBlockStateModelSet(), this.modelManager.getFluidStateModelSet(), blockColors);
        if (this.sectionRenderDispatcher == null) {
            this.sectionRenderDispatcher = new SectionRenderDispatcher(Util.backgroundExecutor(), this.renderBuffers, sectionCompiler, this.sectionOcclusionGraph::schedulePropagationFrom);
        } else {
            this.sectionRenderDispatcher.setCompiler(sectionCompiler);
        }
        this.cloudRenderer().markForRebuild();
        LeavesBlock.setCutoutLeaves(options.cutoutLeaves().get());
        if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
        }
        this.sectionRenderDispatcher.clearCompileQueue();
        this.viewArea = new ViewArea(this.sectionRenderDispatcher, level.getMinY(), level.getMaxY(), level.getMinSectionY(), level.getMaxSectionY(), options.getEffectiveRenderDistance(), this.sectionOcclusionGraph);
        this.sectionOcclusionGraph().waitAndReset(this.viewArea);
        this.clearVisibleSections();
        SectionPos cameraSectionPos = SectionPos.of(camera.position());
        this.viewArea.repositionCamera(cameraSectionPos);
    }

    private @Nullable PostChain getTransparencyChain() {
        if (!this.gameRenderer.gameRenderState().useShaderTransparency()) {
            return null;
        }
        return this.shaderManager.getPostChain(TRANSPARENCY_POST_CHAIN_ID, LevelTargetBundle.SORTING_TARGETS);
    }

    private void scheduleTranslucentSectionResort(Vec3 cameraPos) {
        if (this.visibleSections.isEmpty()) {
            return;
        }
        BlockPos cameraBlockPos = BlockPos.containing(cameraPos);
        boolean blockPosChanged = !cameraBlockPos.equals(this.lastTranslucentSortBlockPos);
        TranslucencyPointOfView pointOfView = new TranslucencyPointOfView();
        for (SectionRenderDispatcher.RenderSection section : this.nearbyVisibleSections) {
            this.scheduleResort(section, pointOfView, cameraPos, blockPosChanged, true);
        }
        this.translucencyResortIterationIndex %= this.visibleSections.size();
        int resortsLeft = Math.max(this.visibleSections.size() / 8, 15);
        while (resortsLeft-- > 0) {
            int index = this.translucencyResortIterationIndex++ % this.visibleSections.size();
            this.scheduleResort((SectionRenderDispatcher.RenderSection)this.visibleSections.get(index), pointOfView, cameraPos, blockPosChanged, false);
        }
        this.lastTranslucentSortBlockPos = cameraBlockPos;
    }

    private void scheduleResort(SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pointOfView, Vec3 cameraPos, boolean blockPosChanged, boolean isNearby) {
        boolean resortBecauseBlockPosChanged;
        pointOfView.set(cameraPos, section.getSectionNode());
        boolean pointOfViewChanged = section.getSectionMesh().isDifferentPointOfView(pointOfView);
        boolean bl = resortBecauseBlockPosChanged = blockPosChanged && (pointOfView.isAxisAligned() || isNearby);
        if ((resortBecauseBlockPosChanged || pointOfViewChanged) && !section.transparencyResortingScheduled() && section.hasTranslucentGeometry()) {
            section.resortTransparency();
        }
    }

    public void clearVisibleSections() {
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
    }

    public void resetLevelRenderData() {
        if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
            this.viewArea = null;
        }
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.dispose();
        }
        this.sectionRenderDispatcher = null;
        this.sectionOcclusionGraph.waitAndReset(null);
        this.clearVisibleSections();
    }

    public boolean hasRenderedAllSections() {
        return this.sectionRenderDispatcher == null || this.sectionRenderDispatcher.isQueueEmpty();
    }

    public boolean isSectionCompiledAndVisible(BlockPos blockPos) {
        if (this.viewArea == null) {
            return false;
        }
        SectionRenderDispatcher.RenderSection renderSection = this.viewArea.getRenderSectionAt(blockPos);
        if (renderSection == null || renderSection.sectionMesh.get() == CompiledSectionMesh.UNCOMPILED) {
            return false;
        }
        return renderSection.getVisibility(Util.getMillis()) >= 0.3f;
    }

    public @Nullable SectionRenderDispatcher sectionRenderDispatcher() {
        return this.sectionRenderDispatcher;
    }

    public EntityRenderDispatcher entityRenderDispatcher() {
        return this.entityRenderDispatcher;
    }

    public BlockEntityRenderDispatcher blockEntityRenderDispatcher() {
        return this.blockEntityRenderDispatcher;
    }

    public @Nullable RenderTarget entityOutlineTarget() {
        return this.targets.entityOutline != null ? this.targets.entityOutline.get() : null;
    }

    public @Nullable RenderTarget translucentTarget() {
        return this.targets.translucent != null ? this.targets.translucent.get() : null;
    }

    public @Nullable RenderTarget itemEntityTarget() {
        return this.targets.itemEntity != null ? this.targets.itemEntity.get() : null;
    }

    public @Nullable RenderTarget particlesTarget() {
        return this.targets.particles != null ? this.targets.particles.get() : null;
    }

    public @Nullable RenderTarget weatherTarget() {
        return this.targets.weather != null ? this.targets.weather.get() : null;
    }

    public @Nullable RenderTarget cloudsTarget() {
        return this.targets.clouds != null ? this.targets.clouds.get() : null;
    }

    public CloudRenderer cloudRenderer() {
        return this.cloudRenderer;
    }

    public @Nullable SkyRenderer skyRenderer() {
        return this.skyRenderer;
    }

    public WeatherEffectRenderer weatherEffectRenderer() {
        return this.weatherEffectRenderer;
    }

    public WorldBorderRenderer worldBorderRenderer() {
        return this.worldBorderRenderer;
    }

    public @Nullable ViewArea viewArea() {
        return this.viewArea;
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
        return this.visibleSections;
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections() {
        return this.nearbyVisibleSections;
    }

    public LongCollection expectedChunks() {
        return this.sectionOcclusionGraph.expectedChunks();
    }

    public SectionOcclusionGraph sectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    public Gizmos.TemporaryCollection collectPerFrameRenderThreadGizmos() {
        return Gizmos.withCollector(this.renderThreadGizmos);
    }

    private void finalizeGizmoCollection() {
        DrawableGizmoPrimitives standardPrimitives = new DrawableGizmoPrimitives();
        DrawableGizmoPrimitives alwaysOnTopPrimitives = new DrawableGizmoPrimitives();
        long currentMillis = Util.getMillis();
        for (SimpleGizmoCollector.GizmoInstance instance : this.renderThreadGizmos.drainGizmos()) {
            instance.gizmo().emit(instance.isAlwaysOnTop() ? alwaysOnTopPrimitives : standardPrimitives, instance.getAlphaMultiplier(currentMillis));
        }
        this.finalizedGizmos = new FinalizedGizmos(standardPrimitives, alwaysOnTopPrimitives);
    }

    public void addMainThreadGizmos(List<SimpleGizmoCollector.GizmoInstance> mainThreadGizmos) {
        this.renderThreadGizmos.addTemporaryGizmos(mainThreadGizmos);
    }

    @Environment(value=EnvType.CLIENT)
    private record FinalizedGizmos(DrawableGizmoPrimitives standardPrimitives, DrawableGizmoPrimitives alwaysOnTopPrimitives) {
    }
}

