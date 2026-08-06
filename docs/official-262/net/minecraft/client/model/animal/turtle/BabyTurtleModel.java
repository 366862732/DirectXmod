/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.turtle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.turtle.TurtleModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Environment(value=EnvType.CLIENT)
public class BabyTurtleModel
extends TurtleModel {
    public BabyTurtleModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 2.0f, 4.0f), PartPose.offset(0.0f, 22.9f, 1.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 6).addBox(-1.5f, -2.0f, -3.0f, 3.0f, 3.0f, 3.0f), PartPose.offset(0.0f, 22.9f, -1.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0f, 0.0f, -0.5f, 2.0f, 0.0f, 1.0f), PartPose.offset(-2.0f, 23.9f, 2.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(-1, 1).addBox(0.0f, 0.0f, -0.5f, 2.0f, 0.0f, 1.0f), PartPose.offset(2.0f, 23.9f, 2.5f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(8, 6).addBox(-2.0f, 0.0f, -0.5f, 2.0f, 0.0f, 1.0f), PartPose.offset(-2.0f, 23.9f, -0.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(8, 7).addBox(0.0f, 0.0f, -0.5f, 2.0f, 0.0f, 1.0f), PartPose.offset(2.0f, 23.9f, -0.5f));
        return LayerDefinition.create(mesh, 16, 16);
    }
}

