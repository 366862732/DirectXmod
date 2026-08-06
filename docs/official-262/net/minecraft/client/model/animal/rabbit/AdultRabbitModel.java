/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.rabbit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.definitions.RabbitAnimation;
import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class AdultRabbitModel
extends RabbitModel {
    public AdultRabbitModel(ModelPart root) {
        super(root, RabbitAnimation.HOP, RabbitAnimation.IDLE_HEAD_TILT);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -6.0f, -9.0f, 8.0f, 6.0f, 10.0f), PartPose.offsetAndRotation(0.0f, 23.0f, 4.0f, -0.3927f, 0.0f, 0.0f));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 16).addBox(-2.0f, -3.0084f, -1.0125f, 4.0f, 4.0f, 4.0f), PartPose.offset(0.0f, -4.9916f, 0.0125f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-2.5f, -3.0f, -4.0f, 5.0f, 5.0f, 5.0f), PartPose.offsetAndRotation(0.0f, -5.2929f, -8.1213f, 0.3927f, 0.0f, 0.0f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0f, -4.2929f, -0.1213f, 2.0f, 5.0f, 1.0f), PartPose.offset(1.5f, -3.7071f, -0.8787f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(26, 0).addBox(-1.0f, -4.2929f, -0.1213f, 2.0f, 5.0f, 1.0f), PartPose.offset(-1.5f, -3.7071f, -0.8787f));
        PartDefinition frontLegs = body.addOrReplaceChild("frontlegs", CubeListBuilder.create(), PartPose.offset(0.0f, -1.5349f, -6.3108f));
        frontLegs.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(36, 18).addBox(-0.9f, -1.0f, -0.9f, 2.0f, 4.0f, 2.0f), PartPose.offsetAndRotation(-2.0f, 1.9239f, 0.3827f, 0.3927f, 0.0f, 0.0f));
        frontLegs.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(44, 18).addBox(-1.0f, -1.0f, -1.0f, 2.0f, 4.0f, 2.0f), PartPose.offsetAndRotation(2.0f, 1.9239f, 0.4827f, 0.3927f, 0.0f, 0.0f));
        PartDefinition backLegs = root.addOrReplaceChild("backlegs", CubeListBuilder.create(), PartPose.offset(0.0f, 23.0f, 4.0f));
        PartDefinition rightBackLeg = backLegs.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(), PartPose.offset(-3.0f, 0.5f, 0.0f));
        rightBackLeg.addOrReplaceChild("right_haunch", CubeListBuilder.create().texOffs(20, 24).addBox(-1.0f, 0.0f, -5.0f, 2.0f, 1.0f, 6.0f), PartPose.offsetAndRotation(0.0f, -0.5f, 0.0f, 0.0f, 0.3927f, 0.0f));
        PartDefinition leftBackLeg = backLegs.addOrReplaceChild("left_hind_leg", CubeListBuilder.create(), PartPose.offset(3.0f, 0.5f, 0.0f));
        leftBackLeg.addOrReplaceChild("left_haunch", CubeListBuilder.create().texOffs(36, 24).addBox(-1.0f, 0.0f, -5.0f, 2.0f, 1.0f, 6.0f), PartPose.offsetAndRotation(0.0f, -0.5f, 0.0f, 0.0f, -0.3927f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

