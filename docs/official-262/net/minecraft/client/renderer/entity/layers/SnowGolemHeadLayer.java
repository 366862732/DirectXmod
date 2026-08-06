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
import net.minecraft.client.model.animal.golem.SnowGolemModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class SnowGolemHeadLayer
extends RenderLayer<SnowGolemRenderState, SnowGolemModel> {
    public SnowGolemHeadLayer(RenderLayerParent<SnowGolemRenderState, SnowGolemModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, SnowGolemRenderState state, float yRot, float xRot) {
        if (state.headBlock.isEmpty()) {
            return;
        }
        if (state.isInvisible && !state.appearsGlowing()) {
            return;
        }
        poseStack.pushPose();
        ((SnowGolemModel)this.getParentModel()).getHead().translateAndRotate(poseStack);
        float s = 0.625f;
        poseStack.translate(0.0f, -0.34375f, 0.0f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(0.625f, -0.625f, -0.625f);
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0f);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        if (state.appearsGlowing() && state.isInvisible) {
            state.headBlock.submitOnlyOutline(poseStack, submitNodeCollector, lightCoords, overlayCoords, state.outlineColor);
        } else {
            state.headBlock.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, state.outlineColor);
        }
        poseStack.popPose();
    }
}

