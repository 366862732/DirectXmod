/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.cart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;

@Environment(value=EnvType.CLIENT)
public class MinecartModel
extends EntityModel<MinecartRenderState> {
    public MinecartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int w = 20;
        int d = 8;
        int h = 16;
        int yOff = 4;
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 10).addBox(-10.0f, -8.0f, -1.0f, 20.0f, 16.0f, 2.0f), PartPose.offsetAndRotation(0.0f, 4.0f, 0.0f, 1.5707964f, 0.0f, 0.0f));
        root.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -9.0f, -1.0f, 16.0f, 8.0f, 2.0f), PartPose.offsetAndRotation(-9.0f, 4.0f, 0.0f, 0.0f, 4.712389f, 0.0f));
        root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -9.0f, -1.0f, 16.0f, 8.0f, 2.0f), PartPose.offsetAndRotation(9.0f, 4.0f, 0.0f, 0.0f, 1.5707964f, 0.0f));
        root.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -9.0f, -1.0f, 16.0f, 8.0f, 2.0f), PartPose.offsetAndRotation(0.0f, 4.0f, -7.0f, 0.0f, (float)Math.PI, 0.0f));
        root.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -9.0f, -1.0f, 16.0f, 8.0f, 2.0f), PartPose.offset(0.0f, 4.0f, 7.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }
}

