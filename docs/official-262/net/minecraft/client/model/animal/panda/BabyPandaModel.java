/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.panda;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.panda.PandaModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class BabyPandaModel
extends PandaModel {
    public BabyPandaModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 11).addBox(-4.5f, -3.5f, -5.5f, 9.0f, 7.0f, 11.0f), PartPose.offset(0.0f, 18.5f, 2.5f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -3.0f, -5.0f, 7.0f, 6.0f, 5.0f).texOffs(24, 6).addBox(-2.0f, 1.0f, -6.0f, 4.0f, 2.0f, 1.0f).texOffs(24, 0).addBox(-4.5f, -4.0f, -3.5f, 3.0f, 3.0f, 1.0f).texOffs(33, 0).addBox(1.5f, -4.0f, -3.5f, 3.0f, 3.0f, 1.0f), PartPose.offset(0.0f, 19.0f, -3.0f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 2.0f, 3.0f), PartPose.offset(-3.0f, 22.0f, 6.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 2.0f, 3.0f), PartPose.offset(3.0f, 22.0f, 6.5f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 29).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 2.0f, 3.0f), PartPose.offset(-3.0f, 22.0f, -1.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 29).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 2.0f, 3.0f), PartPose.offset(3.0f, 22.0f, -1.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected void animateSitting(PandaRenderState state) {
        this.body.xRot = Mth.rotLerpRad(state.sitAmount, this.body.xRot, 0.17453292f);
        this.body.z = Mth.lerp(state.sitAmount, this.body.z, -1.5f);
        this.head.z = Mth.lerp(state.sitAmount, this.head.z, -11.5f);
        this.head.y = Mth.lerp(state.sitAmount, this.head.y, 17.5f);
        this.rightFrontLeg.z = Mth.lerp(state.sitAmount, this.rightFrontLeg.z, -5.0f);
        this.leftFrontLeg.z = Mth.lerp(state.sitAmount, this.leftFrontLeg.z, -5.0f);
        this.rightHindLeg.z = Mth.lerp(state.sitAmount, this.rightHindLeg.z, 3.0f);
        this.leftHindLeg.z = Mth.lerp(state.sitAmount, this.leftHindLeg.z, 3.0f);
        this.rightFrontLeg.zRot = -0.27079642f;
        this.leftFrontLeg.zRot = 0.27079642f;
        this.rightHindLeg.zRot = 0.5707964f;
        this.leftHindLeg.zRot = -0.5707964f;
    }
}

