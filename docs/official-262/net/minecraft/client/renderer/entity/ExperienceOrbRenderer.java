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
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class ExperienceOrbRenderer
extends EntityRenderer<ExperienceOrb, ExperienceOrbRenderState> {
    private static final Identifier EXPERIENCE_ORB_LOCATION = Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCullItemTarget(EXPERIENCE_ORB_LOCATION);

    public ExperienceOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15f;
        this.shadowStrength = 0.75f;
    }

    @Override
    protected int getBlockLightLevel(ExperienceOrb entity, BlockPos blockPos) {
        return Mth.clamp(super.getBlockLightLevel(entity, blockPos) + 7, 0, 15);
    }

    @Override
    public void submit(ExperienceOrbRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        int icon = state.icon;
        float u0 = (float)(icon % 4 * 16 + 0) / 64.0f;
        float u1 = (float)(icon % 4 * 16 + 16) / 64.0f;
        float v0 = (float)(icon / 4 * 16 + 0) / 64.0f;
        float v1 = (float)(icon / 4 * 16 + 16) / 64.0f;
        float r = 1.0f;
        float xo = 0.5f;
        float yo = 0.25f;
        float br = 255.0f;
        float rr = state.ageInTicks / 2.0f;
        int rc = (int)((Mth.sin(rr + 0.0f) + 1.0f) * 0.5f * 255.0f);
        int gc = 255;
        int bc = (int)((Mth.sin(rr + 4.1887903f) + 1.0f) * 0.1f * 255.0f);
        poseStack.translate(0.0f, 0.1f, 0.0f);
        poseStack.mulPose((Quaternionfc)camera.orientation);
        float s = 0.3f;
        poseStack.scale(0.3f, 0.3f, 0.3f);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            ExperienceOrbRenderer.vertex(buffer, pose, -0.5f, -0.25f, rc, 255, bc, u0, v1, state.lightCoords);
            ExperienceOrbRenderer.vertex(buffer, pose, 0.5f, -0.25f, rc, 255, bc, u1, v1, state.lightCoords);
            ExperienceOrbRenderer.vertex(buffer, pose, 0.5f, 0.75f, rc, 255, bc, u1, v0, state.lightCoords);
            ExperienceOrbRenderer.vertex(buffer, pose, -0.5f, 0.75f, rc, 255, bc, u0, v0, state.lightCoords);
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int r, int g, int b, float u, float v, int lightCoords) {
        buffer.addVertex(pose, x, y, 0.0f).setColor(r, g, b, 128).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ExperienceOrbRenderState createRenderState() {
        return new ExperienceOrbRenderState();
    }

    @Override
    public void extractRenderState(ExperienceOrb entity, ExperienceOrbRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.icon = entity.getIcon();
    }
}

