/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.skeleton;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

@Environment(value=EnvType.CLIENT)
public class SkeletonModel<S extends SkeletonRenderState>
extends HumanoidModel<S> {
    public SkeletonModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        SkeletonModel.createDefaultSkeletonMesh(root);
        return LayerDefinition.create(mesh, 64, 32);
    }

    protected static void createDefaultSkeletonMesh(PartDefinition root) {
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(-5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(-2.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(2.0f, 12.0f, 0.0f));
    }

    public static LayerDefinition createSingleModelDualBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition root = meshdefinition.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f).texOffs(28, 0).addBox(-4.0f, 10.0f, -2.0f, 8.0f, 1.0f, 4.0f).texOffs(16, 48).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.025f)), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f).texOffs(0, 32).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.2f)), PartPose.offset(0.0f, 0.0f, 0.0f)).addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f).texOffs(42, 33).addBox(-1.55f, -2.025f, -1.5f, 3.0f, 12.0f, 3.0f), PartPose.offset(-5.5f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 16).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f).texOffs(40, 48).addBox(-1.45f, -2.025f, -1.5f, 3.0f, 12.0f, 3.0f), PartPose.offset(5.5f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 12.0f, 2.0f).texOffs(0, 49).addBox(-1.5f, -0.0f, -1.5f, 3.0f, 12.0f, 3.0f), PartPose.offset(-2.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 12.0f, 2.0f).texOffs(4, 49).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 12.0f, 3.0f), PartPose.offset(2.0f, 12.0f, 0.0f));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        if (((SkeletonRenderState)state).isAggressive && !((SkeletonRenderState)state).isHoldingBow) {
            float attackTime = ((SkeletonRenderState)state).attackTime;
            float attack2 = Mth.sin(attackTime * (float)Math.PI);
            float attack = Mth.sin((1.0f - (1.0f - attackTime) * (1.0f - attackTime)) * (float)Math.PI);
            this.rightArm.zRot = 0.0f;
            this.leftArm.zRot = 0.0f;
            this.rightArm.yRot = -(0.1f - attack2 * 0.6f);
            this.leftArm.yRot = 0.1f - attack2 * 0.6f;
            this.rightArm.xRot = -1.5707964f;
            this.leftArm.xRot = -1.5707964f;
            this.rightArm.xRot -= attack2 * 1.2f - attack * 0.4f;
            this.leftArm.xRot -= attack2 * 1.2f - attack * 0.4f;
            AnimationUtils.bobArms(this.rightArm, this.leftArm, ((SkeletonRenderState)state).ageInTicks);
        }
    }

    @Override
    public void translateToHand(SkeletonRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        float offset = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        ModelPart part = this.getArm(arm);
        part.x += offset;
        part.translateAndRotate(poseStack);
        part.x -= offset;
    }
}

