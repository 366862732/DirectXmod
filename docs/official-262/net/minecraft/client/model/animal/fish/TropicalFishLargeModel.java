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
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class TropicalFishLargeModel
extends EntityModel<TropicalFishRenderState> {
    private final ModelPart tail;

    public TropicalFishLargeModel(ModelPart root) {
        super(root);
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int yo = 19;
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20).addBox(-1.0f, -3.0f, -3.0f, 2.0f, 6.0f, 6.0f, g), PartPose.offset(0.0f, 19.0f, 0.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(21, 16).addBox(0.0f, -3.0f, 0.0f, 0.0f, 6.0f, 5.0f, g), PartPose.offset(0.0f, 19.0f, 3.0f));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(2, 16).addBox(-2.0f, 0.0f, 0.0f, 2.0f, 2.0f, 0.0f, g), PartPose.offsetAndRotation(-1.0f, 20.0f, 0.0f, 0.0f, 0.7853982f, 0.0f));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(2, 12).addBox(0.0f, 0.0f, 0.0f, 2.0f, 2.0f, 0.0f, g), PartPose.offsetAndRotation(1.0f, 20.0f, 0.0f, 0.0f, -0.7853982f, 0.0f));
        root.addOrReplaceChild("top_fin", CubeListBuilder.create().texOffs(20, 11).addBox(0.0f, -4.0f, 0.0f, 0.0f, 4.0f, 6.0f, g), PartPose.offset(0.0f, 16.0f, -3.0f));
        root.addOrReplaceChild("bottom_fin", CubeListBuilder.create().texOffs(20, 21).addBox(0.0f, 0.0f, 0.0f, 0.0f, 4.0f, 6.0f, g), PartPose.offset(0.0f, 22.0f, -3.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(TropicalFishRenderState state) {
        super.setupAnim(state);
        float amplitudeMultiplier = state.isInWater ? 1.0f : 1.5f;
        this.tail.yRot = -amplitudeMultiplier * 0.45f * Mth.sin(0.6f * state.ageInTicks);
    }
}

