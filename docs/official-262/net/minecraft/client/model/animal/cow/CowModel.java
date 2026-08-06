/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.cow;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@Environment(value=EnvType.CLIENT)
public class CowModel
extends QuadrupedModel<LivingEntityRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(false, 8.0f, 6.0f, Set.of("head"));
    private static final int LEG_SIZE = 12;

    public CowModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = CowModel.createBaseCowModel();
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static MeshDefinition createBaseCowModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -6.0f, 8.0f, 8.0f, 6.0f).texOffs(1, 33).addBox(-3.0f, 1.0f, -7.0f, 6.0f, 3.0f, 1.0f).texOffs(22, 0).addBox("right_horn", -5.0f, -5.0f, -5.0f, 1.0f, 3.0f, 1.0f).texOffs(22, 0).addBox("left_horn", 4.0f, -5.0f, -5.0f, 1.0f, 3.0f, 1.0f), PartPose.offset(0.0f, 4.0f, -8.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 4).addBox(-6.0f, -10.0f, -7.0f, 12.0f, 18.0f, 10.0f).texOffs(52, 0).addBox(-2.0f, 2.0f, -8.0f, 4.0f, 6.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 5.0f, 2.0f, 1.5707964f, 0.0f, 0.0f));
        CubeListBuilder leftLeg = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f);
        CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f);
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-4.0f, 12.0f, 7.0f));
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(4.0f, 12.0f, 7.0f));
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-4.0f, 12.0f, -5.0f));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(4.0f, 12.0f, -5.0f));
        return mesh;
    }

    public ModelPart getHead() {
        return this.head;
    }
}

