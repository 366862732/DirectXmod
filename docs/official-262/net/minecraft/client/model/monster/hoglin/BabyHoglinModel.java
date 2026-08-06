/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.hoglin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.hoglin.HoglinModel;

@Environment(value=EnvType.CLIENT)
public class BabyHoglinModel
extends HoglinModel {
    public BabyHoglinModel(ModelPart root) {
        super(root);
    }

    @Override
    protected void animateHeadbutt(float headbuttLerpFactor) {
        super.animateHeadbutt(headbuttLerpFactor);
        this.head.y += headbuttLerpFactor * 2.5f;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -2.2605f, -10.547f, 10.0f, 4.0f, 12.0f).texOffs(44, 29).addBox(-7.0f, -4.0981f, -8.4879f, 2.0f, 5.0f, 2.0f).texOffs(52, 29).addBox(5.0f, -4.0981f, -8.4879f, 2.0f, 5.0f, 2.0f), PartPose.offsetAndRotation(0.0f, 13.0f, -7.0f, 0.8727f, 0.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0f, -14.0f, -7.0f, 8.0f, 8.0f, 14.0f, new CubeDeformation(0.02f)).texOffs(24, 39).addBox(0.0f, -18.0f, -8.0f, 0.0f, 6.0f, 11.0f, new CubeDeformation(0.02f)), PartPose.offset(0.0f, 24.0f, 0.0f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(32, 5).addBox(-5.1f, -0.5f, -2.0f, 6.0f, 1.0f, 4.0f), PartPose.offsetAndRotation(-5.0f, -1.0f, -1.5f, 0.0f, 0.0f, -0.8727f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-0.9f, -0.5f, -2.0f, 6.0f, 1.0f, 4.0f).mirror(false), PartPose.offsetAndRotation(5.0f, -1.0f, -1.5f, 0.0f, 0.0f, 0.8727f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 47).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(-2.5f, 18.0f, 4.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 47).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(2.5f, 18.0f, 4.5f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(-2.5f, 18.0f, -4.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), PartPose.offset(2.5f, 18.0f, -4.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

