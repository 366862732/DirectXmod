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
import net.minecraft.client.animation.definitions.BabyRabbitAnimation;
import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class BabyRabbitModel
extends RabbitModel {
    public BabyRabbitModel(ModelPart root) {
        super(root, BabyRabbitAnimation.HOP, BabyRabbitAnimation.IDLE_HEAD_TILT);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0f, 23.0f, 1.6f));
        body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0f, -2.0f, -3.0f, 4.0f, 3.0f, 6.0f), PartPose.offsetAndRotation(0.0f, -2.0f, -1.6f, -0.5236f, 0.0f, 0.0f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0f, -2.2f, 2.0f));
        tail.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(0, 21).addBox(-1.4f, -2.0268f, -1.0177f, 3.0f, 3.0f, 3.0f), PartPose.offsetAndRotation(-0.1f, 0.0f, 0.0f, -0.5236f, 0.0f, 0.0f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -3.0f, -3.0f, 5.0f, 4.0f, 4.0f), PartPose.offset(0.0f, -5.0f, -2.6f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0f, -3.5f, -0.5f, 2.0f, 4.0f, 1.0f), PartPose.offset(-1.5f, -3.5f, -0.5f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0f, -3.5f, -0.5f, 2.0f, 4.0f, 1.0f), PartPose.offset(1.5f, -3.5f, -0.5f));
        PartDefinition frontLegs = body.addOrReplaceChild("frontlegs", CubeListBuilder.create(), PartPose.offset(0.0f, -2.5f, -2.6f));
        PartDefinition leftFrontLeg = frontLegs.addOrReplaceChild("left_front_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(1.0f, 1.0f, -0.5f, 0.3927f, 0.0f, 0.0f));
        leftFrontLeg.addOrReplaceChild("left_front_leg_r1", CubeListBuilder.create().texOffs(18, 8).addBox(-0.5f, -1.5f, -0.5f, 1.0f, 3.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 1.0f, 0.0f, -0.3927f, 0.0f, 0.0f));
        PartDefinition rightFrontLeg = frontLegs.addOrReplaceChild("right_front_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0f, 1.0f, -0.5f, 0.3927f, 0.0f, 0.0f));
        rightFrontLeg.addOrReplaceChild("right_front_leg_r1", CubeListBuilder.create().texOffs(14, 8).addBox(-0.5f, -1.5f, -0.5f, 1.0f, 3.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 1.0f, 0.0f, -0.3927f, 0.0f, 0.0f));
        PartDefinition backLegs = root.addOrReplaceChild("backlegs", CubeListBuilder.create(), PartPose.offset(0.0f, 23.0f, 2.0f));
        PartDefinition leftBackLeg = backLegs.addOrReplaceChild("left_hind_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(1.5f, 0.5f, 0.5f, 0.0f, 3.1416f, 0.0f));
        leftBackLeg.addOrReplaceChild("left_haunch", CubeListBuilder.create().texOffs(10, 17).addBox(-2.0f, -0.5f, 0.0f, 2.0f, 1.0f, 3.0f), PartPose.offsetAndRotation(1.0f, 0.0f, 0.5f, 0.0f, -0.7854f, 0.0f));
        PartDefinition rightBackLeg = backLegs.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.5f, 0.5f, 0.5f, 0.0f, 3.1416f, 0.0f));
        rightBackLeg.addOrReplaceChild("right_haunch", CubeListBuilder.create().texOffs(0, 17).addBox(-2.0f, -0.5f, 0.0f, 2.0f, 1.0f, 3.0f), PartPose.offsetAndRotation(0.5f, 0.0f, -0.9f, 0.0f, 0.7854f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }
}

