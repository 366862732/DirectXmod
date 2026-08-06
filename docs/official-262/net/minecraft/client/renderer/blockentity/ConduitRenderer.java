/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SpriteMapper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.ConduitRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ConduitRenderer
implements BlockEntityRenderer<ConduitBlockEntity, ConduitRenderState> {
    public static final Transformation DEFAULT_TRANSFORMATION = new Transformation((Vector3fc)new Vector3f(0.5f, 0.5f, 0.5f), null, null, null);
    public static final SpriteMapper MAPPER = new SpriteMapper(TextureAtlas.LOCATION_BLOCKS, "entity/conduit");
    public static final SpriteId SHELL_TEXTURE = MAPPER.defaultNamespaceApply("base");
    public static final SpriteId ACTIVE_SHELL_TEXTURE = MAPPER.defaultNamespaceApply("cage");
    public static final SpriteId WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind");
    public static final SpriteId VERTICAL_WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind_vertical");
    public static final SpriteId OPEN_EYE_TEXTURE = MAPPER.defaultNamespaceApply("open_eye");
    public static final SpriteId CLOSED_EYE_TEXTURE = MAPPER.defaultNamespaceApply("closed_eye");
    private final SpriteGetter sprites;
    private final ModelPart eye;
    private final ModelPart wind;
    private final ModelPart shell;
    private final ModelPart cage;

    public ConduitRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.eye = context.bakeLayer(ModelLayers.CONDUIT_EYE);
        this.wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
        this.shell = context.bakeLayer(ModelLayers.CONDUIT_SHELL);
        this.cage = context.bakeLayer(ModelLayers.CONDUIT_CAGE);
    }

    public static LayerDefinition createEyeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, 0.0f, 8.0f, 8.0f, 0.0f, new CubeDeformation(0.01f)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    public static LayerDefinition createWindLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -8.0f, -8.0f, 16.0f, 16.0f, 16.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createShellLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    public static LayerDefinition createCageLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public ConduitRenderState createRenderState() {
        return new ConduitRenderState();
    }

    @Override
    public void extractRenderState(ConduitBlockEntity blockEntity, ConduitRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.isActive = blockEntity.isActive();
        state.activeRotation = blockEntity.getActiveRotation(blockEntity.isActive() ? partialTicks : 0.0f);
        state.animTime = (float)blockEntity.tickCount + partialTicks;
        state.animationPhase = blockEntity.tickCount / 66 % 3;
        state.isHunting = blockEntity.isHunting();
    }

    @Override
    public void submit(ConduitRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.isActive) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.mulPose((Quaternionfc)new Quaternionf().rotationY(state.activeRotation * ((float)Math.PI / 180)));
            submitNodeCollector.submitModelPart(this.shell, poseStack, SHELL_TEXTURE.renderType(RenderTypes::entitySolid), state.lightCoords, OverlayTexture.NO_OVERLAY, this.sprites.get(SHELL_TEXTURE), -1, state.breakProgress);
            poseStack.popPose();
            return;
        }
        float rotation = state.activeRotation * 57.295776f;
        float hh = Mth.sin(state.animTime * 0.1f) / 2.0f + 0.5f;
        hh = hh * hh + hh;
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.3f + hh * 0.2f, 0.5f);
        Vector3f axis = new Vector3f(0.5f, 1.0f, 0.5f).normalize();
        poseStack.mulPose((Quaternionfc)new Quaternionf().rotationAxis(rotation * ((float)Math.PI / 180), (Vector3fc)axis));
        submitNodeCollector.submitModelPart(this.cage, poseStack, ACTIVE_SHELL_TEXTURE.renderType(RenderTypes::entityCutout), state.lightCoords, OverlayTexture.NO_OVERLAY, this.sprites.get(ACTIVE_SHELL_TEXTURE), -1, state.breakProgress);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        if (state.animationPhase == 1) {
            poseStack.mulPose((Quaternionfc)new Quaternionf().rotationX(1.5707964f));
        } else if (state.animationPhase == 2) {
            poseStack.mulPose((Quaternionfc)new Quaternionf().rotationZ(1.5707964f));
        }
        SpriteId windSpriteId = state.animationPhase == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE;
        RenderType windRenderType = windSpriteId.renderType(RenderTypes::entityCutout);
        TextureAtlasSprite windSprite = this.sprites.get(windSpriteId);
        submitNodeCollector.submitModelPart(this.wind, poseStack, windRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, windSprite);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.scale(0.875f, 0.875f, 0.875f);
        poseStack.mulPose((Quaternionfc)new Quaternionf().rotationXYZ((float)Math.PI, 0.0f, (float)Math.PI));
        submitNodeCollector.submitModelPart(this.wind, poseStack, windRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, windSprite);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.3f + hh * 0.2f, 0.5f);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.mulPose((Quaternionfc)camera.orientation);
        poseStack.mulPose((Quaternionfc)new Quaternionf().rotationZ((float)Math.PI).rotateY((float)Math.PI));
        float scale = 1.3333334f;
        poseStack.scale(1.3333334f, 1.3333334f, 1.3333334f);
        SpriteId eyeSprite = state.isHunting ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE;
        submitNodeCollector.submitModelPart(this.eye, poseStack, eyeSprite.renderType(RenderTypes::entityCutout), state.lightCoords, OverlayTexture.NO_OVERLAY, this.sprites.get(eyeSprite));
        poseStack.popPose();
    }
}

