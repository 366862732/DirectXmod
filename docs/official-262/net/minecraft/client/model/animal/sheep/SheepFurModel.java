/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.sheep;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SheepRenderState;

@Environment(value=EnvType.CLIENT)
public class SheepFurModel
extends QuadrupedModel<SheepRenderState> {
    public SheepFurModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createFurLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -4.0f, -4.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.6f)), PartPose.offset(0.0f, 6.0f, -8.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8).addBox(-4.0f, -10.0f, -7.0f, 8.0f, 16.0f, 6.0f, new CubeDeformation(1.75f)), PartPose.offsetAndRotation(0.0f, 5.0f, 2.0f, 1.5707964f, 0.0f, 0.0f));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.5f));
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0f, 12.0f, 7.0f));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(3.0f, 12.0f, 7.0f));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0f, 12.0f, -5.0f));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(3.0f, 12.0f, -5.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SheepRenderState state) {
        super.setupAnim(state);
        this.head.y += state.headEatPositionScale * 9.0f * state.ageScale;
        this.head.xRot = state.headEatAngleScale;
    }
}

