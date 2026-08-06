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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TrialSpawnerRenderer
implements BlockEntityRenderer<TrialSpawnerBlockEntity, SpawnerRenderState> {
    private final EntityRenderDispatcher entityRenderer;

    public TrialSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    @Override
    public void extractRenderState(TrialSpawnerBlockEntity blockEntity, SpawnerRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (blockEntity.getLevel() == null) {
            return;
        }
        TrialSpawner spawner = blockEntity.getTrialSpawner();
        TrialSpawnerStateData data = spawner.getStateData();
        Entity displayEntity = data.getOrCreateDisplayEntity(spawner, blockEntity.getLevel(), spawner.getState());
        TrialSpawnerRenderer.extractSpawnerData(state, partialTicks, displayEntity, this.entityRenderer, data.getOSpin(), data.getSpin());
    }

    public static void extractSpawnerData(SpawnerRenderState state, float partialTicks, @Nullable Entity displayEntity, EntityRenderDispatcher entityRenderer, double oSpin, double spin) {
        if (displayEntity == null) {
            return;
        }
        state.displayEntity = entityRenderer.extractEntity(displayEntity, partialTicks);
        state.displayEntity.lightCoords = state.lightCoords;
        state.spin = (float)Mth.lerp((double)partialTicks, oSpin, spin) * 10.0f;
        state.scale = 0.53125f;
        float maxLength = Math.max(displayEntity.getBbWidth(), displayEntity.getBbHeight());
        if ((double)maxLength > 1.0) {
            state.scale /= maxLength;
        }
    }

    @Override
    public void submit(SpawnerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.displayEntity != null) {
            SpawnerRenderer.submitEntityInSpawner(poseStack, submitNodeCollector, state.displayEntity, this.entityRenderer, state.spin, state.scale, camera);
        }
    }
}

