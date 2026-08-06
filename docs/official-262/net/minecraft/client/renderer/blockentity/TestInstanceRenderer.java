/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityWithBoundingBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.blockentity.state.TestInstanceRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TestInstanceRenderer
implements BlockEntityRenderer<TestInstanceBlockEntity, TestInstanceRenderState> {
    private static final float ERROR_PADDING = 0.02f;
    private final BeaconRenderer<TestInstanceBlockEntity> beacon = new BeaconRenderer();
    private final BlockEntityWithBoundingBoxRenderer<TestInstanceBlockEntity> box = new BlockEntityWithBoundingBoxRenderer();

    @Override
    public TestInstanceRenderState createRenderState() {
        return new TestInstanceRenderState();
    }

    @Override
    public void extractRenderState(TestInstanceBlockEntity blockEntity, TestInstanceRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.beaconRenderState = new BeaconRenderState();
        BlockEntityRenderState.extractBase(blockEntity, state.beaconRenderState, breakProgress);
        BeaconRenderer.extract(blockEntity, state.beaconRenderState, partialTicks, cameraPosition);
        state.blockEntityWithBoundingBoxRenderState = new BlockEntityWithBoundingBoxRenderState();
        BlockEntityRenderState.extractBase(blockEntity, state.blockEntityWithBoundingBoxRenderState, breakProgress);
        BlockEntityWithBoundingBoxRenderer.extract(blockEntity, state.blockEntityWithBoundingBoxRenderState);
        state.errorMarkers.clear();
        for (TestInstanceBlockEntity.ErrorMarker marker : blockEntity.getErrorMarkers()) {
            state.errorMarkers.add(new TestInstanceBlockEntity.ErrorMarker(marker.pos(), marker.text()));
        }
    }

    @Override
    public void submit(TestInstanceRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.beacon.submit(state.beaconRenderState, poseStack, submitNodeCollector, camera);
        this.box.submit(state.blockEntityWithBoundingBoxRenderState, poseStack, submitNodeCollector, camera);
        for (TestInstanceBlockEntity.ErrorMarker error : state.errorMarkers) {
            this.submitErrorMarker(error);
        }
    }

    private void submitErrorMarker(TestInstanceBlockEntity.ErrorMarker error) {
        BlockPos pos = error.pos();
        Gizmos.cuboid(new AABB(pos).inflate(0.02f), GizmoStyle.fill(ARGB.colorFromFloat(0.375f, 1.0f, 0.0f, 0.0f)));
        String text = error.text().getString();
        float scale = 0.16f;
        Gizmos.billboardText(text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16f)).setAlwaysOnTop();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return this.beacon.shouldRenderOffScreen() || this.box.shouldRenderOffScreen();
    }

    @Override
    public int getViewDistance() {
        return Math.max(this.beacon.getViewDistance(), this.box.getViewDistance());
    }

    @Override
    public boolean shouldRender(TestInstanceBlockEntity blockEntity, Vec3 cameraPosition) {
        return this.beacon.shouldRender(blockEntity, cameraPosition) || this.box.shouldRender(blockEntity, cameraPosition);
    }
}

