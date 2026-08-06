/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.slime;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Environment(value=EnvType.CLIENT)
public class SlimeModel
extends EntityModel<EntityRenderState> {
    public SlimeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createOuterBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 16.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createInnerBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0f, 17.0f, -3.0f, 6.0f, 6.0f, 6.0f), PartPose.ZERO);
        root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(32, 0).addBox(-3.25f, 18.0f, -3.5f, 2.0f, 2.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(32, 4).addBox(1.25f, 18.0f, -3.5f, 2.0f, 2.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(32, 8).addBox(0.0f, 21.0f, -3.5f, 1.0f, 1.0f, 1.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}

