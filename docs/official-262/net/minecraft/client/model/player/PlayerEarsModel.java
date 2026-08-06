/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;

@Environment(value=EnvType.CLIENT)
public class PlayerEarsModel
extends PlayerModel {
    public PlayerEarsModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createEarsLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot().clearRecursively();
        PartDefinition head = root.getChild("head");
        CubeListBuilder earCube = CubeListBuilder.create().texOffs(24, 0).addBox(-3.0f, -6.0f, -1.0f, 6.0f, 6.0f, 1.0f, new CubeDeformation(1.0f));
        head.addOrReplaceChild("left_ear", earCube, PartPose.offset(-6.0f, -6.0f, 0.0f));
        head.addOrReplaceChild("right_ear", earCube, PartPose.offset(6.0f, -6.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

