/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.hoglin;

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
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class HoglinModel
extends EntityModel<HoglinRenderState> {
    private static final float DEFAULT_HEAD_X_ROT = 0.87266463f;
    private static final float ATTACK_HEAD_X_ROT_END = -0.34906584f;
    protected final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public HoglinModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(1, 1).addBox(-8.0f, -7.0f, -13.0f, 16.0f, 14.0f, 26.0f), PartPose.offset(0.0f, 7.0f, 0.0f));
        body.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(90, 33).addBox(0.0f, 0.0f, -9.0f, 0.0f, 10.0f, 19.0f, new CubeDeformation(0.001f)), PartPose.offset(0.0f, -14.0f, -7.0f));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(61, 1).addBox(-7.0f, -3.0f, -19.0f, 14.0f, 6.0f, 19.0f), PartPose.offsetAndRotation(0.0f, 2.0f, -12.0f, 0.87266463f, 0.0f, 0.0f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(1, 1).addBox(-6.0f, -1.0f, -2.0f, 6.0f, 1.0f, 4.0f), PartPose.offsetAndRotation(-6.0f, -2.0f, -3.0f, 0.0f, 0.0f, -0.6981317f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(1, 6).addBox(0.0f, -1.0f, -2.0f, 6.0f, 1.0f, 4.0f), PartPose.offsetAndRotation(6.0f, -2.0f, -3.0f, 0.0f, 0.0f, 0.6981317f));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(10, 13).addBox(-1.0f, -11.0f, -1.0f, 2.0f, 11.0f, 2.0f), PartPose.offset(-7.0f, 2.0f, -12.0f));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(1, 13).addBox(-1.0f, -11.0f, -1.0f, 2.0f, 11.0f, 2.0f), PartPose.offset(7.0f, 2.0f, -12.0f));
        int frontLegHeight = 14;
        int backLegHeight = 11;
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(66, 42).addBox(-3.0f, 0.0f, -3.0f, 6.0f, 14.0f, 6.0f), PartPose.offset(-4.0f, 10.0f, -8.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(41, 42).addBox(-3.0f, 0.0f, -3.0f, 6.0f, 14.0f, 6.0f), PartPose.offset(4.0f, 10.0f, -8.5f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(21, 45).addBox(-2.5f, 0.0f, -2.5f, 5.0f, 11.0f, 5.0f), PartPose.offset(-5.0f, 13.0f, 10.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 45).addBox(-2.5f, 0.0f, -2.5f, 5.0f, 11.0f, 5.0f), PartPose.offset(5.0f, 13.0f, 10.0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(HoglinRenderState state) {
        super.setupAnim(state);
        float animationSpeed = state.walkAnimationSpeed;
        float animationPos = state.walkAnimationPos;
        this.rightEar.zRot = -0.6981317f - animationSpeed * Mth.sin(animationPos);
        this.leftEar.zRot = 0.6981317f + animationSpeed * Mth.sin(animationPos);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        float headbuttLerpFactor = 1.0f - (float)Mth.abs(10 - 2 * state.attackAnimationRemainingTicks) / 10.0f;
        this.animateHeadbutt(headbuttLerpFactor);
        float amplitudeMultiplier = 1.2f;
        this.rightFrontLeg.xRot = Mth.cos(animationPos) * 1.2f * animationSpeed;
        this.rightHindLeg.xRot = this.leftFrontLeg.xRot = Mth.cos(animationPos + (float)Math.PI) * 1.2f * animationSpeed;
        this.leftHindLeg.xRot = this.rightFrontLeg.xRot;
    }

    protected void animateHeadbutt(float headbuttLerpFactor) {
        this.head.xRot = Mth.lerp(headbuttLerpFactor, 0.87266463f, -0.34906584f);
    }
}

