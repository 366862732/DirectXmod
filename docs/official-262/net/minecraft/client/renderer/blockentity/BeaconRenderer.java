/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BeaconRenderer<T extends BlockEntity>
implements BlockEntityRenderer<T, BeaconRenderState> {
    public static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png");
    public static final int MAX_RENDER_Y = 2048;
    private static final float BEAM_SCALE_THRESHOLD = 96.0f;
    public static final float SOLID_BEAM_RADIUS = 0.2f;
    public static final float BEAM_GLOW_RADIUS = 0.25f;

    @Override
    public BeaconRenderState createRenderState() {
        return new BeaconRenderState();
    }

    @Override
    public void extractRenderState(T blockEntity, BeaconRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BeaconRenderer.extract(blockEntity, state, partialTicks, cameraPosition);
    }

    public static <T extends BlockEntity> void extract(T blockEntity, BeaconRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.animationTime = blockEntity.getLevel() != null ? (float)Math.floorMod(blockEntity.getLevel().getGameTime(), 40) + partialTicks : 0.0f;
        state.sections = ((BeaconBeamOwner)((Object)blockEntity)).getBeamSections().stream().map(section -> new BeaconRenderState.Section(section.getColor(), section.getHeight())).toList();
        float distanceToBeacon = (float)cameraPosition.subtract(Vec3.atCenterOf(state.blockPos)).horizontalDistance();
        LocalPlayer player = Minecraft.getInstance().player;
        state.beamRadiusScale = player != null && player.isScoping() ? 1.0f : Math.max(1.0f, distanceToBeacon / 96.0f);
    }

    @Override
    public void submit(BeaconRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        int beamStart = 0;
        for (int i = 0; i < state.sections.size(); ++i) {
            BeaconRenderState.Section beamSection = state.sections.get(i);
            BeaconRenderer.submitBeaconBeam(poseStack, submitNodeCollector, state.beamRadiusScale, state.animationTime, beamStart, i == state.sections.size() - 1 ? 2048 : beamSection.height(), beamSection.color());
            beamStart += beamSection.height();
        }
    }

    private static void submitBeaconBeam(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float beamRadiusScale, float animationTime, int beamStart, int height, int color) {
        BeaconRenderer.submitBeaconBeam(poseStack, submitNodeCollector, BEAM_LOCATION, 1.0f, animationTime, beamStart, height, color, 0.2f * beamRadiusScale, 0.25f * beamRadiusScale);
    }

    public static void submitBeaconBeam(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Identifier beamLocation, float scale, float animationTime, int beamStart, int height, int color, float solidBeamRadius, float beamGlowRadius) {
        int beamEnd = beamStart + height;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        float scroll = height < 0 ? animationTime : -animationTime;
        float texVOff = Mth.frac(scroll * 0.2f - (float)Mth.floor(scroll * 0.1f));
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(animationTime * 2.25f - 45.0f));
        float wnx = 0.0f;
        float wnz = solidBeamRadius;
        float enx = solidBeamRadius;
        float enz = 0.0f;
        float wsx = -solidBeamRadius;
        float wsz = 0.0f;
        float esx = 0.0f;
        float esz = -solidBeamRadius;
        float uu1 = 0.0f;
        float uu2 = 1.0f;
        float vv2 = -1.0f + texVOff;
        float vv1 = (float)height * scale * (0.5f / solidBeamRadius) + vv2;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(beamLocation, false), (pose, buffer) -> BeaconRenderer.renderPart(pose, buffer, color, beamStart, beamEnd, 0.0f, wnz, enx, 0.0f, wsx, 0.0f, 0.0f, esz, 0.0f, 1.0f, vv1, vv2));
        poseStack.popPose();
        wnx = -beamGlowRadius;
        wnz = -beamGlowRadius;
        enx = beamGlowRadius;
        enz = -beamGlowRadius;
        wsx = -beamGlowRadius;
        wsz = beamGlowRadius;
        esx = beamGlowRadius;
        esz = beamGlowRadius;
        uu1 = 0.0f;
        uu2 = 1.0f;
        vv2 = -1.0f + texVOff;
        vv1 = (float)height * scale + vv2;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(beamLocation, true), (pose, buffer) -> BeaconRenderer.renderPart(pose, buffer, ARGB.color(32, color), beamStart, beamEnd, wnx, wnz, enx, enz, wsx, wsz, esx, esz, 0.0f, 1.0f, vv1, vv2));
        poseStack.popPose();
    }

    private static void renderPart(PoseStack.Pose pose, VertexConsumer builder, int color, int beamStart, int beamEnd, float wnx, float wnz, float enx, float enz, float wsx, float wsz, float esx, float esz, float uu1, float uu2, float vv1, float vv2) {
        BeaconRenderer.renderQuad(pose, builder, color, beamStart, beamEnd, wnx, wnz, enx, enz, uu1, uu2, vv1, vv2);
        BeaconRenderer.renderQuad(pose, builder, color, beamStart, beamEnd, esx, esz, wsx, wsz, uu1, uu2, vv1, vv2);
        BeaconRenderer.renderQuad(pose, builder, color, beamStart, beamEnd, enx, enz, esx, esz, uu1, uu2, vv1, vv2);
        BeaconRenderer.renderQuad(pose, builder, color, beamStart, beamEnd, wsx, wsz, wnx, wnz, uu1, uu2, vv1, vv2);
    }

    private static void renderQuad(PoseStack.Pose pose, VertexConsumer builder, int color, int beamStart, int beamEnd, float wnx, float wnz, float enx, float enz, float uu1, float uu2, float vv1, float vv2) {
        BeaconRenderer.addVertex(pose, builder, color, beamEnd, wnx, wnz, uu2, vv1);
        BeaconRenderer.addVertex(pose, builder, color, beamStart, wnx, wnz, uu2, vv2);
        BeaconRenderer.addVertex(pose, builder, color, beamStart, enx, enz, uu1, vv2);
        BeaconRenderer.addVertex(pose, builder, color, beamEnd, enx, enz, uu1, vv1);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer builder, int color, int y, float x, float z, float u, float v) {
        builder.addVertex(pose, x, (float)y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        return Vec3.atCenterOf(((BlockEntity)blockEntity).getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(cameraPosition.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }
}

