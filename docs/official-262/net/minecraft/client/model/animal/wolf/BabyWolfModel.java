/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.wolf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WolfRenderState;

@Environment(value=EnvType.CLIENT)
public class BabyWolfModel
extends WolfModel {
    public BabyWolfModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-2.99f, -3.25f, -3.0f, 6.0f, 5.0f, 5.0f, new CubeDeformation(0.025f)).texOffs(17, 12).addBox(-1.5f, -0.24f, -5.0f, 3.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 18.25f, -4.0f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 5).addBox(-1.0f, -1.0f, -0.5f, 2.0f, 2.0f, 1.0f), PartPose.offset(-2.0f, -4.25f, -0.5f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(20, 5).addBox(-1.0f, -1.0f, -0.5f, 2.0f, 2.0f, 1.0f), PartPose.offset(2.0f, -4.25f, -0.5f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -2.0f, -4.0f, 6.0f, 4.0f, 8.0f), PartPose.offset(0.0f, 19.0f, 0.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(-1.5f, 21.0f, 3.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(8, 22).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(1.5f, 21.0f, 3.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(-1.5f, 21.0f, -3.0f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(1.5f, 21.0f, -3.0f));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0f, 19.0f, 3.0f, -0.5236f, 0.0f, 0.0f));
        tail.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(22, 16).addBox(-1.0f, -5.7f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, -0.6f, 0.2f, -3.1f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void shakeOffWater(WolfRenderState state) {
        super.shakeOffWater(state);
        this.head.zRot = state.headRollAngle + state.getBodyRollAngle(0.0f);
        this.tail.zRot = state.getBodyRollAngle(-0.2f);
    }

    @Override
    protected void setSittingPose(WolfRenderState state) {
        super.setSittingPose(state);
        this.body.xRot -= 1.5707964f;
    }
}

