/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.LongCollection
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.extract;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.GameTestBlockHighlightRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LevelExtractor
implements ResourceManagerReloadListener {
    private static final float CHUNK_VISIBILITY_THRESHOLD = 0.3f;
    private final Minecraft minecraft;
    private final LevelRenderer levelRenderer;
    private @Nullable ClientLevel level;
    private @Nullable SectionUpdateTracker sectionUpdateTracker;
    private final LevelRenderState levelRenderState;
    public final DebugRenderer debugRenderer = new DebugRenderer();
    public final GameTestBlockHighlightRenderer gameTestBlockHighlightRenderer = new GameTestBlockHighlightRenderer();
    private final SimpleGizmoCollector mainThreadGizmos = new SimpleGizmoCollector();
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    private int lastViewDistance = -1;
    private boolean shouldInvalidateCompiledGeometry;
    private boolean shouldResetLevelRenderData;
    private boolean shouldResetChunkLayerSampler;
    private boolean shouldResetSkyRenderer;

    public LevelExtractor(Minecraft minecraft, LevelRenderState levelRenderState, LevelRenderer levelRenderer) {
        this.minecraft = minecraft;
        this.levelRenderer = levelRenderer;
        this.levelRenderState = levelRenderState;
    }

    public void extract(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick) {
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }
        Vec3 cameraPos = camera.position();
        if (this.sectionUpdateTracker != null) {
            this.sectionUpdateTracker.repositionCamera(SectionPos.of(cameraPos));
        }
        if (this.shouldResetLevelRenderData) {
            this.levelRenderer.resetLevelRenderData();
            this.shouldResetLevelRenderData = false;
        }
        this.levelRenderState.reset();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("level");
        this.levelRenderState.shouldResetChunkLayerSampler = this.shouldResetChunkLayerSampler;
        this.shouldResetChunkLayerSampler = false;
        this.levelRenderState.shouldResetSkyRenderer = this.shouldResetSkyRenderer;
        this.shouldResetSkyRenderer = false;
        this.levelRenderState.gameTime = this.level.getGameTime();
        Frustum cullFrustum = camera.getCullFrustum();
        profiler.push("prepareDispatchers");
        this.levelRenderer.blockEntityRenderDispatcher().prepare(cameraPos);
        this.levelRenderer.entityRenderDispatcher().prepare(camera, this.minecraft.crosshairPickEntity);
        if (this.shouldInvalidateCompiledGeometry) {
            this.levelRenderer.invalidateCompiledGeometry(this.level, this.minecraft.options, camera, this.minecraft.getBlockColors());
            this.shouldInvalidateCompiledGeometry = false;
        } else if (camera.getCapturedFrustum() == null) {
            double camRotX = Math.floor(camera.xRot() / 2.0f);
            double camRotY = Math.floor(camera.yRot() / 2.0f);
            if (this.levelRenderer.sectionOcclusionGraph().consumeFrustumUpdate() || camRotX != this.prevCamRotX || camRotY != this.prevCamRotY) {
                profiler.popPush("applyFrustum");
                this.applyFrustum(cullFrustum);
                this.prevCamRotX = camRotX;
                this.prevCamRotY = camRotY;
            }
        }
        if (this.sectionUpdateTracker != null && this.level != null) {
            ClientChunkCache chunkCache = this.level.getChunkSource();
            this.levelRenderState.chunkLoadingRenderState.addedEmptySections = chunkCache.addedEmptySections();
            this.levelRenderState.chunkLoadingRenderState.removedEmptySections = chunkCache.removedEmptySections();
            this.levelRenderState.chunkLoadingRenderState.addedLoadedChunks = chunkCache.addedLoadedChunks();
            this.levelRenderState.chunkLoadingRenderState.removedLoadedChunks = chunkCache.removedLoadedChunks();
            chunkCache.flipUpdateTrackingSets();
            LongCollection expectedChunks = this.levelRenderer.expectedChunks();
            expectedChunks.forEach(expectedChunk -> {
                if (chunkCache.hasChunk(ChunkPos.getX(expectedChunk), ChunkPos.getZ(expectedChunk))) {
                    this.levelRenderState.chunkLoadingRenderState.loadedExpectedChunks.add(expectedChunk);
                }
            });
            profiler.popPush("sectionUpdates");
            RenderRegionCache cache = new RenderRegionCache();
            for (SectionRenderDispatcher.RenderSection section : this.levelRenderer.visibleSections()) {
                SectionUpdateTracker.SectionDirtyState dirtyState = this.sectionUpdateTracker.getDirtyState(section.getSectionNode());
                if (dirtyState == null || !dirtyState.isDirty() || section.sectionMesh.get() == CompiledSectionMesh.UNCOMPILED && !this.sectionUpdateTracker.hasAllNeighbors(this.level, section.getSectionNode())) continue;
                this.levelRenderState.sectionUpdateRenderStates.add(new SectionUpdateRenderState(section.getSectionNode(), dirtyState.isDirtyFromPlayer(), cache.createRegion(this.level, section.getSectionNode())));
                dirtyState.setNotDirty();
            }
        }
        profiler.popPush("entities");
        this.extractVisibleEntities(camera, cullFrustum, deltaTracker, this.levelRenderState);
        profiler.popPush("blockEntities");
        this.extractVisibleBlockEntities(camera, deltaPartialTick, this.levelRenderState);
        profiler.popPush("blockOutline");
        this.extractBlockOutline(camera, this.levelRenderState);
        profiler.popPush("blockBreaking");
        this.extractBlockDestroyAnimation(camera, this.levelRenderState);
        profiler.popPush("weather");
        this.levelRenderer.weatherEffectRenderer().extractRenderState(this.level, deltaPartialTick, cameraPos, this.levelRenderState.weatherRenderState);
        SkyRenderer skyRenderer = this.levelRenderer.skyRenderer();
        if (skyRenderer != null) {
            profiler.popPush("sky");
            skyRenderer.extractRenderState(this.level, deltaPartialTick, camera, this.levelRenderState.skyRenderState);
        }
        profiler.popPush("border");
        this.levelRenderer.worldBorderRenderer().extract(this.level.getWorldBorder(), deltaPartialTick, cameraPos, this.minecraft.options.getEffectiveRenderDistance() * 16, this.levelRenderState.worldBorderRenderState);
        profiler.popPush("particles");
        this.minecraft.particleEngine.extract(this.levelRenderState.particlesRenderState, new Frustum(cullFrustum).offset(-3.0f), camera, deltaPartialTick);
        profiler.popPush("cloud");
        this.levelRenderState.cloudColor = camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, deltaPartialTick);
        if (ARGB.alpha(this.levelRenderState.cloudColor) > 0) {
            this.levelRenderState.cloudHeight = camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_HEIGHT, deltaPartialTick).floatValue();
        }
        profiler.popPush("debug");
        this.debugRenderer.emitGizmos(cullFrustum, cameraPos.x, cameraPos.y, cameraPos.z, deltaTracker.getGameTimeDeltaPartialTick(false));
        this.gameTestBlockHighlightRenderer.emitGizmos();
        this.levelRenderState.render3dCrosshair = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR);
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) {
            this.levelRenderState.playerCompiledSectionCallback = connection.getPlayerCompiledSectionCallback();
        }
        this.levelRenderState.shouldShowEntityOutlines = this.shouldShowEntityOutlines(camera);
        this.extractGizmos();
        profiler.pop();
        profiler.pop();
    }

    private void extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output) {
        Vec3 cameraPos = camera.position();
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
        Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());
        EntityRenderDispatcher entityRenderDispatcher = this.levelRenderer.entityRenderDispatcher();
        for (Entity entity : this.level.entitiesForRendering()) {
            if (!this.isEntityVisible(entity, frustum, camX, camY, camZ) || entity == camera.entity() && !camera.isDetached() && (!(camera.entity() instanceof LivingEntity) || !((LivingEntity)camera.entity()).isSleeping()) || entity instanceof LocalPlayer && camera.entity() != entity) continue;
            if (entity.tickCount == 0) {
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
            }
            float partialEntity = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
            EntityRenderState state = this.extractEntity(entity, partialEntity);
            output.entityRenderStates.add(state);
        }
        output.lastEntityRenderStateCount = output.entityRenderStates.size();
    }

    public boolean isEntityVisible(Entity entity, Frustum frustum, double camX, double camY, double camZ) {
        if (this.level == null) {
            return false;
        }
        if (!(this.levelRenderer.entityRenderDispatcher().shouldRender(entity, frustum, camX, camY, camZ) || this.minecraft.player != null && entity.hasIndirectPassenger(this.minecraft.player))) {
            return false;
        }
        BlockPos blockPos = entity.blockPosition();
        return this.level.isOutsideBuildHeight(blockPos.getY()) || this.levelRenderer.isSectionCompiledAndVisible(blockPos);
    }

    private EntityRenderState extractEntity(Entity entity, float partialTickTime) {
        return this.levelRenderer.entityRenderDispatcher().extractEntity(entity, partialTickTime);
    }

    private void extractVisibleBlockEntities(Camera camera, float deltaPartialTick, LevelRenderState levelRenderState) {
        Vec3 cameraPos = camera.position();
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        PoseStack poseStack = new PoseStack();
        for (SectionRenderDispatcher.RenderSection section : this.levelRenderer.visibleSections()) {
            List<BlockEntity> renderableBlockEntities = section.getSectionMesh().getRenderableBlockEntities();
            if (renderableBlockEntities.isEmpty() || section.getVisibility(Util.getMillis()) < 0.3f) continue;
            for (BlockEntity blockEntity : renderableBlockEntities) {
                Object state;
                ModelFeatureRenderer.CrumblingOverlay breakProgress;
                BlockPos blockPos = blockEntity.getBlockPos();
                SortedSet progresses = (SortedSet)this.level.destructionProgress().get(blockPos.asLong());
                if (progresses == null || progresses.isEmpty()) {
                    breakProgress = null;
                } else {
                    poseStack.pushPose();
                    poseStack.translate((double)blockPos.getX() - camX, (double)blockPos.getY() - camY, (double)blockPos.getZ() - camZ);
                    breakProgress = new ModelFeatureRenderer.CrumblingOverlay(((BlockDestructionProgress)progresses.last()).getProgress(), poseStack.last());
                    poseStack.popPose();
                }
                if ((state = this.levelRenderer.blockEntityRenderDispatcher().tryExtractRenderState(blockEntity, deltaPartialTick, breakProgress, false)) == null) continue;
                levelRenderState.blockEntityRenderStates.add((BlockEntityRenderState)state);
            }
        }
        Iterator<BlockEntity> iterator = this.level.getGloballyRenderedBlockEntities().iterator();
        while (iterator.hasNext()) {
            BlockEntity blockEntity = iterator.next();
            if (blockEntity.isRemoved()) {
                iterator.remove();
                continue;
            }
            Object state = this.levelRenderer.blockEntityRenderDispatcher().tryExtractRenderState(blockEntity, deltaPartialTick, null, true);
            if (state == null) continue;
            levelRenderState.blockEntityRenderStates.add((BlockEntityRenderState)state);
        }
    }

    private void extractBlockDestroyAnimation(Camera camera, LevelRenderState levelRenderState) {
        Vec3 cameraPos = camera.position();
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        levelRenderState.blockBreakingRenderStates.clear();
        for (Long2ObjectMap.Entry entry : this.level.destructionProgress().long2ObjectEntrySet()) {
            SortedSet progresses;
            BlockPos pos = BlockPos.of(entry.getLongKey());
            if (pos.distToCenterSqr(camX, camY, camZ) > 1024.0 || (progresses = (SortedSet)entry.getValue()) == null || progresses.isEmpty()) continue;
            int progress = ((BlockDestructionProgress)progresses.last()).getProgress();
            levelRenderState.blockBreakingRenderStates.add(new BlockBreakingRenderState(pos, this.level.getBlockState(pos), progress));
        }
    }

    private void extractBlockOutline(Camera camera, LevelRenderState levelRenderState) {
        levelRenderState.blockOutlineRenderState = null;
        HitResult hitResult = this.minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult)) {
            return;
        }
        BlockHitResult blockHitResult = (BlockHitResult)hitResult;
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return;
        }
        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = this.level.getBlockState(pos);
        if (!state.isAir() && this.level.getWorldBorder().isWithinBounds(pos)) {
            BlockStateModel blockStateModel = this.minecraft.getModelManager().getBlockStateModelSet().get(state);
            boolean isBlockTranslucent = blockStateModel.hasMaterialFlag(1);
            boolean highContrast = this.minecraft.options.highContrastBlockOutline().get();
            CollisionContext context = CollisionContext.of(camera.entity());
            VoxelShape shape = state.getShape(this.level, pos, context);
            if (SharedConstants.DEBUG_SHAPES) {
                VoxelShape collisionShape = state.getCollisionShape(this.level, pos, context);
                VoxelShape occlusionShape = state.getOcclusionShape();
                VoxelShape interactionShape = state.getInteractionShape(this.level, pos);
                levelRenderState.blockOutlineRenderState = new BlockOutlineRenderState(pos, isBlockTranslucent, highContrast, shape, collisionShape, occlusionShape, interactionShape);
            } else {
                levelRenderState.blockOutlineRenderState = new BlockOutlineRenderState(pos, isBlockTranslucent, highContrast, shape);
            }
        }
    }

    private void extractGizmos() {
        this.mainThreadGizmos.addTemporaryGizmos(Minecraft.getInstance().getPerTickGizmos());
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            this.mainThreadGizmos.addTemporaryGizmos(server.getPerTickGizmos());
        }
        this.levelRenderer.addMainThreadGizmos(this.mainThreadGizmos.drainGizmos());
    }

    private void applyFrustum(Frustum frustum) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        }
        this.levelRenderer.clearVisibleSections();
        this.levelRenderer.sectionOcclusionGraph().addSectionsInFrustum(frustum, (List<SectionRenderDispatcher.RenderSection>)this.levelRenderer.visibleSections(), (List<SectionRenderDispatcher.RenderSection>)this.levelRenderer.nearbyVisibleSections());
    }

    private boolean shouldShowEntityOutlines(Camera camera) {
        return !camera.isPanoramicMode() && this.minecraft.player != null;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.shouldResetSkyRenderer = true;
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.level = level;
        if (level != null) {
            this.allChanged();
        } else {
            this.levelRenderer.entityRenderDispatcher().resetCamera();
            this.sectionUpdateTracker = null;
        }
        this.shouldResetLevelRenderData = true;
        this.gameTestBlockHighlightRenderer.clear();
    }

    public void allChanged() {
        if (this.level == null) {
            return;
        }
        this.level.clearTintCaches();
        Options options = this.minecraft.options;
        this.lastViewDistance = options.getEffectiveRenderDistance();
        this.sectionUpdateTracker = new SectionUpdateTracker(this.level, this.lastViewDistance);
        Camera camera = this.minecraft.gameRenderer.mainCamera();
        SectionPos cameraSectionPos = SectionPos.of(camera.position());
        this.sectionUpdateTracker.repositionCamera(cameraSectionPos);
        this.shouldInvalidateCompiledGeometry = true;
    }

    public void resetSampler() {
        this.shouldResetChunkLayerSampler = true;
    }

    public void blockChanged(BlockPos pos, @Block.UpdateFlags int updateFlags) {
        this.setBlockDirty(pos, (updateFlags & 8) != 0);
    }

    private void setBlockDirty(BlockPos pos, boolean playerChanged) {
        for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; ++z) {
            for (int x = pos.getX() - 1; x <= pos.getX() + 1; ++x) {
                for (int y = pos.getY() - 1; y <= pos.getY() + 1; ++y) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z), playerChanged);
                }
            }
        }
    }

    public void setBlocksDirty(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int z = z0 - 1; z <= z1 + 1; ++z) {
            for (int x = x0 - 1; x <= x1 + 1; ++x) {
                for (int y = y0 - 1; y <= y1 + 1; ++y) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z));
                }
            }
        }
    }

    public void setBlockDirty(BlockPos pos, BlockState oldState, BlockState newState) {
        if (this.minecraft.getModelManager().requiresRender(oldState, newState)) {
            this.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ) {
        this.setSectionRangeDirty(sectionX - 1, sectionY - 1, sectionZ - 1, sectionX + 1, sectionY + 1, sectionZ + 1);
    }

    public void setSectionRangeDirty(int minSectionX, int minSectionY, int minSectionZ, int maxSectionX, int maxSectionY, int maxSectionZ) {
        for (int z = minSectionZ; z <= maxSectionZ; ++z) {
            for (int x = minSectionX; x <= maxSectionX; ++x) {
                for (int y = minSectionY; y <= maxSectionY; ++y) {
                    this.setSectionDirty(x, y, z);
                }
            }
        }
    }

    public void setSectionDirty(int sectionX, int sectionY, int sectionZ) {
        this.setSectionDirty(sectionX, sectionY, sectionZ, false);
    }

    private void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean playerChanged) {
        this.sectionUpdateTracker.setDirty(sectionX, sectionY, sectionZ, playerChanged);
    }

    public Gizmos.TemporaryCollection collectPerFrameMainThreadGizmos() {
        return Gizmos.withCollector(this.mainThreadGizmos);
    }

    public int countRenderedSections() {
        int rendered = 0;
        for (SectionRenderDispatcher.RenderSection section : this.levelRenderer.visibleSections()) {
            if (!section.getSectionMesh().hasRenderableLayers()) continue;
            ++rendered;
        }
        return rendered;
    }

    @VisibleForDebug
    public @Nullable String sectionStatistics() {
        ViewArea viewArea = this.levelRenderer.viewArea();
        if (viewArea == null) {
            return null;
        }
        int totalSections = viewArea.size();
        int rendered = this.countRenderedSections();
        SectionRenderDispatcher sectionRenderDispatcher = this.levelRenderer.sectionRenderDispatcher();
        return String.format(Locale.ROOT, "C: %d/%d %sD: %d, %s", rendered, totalSections, this.minecraft.smartCull ? "(s) " : "", this.lastViewDistance, sectionRenderDispatcher == null ? "null" : sectionRenderDispatcher.getStats());
    }

    @VisibleForDebug
    public @Nullable String entityStatistics() {
        if (this.level == null) {
            return null;
        }
        return "E: " + this.levelRenderState.lastEntityRenderStateCount + "/" + this.level.getEntityCount() + ", SD: " + this.level.getServerSimulationDistance();
    }

    @VisibleForDebug
    public double totalSections() {
        return this.sectionUpdateTracker == null ? 0.0 : (double)this.sectionUpdateTracker.size();
    }

    @VisibleForDebug
    public double lastViewDistance() {
        return this.lastViewDistance;
    }
}

