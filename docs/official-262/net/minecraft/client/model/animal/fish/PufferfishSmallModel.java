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
public class PufferfishSmallModel
extends EntityModel<EntityRenderState> {
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public PufferfishSmallModel(ModelPart root) {
        super(root);
        this.leftFin = root.getChild("left_fin");
        this.rightFin = root.getChild("right_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int yo = 23;
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 27).addBox(-1.5f, -2.0f, -1.5f, 3.0f, 2.0f, 3.0f), PartPose.offset(0.0f, 23.0f, 0.0f));
        root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(24, 6).addBox(-1.5f, 0.0f, -1.5f, 1.0f, 1.0f, 1.0f), PartPose.offset(0.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(28, 6).addBox(0.5f, 0.0f, -1.5f, 1.0f, 1.0f, 1.0f), PartPose.offset(0.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("back_fin", CubeListBuilder.create().texOffs(-3, 0).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 0.0f, 3.0f), PartPose.offset(0.0f, 22.0f, 1.5f));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(25, 0).addBox(-1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 2.0f), PartPose.offset(-1.5f, 22.0f, -1.5f));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(25, 0).addBox(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 2.0f), PartPose.offset(1.5f, 22.0f, -1.5f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(EntityRenderState state) {
        super.setupAnim(state);
        this.rightFin.zRot = -0.2f + 0.4f * Mth.sin(state.ageInTicks * 0.2f);
        this.leftFin.zRot = 0.2f - 0.4f * Mth.sin(state.ageInTicks * 0.2f);
    }
}

