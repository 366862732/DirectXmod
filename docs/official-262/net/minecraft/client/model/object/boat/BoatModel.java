/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.boat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.boat.AbstractBoatModel;

@Environment(value=EnvType.CLIENT)
public class BoatModel
extends AbstractBoatModel {
    private static final int BOTTOM_WIDTH = 28;
    private static final int WIDTH = 32;
    private static final int DEPTH = 6;
    private static final int LENGTH = 20;
    private static final int Y_OFFSET = 4;
    private static final String WATER_PATCH = "water_patch";
    private static final String BACK = "back";
    private static final String FRONT = "front";
    private static final String RIGHT = "right";
    private static final String LEFT = "left";

    public BoatModel(ModelPart root) {
        super(root);
    }

    private static void addCommonParts(PartDefinition root) {
        int halfWidth = 16;
        int halfBottomWidth = 14;
        int halfLength = 10;
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0f, -9.0f, -3.0f, 28.0f, 16.0f, 3.0f), PartPose.offsetAndRotation(0.0f, 3.0f, 1.0f, 1.5707964f, 0.0f, 0.0f));
        root.addOrReplaceChild(BACK, CubeListBuilder.create().texOffs(0, 19).addBox(-13.0f, -7.0f, -1.0f, 18.0f, 6.0f, 2.0f), PartPose.offsetAndRotation(-15.0f, 4.0f, 4.0f, 0.0f, 4.712389f, 0.0f));
        root.addOrReplaceChild(FRONT, CubeListBuilder.create().texOffs(0, 27).addBox(-8.0f, -7.0f, -1.0f, 16.0f, 6.0f, 2.0f), PartPose.offsetAndRotation(15.0f, 4.0f, 0.0f, 0.0f, 1.5707964f, 0.0f));
        root.addOrReplaceChild(RIGHT, CubeListBuilder.create().texOffs(0, 35).addBox(-14.0f, -7.0f, -1.0f, 28.0f, 6.0f, 2.0f), PartPose.offsetAndRotation(0.0f, 4.0f, -9.0f, 0.0f, (float)Math.PI, 0.0f));
        root.addOrReplaceChild(LEFT, CubeListBuilder.create().texOffs(0, 43).addBox(-14.0f, -7.0f, -1.0f, 28.0f, 6.0f, 2.0f), PartPose.offset(0.0f, 4.0f, 9.0f));
        int totalLength = 20;
        int bladeLength = 7;
        int bladeWidth = 6;
        float pivot = -5.0f;
        root.addOrReplaceChild("left_paddle", CubeListBuilder.create().texOffs(62, 0).addBox(-1.0f, 0.0f, -5.0f, 2.0f, 2.0f, 18.0f).addBox(-1.001f, -3.0f, 8.0f, 1.0f, 6.0f, 7.0f), PartPose.offsetAndRotation(3.0f, -5.0f, 9.0f, 0.0f, 0.0f, 0.19634955f));
        root.addOrReplaceChild("right_paddle", CubeListBuilder.create().texOffs(62, 20).addBox(-1.0f, 0.0f, -5.0f, 2.0f, 2.0f, 18.0f).addBox(0.001f, -3.0f, 8.0f, 1.0f, 6.0f, 7.0f), PartPose.offsetAndRotation(3.0f, -5.0f, -9.0f, 0.0f, (float)Math.PI, 0.19634955f));
    }

    public static LayerDefinition createBoatModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        BoatModel.addCommonParts(root);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition createChestBoatModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        BoatModel.addCommonParts(root);
        root.addOrReplaceChild("chest_bottom", CubeListBuilder.create().texOffs(0, 76).addBox(0.0f, 0.0f, 0.0f, 12.0f, 8.0f, 12.0f), PartPose.offsetAndRotation(-2.0f, -5.0f, -6.0f, 0.0f, -1.5707964f, 0.0f));
        root.addOrReplaceChild("chest_lid", CubeListBuilder.create().texOffs(0, 59).addBox(0.0f, 0.0f, 0.0f, 12.0f, 4.0f, 12.0f), PartPose.offsetAndRotation(-2.0f, -9.0f, -6.0f, 0.0f, -1.5707964f, 0.0f));
        root.addOrReplaceChild("chest_lock", CubeListBuilder.create().texOffs(0, 59).addBox(0.0f, 0.0f, 0.0f, 2.0f, 4.0f, 1.0f), PartPose.offsetAndRotation(-1.0f, -6.0f, -1.0f, 0.0f, -1.5707964f, 0.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createWaterPatch() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(WATER_PATCH, CubeListBuilder.create().texOffs(0, 0).addBox(-14.0f, -9.0f, -3.0f, 28.0f, 16.0f, 3.0f), PartPose.offsetAndRotation(0.0f, -3.0f, 1.0f, 1.5707964f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 0, 0);
    }
}

