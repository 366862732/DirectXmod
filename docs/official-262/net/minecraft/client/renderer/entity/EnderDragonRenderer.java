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
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class EnderDragonRenderer
extends EntityRenderer<EnderDragon, EnderDragonRenderState> {
    public static final Identifier CRYSTAL_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal_beam.png");
    private static final Identifier DRAGON_EXPLODING_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_exploding.png");
    private static final Identifier DRAGON_TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    private static final Identifier DRAGON_EYES_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
    private static final RenderType DYING_RENDER_TYPE = RenderTypes.entityCutoutDissolve(DRAGON_TEXTURE_LOCATION, DRAGON_EXPLODING_LOCATION);
    private static final RenderType EYES = RenderTypes.eyes(DRAGON_EYES_LOCATION);
    private static final RenderType BEAM = RenderTypes.endCrystalBeam(CRYSTAL_BEAM_LOCATION);
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);
    private final EnderDragonModel model;

    public EnderDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.model = new EnderDragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON));
    }

    @Override
    public void submit(EnderDragonRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        float yr = state.getHistoricalPos(7).yRot();
        float rot2 = (float)(state.getHistoricalPos(5).y() - state.getHistoricalPos(10).y());
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-yr));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(rot2 * 10.0f));
        poseStack.translate(0.0f, 0.0f, 1.0f);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);
        int overlayCoords = OverlayTexture.pack(0.0f, state.hasRedOverlay);
        if (state.deathTime > 0.0f) {
            int color = ARGB.white(1.0f - state.deathTime / 200.0f);
            submitNodeCollector.submitModel(this.model, state, poseStack, DYING_RENDER_TYPE, state.lightCoords, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null);
        } else {
            submitNodeCollector.submitModel(this.model, state, poseStack, DRAGON_TEXTURE_LOCATION, state.lightCoords, overlayCoords, state.outlineColor, null);
        }
        submitNodeCollector.submitModel(this.model, state, poseStack, EYES, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (state.deathTime > 0.0f) {
            float deathTime = state.deathTime / 200.0f;
            poseStack.pushPose();
            poseStack.translate(0.0f, -1.0f, -2.0f);
            EnderDragonRenderer.submitRays(poseStack, deathTime, submitNodeCollector, RenderTypes.dragonRays());
            poseStack.popPose();
        }
        poseStack.popPose();
        if (state.beamOffset != null) {
            EnderDragonRenderer.submitCrystalBeams((float)state.beamOffset.x, (float)state.beamOffset.y, (float)state.beamOffset.z, state.ageInTicks, poseStack, submitNodeCollector, state.lightCoords);
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void submitRays(PoseStack poseStack, float deathTime, SubmitNodeCollector submitNodeCollector, RenderType renderType) {
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            float overDrive = Math.min(deathTime > 0.8f ? (deathTime - 0.8f) / 0.2f : 0.0f, 1.0f);
            int innerColor = ARGB.colorFromFloat(1.0f - overDrive, 1.0f, 1.0f, 1.0f);
            int outerColor = 0xFF00FF;
            RandomSource random = RandomSource.createThreadLocalInstance(432L);
            Vector3f origin = new Vector3f();
            Vector3f outerLeft = new Vector3f();
            Vector3f outerRight = new Vector3f();
            Vector3f outerBottom = new Vector3f();
            Quaternionf rayRotation = new Quaternionf();
            int rayCount = Mth.floor((deathTime + deathTime * deathTime) / 2.0f * 60.0f);
            for (int i = 0; i < rayCount; ++i) {
                rayRotation.rotationXYZ(random.nextFloat() * ((float)Math.PI * 2), random.nextFloat() * ((float)Math.PI * 2), random.nextFloat() * ((float)Math.PI * 2)).rotateXYZ(random.nextFloat() * ((float)Math.PI * 2), random.nextFloat() * ((float)Math.PI * 2), random.nextFloat() * ((float)Math.PI * 2) + deathTime * 1.5707964f);
                pose.rotate((Quaternionfc)rayRotation);
                float length = random.nextFloat() * 20.0f + 5.0f + overDrive * 10.0f;
                float width = random.nextFloat() * 2.0f + 1.0f + overDrive * 2.0f;
                outerLeft.set(-HALF_SQRT_3 * width, length, -0.5f * width);
                outerRight.set(HALF_SQRT_3 * width, length, -0.5f * width);
                outerBottom.set(0.0f, length, width);
                buffer.addVertex(pose, (Vector3fc)origin).setColor(innerColor);
                buffer.addVertex(pose, (Vector3fc)outerLeft).setColor(0xFF00FF);
                buffer.addVertex(pose, (Vector3fc)outerRight).setColor(0xFF00FF);
                buffer.addVertex(pose, (Vector3fc)origin).setColor(innerColor);
                buffer.addVertex(pose, (Vector3fc)outerRight).setColor(0xFF00FF);
                buffer.addVertex(pose, (Vector3fc)outerBottom).setColor(0xFF00FF);
                buffer.addVertex(pose, (Vector3fc)origin).setColor(innerColor);
                buffer.addVertex(pose, (Vector3fc)outerBottom).setColor(0xFF00FF);
                buffer.addVertex(pose, (Vector3fc)outerLeft).setColor(0xFF00FF);
            }
        });
    }

    public static void submitCrystalBeams(float deltaX, float deltaY, float deltaZ, float timeInTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        float horizontalLength = Mth.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        poseStack.pushPose();
        poseStack.translate(0.0f, 2.0f, 0.0f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotation((float)(-Math.atan2(deltaZ, deltaX)) - 1.5707964f));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotation((float)(-Math.atan2(horizontalLength, deltaY)) - 1.5707964f));
        float v0 = 0.0f - timeInTicks * 0.01f;
        float v1 = length / 32.0f - timeInTicks * 0.01f;
        submitNodeCollector.submitCustomGeometry(poseStack, BEAM, (pose, buffer) -> {
            int steps = 8;
            float lastSin = 0.0f;
            float lastCos = 0.75f;
            float lastU = 0.0f;
            for (int i = 1; i <= 8; ++i) {
                float sin = Mth.sin((float)i * ((float)Math.PI * 2) / 8.0f) * 0.75f;
                float cos = Mth.cos((float)i * ((float)Math.PI * 2) / 8.0f) * 0.75f;
                float u = (float)i / 8.0f;
                buffer.addVertex(pose, lastSin * 0.2f, lastCos * 0.2f, 0.0f).setColor(-16777216).setUv(lastU, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0f, -1.0f, 0.0f);
                buffer.addVertex(pose, lastSin, lastCos, length).setColor(-1).setUv(lastU, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0f, -1.0f, 0.0f);
                buffer.addVertex(pose, sin, cos, length).setColor(-1).setUv(u, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0f, -1.0f, 0.0f);
                buffer.addVertex(pose, sin * 0.2f, cos * 0.2f, 0.0f).setColor(-16777216).setUv(u, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0.0f, -1.0f, 0.0f);
                lastSin = sin;
                lastCos = cos;
                lastU = u;
            }
        });
        poseStack.popPose();
    }

    @Override
    public EnderDragonRenderState createRenderState() {
        return new EnderDragonRenderState();
    }

    @Override
    public void extractRenderState(EnderDragon entity, EnderDragonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flapTime = Mth.lerp(partialTicks, entity.oFlapTime, entity.flapTime);
        state.deathTime = entity.dragonDeathTime > 0 ? (float)entity.dragonDeathTime + partialTicks : 0.0f;
        state.hasRedOverlay = entity.hurtTime > 0;
        EndCrystal nearestCrystal = entity.nearestCrystal;
        if (nearestCrystal != null) {
            Vec3 crystalPosition = nearestCrystal.getPosition(partialTicks).add(0.0, EndCrystalRenderer.getY((float)nearestCrystal.time + partialTicks), 0.0);
            state.beamOffset = crystalPosition.subtract(entity.getPosition(partialTicks));
        } else {
            state.beamOffset = null;
        }
        DragonPhaseInstance phase = entity.getPhaseManager().getCurrentPhase();
        state.isLandingOrTakingOff = phase == EnderDragonPhase.LANDING || phase == EnderDragonPhase.TAKEOFF;
        state.isSitting = phase.isSitting();
        BlockPos egg = entity.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(entity.getFightOrigin()));
        state.distanceToEgg = egg.distToCenterSqr(entity.position());
        state.partialTicks = entity.isDeadOrDying() ? 0.0f : partialTicks;
        state.flightHistory.copyFrom(entity.flightHistory);
    }

    @Override
    protected boolean affectedByCulling(EnderDragon entity) {
        return false;
    }
}

