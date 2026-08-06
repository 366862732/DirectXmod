/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.fish;

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
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class PufferfishMidModel
extends EntityModel<EntityRenderState> {
    private final ModelPart leftBlueFin;
    private final ModelPart rightBlueFin;

    public PufferfishMidModel(ModelPart root) {
        super(root);
        this.leftBlueFin = root.getChild("left_blue_fin");
        this.rightBlueFin = root.getChild("right_blue_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int yo = 22;
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(12, 22).addBox(-2.5f, -5.0f, -2.5f, 5.0f, 5.0f, 5.0f), PartPose.offset(0.0f, 22.0f, 0.0f));
        root.addOrReplaceChild("right_blue_fin", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f), PartPose.offset(-2.5f, 18.0f, -1.5f));
        root.addOrReplaceChild("left_blue_fin", CubeListBuilder.create().texOffs(24, 3).addBox(0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f), PartPose.offset(2.5f, 18.0f, -1.5f));
        root.addOrReplaceChild("top_front_fin", CubeListBuilder.create().texOffs(19, 17).addBox(-2.5f, -1.0f, 0.0f, 5.0f, 1.0f, 0.0f), PartPose.offsetAndRotation(0.0f, 17.0f, -2.5f, 0.7853982f, 0.0f, 0.0f));
        root.addOrReplaceChild("top_back_fin", CubeListBuilder.create().texOffs(11, 17).addBox(-2.5f, -1.0f, 0.0f, 5.0f, 1.0f, 0.0f), PartPose.offsetAndRotation(0.0f, 17.0f, 2.5f, -0.7853982f, 0.0f, 0.0f));
        root.addOrReplaceChild("right_front_fin", CubeListBuilder.create().texOffs(5, 17).addBox(-1.0f, -5.0f, 0.0f, 1.0f, 5.0f, 0.0f), PartPose.offsetAndRotation(-2.5f, 22.0f, -2.5f, 0.0f, -0.7853982f, 0.0f));
        root.addOrReplaceChild("right_back_fin", CubeListBuilder.create().texOffs(9, 17).addBox(-1.0f, -5.0f, 0.0f, 1.0f, 5.0f, 0.0f), PartPose.offsetAndRotation(-2.5f, 22.0f, 2.5f, 0.0f, 0.7853982f, 0.0f));
        root.addOrReplaceChild("left_back_fin", CubeListBuilder.create().texOffs(1, 17).addBox(0.0f, -5.0f, 0.0f, 1.0f, 5.0f, 0.0f), PartPose.offsetAndRotation(2.5f, 22.0f, 2.5f, 0.0f, -0.7853982f, 0.0f));
        root.addOrReplaceChild("left_front_fin", CubeListBuilder.create().texOffs(1, 17).addBox(0.0f, -5.0f, 0.0f, 1.0f, 5.0f, 0.0f), PartPose.offsetAndRotation(2.5f, 22.0f, -2.5f, 0.0f, 0.7853982f, 0.0f));
        root.addOrReplaceChild("bottom_back_fin", CubeListBuilder.create().texOffs(18, 20).addBox(0.0f, 0.0f, 0.0f, 5.0f, 1.0f, 0.0f), PartPose.offsetAndRotation(-2.5f, 22.0f, 2.5f, 0.7853982f, 0.0f, 0.0f));
        root.addOrReplaceChild("bottom_front_fin", CubeListBuilder.create().texOffs(17, 19).addBox(-2.5f, 0.0f, 0.0f, 5.0f, 1.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 22.0f, -2.5f, -0.7853982f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(EntityRenderState state) {
        super.setupAnim(state);
        this.rightBlueFin.zRot = -0.2f + 0.4f * Mth.sin(state.ageInTicks * 0.2f);
        this.leftBlueFin.zRot = 0.2f - 0.4f * Mth.sin(state.ageInTicks * 0.2f);
    }
}

