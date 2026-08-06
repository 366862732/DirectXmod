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
import net.minecraft.client.animation.definitions.BabyArmadilloAnimation;
import net.minecraft.client.model.animal.armadillo.ArmadilloModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyArmadilloModel
extends ArmadilloModel {
    public BabyArmadilloModel(ModelPart root) {
        super(root, BabyArmadilloAnimation.ARMADILLO_BABY_WALK, BabyArmadilloAnimation.ARMADILLO_BABY_ROLL_OUT, BabyArmadilloAnimation.ARMADILLO_BABY_ROLL_UP, BabyArmadilloAnimation.ARMADILLO_BABY_PEEK);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -2.0f, -3.5f, 5.0f, 4.0f, 7.0f, new CubeDeformation(0.3f)).texOffs(0, 11).addBox(-2.5f, -2.0f, -3.0f, 5.0f, 4.0f, 6.0f), PartPose.offset(0.0f, 20.0f, 0.5f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 3.4f));
        tail.addOrReplaceChild("right_ear_cube", CubeListBuilder.create().texOffs(22, 11).addBox(-0.5f, -0.5f, -2.0f, 1.0f, 1.0f, 4.0f), PartPose.offsetAndRotation(0.0f, 1.5f, 1.0f, -1.0472f, 0.0f, 0.0f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, -3.2f));
        PartDefinition headGroup = head.addOrReplaceChild("head_cube", CubeListBuilder.create().texOffs(20, 17).addBox(-1.0f, -2.0f, -4.0f, 2.0f, 2.0f, 4.0f), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.7417649f, 0.0f, 0.0f));
        headGroup.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(28, 8).mirror().addBox(-1.8f, -2.0f, 0.0f, 2.0f, 3.0f, 0.0f).mirror(false), PartPose.offsetAndRotation(-1.0f, -2.0f, -0.3f, -0.4363f, -0.1134f, 0.0524f));
        headGroup.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(28, 8).addBox(-0.2f, -2.0f, 0.0f, 2.0f, 3.0f, 0.0f), PartPose.offsetAndRotation(1.0f, -2.0f, -0.3f, -0.4363f, 0.1134f, -0.0524f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(20, 27).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f).mirror(false), PartPose.offset(-1.5f, 22.0f, 2.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(20, 27).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f), PartPose.offset(1.5f, 22.0f, 2.5f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(20, 23).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f), PartPose.offset(1.5f, 22.0f, -1.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f).mirror(false), PartPose.offset(-1.5f, 22.0f, -1.5f));
        root.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 25).addBox(-3.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.3f)), PartPose.offset(0.0f, 20.7f, 0.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

