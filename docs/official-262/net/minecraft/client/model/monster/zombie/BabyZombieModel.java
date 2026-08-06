/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.zombie;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

@Environment(value=EnvType.CLIENT)
public class BabyZombieModel<S extends ZombieRenderState>
extends ZombieModel<S> {
    public BabyZombieModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-2.0f, -2.5f, -1.0f, 4.0f, 5.0f, 2.0f, g), PartPose.offset(0.0f, 17.5f, 0.0f));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(3, 3).addBox(-3.0f, -6.25f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).texOffs(35, 3).addBox(-3.0f, -6.15f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.25f)), PartPose.offset(0.0f, 15.25f, 0.0f));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 16).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f, g), PartPose.offset(-3.0f, 15.5f, 0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 16).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f, g), PartPose.offset(3.0f, 15.5f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 16).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, g), PartPose.offset(-1.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, g), PartPose.offset(1.0f, 20.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

