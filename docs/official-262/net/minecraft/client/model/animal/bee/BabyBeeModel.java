/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.bee;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyBeeModel
extends BeeModel {
    public BabyBeeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(6, 12).addBox(1.0f, -1.6667f, -2.1633f, 1.0f, 2.0f, 2.0f).texOffs(0, 12).addBox(-2.0f, -1.6667f, -2.1933f, 1.0f, 2.0f, 2.0f), PartPose.offset(0.0f, 19.6667f, -1.8567f));
        PartDefinition body = bone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.0f, -2.5f, 4.0f, 4.0f, 5.0f), PartPose.offset(0.0f, 1.3333f, 2.3567f));
        body.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(13, 2).addBox(0.0f, -0.5f, 0.0f, 0.0f, 1.0f, 1.0f), PartPose.offset(0.0f, 0.5f, 2.5f));
        bone.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(3, 9).addBox(-3.0f, 0.0f, 0.0f, 3.0f, 0.0f, 3.0f), PartPose.offsetAndRotation(-1.0f, -0.6667f, 0.8567f, 0.2182f, 0.3491f, 0.0f));
        bone.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(-3, 9).mirror().addBox(0.0f, 0.0f, 0.0f, 3.0f, 0.0f, 3.0f).mirror(false), PartPose.offsetAndRotation(1.0f, -0.6667f, 0.8567f, 0.2182f, -0.3491f, 0.0f));
        bone.addOrReplaceChild("front_legs", CubeListBuilder.create().texOffs(13, 0).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 0.0f), PartPose.offset(0.0f, 3.3333f, 1.8567f));
        bone.addOrReplaceChild("middle_legs", CubeListBuilder.create().texOffs(13, 1).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 0.0f), PartPose.offset(0.0f, 3.3333f, 2.8567f));
        bone.addOrReplaceChild("back_legs", CubeListBuilder.create().texOffs(13, 2).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 0.0f), PartPose.offset(0.0f, 3.3333f, 3.8567f));
        return LayerDefinition.create(mesh, 32, 32);
    }
}

