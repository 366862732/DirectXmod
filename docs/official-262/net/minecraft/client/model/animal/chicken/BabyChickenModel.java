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
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyChickenModel
extends ChickenModel {
    public BabyChickenModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.25f, -0.75f, 4.0f, 4.0f, 4.0f).texOffs(10, 8).addBox(-1.0f, -0.25f, -1.75f, 2.0f, 1.0f, 1.0f), PartPose.offset(0.0f, 20.25f, -1.25f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(2, 2).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 2.0f, 0.0f).texOffs(0, 1).addBox(-0.5f, 2.0f, -1.0f, 1.0f, 0.0f, 1.0f), PartPose.offset(1.0f, 22.0f, 0.5f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 2).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 2.0f, 0.0f).texOffs(0, 0).addBox(-0.5f, 2.0f, -1.0f, 1.0f, 0.0f, 1.0f), PartPose.offset(-1.0f, 22.0f, 0.5f));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(6, 8).addBox(0.0f, 0.0f, -1.0f, 1.0f, 0.0f, 2.0f), PartPose.offset(2.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(4, 8).addBox(-1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 2.0f), PartPose.offset(-2.0f, 20.0f, 0.0f));
        return LayerDefinition.create(mesh, 16, 16);
    }
}

