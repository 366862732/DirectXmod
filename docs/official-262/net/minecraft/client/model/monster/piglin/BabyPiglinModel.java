/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.piglin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.piglin.PiglinModel;

@Environment(value=EnvType.CLIENT)
public class BabyPiglinModel
extends PiglinModel {
    public BabyPiglinModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0f, -3.0f, -1.0f, 6.0f, 5.0f, 3.0f), PartPose.offset(0.0f, 18.0f, -0.5f));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(21, 30).addBox(-1.5f, -3.0f, -4.5f, 3.0f, 3.0f, 1.0f).texOffs(0, 0).addBox(-4.5f, -6.0f, -3.5f, 9.0f, 6.0f, 7.0f), PartPose.offset(0.0f, 15.0f, 0.0f));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f));
        PartDefinition leftear = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(4.2f, -4.0f, 0.0f));
        leftear.addOrReplaceChild("left_ear_r1", CubeListBuilder.create().texOffs(0, 21).addBox(-0.5f, -3.0f, -2.0f, 1.0f, 6.0f, 4.0f), PartPose.offsetAndRotation(1.0f, 1.75f, 0.0f, 0.0f, 0.0f, -0.6109f));
        PartDefinition rightear = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-4.2f, -4.0f, 0.0f));
        rightear.addOrReplaceChild("right_ear_r1", CubeListBuilder.create().texOffs(18, 13).addBox(-0.5f, -3.0f, -2.0f, 1.0f, 6.0f, 4.0f), PartPose.offsetAndRotation(-1.0f, 1.75f, 0.0f, 0.0f, 0.0f, 0.6109f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 13).addBox(-1.0f, 0.0f, -1.5f, 2.0f, 5.0f, 3.0f), PartPose.offset(4.0f, 15.0f, 0.0f));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(10, 30).addBox(-1.0f, 0.0f, -1.5f, 2.0f, 5.0f, 3.0f), PartPose.offset(-4.0f, 15.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(22, 23).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f), PartPose.offset(-1.5f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(10, 23).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f), PartPose.offset(1.5f, 20.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected float getDefaultEarAngleInDegrees() {
        return 5.0f;
    }
}

