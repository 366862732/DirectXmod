/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.armadillo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.definitions.ArmadilloAnimation;
import net.minecraft.client.model.animal.armadillo.ArmadilloModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class AdultArmadilloModel
extends ArmadilloModel {
    public AdultArmadilloModel(ModelPart root) {
        super(root, ArmadilloAnimation.ARMADILLO_WALK, ArmadilloAnimation.ARMADILLO_ROLL_OUT, ArmadilloAnimation.ARMADILLO_ROLL_UP, ArmadilloAnimation.ARMADILLO_PEEK);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20).addBox(-4.0f, -7.0f, -10.0f, 8.0f, 8.0f, 12.0f, new CubeDeformation(0.3f)).texOffs(0, 40).addBox(-4.0f, -7.0f, -10.0f, 8.0f, 8.0f, 12.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 21.0f, 4.0f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 53).addBox(-0.5f, -0.0865f, 0.0933f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, -3.0f, 1.0f, 0.5061f, 0.0f, 0.0f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0f, -2.0f, -11.0f));
        head.addOrReplaceChild("head_cube", CubeListBuilder.create().texOffs(43, 15).addBox(-1.5f, -1.0f, -1.0f, 3.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.3927f, 0.0f, 0.0f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-1.0f, -1.0f, 0.0f));
        rightEar.addOrReplaceChild("right_ear_cube", CubeListBuilder.create().texOffs(43, 10).addBox(-2.0f, -3.0f, 0.0f, 2.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(-0.5f, 0.0f, -0.6f, 0.1886f, -0.3864f, -0.0718f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(1.0f, -2.0f, 0.0f));
        leftEar.addOrReplaceChild("left_ear_cube", CubeListBuilder.create().texOffs(47, 10).addBox(0.0f, -3.0f, 0.0f, 2.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.5f, 1.0f, -0.6f, 0.1886f, 0.3864f, 0.0718f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(51, 31).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-2.0f, 21.0f, 4.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(42, 31).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(2.0f, 21.0f, 4.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(51, 43).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-2.0f, 21.0f, -4.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(42, 43).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(2.0f, 21.0f, -4.0f));
        root.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -10.0f, -6.0f, 10.0f, 10.0f, 10.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 24.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

