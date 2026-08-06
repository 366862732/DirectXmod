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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class FishingHookRenderer
extends EntityRenderer<FishingHook, FishingHookRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutCull(TEXTURE_LOCATION);
    private static final double VIEW_BOBBING_SCALE = 960.0;

    public FishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FishingHook entity, Frustum culler, double camX, double camY, double camZ) {
        return super.shouldRender(entity, culler, camX, camY, camZ) && entity.getPlayerOwner() != null;
    }

    @Override
    public void submit(FishingHookRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.mulPose((Quaternionfc)camera.orientation);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            FishingHookRenderer.vertex(buffer, pose, state.lightCoords, 0.0f, 0, 0, 1);
            FishingHookRenderer.vertex(buffer, pose, state.lightCoords, 1.0f, 0, 1, 1);
            FishingHookRenderer.vertex(buffer, pose, state.lightCoords, 1.0f, 1, 1, 0);
            FishingHookRenderer.vertex(buffer, pose, state.lightCoords, 0.0f, 1, 0, 0);
        });
        poseStack.popPose();
        float xa = (float)state.lineOriginOffset.x;
        float ya = (float)state.lineOriginOffset.y;
        float za = (float)state.lineOriginOffset.z;
        float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            int steps = 16;
            for (int i = 0; i < 16; ++i) {
                float a0 = FishingHookRenderer.fraction(i, 16);
                float a1 = FishingHookRenderer.fraction(i + 1, 16);
                FishingHookRenderer.stringVertex(xa, ya, za, buffer, pose, a0, a1, width);
                FishingHookRenderer.stringVertex(xa, ya, za, buffer, pose, a1, a0, width);
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static HumanoidArm getHoldingArm(Player owner) {
        return owner.getMainHandItem().getItem() instanceof FishingRodItem ? owner.getMainArm() : owner.getMainArm().getOpposite();
    }

    private Vec3 getPlayerHandPos(Player owner, float swing, float partialTicks) {
        int invert;
        int n = invert = FishingHookRenderer.getHoldingArm(owner) == HumanoidArm.RIGHT ? 1 : -1;
        if (!this.entityRenderDispatcher.options.getCameraType().isFirstPerson() || owner != Minecraft.getInstance().player) {
            float ownerYRot = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * ((float)Math.PI / 180);
            double sin = Mth.sin(ownerYRot);
            double cos = Mth.cos(ownerYRot);
            float playerScale = owner.getScale();
            double rightOffset = (double)invert * 0.35 * (double)playerScale;
            double forwardOffset = 0.8 * (double)playerScale;
            float yOffset = owner.isCrouching() ? -0.1875f : 0.0f;
            return owner.getEyePosition(partialTicks).add(-cos * rightOffset - sin * forwardOffset, (double)yOffset - 0.45 * (double)playerScale, -sin * rightOffset + cos * forwardOffset);
        }
        float fov = this.entityRenderDispatcher.options.fov().get().intValue();
        double viewBobbingScale = 960.0 / (double)fov;
        Vec3 viewVec = this.entityRenderDispatcher.camera.getNearPlane(fov).getPointOnPlane((float)invert * 0.525f, -0.1f).scale(viewBobbingScale).yRot(swing * 0.5f).xRot(-swing * 0.7f);
        return owner.getEyePosition(partialTicks).add(viewVec);
    }

    private static float fraction(int i, int steps) {
        return (float)i / (float)steps;
    }

    private static void vertex(VertexConsumer builder, PoseStack.Pose pose, int lightCoords, float x, int y, int u, int v) {
        builder.addVertex(pose, x - 0.5f, (float)y - 0.5f, 0.0f).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    private static void stringVertex(float xa, float ya, float za, VertexConsumer stringBuffer, PoseStack.Pose stringPose, float aa, float nexta, float width) {
        float x = xa * aa;
        float y = ya * (aa * aa + aa) * 0.5f + 0.25f;
        float z = za * aa;
        float nx = xa * nexta - x;
        float ny = ya * (nexta * nexta + nexta) * 0.5f + 0.25f - y;
        float nz = za * nexta - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        stringBuffer.addVertex(stringPose, x, y, z).setColor(-16777216).setNormal(stringPose, nx /= length, ny /= length, nz /= length).setLineWidth(width);
    }

    @Override
    public FishingHookRenderState createRenderState() {
        return new FishingHookRenderState();
    }

    @Override
    public void extractRenderState(FishingHook entity, FishingHookRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Player owner = entity.getPlayerOwner();
        if (owner == null) {
            state.lineOriginOffset = Vec3.ZERO;
            return;
        }
        float swing = owner.getAttackAnim(partialTicks);
        float swing2 = Mth.sin(Mth.sqrt(swing) * (float)Math.PI);
        Vec3 playerPos = this.getPlayerHandPos(owner, swing2, partialTicks);
        Vec3 hookPos = entity.getPosition(partialTicks).add(0.0, 0.25, 0.0);
        state.lineOriginOffset = playerPos.subtract(hookPos);
    }

    @Override
    protected boolean affectedByCulling(FishingHook entity) {
        return false;
    }
}

