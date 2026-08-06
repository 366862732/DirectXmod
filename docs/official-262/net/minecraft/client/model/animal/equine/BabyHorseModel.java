/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.equine;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class BabyHorseModel
extends HorseModel {
    public BabyHorseModel(ModelPart root) {
        super(root);
    }

    public static MeshDefinition createBabyMesh(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition Body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13).addBox(-4.0f, -3.5f, -7.0f, 8.0f, 7.0f, 14.0f, g), PartPose.offset(0.0f, 12.5f, 0.0f));
        Body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 34).addBox(-1.5f, -1.5f, -1.0f, 3.0f, 3.0f, 8.0f, g), PartPose.offsetAndRotation(0.0f, -1.0f, 7.0f, -0.7418f, 0.0f, 0.0f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 46).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 9.0f, 3.0f, g), PartPose.offset(2.4f, 16.0f, 5.4f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 46).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 9.0f, 3.0f, g), PartPose.offset(-2.4f, 16.0f, 5.4f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 9.0f, 3.0f, g), PartPose.offset(2.4f, 16.0f, -5.4f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 9.0f, 3.0f, g), PartPose.offset(-2.4f, 16.0f, -5.4f));
        PartDefinition neck = root.addOrReplaceChild("head_parts", CubeListBuilder.create().texOffs(30, 0).addBox(-2.0f, -6.0f, -2.0f, 4.0f, 8.0f, 4.0f, g), PartPose.offsetAndRotation(0.0f, 10.0f, -6.0f, 0.6109f, 0.0f, 0.0f));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.9484f, -6.705f, 6.0f, 4.0f, 9.0f, g), PartPose.offset(0.0f, -6.0516f, -0.2951f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 4).addBox(-1.0f, -2.5f, -0.8f, 2.0f, 3.0f, 1.0f, g), PartPose.offsetAndRotation(2.0f, -4.2484f, 1.9451f, 0.0f, 0.0f, 0.2618f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 3.0f, 1.0f, g), PartPose.offsetAndRotation(-2.0f, -4.2484f, 1.645f, 0.0f, 0.0f, -0.2618f));
        return mesh;
    }

    @Override
    protected float getLegStandingYOffset() {
        return 4.0f;
    }

    @Override
    protected float getLegStandingZOffset() {
        return 0.0f;
    }

    @Override
    protected float getTailXRotOffset() {
        return -1.5707964f;
    }

    @Override
    protected void animateHeadPartsPlacement(float eating, float standing) {
        this.headParts.y += Mth.lerp(eating, Mth.lerp(standing, 0.0f, -2.0f), 2.0f);
        this.headParts.z = Mth.lerp(standing, this.headParts.z, -4.0f);
    }
}

