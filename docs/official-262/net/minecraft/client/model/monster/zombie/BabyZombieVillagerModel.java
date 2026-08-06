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
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;

@Environment(value=EnvType.CLIENT)
public class BabyZombieVillagerModel<S extends ZombieVillagerRenderState>
extends ZombieVillagerModel<S> {
    public BabyZombieVillagerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-2.0f, -2.75f, -1.5f, 4.0f, 5.0f, 3.0f).texOffs(16, 22).addBox(-2.0f, -2.75f, -1.5f, 4.0f, 6.0f, 3.0f, new CubeDeformation(0.1f)), PartPose.offset(0.0f, 18.75f, 0.0f));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -3.5f, 8.0f, 8.0f, 7.0f), PartPose.offset(0.0f, 16.0f, 0.0f));
        head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 31).addBox(-4.0f, -4.0f, -3.5f, 8.0f, 8.0f, 7.0f, new CubeDeformation(0.3f)), PartPose.offset(0.0f, -4.0f, 0.0f));
        head.addOrReplaceChild("hat_rim", CubeListBuilder.create().texOffs(0, 46).addBox(-7.0f, -0.5f, -6.0f, 14.0f, 1.0f, 12.0f), PartPose.offset(0.0f, -4.5f, 0.0f));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(23, 0).addBox(-1.0f, -1.0f, -0.5f, 2.0f, 2.0f, 1.0f), PartPose.offset(0.0f, -1.0f, -4.0f));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 15).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(-3.0f, 15.5f, 0.0f));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 15).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offset(3.0f, 15.5f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(-1.0f, 21.5f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(1.0f, 21.5f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createNoHatLayer() {
        return BabyZombieVillagerModel.createBodyLayer().apply(mesh -> {
            mesh.getRoot().clearChild("head").clearRecursively();
            return mesh;
        });
    }
}

