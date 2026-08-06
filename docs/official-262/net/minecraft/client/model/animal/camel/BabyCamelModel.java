/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.camel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.definitions.CamelBabyAnimation;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyCamelModel
extends CamelModel {
    public BabyCamelModel(ModelPart root) {
        super(root, CamelBabyAnimation.CAMEL_BABY_WALK, CamelBabyAnimation.CAMEL_BABY_SIT, CamelBabyAnimation.CAMEL_BABY_SIT_POSE, CamelBabyAnimation.CAMEL_BABY_STANDUP, CamelBabyAnimation.CAMEL_BABY_IDLE, CamelBabyAnimation.CAMEL_BABY_DASH);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 14).addBox(-4.5f, -4.0f, -8.0f, 9.0f, 8.0f, 16.0f), PartPose.offset(0.0f, 7.0f, 0.0f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(50, 38).addBox(-1.5f, -0.5f, 0.0f, 3.0f, 9.0f, 0.0f), PartPose.offset(0.0f, -1.5f, 8.05f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 0).addBox(-2.5f, -3.0f, -7.5f, 5.0f, 5.0f, 7.0f).texOffs(0, 0).addBox(-2.5f, -12.0f, -7.5f, 5.0f, 9.0f, 5.0f).texOffs(0, 14).addBox(-2.5f, -12.0f, -10.5f, 5.0f, 4.0f, 3.0f), PartPose.offset(0.0f, 1.0f, -7.5f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(37, 0).addBox(-3.0f, -0.5f, -1.0f, 3.0f, 1.0f, 2.0f), PartPose.offset(-2.5f, -11.0f, -4.0f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(47, 0).addBox(0.0f, -0.5f, -1.0f, 3.0f, 1.0f, 2.0f), PartPose.offset(2.5f, -11.0f, -4.0f));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(36, 14).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 13.0f, 3.0f), PartPose.offset(-3.0f, 11.5f, -5.5f));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(48, 14).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 13.0f, 3.0f), PartPose.offset(3.0f, 11.5f, -5.5f));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 38).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 13.0f, 3.0f), PartPose.offset(3.0f, 11.5f, 5.5f));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 38).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 13.0f, 3.0f), PartPose.offset(-3.0f, 11.5f, 5.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

