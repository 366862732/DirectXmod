/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.feline;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class BabyFelineModel<S extends FelineRenderState>
extends AbstractFelineModel<S> {
    protected BabyFelineModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBabyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -3.0f, -2.875f, 5.0f, 4.0f, 4.0f).texOffs(18, 0).addBox(-2.0f, -4.0f, -0.875f, 1.0f, 1.0f, 2.0f).texOffs(24, 0).addBox(1.0f, -4.0f, -0.875f, 1.0f, 1.0f, 2.0f).texOffs(18, 3).addBox(-1.5f, -1.0f, -3.875f, 3.0f, 2.0f, 1.0f), PartPose.offset(0.0f, 20.0f, -3.125f));
        partdefinition.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(18, 18).addBox(-0.5f, 0.0f, -1.0f, 1.0f, 2.0f, 2.0f), PartPose.offset(1.0f, 22.0f, -1.5f));
        partdefinition.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(12, 18).addBox(-0.5f, 0.0f, -1.0f, 1.0f, 2.0f, 2.0f), PartPose.offset(-1.0f, 22.0f, -1.5f));
        partdefinition.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(18, 22).addBox(-0.5f, 0.0f, -1.0f, 1.0f, 2.0f, 2.0f), PartPose.offset(1.0f, 22.0f, 2.5f));
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0f, -1.5f, -3.5f, 4.0f, 3.0f, 7.0f), PartPose.offset(0.0f, 20.5f, 0.5f));
        partdefinition.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(12, 22).addBox(-0.5f, 0.0f, -1.0f, 1.0f, 2.0f, 2.0f), PartPose.offset(-1.0f, 22.0f, 2.5f));
        partdefinition.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 18).addBox(-0.5f, -0.107f, 0.0849f, 1.0f, 1.0f, 5.0f), PartPose.offsetAndRotation(0.0f, 19.107f, 3.9151f, -0.567232f, 0.0f, 0.0f));
        partdefinition.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        float ageScale = ((FelineRenderState)state).ageScale;
        if (((FelineRenderState)state).isCrouching) {
            this.body.y += 1.0f * ageScale;
            this.head.y += 2.0f * ageScale;
            this.tail1.y += 1.0f * ageScale;
            this.tail2.y += -4.0f * ageScale;
            this.tail2.z += 2.0f * ageScale;
            this.tail1.xRot = 1.5707964f;
            this.tail2.xRot = 1.5707964f;
        } else if (((FelineRenderState)state).isSprinting) {
            this.tail2.y = this.tail1.y;
            this.tail2.z += 2.0f * ageScale;
            this.tail1.xRot = 1.5707964f;
            this.tail2.xRot = 1.5707964f;
        }
        this.head.xRot = ((FelineRenderState)state).xRot * ((float)Math.PI / 180);
        this.head.yRot = ((FelineRenderState)state).yRot * ((float)Math.PI / 180);
        if (!((FelineRenderState)state).isSitting) {
            float animationSpeed = ((FelineRenderState)state).walkAnimationSpeed;
            float animationPos = ((FelineRenderState)state).walkAnimationPos;
            if (((FelineRenderState)state).isSprinting) {
                this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * animationSpeed;
                this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f + 0.3f) * animationSpeed;
                this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI + 0.3f) * animationSpeed;
                this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * animationSpeed;
                this.tail2.xRot = 1.7278761f + 0.31415927f * Mth.cos(animationPos) * animationSpeed;
            } else {
                this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * animationSpeed;
                this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * animationSpeed;
                this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * animationSpeed;
                this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f) * animationSpeed;
                this.tail2.xRot = !((FelineRenderState)state).isCrouching ? 1.7278761f + 0.7853982f * Mth.cos(animationPos) * animationSpeed : 1.7278761f + 0.47123894f * Mth.cos(animationPos) * animationSpeed;
            }
        } else if (((FelineRenderState)state).isSitting) {
            this.body.xRot += -0.43633232f;
            this.body.y += 1.25f;
            this.head.z += 0.75f;
            this.tail1.xRot += 0.5454154f;
            this.tail1.y += 4.0f;
            this.tail1.z -= 0.9f;
            this.leftHindLeg.z -= 0.9f;
            this.rightHindLeg.z -= 0.9f;
        }
        if (((FelineRenderState)state).lieDownAmount > 0.0f) {
            this.body.x += 1.0f;
            this.head.xRot = Mth.rotLerp(((FelineRenderState)state).lieDownAmount, this.head.xRot, 0.17453292f);
            this.head.zRot = Mth.rotLerp(((FelineRenderState)state).lieDownAmount, this.head.zRot, -1.3089969f);
            this.head.x += 1.5f;
            this.head.y += 0.75f;
            this.head.z -= 0.5f;
            this.rightFrontLeg.xRot = -0.7853982f;
            this.rightFrontLeg.x += 3.5f;
            this.rightFrontLeg.y -= 0.5f;
            this.rightFrontLeg.z += 0.0f;
            this.leftFrontLeg.xRot = -1.5707964f;
            this.leftFrontLeg.x += 1.5f;
            this.leftFrontLeg.y -= 1.0f;
            this.leftFrontLeg.z -= 2.0f;
            this.rightHindLeg.xRot = 0.6981317f;
            this.rightHindLeg.yRot = 0.34906584f;
            this.rightHindLeg.zRot = -0.34906584f;
            this.rightHindLeg.x += 2.5f;
            this.rightHindLeg.y -= 0.25f;
            this.rightHindLeg.z += 0.5f;
            this.leftHindLeg.x += 1.5f;
            this.leftHindLeg.z -= 1.0f;
            this.tail1.xRot += Mth.rotLerp(((FelineRenderState)state).lieDownAmountTail, this.tail1.xRot, -0.5235988f);
            this.tail1.yRot += Mth.rotLerp(((FelineRenderState)state).lieDownAmountTail, this.tail1.yRot, 0.0f);
            this.tail1.zRot += Mth.rotLerp(((FelineRenderState)state).lieDownAmountTail, this.tail1.zRot, -0.17453292f);
            this.tail1.x += 1.0f;
            this.tail1.y += 0.5f;
            this.tail1.z -= 0.25f;
        }
        if (((FelineRenderState)state).relaxStateOneAmount > 0.0f) {
            this.head.xRot = Mth.rotLerp(((FelineRenderState)state).relaxStateOneAmount, this.head.xRot, -0.58177644f);
        }
    }
}

