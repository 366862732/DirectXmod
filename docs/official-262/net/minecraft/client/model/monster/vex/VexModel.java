/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.vex;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

@Environment(value=EnvType.CLIENT)
public class VexModel
extends EntityModel<VexRenderState>
implements ArmedModel<VexRenderState> {
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart head;

    public VexModel(ModelPart root) {
        super(root.getChild("root"), RenderTypes::entityTranslucent);
        this.body = this.root.getChild("body");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightWing = this.body.getChild("right_wing");
        this.leftWing = this.body.getChild("left_wing");
        this.head = this.root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0f, -2.5f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -5.0f, -2.5f, 5.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 20.0f, 0.0f));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-1.5f, 0.0f, -1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(0, 16).addBox(-1.5f, 1.0f, -1.0f, 3.0f, 5.0f, 2.0f, new CubeDeformation(-0.2f)), PartPose.offset(0.0f, 20.0f, 0.0f));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(23, 0).addBox(-1.25f, -0.5f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.1f)), PartPose.offset(-1.75f, 0.25f, 0.0f));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(23, 6).addBox(-0.75f, -0.5f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.1f)), PartPose.offset(1.75f, 0.25f, 0.0f));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(16, 14).mirror().addBox(0.0f, 0.0f, 0.0f, 0.0f, 5.0f, 8.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offset(0.5f, 1.0f, 1.0f));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(16, 14).addBox(0.0f, 0.0f, 0.0f, 0.0f, 5.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(-0.5f, 1.0f, 1.0f));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(VexRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        float movingArmZBob = Mth.cos(state.ageInTicks * 5.5f * ((float)Math.PI / 180)) * 0.1f;
        this.rightArm.zRot = 0.62831855f + movingArmZBob;
        this.leftArm.zRot = -(0.62831855f + movingArmZBob);
        if (state.isCharging) {
            this.body.xRot = 0.0f;
            this.setArmsCharging(!state.rightHandItemState.isEmpty(), !state.leftHandItemState.isEmpty(), movingArmZBob);
        } else {
            this.body.xRot = 0.15707964f;
        }
        this.leftWing.yRot = 1.0995574f + Mth.cos(state.ageInTicks * 45.836624f * ((float)Math.PI / 180)) * ((float)Math.PI / 180) * 16.2f;
        this.rightWing.yRot = -this.leftWing.yRot;
        this.leftWing.xRot = 0.47123888f;
        this.leftWing.zRot = -0.47123888f;
        this.rightWing.xRot = 0.47123888f;
        this.rightWing.zRot = 0.47123888f;
    }

    private void setArmsCharging(boolean hasItemInRightHand, boolean hasItemInLeftHand, float movingArmZBob) {
        if (!hasItemInRightHand && !hasItemInLeftHand) {
            this.rightArm.xRot = -1.2217305f;
            this.rightArm.yRot = 0.2617994f;
            this.rightArm.zRot = -0.47123888f - movingArmZBob;
            this.leftArm.xRot = -1.2217305f;
            this.leftArm.yRot = -0.2617994f;
            this.leftArm.zRot = 0.47123888f + movingArmZBob;
            return;
        }
        if (hasItemInRightHand) {
            this.rightArm.xRot = 3.6651914f;
            this.rightArm.yRot = 0.2617994f;
            this.rightArm.zRot = -0.47123888f - movingArmZBob;
        }
        if (hasItemInLeftHand) {
            this.leftArm.xRot = 3.6651914f;
            this.leftArm.yRot = -0.2617994f;
            this.leftArm.zRot = 0.47123888f + movingArmZBob;
        }
    }

    @Override
    public void translateToHand(VexRenderState state, HumanoidArm arm, PoseStack poseStack) {
        boolean mainArm = arm == HumanoidArm.RIGHT;
        ModelPart activeArm = mainArm ? this.rightArm : this.leftArm;
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        activeArm.translateAndRotate(poseStack);
        poseStack.scale(0.55f, 0.55f, 0.55f);
        this.offsetStackPosition(poseStack, mainArm);
    }

    private void offsetStackPosition(PoseStack poseStack, boolean mainArm) {
        if (mainArm) {
            poseStack.translate(0.046875, -0.15625, 0.078125);
        } else {
            poseStack.translate(-0.046875, -0.15625, 0.078125);
        }
    }
}

