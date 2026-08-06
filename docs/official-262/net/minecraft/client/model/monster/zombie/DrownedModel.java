/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.zombie;

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
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class DrownedModel
extends ZombieModel<ZombieRenderState> {
    public DrownedModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, g), PartPose.offset(5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, g), PartPose.offset(1.9f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ZombieRenderState state) {
        float swimAmount;
        super.setupAnim(state);
        if (state.leftArmPose == HumanoidModel.ArmPose.THROW_TRIDENT) {
            this.leftArm.xRot = this.leftArm.xRot * 0.5f - (float)Math.PI;
            this.leftArm.yRot = 0.0f;
        }
        if (state.rightArmPose == HumanoidModel.ArmPose.THROW_TRIDENT) {
            this.rightArm.xRot = this.rightArm.xRot * 0.5f - (float)Math.PI;
            this.rightArm.yRot = 0.0f;
        }
        if ((swimAmount = state.swimAmount) > 0.0f) {
            this.rightArm.xRot = Mth.rotLerpRad(swimAmount, this.rightArm.xRot, -2.5132742f) + swimAmount * 0.35f * Mth.sin(0.1f * state.ageInTicks);
            this.leftArm.xRot = Mth.rotLerpRad(swimAmount, this.leftArm.xRot, -2.5132742f) - swimAmount * 0.35f * Mth.sin(0.1f * state.ageInTicks);
            this.rightArm.zRot = Mth.rotLerpRad(swimAmount, this.rightArm.zRot, -0.15f);
            this.leftArm.zRot = Mth.rotLerpRad(swimAmount, this.leftArm.zRot, 0.15f);
            this.leftLeg.xRot -= swimAmount * 0.55f * Mth.sin(0.1f * state.ageInTicks);
            this.rightLeg.xRot += swimAmount * 0.55f * Mth.sin(0.1f * state.ageInTicks);
            this.head.xRot = 0.0f;
        }
    }
}

