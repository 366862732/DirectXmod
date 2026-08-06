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
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class WitchItemLayer
extends CrossedArmsItemLayer<WitchRenderState, WitchModel> {
    public WitchItemLayer(RenderLayerParent<WitchRenderState, WitchModel> renderer) {
        super(renderer);
    }

    @Override
    protected void applyTranslation(WitchRenderState state, PoseStack poseStack) {
        if (state.isHoldingPotion) {
            ((WitchModel)this.getParentModel()).root().translateAndRotate(poseStack);
            ((WitchModel)this.getParentModel()).translateToHead(poseStack);
            ((WitchModel)this.getParentModel()).getNose().translateAndRotate(poseStack);
            poseStack.translate(0.0625f, 0.25f, 0.0f);
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(180.0f));
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(140.0f));
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(10.0f));
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(180.0f));
            return;
        }
        super.applyTranslation(state, poseStack);
    }
}

