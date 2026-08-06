/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.goat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.GoatRenderState;

@Environment(value=EnvType.CLIENT)
public class BabyGoatModel
extends GoatModel {
    private static final String HEAD_MAIN = "HeadMain";

    public BabyGoatModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(29, 12).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(1.5f, 19.5f, 3.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(21, 12).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(-1.5f, 19.5f, 3.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(21, 5).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(-1.5f, 19.5f, -2.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(29, 5).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f), PartPose.offset(1.5f, 19.5f, -2.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-3.0f, -2.3f, -4.5f, 6.0f, 5.0f, 9.0f).texOffs(0, 24).addBox(-2.5f, -2.2f, -4.0f, 5.0f, 4.0f, 8.0f), PartPose.offset(0.0f, 17.8f, 0.0f));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -3.8126f, -5.1548f, 4.0f, 4.0f, 6.0f), PartPose.offsetAndRotation(0.0f, 15.5f, -3.0f, 0.4363f, 0.0f, 0.0f));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(0.0f, -4.5f, 0.0f, 1.0f, 2.0f, 1.0f).mirror(false), PartPose.offsetAndRotation(-1.5f, -1.5f, -1.0f, -0.3926991f, 0.0f, 0.0f));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(2.0f, -4.5f, 0.0f, 1.0f, 2.0f, 1.0f).mirror(false), PartPose.offsetAndRotation(-1.5f, -1.5f, -1.0f, -0.3926991f, 0.0f, 0.0f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-2.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f).mirror(false), PartPose.offsetAndRotation(-1.7f, -2.3126f, 0.1452f, 0.0f, -0.5236f, 0.0f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 12).addBox(0.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f), PartPose.offsetAndRotation(1.7f, -2.3126f, 0.1452f, 0.0f, 0.5236f, 0.0f));
        head.addOrReplaceChild(HEAD_MAIN, CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.5f, -4.0f, 4.0f, 4.0f, 6.0f), PartPose.offset(0.0f, -1.3126f, -1.1548f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(GoatRenderState state) {
        super.setupAnim(state);
        if (state.rammingXHeadRot == 0.0f) {
            this.head.xRot = 0.3926991f;
        }
    }
}

