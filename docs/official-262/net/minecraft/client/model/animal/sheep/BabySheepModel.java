/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.sheep;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabySheepModel
extends SheepModel {
    public BabySheepModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-3.0f, -2.0f, -4.5f, 6.0f, 4.0f, 9.0f), PartPose.offset(0.0f, 17.0f, 0.5f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -4.5f, -3.5f, 5.0f, 5.0f, 5.0f), PartPose.offset(0.0f, 15.5f, -2.5f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(-2.0f, 19.0f, 3.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(2.0f, 19.0f, 3.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(-2.0f, 19.0f, -2.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(24, 5).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(2.0f, 19.0f, -2.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }
}

