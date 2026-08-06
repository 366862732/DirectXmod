/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.runtime.SwitchBootstraps;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class EntityRenderDispatcher
implements ResourceManagerReloadListener {
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers = ImmutableMap.of();
    private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers = Map.of();
    private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers = Map.of();
    public final TextureManager textureManager;
    public @Nullable Camera camera;
    public Entity crosshairPickEntity;
    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;
    private final MapRenderer mapRenderer;
    private final ItemInHandRenderer itemInHandRenderer;
    private final AtlasManager atlasManager;
    private final Font font;
    public final Options options;
    private final Supplier<EntityModelSet> entityModels;
    private final EquipmentAssetManager equipmentAssets;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public <E extends Entity> int getPackedLightCoords(E entity, float partialTickTime) {
        return this.getRenderer((EntityRenderState)((Object)entity)).getPackedLightCoords(entity, partialTickTime);
    }

    public EntityRenderDispatcher(Minecraft minecraft, TextureManager textureManager, BlockModelResolver blockModelResolver, ItemModelResolver itemModelResolver, MapRenderer mapRenderer, AtlasManager atlasManager, Font font, Options options, Supplier<EntityModelSet> entityModels, EquipmentAssetManager equipmentAssets, PlayerSkinRenderCache playerSkinRenderCache) {
        this.textureManager = textureManager;
        this.blockModelResolver = blockModelResolver;
        this.itemModelResolver = itemModelResolver;
        this.mapRenderer = mapRenderer;
        this.atlasManager = atlasManager;
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.itemInHandRenderer = new ItemInHandRenderer(minecraft, this, itemModelResolver);
        this.font = font;
        this.options = options;
        this.entityModels = entityModels;
        this.equipmentAssets = equipmentAssets;
    }

    public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity) {
        T t = entity;
        Objects.requireNonNull(t);
        T t2 = t;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{AbstractClientPlayer.class, ClientMannequin.class}, t2, n)) {
            case 0 -> {
                AbstractClientPlayer player = (AbstractClientPlayer)t2;
                yield this.getAvatarRenderer(this.playerRenderers, player);
            }
            case 1 -> {
                ClientMannequin mannequin = (ClientMannequin)t2;
                yield this.getAvatarRenderer(this.mannequinRenderers, mannequin);
            }
            default -> this.renderers.get(entity.getType());
        };
    }

    public AvatarRenderer<AbstractClientPlayer> getPlayerRenderer(AbstractClientPlayer player) {
        return this.getAvatarRenderer(this.playerRenderers, player);
    }

    private <T extends Avatar> AvatarRenderer<T> getAvatarRenderer(Map<PlayerModelType, AvatarRenderer<T>> renderers, T entity) {
        PlayerModelType model = ((ClientAvatarEntity)((Object)entity)).getSkin().model();
        AvatarRenderer<T> playerRenderer = renderers.get(model);
        if (playerRenderer != null) {
            return playerRenderer;
        }
        return renderers.get(PlayerModelType.WIDE);
    }

    public <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S entityRenderState) {
        if (entityRenderState instanceof AvatarRenderState) {
            AvatarRenderState player = (AvatarRenderState)entityRenderState;
            PlayerModelType model = player.skin.model();
            EntityRenderer playerRenderer = this.playerRenderers.get(model);
            if (playerRenderer != null) {
                return playerRenderer;
            }
            return this.playerRenderers.get(PlayerModelType.WIDE);
        }
        return this.renderers.get(entityRenderState.entityType);
    }

    public void prepare(Camera camera, Entity crosshairPickEntity) {
        this.camera = camera;
        this.crosshairPickEntity = crosshairPickEntity;
    }

    public <E extends Entity> boolean shouldRender(E entity, Frustum culler, double camX, double camY, double camZ) {
        EntityRenderer<?, E> renderer = this.getRenderer((EntityRenderState)((Object)entity));
        return renderer.shouldRender(entity, culler, camX, camY, camZ);
    }

    public <E extends Entity> EntityRenderState extractEntity(E entity, float partialTicks) {
        EntityRenderer<?, E> renderer = this.getRenderer((EntityRenderState)((Object)entity));
        try {
            return renderer.createRenderState(entity, partialTicks);
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Extracting render state for an entity in world");
            CrashReportCategory entityCat = report.addCategory("Entity being extracted");
            entity.fillCrashReportCategory(entityCat);
            CrashReportCategory rendererCategory = this.fillRendererDetails(renderer, report);
            rendererCategory.setDetail("Delta", Float.valueOf(partialTicks));
            throw new ReportedException(report);
        }
    }

    public <S extends EntityRenderState> void submit(S renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        EntityRenderer<?, S> renderer = this.getRenderer(renderState);
        try {
            Vec3 pos = renderer.getRenderOffset(renderState);
            double relativeX = x + pos.x();
            double relativeY = y + pos.y();
            double relativeZ = z + pos.z();
            poseStack.pushPose();
            poseStack.translate(relativeX, relativeY, relativeZ);
            renderer.submit(renderState, poseStack, submitNodeCollector, camera);
            if (renderState.displayFireAnimation) {
                submitNodeCollector.submitFlame(poseStack, renderState, Mth.rotationAroundAxis(Mth.Y_AXIS, camera.orientation, new Quaternionf()));
            }
            if (renderState instanceof AvatarRenderState) {
                poseStack.translate(-pos.x(), -pos.y(), -pos.z());
            }
            if (!renderState.shadowPieces.isEmpty()) {
                submitNodeCollector.submitShadow(poseStack, renderState.shadowRadius, renderState.shadowPieces);
            }
            if (!(renderState instanceof AvatarRenderState)) {
                poseStack.translate(-pos.x(), -pos.y(), -pos.z());
            }
            poseStack.popPose();
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Rendering entity in world");
            CrashReportCategory entityCat = report.addCategory("EntityRenderState being rendered");
            renderState.fillCrashReportCategory(entityCat);
            this.fillRendererDetails(renderer, report);
            throw new ReportedException(report);
        }
    }

    private <S extends EntityRenderState> CrashReportCategory fillRendererDetails(EntityRenderer<?, S> renderer, CrashReport report) {
        CrashReportCategory category = report.addCategory("Renderer details");
        category.setDetail("Assigned renderer", renderer);
        return category;
    }

    public void resetCamera() {
        this.camera = null;
    }

    public double distanceToSqr(Entity entity) {
        return this.camera.position().distanceToSqr(entity.position());
    }

    public ItemInHandRenderer getItemInHandRenderer() {
        return this.itemInHandRenderer;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(this, this.blockModelResolver, this.itemModelResolver, this.mapRenderer, resourceManager, this.entityModels.get(), this.equipmentAssets, this.atlasManager, this.font, this.playerSkinRenderCache);
        this.renderers = EntityRenderers.createEntityRenderers(context);
        this.playerRenderers = EntityRenderers.createAvatarRenderers(context);
        this.mannequinRenderers = EntityRenderers.createAvatarRenderers(context);
    }
}

