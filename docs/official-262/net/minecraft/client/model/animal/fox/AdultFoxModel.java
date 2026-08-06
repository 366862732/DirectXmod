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
import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class AdultFoxModel
extends FoxModel {
    private static final int LEG_SIZE = 6;
    private static final float HEAD_HEIGHT = 16.5f;
    private static final float LEG_POS = 17.5f;

    public AdultFoxModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(1, 5).addBox(-3.0f, -2.0f, -5.0f, 8.0f, 6.0f, 6.0f), PartPose.offset(-1.0f, 16.5f, -3.0f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(8, 1).addBox(-3.0f, -4.0f, -4.0f, 2.0f, 2.0f, 1.0f), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(15, 1).addBox(3.0f, -4.0f, -4.0f, 2.0f, 2.0f, 1.0f), PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(6, 18).addBox(-1.0f, 2.01f, -8.0f, 4.0f, 2.0f, 3.0f), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 15).addBox(-3.0f, 3.999f, -3.5f, 6.0f, 11.0f, 6.0f), PartPose.offsetAndRotation(0.0f, 16.0f, -6.0f, 1.5707964f, 0.0f, 0.0f));
        CubeDeformation fudge = new CubeDeformation(0.001f);
        CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(4, 24).addBox(2.0f, 0.5f, -1.0f, 2.0f, 6.0f, 2.0f, fudge);
        CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(13, 24).addBox(2.0f, 0.5f, -1.0f, 2.0f, 6.0f, 2.0f, fudge);
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-5.0f, 17.5f, 7.0f));
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(-1.0f, 17.5f, 7.0f));
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-5.0f, 17.5f, 0.0f));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(-1.0f, 17.5f, 0.0f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 0).addBox(2.0f, 0.0f, -1.0f, 4.0f, 9.0f, 5.0f), PartPose.offsetAndRotation(-4.0f, 15.0f, -1.0f, -0.05235988f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 48, 32);
    }

    @Override
    protected void setSittingPose(FoxRenderState state) {
        super.setSittingPose(state);
        this.body.xRot = 0.5235988f;
        this.body.y -= 7.0f;
        this.body.z += 3.0f;
        this.tail.xRot = 0.7853982f;
        this.head.y -= 6.5f;
        this.head.z += 2.75f;
        this.rightFrontLeg.xRot = -0.2617994f;
        this.leftFrontLeg.xRot = -0.2617994f;
        this.rightHindLeg.xRot = -1.3089969f;
        this.rightHindLeg.y += 4.0f;
        this.rightHindLeg.z -= 0.25f;
        this.leftHindLeg.xRot = -1.3089969f;
        this.leftHindLeg.y += 4.0f;
        this.leftHindLeg.z -= 0.25f;
        this.tail.z -= 1.0f;
    }

    @Override
    protected void setSleepingPose(FoxRenderState state) {
        super.setSleepingPose(state);
        this.body.zRot = -1.5707964f;
        this.body.y += 5.0f;
        this.tail.xRot = -2.6179938f;
        this.head.x += 2.0f;
        this.head.y += 2.99f;
        this.head.yRot = -2.0943952f;
        this.head.zRot = 0.0f;
    }

    @Override
    protected void setWalkingPose(FoxRenderState state) {
        super.setWalkingPose(state);
        float animationSpeed = state.walkAnimationSpeed;
        float animationPos = state.walkAnimationPos;
        this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
    }

    @Override
    protected void setCrouchingPose(FoxRenderState state) {
        super.setCrouchingPose(state);
        this.body.y += state.crouchAmount;
    }

    @Override
    protected void setPouncingPose(FoxRenderState state) {
        super.setPouncingPose(state);
        float crouch = state.crouchAmount / 2.0f;
        this.body.y -= crouch;
        this.head.y -= crouch;
    }
}

