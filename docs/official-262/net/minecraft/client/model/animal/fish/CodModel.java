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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class CodModel
extends EntityModel<LivingEntityRenderState> {
    private final ModelPart tailFin;

    public CodModel(ModelPart root) {
        super(root);
        this.tailFin = root.getChild("tail_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int yo = 22;
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -2.0f, 0.0f, 2.0f, 4.0f, 7.0f), PartPose.offset(0.0f, 22.0f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(11, 0).addBox(-1.0f, -2.0f, -3.0f, 2.0f, 4.0f, 3.0f), PartPose.offset(0.0f, 22.0f, 0.0f));
        root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 3.0f, 1.0f), PartPose.offset(0.0f, 22.0f, -3.0f));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(22, 1).addBox(-2.0f, 0.0f, -1.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation(-1.0f, 23.0f, 0.0f, 0.0f, 0.0f, -0.7853982f));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(22, 4).addBox(0.0f, 0.0f, -1.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation(1.0f, 23.0f, 0.0f, 0.0f, 0.0f, 0.7853982f));
        root.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(22, 3).addBox(0.0f, -2.0f, 0.0f, 0.0f, 4.0f, 4.0f), PartPose.offset(0.0f, 22.0f, 7.0f));
        root.addOrReplaceChild("top_fin", CubeListBuilder.create().texOffs(20, -6).addBox(0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 6.0f), PartPose.offset(0.0f, 20.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float amplitudeMultiplier = state.isInWater ? 1.0f : 1.5f;
        this.tailFin.yRot = -amplitudeMultiplier * 0.45f * Mth.sin(0.6f * state.ageInTicks);
    }
}

