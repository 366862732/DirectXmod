/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.pig;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyPigModel
extends PigModel {
    public BabyPigModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -3.0f, -4.5f, 7.0f, 6.0f, 9.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 19.0f, 0.5f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 15).addBox(-3.5f, -5.0f, -5.0f, 7.0f, 6.0f, 6.0f, new CubeDeformation(0.025f)).texOffs(6, 27).addBox(-1.5f, -1.975f, -6.0f, 3.0f, 2.0f, 1.0f, new CubeDeformation(0.015f)), PartPose.offset(0.0f, 19.0f, -2.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(2.5f, 22.0f, -3.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(23, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-2.5f, 22.0f, -3.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 4).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(2.5f, 22.0f, 4.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(23, 4).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-2.5f, 22.0f, 4.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }
}

