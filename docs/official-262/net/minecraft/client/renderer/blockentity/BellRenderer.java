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
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BellRenderer
implements BlockEntityRenderer<BellBlockEntity, BellRenderState> {
    public static final SpriteId BELL_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");
    private final SpriteGetter sprites;
    private final BellModel model;

    public BellRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.model = new BellModel(context.bakeLayer(ModelLayers.BELL));
    }

    @Override
    public BellRenderState createRenderState() {
        return new BellRenderState();
    }

    @Override
    public void extractRenderState(BellBlockEntity blockEntity, BellRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.ticks = (float)blockEntity.ticks + partialTicks;
        state.shakeDirection = blockEntity.shaking ? blockEntity.clickDirection : null;
    }

    @Override
    public void submit(BellRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        BellModel.State modelState = new BellModel.State(state.ticks, state.shakeDirection);
        this.model.setupAnim(modelState);
        submitNodeCollector.submitModel(this.model, modelState, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, BELL_TEXTURE, this.sprites, 0, state.breakProgress);
    }
}

