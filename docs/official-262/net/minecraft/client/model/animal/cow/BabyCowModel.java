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
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyCowModel
extends CowModel {
    public BabyCowModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0f, -4.569f, -4.8333f, 6.0f, 6.0f, 5.0f).texOffs(8, 29).addBox(3.0f, -5.569f, -3.8333f, 1.0f, 2.0f, 1.0f).texOffs(4, 29).mirror().addBox(-4.0f, -5.569f, -3.8333f, 1.0f, 2.0f, 1.0f).mirror(false).texOffs(12, 29).addBox(-2.0f, -1.569f, -5.8333f, 4.0f, 3.0f, 1.0f), PartPose.offset(0.0f, 13.569f, -5.1667f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -7.0f, -1.0f, 8.0f, 6.0f, 12.0f), PartPose.offset(3.0f, 19.0f, -5.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(22, 18).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(-2.5f, 18.0f, -3.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(34, 18).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(2.5f, 18.0f, -3.5f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(22, 27).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(-2.5f, 18.0f, 3.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(34, 27).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(2.5f, 18.0f, 3.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

