/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.dolphin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.dolphin.DolphinModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyDolphinModel
extends DolphinModel {
    public BabyDolphinModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(20, 0).addBox(-3.0f, -2.5f, -4.0f, 6.0f, 5.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 21.5f, 0.0f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.5f, -4.0f, 6.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 1.0f, -4.0f));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 9).addBox(-1.0f, -1.0f, -2.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 0.5f, -4.0f));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(34, 18).addBox(-0.5f, -1.5f, -0.5f, 1.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(1.8f, 0.85f, -2.6f, 0.8727f, 0.0f, 1.7017f));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(48, 18).mirror().addBox(-0.5f, -1.5f, -0.5f, 1.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation(-1.8f, 0.85f, -2.6f, 0.8727f, 0.0f, -1.7017f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 13).addBox(-2.0f, -1.5f, 0.0f, 4.0f, 3.0f, 7.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 1.0f, 4.0f));
        tail.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(22, 13).addBox(-4.0f, -0.5f, -1.0f, 8.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 0.0f, 6.0f));
        body.addOrReplaceChild("back_fin", CubeListBuilder.create().texOffs(42, 0).addBox(-0.5f, -1.0f, 1.0f, 1.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, -1.0f, -2.7f, 0.8727f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

