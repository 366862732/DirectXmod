/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class MushroomCowMushroomLayer
extends RenderLayer<MushroomCowRenderState, CowModel> {
    public MushroomCowMushroomLayer(RenderLayerParent<MushroomCowRenderState, CowModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, MushroomCowRenderState state, float yRot, float xRot) {
        boolean appearsGlowingWithInvisibility;
        if (state.isBaby || state.mushroomModel.isEmpty()) {
            return;
        }
        boolean bl = appearsGlowingWithInvisibility = state.appearsGlowing() && state.isInvisible;
        if (state.isInvisible && !appearsGlowingWithInvisibility) {
            return;
        }
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0f);
        poseStack.pushPose();
        poseStack.translate(0.2f, -0.35f, 0.5f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-48.0f));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        this.submitMushroomBlock(poseStack, submitNodeCollector, lightCoords, appearsGlowingWithInvisibility, state.outlineColor, state.mushroomModel, overlayCoords);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.2f, -0.35f, 0.5f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(42.0f));
        poseStack.translate(0.1f, 0.0f, -0.6f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-48.0f));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        this.submitMushroomBlock(poseStack, submitNodeCollector, lightCoords, appearsGlowingWithInvisibility, state.outlineColor, state.mushroomModel, overlayCoords);
        poseStack.popPose();
        poseStack.pushPose();
        ((CowModel)this.getParentModel()).getHead().translateAndRotate(poseStack);
        poseStack.translate(0.0f, -0.7f, -0.2f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-78.0f));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        this.submitMushroomBlock(poseStack, submitNodeCollector, lightCoords, appearsGlowingWithInvisibility, state.outlineColor, state.mushroomModel, overlayCoords);
        poseStack.popPose();
    }

    private void submitMushroomBlock(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, boolean appearsGlowingWithInvisibility, int outlineColor, BlockModelRenderState mushroomModel, int overlayCoords) {
        if (appearsGlowingWithInvisibility) {
            mushroomModel.submitOnlyOutline(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
        } else {
            mushroomModel.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
        }
    }
}

