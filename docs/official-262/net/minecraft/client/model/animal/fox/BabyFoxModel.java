/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.fox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.FoxBabyAnimation;
import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FoxRenderState;

@Environment(value=EnvType.CLIENT)
public class BabyFoxModel
extends FoxModel {
    private static final float MAX_WALK_ANIMATION_SPEED = 1.0f;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5f;
    private final KeyframeAnimation babyWalkAnimation;

    public BabyFoxModel(ModelPart root) {
        super(root);
        this.babyWalkAnimation = FoxBabyAnimation.FOX_BABY_WALK.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition root = meshdefinition.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -2.125f, -5.125f, 6.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)).texOffs(18, 20).addBox(-1.0f, 0.875f, -7.125f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(22, 8).addBox(-3.0f, -4.125f, -4.125f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(22, 11).addBox(1.0f, -4.125f, -4.125f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 18.125f, 0.125f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(22, 4).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-1.5f, 22.0f, 4.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(1.5f, 22.0f, 4.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(22, 4).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-1.5f, 22.0f, 0.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(1.5f, 22.0f, 0.0f));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-2.5f, -2.0f, -3.0f, 5.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 20.0f, 2.0f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5f, -1.48f, -1.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, -0.5f, 3.0f));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    protected void setSittingPose(FoxRenderState state) {
        super.setSittingPose(state);
        this.body.xRot = -0.959931f;
        this.body.z -= 4.5f * state.ageScale;
        this.body.y += 3.0f * state.ageScale;
        this.tail.y -= 0.6f;
        this.tail.z -= 2.0f * state.ageScale;
        this.tail.xRot = 0.95993114f;
        this.head.y -= 0.75f;
        this.head.z += 0.0f;
        this.rightFrontLeg.xRot = -0.2617994f;
        this.leftFrontLeg.xRot = -0.2617994f;
        this.rightFrontLeg.z -= 1.5f;
        this.leftFrontLeg.z -= 1.5f;
        this.rightFrontLeg.x += 0.01f;
        this.leftFrontLeg.x -= 0.01f;
        this.rightHindLeg.z -= 3.75f;
        this.leftHindLeg.z -= 3.75f;
        this.rightHindLeg.x += 0.01f;
        this.leftHindLeg.x -= 0.01f;
    }

    @Override
    protected void setSleepingPose(FoxRenderState state) {
        super.setSleepingPose(state);
        this.body.zRot = -1.5707964f;
        this.body.xRot = -0.17453292f;
        this.body.y += 1.5f;
        this.body.z -= 1.5f;
        this.body.x -= 1.5f;
        this.tail.xRot = -2.6179938f;
        this.tail.xRot = -2.1816616f;
        this.tail.x -= 0.7f;
        this.tail.z += 0.6f;
        this.tail.y += 0.9f;
        this.head.x -= 2.0f;
        this.head.y += 2.8f;
        this.head.z -= 4.0f;
        this.head.yRot = -2.0943952f;
        this.head.zRot = 0.0f;
    }

    @Override
    protected void setWalkingPose(FoxRenderState state) {
        super.setWalkingPose(state);
        this.babyWalkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 1.0f, 2.5f);
    }

    @Override
    protected void setCrouchingPose(FoxRenderState state) {
        super.setCrouchingPose(state);
        this.body.y += state.crouchAmount / 6.0f;
    }
}

