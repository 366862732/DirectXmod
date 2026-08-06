/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.chicken;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

@Environment(value=EnvType.CLIENT)
public class ColdChickenModel
extends AdultChickenModel {
    public ColdChickenModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = ColdChickenModel.createBaseChickenModel();
        mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-3.0f, -4.0f, -3.0f, 6.0f, 8.0f, 6.0f).texOffs(38, 9).addBox(0.0f, 3.0f, -1.0f, 0.0f, 3.0f, 5.0f), PartPose.offsetAndRotation(0.0f, 16.0f, 0.0f, 1.5707964f, 0.0f, 0.0f));
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -6.0f, -2.0f, 4.0f, 6.0f, 3.0f).texOffs(44, 0).addBox(-3.0f, -7.0f, -2.015f, 6.0f, 3.0f, 4.0f), PartPose.offset(0.0f, 15.0f, -4.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }
}

