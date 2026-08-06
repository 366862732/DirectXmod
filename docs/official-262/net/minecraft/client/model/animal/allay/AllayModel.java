/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.model.animal.allay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class AllayModel
extends EntityModel<AllayRenderState>
implements ArmedModel<AllayRenderState> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart right_arm;
    private final ModelPart left_arm;
    private final ModelPart right_wing;
    private final ModelPart left_wing;
    private static final float FLYING_ANIMATION_X_ROT = 0.7853982f;
    private static final float MAX_HAND_HOLDING_ITEM_X_ROT_RAD = -1.134464f;
    private static final float MIN_HAND_HOLDING_ITEM_X_ROT_RAD = -1.0471976f;

    public AllayModel(ModelPart root) {
        super(root.getChild("root"), RenderTypes::entityTranslucent);
        this.head = this.root.getChild("head");
        this.body = this.root.getChild("body");
        this.right_arm = this.body.getChild("right_arm");
        this.left_arm = this.body.getChild("left_arm");
        this.right_wing = this.body.getChild("right_wing");
        this.left_wing = this.body.getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0f, 23.5f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -5.0f, -2.5f, 5.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -3.99f, 0.0f));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-1.5f, 0.0f, -1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(0, 16).addBox(-1.5f, 0.0f, -1.0f, 3.0f, 5.0f, 2.0f, new CubeDeformation(-0.2f)), PartPose.offset(0.0f, -4.0f, 0.0f));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(23, 0).addBox(-0.75f, -0.5f, -1.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(-0.01f)), PartPose.offset(-1.75f, 0.5f, 0.0f));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(23, 6).addBox(-0.25f, -0.5f, -1.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(-0.01f)), PartPose.offset(1.75f, 0.5f, 0.0f));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(16, 14).addBox(0.0f, 1.0f, 0.0f, 0.0f, 5.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(-0.5f, 0.0f, 0.6f));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(16, 14).addBox(0.0f, 1.0f, 0.0f, 0.0f, 5.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(0.5f, 0.0f, 0.6f));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(AllayRenderState state) {
        super.setupAnim(state);
        float animationSpeed = state.walkAnimationSpeed;
        float animationPos = state.walkAnimationPos;
        float flapSpeed = state.ageInTicks * 20.0f * ((float)Math.PI / 180) + animationPos;
        float flapAmount = Mth.cos(flapSpeed) * (float)Math.PI * 0.15f + animationSpeed;
        float idleBobSpeed = state.ageInTicks * 9.0f * ((float)Math.PI / 180);
        float flyingFactor = Math.min(animationSpeed / 0.3f, 1.0f);
        float idleBobFactor = 1.0f - flyingFactor;
        float holdingItemFactor = state.holdingAnimationProgress;
        if (state.isDancing) {
            float danceSpeed = state.ageInTicks * 8.0f * ((float)Math.PI / 180) + animationSpeed;
            float danceFrequency = Mth.cos(danceSpeed) * 16.0f * ((float)Math.PI / 180);
            float spinningRotation = state.spinningProgress;
            float headTiltZ = Mth.cos(danceSpeed) * 14.0f * ((float)Math.PI / 180);
            float headTiltY = Mth.cos(danceSpeed) * 30.0f * ((float)Math.PI / 180);
            this.root.yRot = state.isSpinning ? (float)Math.PI * 4 * spinningRotation : this.root.yRot;
            this.root.zRot = danceFrequency * (1.0f - spinningRotation);
            this.head.yRot = headTiltY * (1.0f - spinningRotation);
            this.head.zRot = headTiltZ * (1.0f - spinningRotation);
        } else {
            this.head.xRot = state.xRot * ((float)Math.PI / 180);
            this.head.yRot = state.yRot * ((float)Math.PI / 180);
        }
        this.right_wing.xRot = 0.43633232f * (1.0f - flyingFactor);
        this.right_wing.yRot = -0.7853982f + flapAmount;
        this.left_wing.xRot = 0.43633232f * (1.0f - flyingFactor);
        this.left_wing.yRot = 0.7853982f - flapAmount;
        this.body.xRot = flyingFactor * 0.7853982f;
        float armFlyingRotX = holdingItemFactor * Mth.lerp(flyingFactor, -1.0471976f, -1.134464f);
        this.root.y += (float)Math.cos(idleBobSpeed) * 0.25f * idleBobFactor;
        this.right_arm.xRot = armFlyingRotX;
        this.left_arm.xRot = armFlyingRotX;
        float armIdleBobFactor = idleBobFactor * (1.0f - holdingItemFactor);
        float armIdleBobAmount = 0.43633232f - Mth.cos(idleBobSpeed + 4.712389f) * (float)Math.PI * 0.075f * armIdleBobFactor;
        this.left_arm.zRot = -armIdleBobAmount;
        this.right_arm.zRot = armIdleBobAmount;
        this.right_arm.yRot = 0.27925268f * holdingItemFactor;
        this.left_arm.yRot = -0.27925268f * holdingItemFactor;
    }

    @Override
    public void translateToHand(AllayRenderState state, HumanoidArm arm, PoseStack poseStack) {
        float yOffset = 1.0f;
        float zOffset = 3.0f;
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        poseStack.translate(0.0f, 0.0625f, 0.1875f);
        poseStack.mulPose((Quaternionfc)Axis.XP.rotation(this.right_arm.xRot));
        poseStack.scale(0.7f, 0.7f, 0.7f);
        poseStack.translate(0.0625f, 0.0f, 0.0f);
    }
}

