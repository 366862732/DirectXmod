/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.equine;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class BabyDonkeyModel
extends DonkeyModel {
    public BabyDonkeyModel(ModelPart root) {
        ModelPart body = root.getChild("body");
        ModelPart rightHindLeg = body.getChild("right_hind_leg");
        ModelPart leftHindLeg = body.getChild("left_hind_leg");
        ModelPart rightFrontLeg = body.getChild("right_front_leg");
        ModelPart leftFrontLeg = body.getChild("left_front_leg");
        ModelPart headParts = body.getChild("head_parts");
        ModelPart tail = body.getChild("tail");
        super(root, headParts, rightHindLeg, rightFrontLeg, leftHindLeg, leftFrontLeg, tail);
    }

    public static LayerDefinition createBabyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13).addBox(-5.0f, -3.0f, -7.0f, 8.0f, 6.0f, 14.0f, new CubeDeformation(0.0f)), PartPose.offset(1.0f, 14.0f, 0.0f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0f, -1.5f, 6.5f));
        tail.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(24, 33).addBox(-2.5f, -1.0f, -0.5f, 3.0f, 3.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.7418f, 0.0f, 0.0f));
        body.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 44).addBox(-2.5f, -1.5f, -1.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offset(2.25f, 3.5f, 5.25f));
        body.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 44).addBox(-2.5f, -1.5f, -1.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offset(-2.4f, 3.5f, 5.4f));
        body.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 33).addBox(-2.5f, -1.5f, -1.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offset(2.4f, 3.5f, -5.3f));
        body.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 33).addBox(-2.5f, -1.5f, -1.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offset(-2.4f, 3.5f, -5.4f));
        PartDefinition neck = body.addOrReplaceChild("head_parts", CubeListBuilder.create(), PartPose.offset(0.0f, -3.0f, -5.0f));
        neck.addOrReplaceChild("neck_r1", CubeListBuilder.create().texOffs(30, 9).addBox(-3.0f, -6.0f, -3.0f, 4.0f, 8.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.3927f, 0.0f, 0.0f));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0f, -5.0f, -3.0f));
        head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -3.6f, -8.4f, 6.0f, 4.0f, 9.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, -1.0f, 1.0f, 0.3927f, 0.0f, 0.0f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -6.5f, -0.3f, 2.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(2.0f, -3.5f, -1.0f, 0.48f, 0.0f, 0.48f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(22, 0).mirror().addBox(-2.0f, -6.5f, -0.3f, 2.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation(-2.0f, -3.5f, -1.0f, 0.48f, 0.0f, -0.48f));
        body.addOrReplaceChild("right_chest", CubeListBuilder.create(), PartPose.offset(-1.0f, 10.0f, 0.0f));
        body.addOrReplaceChild("left_chest", CubeListBuilder.create(), PartPose.offset(-1.0f, 10.0f, 0.0f));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(DonkeyRenderState state) {
        super.setupAnim(state);
        state.xRot = -30.0f;
        float headRotXRad = state.xRot * ((float)Math.PI / 180);
        float eating = state.eatAnimation;
        float standing = state.standAnimation;
        float feedingAnim = state.feedingAnimation;
        float baseHeadAngle = (1.0f - Math.max(standing, eating)) * (0.5235988f + headRotXRad + feedingAnim * Mth.sin(state.ageInTicks) * 0.05f);
        this.headParts.xRot = standing * (0.2617994f + headRotXRad) + eating * (1.5707964f + Mth.sin(state.ageInTicks) * 0.05f) + baseHeadAngle;
    }

    @Override
    protected void offsetLegPositionWhenStanding(float standing) {
        this.leftHindLeg.y = Mth.lerp(standing, this.leftHindLeg.y, -0.3f);
        this.rightHindLeg.y = Mth.lerp(standing, this.leftHindLeg.y, -0.3f);
    }

    @Override
    protected float getLegStandAngle() {
        return 1.0471976f;
    }

    @Override
    protected float getLegStandingYOffset() {
        return 1.0f;
    }

    @Override
    protected float getLegStandingZOffset() {
        return 0.5f;
    }

    @Override
    protected float getLegStandingXRotOffset() {
        return 0.0f;
    }

    @Override
    protected float getTailXRotOffset() {
        return -0.7853982f;
    }

    @Override
    protected void animateHeadPartsPlacement(float eating, float standing) {
        this.headParts.y = Mth.lerp(eating, this.headParts.y, -1.2f);
        this.headParts.z = Mth.lerp(standing, this.headParts.z, -3.6f);
    }
}

