/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.polarbear;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.polarbear.PolarBearModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyPolarBearModel
extends PolarBearModel {
    public BabyPolarBearModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-4.0f, -3.5f, -6.0f, 8.0f, 7.0f, 12.0f), PartPose.offset(0.0f, 17.5f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -2.625f, -4.25f, 6.0f, 5.0f, 4.0f).texOffs(20, 3).addBox(-2.0f, 0.375f, -6.25f, 4.0f, 2.0f, 2.0f).texOffs(20, 0).addBox(-4.0f, -3.625f, -2.75f, 2.0f, 2.0f, 1.0f).texOffs(26, 0).addBox(2.0f, -3.625f, -2.75f, 2.0f, 2.0f, 1.0f), PartPose.offset(0.0f, 18.625f, -5.75f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 3.0f, 3.0f), PartPose.offset(-2.5f, 21.5f, 4.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 3.0f, 3.0f), PartPose.offset(2.5f, 21.5f, 4.5f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 3.0f, 3.0f), PartPose.offset(-2.5f, 21.5f, -4.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 28).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 3.0f, 3.0f), PartPose.offset(2.5f, 21.5f, -4.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

