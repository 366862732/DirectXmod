/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.armorstand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.HumanoidArm;

@Environment(value=EnvType.CLIENT)
public class ArmorStandModel
extends ArmorStandArmorModel {
    private static final String RIGHT_BODY_STICK = "right_body_stick";
    private static final String LEFT_BODY_STICK = "left_body_stick";
    private static final String SHOULDER_STICK = "shoulder_stick";
    private static final String BASE_PLATE = "base_plate";
    private final ModelPart rightBodyStick;
    private final ModelPart leftBodyStick;
    private final ModelPart shoulderStick;
    private final ModelPart basePlate;

    public ArmorStandModel(ModelPart root) {
        super(root);
        this.rightBodyStick = root.getChild(RIGHT_BODY_STICK);
        this.leftBodyStick = root.getChild(LEFT_BODY_STICK);
        this.shoulderStick = root.getChild(SHOULDER_STICK);
        this.basePlate = root.getChild(BASE_PLATE);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f), PartPose.offset(0.0f, 1.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 26).addBox(-6.0f, 0.0f, -1.5f, 12.0f, 3.0f, 3.0f), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(-5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(0.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f), PartPose.offset(-1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f), PartPose.offset(1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild(RIGHT_BODY_STICK, CubeListBuilder.create().texOffs(16, 0).addBox(-3.0f, 3.0f, -1.0f, 2.0f, 7.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild(LEFT_BODY_STICK, CubeListBuilder.create().texOffs(48, 16).addBox(1.0f, 3.0f, -1.0f, 2.0f, 7.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild(SHOULDER_STICK, CubeListBuilder.create().texOffs(0, 48).addBox(-4.0f, 10.0f, -1.0f, 8.0f, 2.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild(BASE_PLATE, CubeListBuilder.create().texOffs(0, 32).addBox(-6.0f, 11.0f, -6.0f, 12.0f, 1.0f, 12.0f), PartPose.offset(0.0f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ArmorStandRenderState state) {
        super.setupAnim(state);
        this.basePlate.yRot = (float)Math.PI / 180 * -state.yRot;
        this.leftArm.visible = state.showArms;
        this.rightArm.visible = state.showArms;
        this.basePlate.visible = state.showBasePlate;
        this.rightBodyStick.xRot = (float)Math.PI / 180 * state.bodyPose.x();
        this.rightBodyStick.yRot = (float)Math.PI / 180 * state.bodyPose.y();
        this.rightBodyStick.zRot = (float)Math.PI / 180 * state.bodyPose.z();
        this.leftBodyStick.xRot = (float)Math.PI / 180 * state.bodyPose.x();
        this.leftBodyStick.yRot = (float)Math.PI / 180 * state.bodyPose.y();
        this.leftBodyStick.zRot = (float)Math.PI / 180 * state.bodyPose.z();
        this.shoulderStick.xRot = (float)Math.PI / 180 * state.bodyPose.x();
        this.shoulderStick.yRot = (float)Math.PI / 180 * state.bodyPose.y();
        this.shoulderStick.zRot = (float)Math.PI / 180 * state.bodyPose.z();
    }

    @Override
    public void translateToHand(ArmorStandRenderState state, HumanoidArm arm, PoseStack poseStack) {
        ModelPart modelPart = this.getArm(arm);
        boolean handVisible = modelPart.visible;
        modelPart.visible = true;
        super.translateToHand(state, arm, poseStack);
        modelPart.visible = handVisible;
    }
}

