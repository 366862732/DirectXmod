/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.model.animal.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.CopperGolemAnimation;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class CopperGolemModel
extends EntityModel<CopperGolemRenderState>
implements ArmedModel<CopperGolemRenderState>,
HeadedModel {
    private static final float MAX_WALK_ANIMATION_SPEED = 2.0f;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5f;
    private static final float Z_FIGHT_MITIGATION = 0.015f;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation walkWithItemAnimation;
    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation interactionGetItem;
    private final KeyframeAnimation interactionGetNoItem;
    private final KeyframeAnimation interactionDropItem;
    private final KeyframeAnimation interactionDropNoItem;

    public CopperGolemModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.walkAnimation = CopperGolemAnimation.COPPER_GOLEM_WALK.bake(root);
        this.walkWithItemAnimation = CopperGolemAnimation.COPPER_GOLEM_WALK_ITEM.bake(root);
        this.idleAnimation = CopperGolemAnimation.COPPER_GOLEM_IDLE.bake(root);
        this.interactionGetItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_GET.bake(root);
        this.interactionGetNoItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_NOGET.bake(root);
        this.interactionDropItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_ITEM_DROP.bake(root);
        this.interactionDropNoItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_ITEM_NODROP.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0f, 24.0f, 0.0f));
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0f, -6.0f, -3.0f, 8.0f, 6.0f, 6.0f, CubeDeformation.NONE), PartPose.offset(0.0f, -5.0f, 0.0f));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -5.0f, -5.0f, 8.0f, 5.0f, 10.0f, new CubeDeformation(0.015f)).texOffs(56, 0).addBox(-1.0f, -2.0f, -6.0f, 2.0f, 3.0f, 2.0f, CubeDeformation.NONE).texOffs(37, 8).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.015f)).texOffs(37, 0).addBox(-2.0f, -13.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(-0.015f)), PartPose.offset(0.0f, -6.0f, 0.0f));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 16).addBox(-3.0f, -1.0f, -2.0f, 3.0f, 10.0f, 4.0f, CubeDeformation.NONE), PartPose.offset(-4.0f, -6.0f, 0.0f));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(50, 16).addBox(0.0f, -1.0f, -2.0f, 3.0f, 10.0f, 4.0f, CubeDeformation.NONE), PartPose.offset(4.0f, -6.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 27).addBox(-4.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f, CubeDeformation.NONE), PartPose.offset(0.0f, -5.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 27).addBox(0.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f, CubeDeformation.NONE), PartPose.offset(0.0f, -5.0f, 0.0f));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    public static LayerDefinition createRunningPoseBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0f, 0.0f, 0.0f));
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(-1.064f, -5.0f, 0.0f));
        body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(0, 15).addBox(-4.02f, -6.116f, -3.5f, 8.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(1.1f, 0.1f, 0.7f, 0.1204f, -0.0064f, -0.0779f));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -5.1f, -5.0f, 8.0f, 5.0f, 10.0f, new CubeDeformation(0.0f)).texOffs(56, 0).addBox(-1.02f, -2.1f, -6.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(37, 8).addBox(-1.02f, -9.1f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.015f)).texOffs(37, 0).addBox(-2.0f, -13.1f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(-0.015f)), PartPose.offset(0.7f, -5.6f, -1.8f));
        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0f, -6.0f, 0.0f));
        right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(36, 16).addBox(-3.052f, -1.11f, -2.036f, 3.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.7f, -0.248f, -1.62f, 1.0036f, 0.0f, 0.0f));
        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0f, -6.0f, 0.0f));
        left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(50, 16).addBox(0.032f, -1.1f, -2.0f, 3.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.732f, 0.0f, 0.0f, -0.8715f, -0.0535f, -0.0449f));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.064f, -5.0f, 0.0f));
        right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(0, 27).addBox(-1.856f, -0.1f, -1.09f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(1.048f, 0.0f, -0.9f, -0.8727f, 0.0f, 0.0f));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(0.936f, -5.0f, 0.0f));
        left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(16, 27).addBox(-2.088f, -0.1f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(1.0f, 0.0f, 0.0f, 0.7854f, 0.0f, 0.0f));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    public static LayerDefinition createSittingPoseBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0f, 0.0f, 0.0f));
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(3, 19).addBox(-3.0f, -4.0f, -4.525f, 6.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)).texOffs(0, 15).addBox(-4.0f, -3.0f, -3.525f, 8.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -3.0f, 2.325f));
        body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(3, 18).addBox(-4.0f, -3.0f, -2.2f, 8.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, -1.0f, -4.325f, 0.0f, 0.0f, -3.1416f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(37, 8).addBox(-1.0f, -7.0f, -3.3f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.015f)).texOffs(37, 0).addBox(-2.0f, -11.0f, -4.3f, 4.0f, 4.0f, 4.0f, new CubeDeformation(-0.015f)).texOffs(0, 0).addBox(-4.0f, -3.0f, -7.325f, 8.0f, 5.0f, 10.0f, new CubeDeformation(0.0f)).texOffs(56, 0).addBox(-1.0f, 0.0f, -8.325f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -6.0f, -0.2f));
        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0f, -5.6f, -1.8f, 0.4363f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(36, 16).addBox(-3.075f, -0.9733f, -1.9966f, 3.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0893f, 0.1198f, -1.0472f, 0.0f, 0.0f));
        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0f, -5.6f, -1.7f, 0.4363f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(50, 16).addBox(0.075f, -1.0443f, -1.8997f, 3.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, -0.0015f, -0.0808f, -1.0472f, 0.0f, 0.0f));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2.1f, -2.1f, -2.075f));
        right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(0, 27).addBox(-2.0f, 0.975f, 0.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.05f, -1.9f, 1.075f, -1.5708f, 0.0f, 0.0f));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.0f, -2.0f, -2.075f));
        left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(16, 27).addBox(-2.0f, 0.975f, 0.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.05f, -2.0f, 1.075f, -1.5708f, 0.0f, 0.0f));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    public static LayerDefinition createStarPoseBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0f, 0.0f, 0.0f));
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0f, -6.0f, -3.0f, 8.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -5.0f, 0.0f));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -5.0f, -5.0f, 8.0f, 5.0f, 10.0f, new CubeDeformation(0.0f)).texOffs(56, 0).addBox(-1.0f, -2.0f, -6.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(37, 8).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.015f)).texOffs(37, 0).addBox(-2.0f, -13.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(-0.015f)), PartPose.offset(0.0f, -6.0f, 0.0f));
        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0f, -6.0f, 0.0f));
        right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(36, 16).addBox(-1.5f, -5.0f, -2.0f, 3.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.9199f));
        right_arm.addOrReplaceChild("rightItem", CubeListBuilder.create(), PartPose.offset(-1.0f, 7.4f, -1.0f));
        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0f, -6.0f, 0.0f));
        left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(50, 16).addBox(-1.5f, -5.0f, -2.0f, 3.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(-1.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.9199f));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.0f, -5.0f, 0.0f));
        right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(0, 27).addBox(-2.0f, -2.5f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.35f, 2.0f, 0.01f, 0.0f, 0.0f, 0.2618f));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.0f, -5.0f, 0.0f));
        left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(16, 27).addBox(-2.0f, -2.5f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(1.65f, 2.0f, 0.0f, 0.0f, 0.0f, -0.2618f));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    public static LayerDefinition createEyesLayer() {
        return CopperGolemModel.createBodyLayer().apply(mesh -> {
            mesh.getRoot().retainPartsAndChildren(Set.of("eyes"));
            return mesh;
        });
    }

    @Override
    public void setupAnim(CopperGolemRenderState state) {
        super.setupAnim(state);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        if (state.rightHandItemState.isEmpty() && state.leftHandItemState.isEmpty()) {
            this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0f, 2.5f);
        } else {
            this.walkWithItemAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0f, 2.5f);
            this.poseHeldItemArmsIfStill();
        }
        this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
        this.interactionGetItem.apply(state.interactionGetItem, state.ageInTicks);
        this.interactionGetNoItem.apply(state.interactionGetNoItem, state.ageInTicks);
        this.interactionDropItem.apply(state.interactionDropItem, state.ageInTicks);
        this.interactionDropNoItem.apply(state.interactionDropNoItem, state.ageInTicks);
    }

    @Override
    public void translateToHand(CopperGolemRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        ModelPart activeArm = arm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;
        activeArm.translateAndRotate(poseStack);
        if (state.copperGolemState.equals(CopperGolemState.IDLE)) {
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(arm == HumanoidArm.RIGHT ? -90.0f : 90.0f));
            poseStack.translate(0.0f, 0.0f, 0.125f);
        } else {
            poseStack.scale(0.55f, 0.55f, 0.55f);
            poseStack.translate(-0.125f, 0.3125f, -0.1875f);
        }
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @Override
    public void translateToHead(PoseStack poseStack) {
        this.body.translateAndRotate(poseStack);
        this.head.translateAndRotate(poseStack);
        poseStack.translate(0.0f, 0.125f, 0.0f);
        poseStack.scale(1.0625f, 1.0625f, 1.0625f);
    }

    public void applyBlockOnAntennaTransform(PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        this.head.translateAndRotate(poseStack);
        poseStack.translate(0.0, -1.75, 0.0);
    }

    private void poseHeldItemArmsIfStill() {
        this.rightArm.xRot = Math.min(this.rightArm.xRot, -0.87266463f);
        this.leftArm.xRot = Math.min(this.leftArm.xRot, -0.87266463f);
        this.rightArm.yRot = Math.min(this.rightArm.yRot, -0.1134464f);
        this.leftArm.yRot = Math.max(this.leftArm.yRot, 0.1134464f);
        this.rightArm.zRot = Math.min(this.rightArm.zRot, -0.064577185f);
        this.leftArm.zRot = Math.max(this.leftArm.zRot, 0.064577185f);
    }
}

