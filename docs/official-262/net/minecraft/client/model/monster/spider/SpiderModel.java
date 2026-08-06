/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.spider;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class SpiderModel
extends EntityModel<LivingEntityRenderState> {
    private static final String BODY_0 = "body0";
    private static final String BODY_1 = "body1";
    private static final String RIGHT_MIDDLE_FRONT_LEG = "right_middle_front_leg";
    private static final String LEFT_MIDDLE_FRONT_LEG = "left_middle_front_leg";
    private static final String RIGHT_MIDDLE_HIND_LEG = "right_middle_hind_leg";
    private static final String LEFT_MIDDLE_HIND_LEG = "left_middle_hind_leg";
    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightMiddleHindLeg;
    private final ModelPart leftMiddleHindLeg;
    private final ModelPart rightMiddleFrontLeg;
    private final ModelPart leftMiddleFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public SpiderModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightMiddleHindLeg = root.getChild(RIGHT_MIDDLE_HIND_LEG);
        this.leftMiddleHindLeg = root.getChild(LEFT_MIDDLE_HIND_LEG);
        this.rightMiddleFrontLeg = root.getChild(RIGHT_MIDDLE_FRONT_LEG);
        this.leftMiddleFrontLeg = root.getChild(LEFT_MIDDLE_FRONT_LEG);
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createSpiderBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int yo = 15;
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 4).addBox(-4.0f, -4.0f, -8.0f, 8.0f, 8.0f, 8.0f), PartPose.offset(0.0f, 15.0f, -3.0f));
        root.addOrReplaceChild(BODY_0, CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f), PartPose.offset(0.0f, 15.0f, 0.0f));
        root.addOrReplaceChild(BODY_1, CubeListBuilder.create().texOffs(0, 12).addBox(-5.0f, -4.0f, -6.0f, 10.0f, 8.0f, 12.0f), PartPose.offset(0.0f, 15.0f, 9.0f));
        CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(18, 0).addBox(-15.0f, -1.0f, -1.0f, 16.0f, 2.0f, 2.0f);
        CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(18, 0).mirror().addBox(-1.0f, -1.0f, -1.0f, 16.0f, 2.0f, 2.0f);
        float legZRot = 0.7853982f;
        float legYRotSpan = 0.3926991f;
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offsetAndRotation(-4.0f, 15.0f, 2.0f, 0.0f, 0.7853982f, -0.7853982f));
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offsetAndRotation(4.0f, 15.0f, 2.0f, 0.0f, -0.7853982f, 0.7853982f));
        root.addOrReplaceChild(RIGHT_MIDDLE_HIND_LEG, rightLeg, PartPose.offsetAndRotation(-4.0f, 15.0f, 1.0f, 0.0f, 0.3926991f, -0.58119464f));
        root.addOrReplaceChild(LEFT_MIDDLE_HIND_LEG, leftLeg, PartPose.offsetAndRotation(4.0f, 15.0f, 1.0f, 0.0f, -0.3926991f, 0.58119464f));
        root.addOrReplaceChild(RIGHT_MIDDLE_FRONT_LEG, rightLeg, PartPose.offsetAndRotation(-4.0f, 15.0f, 0.0f, 0.0f, -0.3926991f, -0.58119464f));
        root.addOrReplaceChild(LEFT_MIDDLE_FRONT_LEG, leftLeg, PartPose.offsetAndRotation(4.0f, 15.0f, 0.0f, 0.0f, 0.3926991f, 0.58119464f));
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offsetAndRotation(-4.0f, 15.0f, -1.0f, 0.0f, -0.7853982f, -0.7853982f));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offsetAndRotation(4.0f, 15.0f, -1.0f, 0.0f, 0.7853982f, 0.7853982f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        float animationPos = state.walkAnimationPos * 0.6662f;
        float animationSpeed = state.walkAnimationSpeed;
        float swingHind = -(Mth.cos(animationPos * 2.0f + 0.0f) * 0.4f) * animationSpeed;
        float swingMiddleHind = -(Mth.cos(animationPos * 2.0f + (float)Math.PI) * 0.4f) * animationSpeed;
        float swingMiddleFront = -(Mth.cos(animationPos * 2.0f + 1.5707964f) * 0.4f) * animationSpeed;
        float swingFront = -(Mth.cos(animationPos * 2.0f + 4.712389f) * 0.4f) * animationSpeed;
        float stepHind = Math.abs(Mth.sin(animationPos + 0.0f) * 0.4f) * animationSpeed;
        float stepMiddleHind = Math.abs(Mth.sin(animationPos + (float)Math.PI) * 0.4f) * animationSpeed;
        float stepMiddleFrontHind = Math.abs(Mth.sin(animationPos + 1.5707964f) * 0.4f) * animationSpeed;
        float stepFront = Math.abs(Mth.sin(animationPos + 4.712389f) * 0.4f) * animationSpeed;
        this.rightHindLeg.yRot += swingHind;
        this.leftHindLeg.yRot -= swingHind;
        this.rightMiddleHindLeg.yRot += swingMiddleHind;
        this.leftMiddleHindLeg.yRot -= swingMiddleHind;
        this.rightMiddleFrontLeg.yRot += swingMiddleFront;
        this.leftMiddleFrontLeg.yRot -= swingMiddleFront;
        this.rightFrontLeg.yRot += swingFront;
        this.leftFrontLeg.yRot -= swingFront;
        this.rightHindLeg.zRot += stepHind;
        this.leftHindLeg.zRot -= stepHind;
        this.rightMiddleHindLeg.zRot += stepMiddleHind;
        this.leftMiddleHindLeg.zRot -= stepMiddleHind;
        this.rightMiddleFrontLeg.zRot += stepMiddleFrontHind;
        this.leftMiddleFrontLeg.zRot -= stepMiddleFrontHind;
        this.rightFrontLeg.zRot += stepFront;
        this.leftFrontLeg.zRot -= stepFront;
    }
}

