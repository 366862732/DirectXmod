/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.npc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.VillagerModel;

@Environment(value=EnvType.CLIENT)
public class BabyVillagerModel
extends VillagerModel {
    public BabyVillagerModel(ModelPart root) {
        super(root);
    }

    public static MeshDefinition createNoHatModel() {
        MeshDefinition mesh = BabyVillagerModel.createBodyModel();
        mesh.getRoot().clearChild("head").clearRecursively();
        return mesh;
    }

    public static MeshDefinition createBodyModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0f, 17.5f, 0.0f));
        arms.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(36, 15).addBox(-1.0f, -2.4925f, -1.8401f, 2.0f, 4.0f, 2.0f).texOffs(16, 15).addBox(5.0f, -2.4925f, -1.8401f, 2.0f, 4.0f, 2.0f), PartPose.offsetAndRotation(-3.0f, 1.4025f, -0.9599f, -1.0472f, 0.0f, 0.0f));
        arms.addOrReplaceChild("middlearm_r1", CubeListBuilder.create().texOffs(24, 17).addBox(-2.0f, -0.9924f, -0.9825f, 4.0f, 2.0f, 2.0f), PartPose.offsetAndRotation(0.0f, 0.9024f, -1.8175f, -1.0472f, 0.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(-1.0f, 21.5f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(1.0f, 21.5f, 0.0f));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -3.5f, 8.0f, 8.0f, 7.0f), PartPose.offset(0.0f, 16.0f, 0.0f));
        head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 30).addBox(-4.0f, -4.0f, -3.5f, 8.0f, 8.0f, 7.0f, new CubeDeformation(0.3f)), PartPose.offset(0.0f, -4.0f, 0.0f));
        head.addOrReplaceChild("hat_rim", CubeListBuilder.create().texOffs(0, 45).addBox(-7.0f, -0.5f, -6.0f, 14.0f, 1.0f, 12.0f), PartPose.offset(0.0f, -4.5f, 0.0f));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(23, 0).addBox(-1.0f, 0.0f, -0.5f, 2.0f, 2.0f, 1.0f), PartPose.offset(0.0f, -2.0f, -4.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-2.0f, -2.75f, -1.5f, 4.0f, 5.0f, 3.0f), PartPose.offset(0.0f, 18.75f, 0.0f));
        root.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(16, 21).addBox(-2.5f, -8.0f, -1.5f, 4.0f, 6.0f, 3.0f, new CubeDeformation(0.2f)), PartPose.offset(0.5f, 24.0f, 0.0f));
        return mesh;
    }
}

