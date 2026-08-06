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
public class ColdPigModel
extends PigModel {
    public ColdPigModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition basePigModel = ColdPigModel.createBasePigModel(g);
        PartDefinition root = basePigModel.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8).addBox(-5.0f, -10.0f, -7.0f, 10.0f, 16.0f, 8.0f).texOffs(28, 32).addBox(-5.0f, -10.0f, -7.0f, 10.0f, 16.0f, 8.0f, new CubeDeformation(0.5f)), PartPose.offsetAndRotation(0.0f, 11.0f, 2.0f, 1.5707964f, 0.0f, 0.0f));
        return LayerDefinition.create(basePigModel, 64, 64);
    }
}

