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
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S>>
extends RenderLayer<S, M> {
    public ItemInHandLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        this.submitArmWithItem(state, ((ArmedEntityRenderState)state).rightHandItemState, ((ArmedEntityRenderState)state).rightHandItemStack, HumanoidArm.RIGHT, poseStack, submitNodeCollector, lightCoords);
        this.submitArmWithItem(state, ((ArmedEntityRenderState)state).leftHandItemState, ((ArmedEntityRenderState)state).leftHandItemStack, HumanoidArm.LEFT, poseStack, submitNodeCollector, lightCoords);
    }

    protected void submitArmWithItem(S state, ItemStackRenderState item, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        float ticksUsingItem;
        if (item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        ((ArmedModel)this.getParentModel()).translateToHand(state, arm, poseStack);
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f));
        boolean isLeftHand = arm == HumanoidArm.LEFT;
        float offsetX = this.useBabyOffset(state) ? 0.0f : 1.0f;
        float offsetY = this.useBabyOffset(state) ? 1.0f : 2.0f;
        float offsetZ = this.useBabyOffset(state) ? -4.5f : -10.0f;
        poseStack.translate((float)(isLeftHand ? -1 : 1) * offsetX / 16.0f, offsetY / 16.0f, offsetZ / 16.0f);
        if (((ArmedEntityRenderState)state).attackTime > 0.0f && ((ArmedEntityRenderState)state).attackArm == arm && ((ArmedEntityRenderState)state).swingAnimationType == SwingAnimationType.STAB) {
            SpearAnimations.thirdPersonAttackItem(state, poseStack);
        }
        if ((ticksUsingItem = ((ArmedEntityRenderState)state).ticksUsingItem(arm)) != 0.0f) {
            (arm == HumanoidArm.RIGHT ? ((ArmedEntityRenderState)state).rightArmPose : ((ArmedEntityRenderState)state).leftArmPose).animateUseItem(state, poseStack, ticksUsingItem, arm, itemStack);
        }
        item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, ((ArmedEntityRenderState)state).outlineColor);
        poseStack.popPose();
    }

    private boolean useBabyOffset(S state) {
        return ((ArmedEntityRenderState)state).isBaby && ((ArmedEntityRenderState)state).entityType != EntityTypes.ARMOR_STAND;
    }
}

