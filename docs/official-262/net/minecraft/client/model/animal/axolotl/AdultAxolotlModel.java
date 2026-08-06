/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.axolotl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class AdultAxolotlModel
extends EntityModel<AxolotlRenderState> {
    private static final float SWIMMING_LEG_XROT = 1.8849558f;
    private final ModelPart tail;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart topGills;
    private final ModelPart leftGills;
    private final ModelPart rightGills;

    public AdultAxolotlModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightHindLeg = this.body.getChild("right_hind_leg");
        this.leftHindLeg = this.body.getChild("left_hind_leg");
        this.rightFrontLeg = this.body.getChild("right_front_leg");
        this.leftFrontLeg = this.body.getChild("left_front_leg");
        this.tail = this.body.getChild("tail");
        this.topGills = this.head.getChild("top_gills");
        this.leftGills = this.head.getChild("left_gills");
        this.rightGills = this.head.getChild("right_gills");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 11).addBox(-4.0f, -2.0f, -9.0f, 8.0f, 4.0f, 10.0f).texOffs(2, 17).addBox(0.0f, -3.0f, -8.0f, 0.0f, 5.0f, 9.0f), PartPose.offset(0.0f, 19.5f, 5.0f));
        CubeDeformation fudge = new CubeDeformation(0.001f);
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 1).addBox(-4.0f, -3.0f, -5.0f, 8.0f, 5.0f, 5.0f, fudge), PartPose.offset(0.0f, 0.0f, -9.0f));
        CubeListBuilder topGills = CubeListBuilder.create().texOffs(3, 37).addBox(-4.0f, -3.0f, 0.0f, 8.0f, 3.0f, 0.0f, fudge);
        CubeListBuilder leftGills = CubeListBuilder.create().texOffs(0, 40).addBox(-3.0f, -5.0f, 0.0f, 3.0f, 7.0f, 0.0f, fudge);
        CubeListBuilder rightGills = CubeListBuilder.create().texOffs(11, 40).addBox(0.0f, -5.0f, 0.0f, 3.0f, 7.0f, 0.0f, fudge);
        head.addOrReplaceChild("top_gills", topGills, PartPose.offset(0.0f, -3.0f, -1.0f));
        head.addOrReplaceChild("left_gills", leftGills, PartPose.offset(-4.0f, 0.0f, -1.0f));
        head.addOrReplaceChild("right_gills", rightGills, PartPose.offset(4.0f, 0.0f, -1.0f));
        CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(2, 13).addBox(-1.0f, 0.0f, 0.0f, 3.0f, 5.0f, 0.0f, fudge);
        CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(2, 13).addBox(-2.0f, 0.0f, 0.0f, 3.0f, 5.0f, 0.0f, fudge);
        body.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-3.5f, 1.0f, -1.0f));
        body.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(3.5f, 1.0f, -1.0f));
        body.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-3.5f, 1.0f, -8.0f));
        body.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(3.5f, 1.0f, -8.0f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(2, 19).addBox(0.0f, -3.0f, 0.0f, 0.0f, 5.0f, 12.0f), PartPose.offset(0.0f, 0.0f, 1.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AxolotlRenderState state) {
        super.setupAnim(state);
        float playingDeadFactor = state.playingDeadFactor;
        float inWaterFactor = state.inWaterFactor;
        float onGroundFactor = state.onGroundFactor;
        float movingFactor = state.movingFactor;
        float notMovingFactor = 1.0f - movingFactor;
        float mirroredLegsFactor = 1.0f - Math.min(onGroundFactor, movingFactor);
        this.body.yRot += state.yRot * ((float)Math.PI / 180);
        this.setupSwimmingAnimation(state.ageInTicks, state.xRot, Math.min(movingFactor, inWaterFactor));
        this.setupWaterHoveringAnimation(state.ageInTicks, Math.min(notMovingFactor, inWaterFactor));
        this.setupGroundCrawlingAnimation(state.ageInTicks, Math.min(movingFactor, onGroundFactor));
        this.setupLayStillOnGroundAnimation(state.ageInTicks, Math.min(notMovingFactor, onGroundFactor));
        this.setupPlayDeadAnimation(playingDeadFactor);
        this.applyMirrorLegRotations(mirroredLegsFactor);
    }

    private void setupLayStillOnGroundAnimation(float ageInTicks, float factor) {
        if (factor <= 1.0E-5f) {
            return;
        }
        float animMoveSpeed = ageInTicks * 0.09f;
        float sineSway = Mth.sin(animMoveSpeed);
        float cosineSway = Mth.cos(animMoveSpeed);
        float movement = sineSway * sineSway - 2.0f * sineSway;
        float movement2 = cosineSway * cosineSway - 3.0f * sineSway;
        this.head.xRot += -0.09f * movement * factor;
        this.head.zRot += -0.2f * factor;
        this.tail.yRot += (-0.1f + 0.1f * movement) * factor;
        float gillAngle = (0.6f + 0.05f * movement2) * factor;
        this.topGills.xRot += gillAngle;
        this.leftGills.yRot -= gillAngle;
        this.rightGills.yRot += gillAngle;
        this.leftHindLeg.xRot += 1.1f * factor;
        this.leftHindLeg.yRot += 1.0f * factor;
        this.leftFrontLeg.xRot += 0.8f * factor;
        this.leftFrontLeg.yRot += 2.3f * factor;
        this.leftFrontLeg.zRot -= 0.5f * factor;
    }

    private void setupGroundCrawlingAnimation(float ageInTicks, float factor) {
        if (factor <= 1.0E-5f) {
            return;
        }
        float animMoveSpeed = ageInTicks * 0.11f;
        float cosineSway = Mth.cos(animMoveSpeed);
        float hindLegYRotSway = (cosineSway * cosineSway - 2.0f * cosineSway) / 5.0f;
        float frontLegYRotSway = 0.7f * cosineSway;
        float headAndTailYRot = 0.09f * cosineSway * factor;
        this.head.yRot += headAndTailYRot;
        this.tail.yRot += headAndTailYRot;
        float gillAngle = (0.6f - 0.08f * (cosineSway * cosineSway + 2.0f * Mth.sin(animMoveSpeed))) * factor;
        this.topGills.xRot += gillAngle;
        this.leftGills.yRot -= gillAngle;
        this.rightGills.yRot += gillAngle;
        float hindLegXRot = 0.9424779f * factor;
        float frontLegXRot = 1.0995574f * factor;
        this.leftHindLeg.xRot += hindLegXRot;
        this.leftHindLeg.yRot += (1.5f - hindLegYRotSway) * factor;
        this.leftHindLeg.zRot += -0.1f * factor;
        this.leftFrontLeg.xRot += frontLegXRot;
        this.leftFrontLeg.yRot += (1.5707964f - frontLegYRotSway) * factor;
        this.rightHindLeg.xRot += hindLegXRot;
        this.rightHindLeg.yRot += (-1.0f - hindLegYRotSway) * factor;
        this.rightFrontLeg.xRot += frontLegXRot;
        this.rightFrontLeg.yRot += (-1.5707964f - frontLegYRotSway) * factor;
    }

    private void setupWaterHoveringAnimation(float ageInTicks, float factor) {
        if (factor <= 1.0E-5f) {
            return;
        }
        float animMoveSpeed = ageInTicks * 0.075f;
        float cosineSway = Mth.cos(animMoveSpeed);
        float sineSway = Mth.sin(animMoveSpeed) * 0.15f;
        float bodyXRot = (-0.15f + 0.075f * cosineSway) * factor;
        this.body.xRot += bodyXRot;
        this.body.y -= sineSway * factor;
        this.head.xRot -= bodyXRot;
        this.topGills.xRot += 0.2f * cosineSway * factor;
        float gillYRot = (-0.3f * cosineSway - 0.19f) * factor;
        this.leftGills.yRot += gillYRot;
        this.rightGills.yRot -= gillYRot;
        this.leftHindLeg.xRot += (2.3561945f - cosineSway * 0.11f) * factor;
        this.leftHindLeg.yRot += 0.47123894f * factor;
        this.leftHindLeg.zRot += 1.7278761f * factor;
        this.leftFrontLeg.xRot += (0.7853982f - cosineSway * 0.2f) * factor;
        this.leftFrontLeg.yRot += 2.042035f * factor;
        this.tail.yRot += 0.5f * cosineSway * factor;
    }

    private void setupSwimmingAnimation(float ageInTicks, float xRot, float factor) {
        if (factor <= 1.0E-5f) {
            return;
        }
        float animMoveSpeed = ageInTicks * 0.33f;
        float sineSway = Mth.sin(animMoveSpeed);
        float cosineSway = Mth.cos(animMoveSpeed);
        float bodySway = 0.13f * sineSway;
        this.body.xRot += (xRot * ((float)Math.PI / 180) + bodySway) * factor;
        this.head.xRot -= bodySway * 1.8f * factor;
        this.body.y -= 0.45f * cosineSway * factor;
        this.topGills.xRot += (-0.5f * sineSway - 0.8f) * factor;
        float gillYRot = (0.3f * sineSway + 0.9f) * factor;
        this.leftGills.yRot += gillYRot;
        this.rightGills.yRot -= gillYRot;
        this.tail.yRot += 0.3f * Mth.cos(animMoveSpeed * 0.9f) * factor;
        this.leftHindLeg.xRot += 1.8849558f * factor;
        this.leftHindLeg.yRot += -0.4f * sineSway * factor;
        this.leftHindLeg.zRot += 1.5707964f * factor;
        this.leftFrontLeg.xRot += 1.8849558f * factor;
        this.leftFrontLeg.yRot += (-0.2f * cosineSway - 0.1f) * factor;
        this.leftFrontLeg.zRot += 1.5707964f * factor;
    }

    private void setupPlayDeadAnimation(float factor) {
        if (factor <= 1.0E-5f) {
            return;
        }
        this.leftHindLeg.xRot += 1.4137167f * factor;
        this.leftHindLeg.yRot += 1.0995574f * factor;
        this.leftHindLeg.zRot += 0.7853982f * factor;
        this.leftFrontLeg.xRot += 0.7853982f * factor;
        this.leftFrontLeg.yRot += 2.042035f * factor;
        this.body.xRot += -0.15f * factor;
        this.body.zRot += 0.35f * factor;
    }

    private void applyMirrorLegRotations(float factor) {
        if (factor <= 1.0E-5f) {
            return;
        }
        this.rightHindLeg.xRot += this.leftHindLeg.xRot * factor;
        ModelPart modelPart = this.rightHindLeg;
        modelPart.yRot = modelPart.yRot + -this.leftHindLeg.yRot * factor;
        modelPart = this.rightHindLeg;
        modelPart.zRot = modelPart.zRot + -this.leftHindLeg.zRot * factor;
        this.rightFrontLeg.xRot += this.leftFrontLeg.xRot * factor;
        modelPart = this.rightFrontLeg;
        modelPart.yRot = modelPart.yRot + -this.leftFrontLeg.yRot * factor;
        modelPart = this.rightFrontLeg;
        modelPart.zRot = modelPart.zRot + -this.leftFrontLeg.zRot * factor;
    }
}

