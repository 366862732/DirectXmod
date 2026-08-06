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
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class AdultFelineModel<T extends FelineRenderState>
extends AbstractFelineModel<T> {
    private static final float XO = 0.0f;
    private static final float YO = 16.0f;
    private static final float ZO = -9.0f;
    protected static final float BACK_LEG_Y = 18.0f;
    protected static final float BACK_LEG_Z = 5.0f;
    protected static final float FRONT_LEG_Y = 14.1f;
    private static final float FRONT_LEG_Z = -5.0f;

    public AdultFelineModel(ModelPart root) {
        super(root);
    }

    public static MeshDefinition createBodyMesh(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation tail_g = new CubeDeformation(-0.02f);
        root.addOrReplaceChild("head", CubeListBuilder.create().addBox("main", -2.5f, -2.0f, -3.0f, 5.0f, 4.0f, 5.0f, g).addBox("nose", -1.5f, -0.001f, -4.0f, 3, 2, 2, g, 0, 24).addBox("ear1", -2.0f, -3.0f, 0.0f, 1, 1, 2, g, 0, 10).addBox("ear2", 1.0f, -3.0f, 0.0f, 1, 1, 2, g, 6, 10), PartPose.offset(0.0f, 15.0f, -9.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(20, 0).addBox(-2.0f, 3.0f, -8.0f, 4.0f, 16.0f, 6.0f, g), PartPose.offsetAndRotation(0.0f, 12.0f, -10.0f, 1.5707964f, 0.0f, 0.0f));
        root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 15).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 8.0f, 1.0f, g), PartPose.offsetAndRotation(0.0f, 15.0f, 8.0f, 0.9f, 0.0f, 0.0f));
        root.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(4, 15).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 8.0f, 1.0f, tail_g), PartPose.offset(0.0f, 20.0f, 14.0f));
        CubeListBuilder hindLeg = CubeListBuilder.create().texOffs(8, 13).addBox(-1.0f, 0.0f, 1.0f, 2.0f, 6.0f, 2.0f, g);
        root.addOrReplaceChild("left_hind_leg", hindLeg, PartPose.offset(1.1f, 18.0f, 5.0f));
        root.addOrReplaceChild("right_hind_leg", hindLeg, PartPose.offset(-1.1f, 18.0f, 5.0f));
        CubeListBuilder frontLeg = CubeListBuilder.create().texOffs(40, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 10.0f, 2.0f, g);
        root.addOrReplaceChild("left_front_leg", frontLeg, PartPose.offset(1.2f, 14.1f, -5.0f));
        root.addOrReplaceChild("right_front_leg", frontLeg, PartPose.offset(-1.2f, 14.1f, -5.0f));
        return mesh;
    }

    @Override
    public void setupAnim(T state) {
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
            this.body.xRot = 1.5707964f;
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
        }
        if (((FelineRenderState)state).isSitting) {
            this.body.xRot = 0.7853982f;
            this.body.y += -4.0f * ageScale;
            this.body.z += 5.0f * ageScale;
            this.head.y += -3.3f * ageScale;
            this.head.z += 1.0f * ageScale;
            this.tail1.y += 8.0f * ageScale;
            this.tail1.z += -2.0f * ageScale;
            this.tail2.y += 2.0f * ageScale;
            this.tail2.z += -0.8f * ageScale;
            this.tail1.xRot = 1.7278761f;
            this.tail2.xRot = 2.670354f;
            this.leftFrontLeg.xRot = -0.15707964f;
            this.leftFrontLeg.y += 2.0f * ageScale;
            this.leftFrontLeg.z -= 2.0f * ageScale;
            this.rightFrontLeg.xRot = -0.15707964f;
            this.rightFrontLeg.y += 2.0f * ageScale;
            this.rightFrontLeg.z -= 2.0f * ageScale;
            this.leftHindLeg.xRot = -1.5707964f;
            this.leftHindLeg.y += 3.0f * ageScale;
            this.leftHindLeg.z -= 4.0f * ageScale;
            this.rightHindLeg.xRot = -1.5707964f;
            this.rightHindLeg.y += 3.0f * ageScale;
            this.rightHindLeg.z -= 4.0f * ageScale;
        }
        if (((FelineRenderState)state).lieDownAmount > 0.0f) {
            this.head.zRot = Mth.rotLerp(((FelineRenderState)state).lieDownAmount, this.head.zRot, -1.2707963f);
            this.head.yRot = Mth.rotLerp(((FelineRenderState)state).lieDownAmount, this.head.yRot, 1.2707963f);
            this.leftFrontLeg.xRot = -1.2707963f;
            this.rightFrontLeg.xRot = -0.47079635f;
            this.rightFrontLeg.zRot = -0.2f;
            this.rightFrontLeg.x += ageScale;
            this.leftHindLeg.xRot = -0.4f;
            this.rightHindLeg.xRot = 0.5f;
            this.rightHindLeg.zRot = -0.5f;
            this.rightHindLeg.x += 0.8f * ageScale;
            this.rightHindLeg.y += 2.0f * ageScale;
            this.tail1.xRot = Mth.rotLerp(((FelineRenderState)state).lieDownAmountTail, this.tail1.xRot, 0.8f);
            this.tail2.xRot = Mth.rotLerp(((FelineRenderState)state).lieDownAmountTail, this.tail2.xRot, -0.4f);
        }
        if (((FelineRenderState)state).relaxStateOneAmount > 0.0f) {
            this.head.xRot = Mth.rotLerp(((FelineRenderState)state).relaxStateOneAmount, this.head.xRot, -0.58177644f);
        }
    }
}

