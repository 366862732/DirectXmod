/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.parrot.Parrot;

@Environment(value=EnvType.CLIENT)
public class ParrotOnShoulderLayer
extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final ParrotModel model;

    public ParrotOnShoulderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new ParrotModel(modelSet.bakeLayer(ModelLayers.PARROT));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Parrot.Variant parrotOnRightShoulder;
        Parrot.Variant parrotOnLeftShoulder = state.parrotOnLeftShoulder;
        if (parrotOnLeftShoulder != null) {
            this.submitOnShoulder(poseStack, submitNodeCollector, lightCoords, state, parrotOnLeftShoulder, yRot, xRot, true);
        }
        if ((parrotOnRightShoulder = state.parrotOnRightShoulder) != null) {
            this.submitOnShoulder(poseStack, submitNodeCollector, lightCoords, state, parrotOnRightShoulder, yRot, xRot, false);
        }
    }

    private void submitOnShoulder(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState playerState, Parrot.Variant parrotVariant, float yRot, float xRot, boolean isLeft) {
        poseStack.pushPose();
        poseStack.translate(isLeft ? 0.4f : -0.4f, playerState.isCrouching ? -1.3f : -1.5f, 0.0f);
        ParrotRenderState parrotState = new ParrotRenderState();
        parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
        parrotState.ageInTicks = playerState.ageInTicks;
        parrotState.walkAnimationPos = playerState.walkAnimationPos;
        parrotState.walkAnimationSpeed = playerState.walkAnimationSpeed;
        parrotState.yRot = yRot;
        parrotState.xRot = xRot;
        submitNodeCollector.submitModel(this.model, parrotState, poseStack, ParrotRenderer.getVariantTexture(parrotVariant), lightCoords, OverlayTexture.NO_OVERLAY, playerState.outlineColor, null);
        poseStack.popPose();
    }
}

