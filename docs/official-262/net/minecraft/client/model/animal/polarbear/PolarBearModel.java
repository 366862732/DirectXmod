/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.polarbear;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;

@Environment(value=EnvType.CLIENT)
public class PolarBearModel
extends QuadrupedModel<PolarBearRenderState> {
    public PolarBearModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -3.0f, -3.0f, 7.0f, 7.0f, 7.0f).texOffs(0, 44).addBox("mouth", -2.5f, 1.0f, -6.0f, 5.0f, 3.0f, 3.0f).texOffs(26, 0).addBox("right_ear", -4.5f, -4.0f, -1.0f, 2.0f, 2.0f, 1.0f).texOffs(26, 0).mirror().addBox("left_ear", 2.5f, -4.0f, -1.0f, 2.0f, 2.0f, 1.0f), PartPose.offset(0.0f, 10.0f, -16.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 19).addBox(-5.0f, -13.0f, -7.0f, 14.0f, 14.0f, 11.0f).texOffs(39, 0).addBox(-4.0f, -25.0f, -7.0f, 12.0f, 12.0f, 10.0f), PartPose.offsetAndRotation(-2.0f, 9.0f, 12.0f, 1.5707964f, 0.0f, 0.0f));
        int legSize = 10;
        CubeListBuilder hindLeg = CubeListBuilder.create().texOffs(50, 22).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 10.0f, 8.0f);
        root.addOrReplaceChild("right_hind_leg", hindLeg, PartPose.offset(-4.5f, 14.0f, 6.0f));
        root.addOrReplaceChild("left_hind_leg", hindLeg, PartPose.offset(4.5f, 14.0f, 6.0f));
        CubeListBuilder frontLeg = CubeListBuilder.create().texOffs(50, 40).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 10.0f, 6.0f);
        root.addOrReplaceChild("right_front_leg", frontLeg, PartPose.offset(-3.5f, 14.0f, -8.0f));
        root.addOrReplaceChild("left_front_leg", frontLeg, PartPose.offset(3.5f, 14.0f, -8.0f));
        return LayerDefinition.create(mesh, 128, 64).apply(MeshTransformer.scaling(1.2f));
    }

    @Override
    public void setupAnim(PolarBearRenderState state) {
        super.setupAnim(state);
        float standScale = state.standScale * state.standScale;
        float bodyAgeScale = state.ageScale;
        this.body.xRot -= standScale * (float)Math.PI * 0.35f;
        this.body.y += standScale * bodyAgeScale * 2.0f;
        this.rightFrontLeg.y -= standScale * bodyAgeScale * 20.0f;
        this.rightFrontLeg.z += standScale * bodyAgeScale * 4.0f;
        this.rightFrontLeg.xRot -= standScale * (float)Math.PI * 0.45f;
        this.leftFrontLeg.y = this.rightFrontLeg.y;
        this.leftFrontLeg.z = this.rightFrontLeg.z;
        this.leftFrontLeg.xRot -= standScale * (float)Math.PI * 0.45f;
        this.head.y -= standScale * 24.0f;
        this.head.z += standScale * 13.0f;
        this.head.xRot += standScale * (float)Math.PI * 0.15f;
    }
}

