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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class PlayerItemInHandLayer<S extends AvatarRenderState, M extends EntityModel<S> & HeadedModel>
extends ItemInHandLayer<S, M> {
    private static final float X_ROT_MIN = -0.5235988f;
    private static final float X_ROT_MAX = 1.5707964f;

    public PlayerItemInHandLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    protected void submitArmWithItem(S state, ItemStackRenderState item, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        InteractionHand currentHand;
        if (item.isEmpty()) {
            return;
        }
        InteractionHand interactionHand = currentHand = arm == ((AvatarRenderState)state).mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (((AvatarRenderState)state).isUsingItem && ((AvatarRenderState)state).useItemHand == currentHand && ((AvatarRenderState)state).attackTime < 1.0E-5f && !((AvatarRenderState)state).heldOnHead.isEmpty()) {
            this.renderItemHeldToEye(state, arm, poseStack, submitNodeCollector, lightCoords);
        } else {
            super.submitArmWithItem(state, item, itemStack, arm, poseStack, submitNodeCollector, lightCoords);
        }
    }

    private void renderItemHeldToEye(S state, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        poseStack.pushPose();
        ((Model)this.getParentModel()).root().translateAndRotate(poseStack);
        ModelPart head = ((HeadedModel)this.getParentModel()).getHead();
        float previousXRot = head.xRot;
        head.xRot = Mth.clamp(head.xRot, -0.5235988f, 1.5707964f);
        head.translateAndRotate(poseStack);
        head.xRot = previousXRot;
        CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
        boolean isLeftHand = arm == HumanoidArm.LEFT;
        poseStack.translate((isLeftHand ? -2.5f : 2.5f) / 16.0f, -0.0625f, 0.0f);
        ((AvatarRenderState)state).heldOnHead.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, ((AvatarRenderState)state).outlineColor);
        poseStack.popPose();
    }
}

