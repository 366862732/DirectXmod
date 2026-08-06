/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.cow;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class ColdCowModel
extends CowModel {
    public ColdCowModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = ColdCowModel.createBaseCowModel();
        mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(20, 32).addBox(-6.0f, -10.0f, -7.0f, 12.0f, 18.0f, 10.0f, new CubeDeformation(0.5f)).texOffs(18, 4).addBox(-6.0f, -10.0f, -7.0f, 12.0f, 18.0f, 10.0f).texOffs(52, 0).addBox(-2.0f, 2.0f, -8.0f, 4.0f, 6.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 5.0f, 2.0f, 1.5707964f, 0.0f, 0.0f));
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -6.0f, 8.0f, 8.0f, 6.0f).texOffs(9, 33).addBox(-3.0f, 1.0f, -7.0f, 6.0f, 3.0f, 1.0f), PartPose.offset(0.0f, 4.0f, -8.0f));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(0, 40).addBox(-1.5f, -4.5f, -0.5f, 2.0f, 6.0f, 2.0f), PartPose.offsetAndRotation(-4.5f, -2.5f, -3.5f, 1.5708f, 0.0f, 0.0f));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(0, 32).addBox(-1.5f, -3.0f, -0.5f, 2.0f, 6.0f, 2.0f), PartPose.offsetAndRotation(5.5f, -2.5f, -5.0f, 1.5708f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

