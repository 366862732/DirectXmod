/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.llama;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyLlamaModel
extends LlamaModel {
    public BabyLlamaModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -9.0f, -4.0f, 6.0f, 11.0f, 4.0f, g).texOffs(0, 15).addBox(-1.5f, -7.0f, -7.0f, 3.0f, 3.0f, 3.0f, g).texOffs(20, 4).addBox(0.5f, -11.0f, -3.0f, 2.0f, 2.0f, 2.0f, g).texOffs(20, 0).addBox(-2.5f, -11.0f, -3.0f, 2.0f, 2.0f, 2.0f, g), PartPose.offset(0.0f, 12.0f, -4.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 45).addBox(-1.4f, -0.5f, -1.5f, 3.0f, 8.0f, 3.0f, g), PartPose.offset(-2.5f, 16.5f, 4.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 45).addBox(-1.6f, -0.5f, -1.5f, 3.0f, 8.0f, 3.0f, g), PartPose.offset(2.5f, 16.5f, 4.5f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.4f, -0.5f, -1.5f, 3.0f, 8.0f, 3.0f, g), PartPose.offset(-2.5f, 16.5f, -3.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.6f, -0.5f, -1.5f, 3.0f, 8.0f, 3.0f, g), PartPose.offset(2.5f, 16.5f, -3.5f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0f, -3.0f, -8.5f, 8.0f, 6.0f, 13.0f, g), PartPose.offset(0.0f, 14.0f, 2.5f));
        root.addOrReplaceChild("right_chest", CubeListBuilder.create().texOffs(45, 28).addBox(-3.0f, 0.0f, 0.0f, 8.0f, 8.0f, 3.0f, g), PartPose.offsetAndRotation(-8.5f, 4.0f, 3.0f, 0.0f, 1.5707964f, 0.0f));
        root.addOrReplaceChild("left_chest", CubeListBuilder.create().texOffs(45, 41).addBox(-3.0f, 0.0f, 0.0f, 8.0f, 8.0f, 3.0f, g), PartPose.offsetAndRotation(5.5f, 4.0f, 3.0f, 0.0f, 1.5707964f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

