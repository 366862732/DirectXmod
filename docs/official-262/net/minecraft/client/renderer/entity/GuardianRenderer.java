/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.guardian.GuardianModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GuardianRenderer
extends MobRenderer<Guardian, GuardianRenderState, GuardianModel> {
    private static final Identifier GUARDIAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian/guardian.png");
    private static final Identifier GUARDIAN_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian/guardian_beam.png");
    private static final RenderType BEAM_RENDER_TYPE = RenderTypes.entityCutout(GUARDIAN_BEAM_LOCATION);

    public GuardianRenderer(EntityRendererProvider.Context context) {
        this(context, 0.5f, ModelLayers.GUARDIAN);
    }

    protected GuardianRenderer(EntityRendererProvider.Context context, float shadow, ModelLayerLocation modelId) {
        super(context, new GuardianModel(context.bakeLayer(modelId)), shadow);
    }

    @Override
    public boolean shouldRender(Guardian entity, Frustum culler, double camX, double camY, double camZ) {
        LivingEntity lookAtEntity;
        if (super.shouldRender(entity, culler, camX, camY, camZ)) {
            return true;
        }
        if (entity.hasActiveAttackTarget() && (lookAtEntity = entity.getActiveAttackTarget()) != null) {
            Vec3 targetPos = this.getPosition(lookAtEntity, (double)lookAtEntity.getBbHeight() * 0.5, 1.0f);
            Vec3 startPos = this.getPosition(entity, entity.getEyeHeight(), 1.0f);
            return culler.isVisible(new AABB(startPos.x, startPos.y, startPos.z, targetPos.x, targetPos.y, targetPos.z));
        }
        return false;
    }

    private Vec3 getPosition(LivingEntity entity, double yOffset, float partialTicks) {
        double sx = Mth.lerp((double)partialTicks, entity.xOld, entity.getX());
        double sy = Mth.lerp((double)partialTicks, entity.yOld, entity.getY()) + yOffset;
        double sz = Mth.lerp((double)partialTicks, entity.zOld, entity.getZ());
        return new Vec3(sx, sy, sz);
    }

    @Override
    public void submit(GuardianRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        Vec3 targetPosition = state.attackTargetPosition;
        if (targetPosition != null) {
            float texVOff = state.attackTime * 0.5f % 1.0f;
            poseStack.pushPose();
            poseStack.translate(0.0f, state.eyeHeight, 0.0f);
            GuardianRenderer.renderBeam(poseStack, submitNodeCollector, targetPosition.subtract(state.eyePosition), state.attackTime, state.attackScale, texVOff);
            poseStack.popPose();
        }
    }

    private static void renderBeam(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Vec3 beamVector, float timeInTicks, float scale, float texVOff) {
        float length = (float)(beamVector.length() + 1.0);
        beamVector = beamVector.normalize();
        float xRot = (float)Math.acos(beamVector.y);
        float yRot = 1.5707964f - (float)Math.atan2(beamVector.z, beamVector.x);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(yRot * 57.295776f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(xRot * 57.295776f));
        float rot = timeInTicks * 0.05f * -1.5f;
        float colorScale = scale * scale;
        int red = 64 + (int)(colorScale * 191.0f);
        int green = 32 + (int)(colorScale * 191.0f);
        int blue = 128 - (int)(colorScale * 64.0f);
        float rr1 = 0.2f;
        float rr2 = 0.282f;
        float wnx = Mth.cos(rot + 2.3561945f) * 0.282f;
        float wnz = Mth.sin(rot + 2.3561945f) * 0.282f;
        float enx = Mth.cos(rot + 0.7853982f) * 0.282f;
        float enz = Mth.sin(rot + 0.7853982f) * 0.282f;
        float wsx = Mth.cos(rot + 3.926991f) * 0.282f;
        float wsz = Mth.sin(rot + 3.926991f) * 0.282f;
        float esx = Mth.cos(rot + 5.4977875f) * 0.282f;
        float esz = Mth.sin(rot + 5.4977875f) * 0.282f;
        float wx = Mth.cos(rot + (float)Math.PI) * 0.2f;
        float wz = Mth.sin(rot + (float)Math.PI) * 0.2f;
        float ex = Mth.cos(rot + 0.0f) * 0.2f;
        float ez = Mth.sin(rot + 0.0f) * 0.2f;
        float nx = Mth.cos(rot + 1.5707964f) * 0.2f;
        float nz = Mth.sin(rot + 1.5707964f) * 0.2f;
        float sx = Mth.cos(rot + 4.712389f) * 0.2f;
        float sz = Mth.sin(rot + 4.712389f) * 0.2f;
        float top = length;
        float minU = 0.0f;
        float maxU = 0.4999f;
        float minV = -1.0f + texVOff;
        float maxV = minV + length * 2.5f;
        submitNodeCollector.submitCustomGeometry(poseStack, BEAM_RENDER_TYPE, (pose, buffer) -> {
            GuardianRenderer.vertex(buffer, pose, wx, top, wz, red, green, blue, 0.4999f, maxV);
            GuardianRenderer.vertex(buffer, pose, wx, 0.0f, wz, red, green, blue, 0.4999f, minV);
            GuardianRenderer.vertex(buffer, pose, ex, 0.0f, ez, red, green, blue, 0.0f, minV);
            GuardianRenderer.vertex(buffer, pose, ex, top, ez, red, green, blue, 0.0f, maxV);
            GuardianRenderer.vertex(buffer, pose, nx, top, nz, red, green, blue, 0.4999f, maxV);
            GuardianRenderer.vertex(buffer, pose, nx, 0.0f, nz, red, green, blue, 0.4999f, minV);
            GuardianRenderer.vertex(buffer, pose, sx, 0.0f, sz, red, green, blue, 0.0f, minV);
            GuardianRenderer.vertex(buffer, pose, sx, top, sz, red, green, blue, 0.0f, maxV);
            float vBase = Mth.floor(timeInTicks) % 2 == 0 ? 0.5f : 0.0f;
            GuardianRenderer.vertex(buffer, pose, wnx, top, wnz, red, green, blue, 0.5f, vBase + 0.5f);
            GuardianRenderer.vertex(buffer, pose, enx, top, enz, red, green, blue, 1.0f, vBase + 0.5f);
            GuardianRenderer.vertex(buffer, pose, esx, top, esz, red, green, blue, 1.0f, vBase);
            GuardianRenderer.vertex(buffer, pose, wsx, top, wsz, red, green, blue, 0.5f, vBase);
        });
    }

    private static void vertex(VertexConsumer builder, PoseStack.Pose pose, float x, float y, float z, int red, int green, int blue, float u, float v) {
        builder.addVertex(pose, x, y, z).setColor(red, green, blue, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public Identifier getTextureLocation(GuardianRenderState state) {
        return GUARDIAN_LOCATION;
    }

    @Override
    public GuardianRenderState createRenderState() {
        return new GuardianRenderState();
    }

    @Override
    public void extractRenderState(Guardian entity, GuardianRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.spikesAnimation = entity.getSpikesAnimation(partialTicks);
        state.tailAnimation = entity.getTailAnimation(partialTicks);
        state.eyePosition = entity.getEyePosition(partialTicks);
        Entity lookAtEntity = GuardianRenderer.getEntityToLookAt(entity);
        if (lookAtEntity != null) {
            state.lookDirection = entity.getViewVector(partialTicks);
            state.lookAtPosition = lookAtEntity.getEyePosition(partialTicks);
        } else {
            state.lookDirection = null;
            state.lookAtPosition = null;
        }
        LivingEntity targetEntity = entity.getActiveAttackTarget();
        if (targetEntity != null) {
            state.attackScale = entity.getAttackAnimationScale(partialTicks);
            state.attackTime = entity.getClientSideAttackTime() + partialTicks;
            state.attackTargetPosition = this.getPosition(targetEntity, (double)targetEntity.getBbHeight() * 0.5, partialTicks);
        } else {
            state.attackTargetPosition = null;
        }
    }

    private static @Nullable Entity getEntityToLookAt(Guardian entity) {
        Entity lookAtEntity = Minecraft.getInstance().getCameraEntity();
        if (entity.hasActiveAttackTarget()) {
            return entity.getActiveAttackTarget();
        }
        return lookAtEntity;
    }
}

