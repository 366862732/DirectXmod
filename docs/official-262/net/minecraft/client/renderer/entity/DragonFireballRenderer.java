/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class DragonFireballRenderer
extends EntityRenderer<DragonFireball, EntityRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_fireball.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE_LOCATION);

    public DragonFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected int getBlockLightLevel(DragonFireball entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 2.0f);
        poseStack.mulPose((Quaternionfc)camera.orientation);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> DragonFireballRenderer.buildQuad(state, pose, buffer, -1));
        if (state.outlineColor != 0 && RENDER_TYPE.outline().isPresent()) {
            submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE.outline().get(), (pose, buffer) -> DragonFireballRenderer.buildQuad(state, pose, buffer, state.outlineColor));
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void buildQuad(EntityRenderState state, PoseStack.Pose pose, VertexConsumer buffer, int color) {
        DragonFireballRenderer.vertex(buffer, pose, state.lightCoords, 0.0f, 0, 0, 1, color);
        DragonFireballRenderer.vertex(buffer, pose, state.lightCoords, 1.0f, 0, 1, 1, color);
        DragonFireballRenderer.vertex(buffer, pose, state.lightCoords, 1.0f, 1, 1, 0, color);
        DragonFireballRenderer.vertex(buffer, pose, state.lightCoords, 0.0f, 1, 0, 0, color);
    }

    private static void vertex(VertexConsumer builder, PoseStack.Pose pose, int lightCoords, float x, int y, int u, int v, int color) {
        builder.addVertex(pose, x - 0.5f, (float)y - 0.25f, 0.0f).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}

