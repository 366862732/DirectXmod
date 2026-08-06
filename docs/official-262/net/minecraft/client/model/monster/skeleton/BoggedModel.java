/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.skeleton;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.state.BoggedRenderState;

@Environment(value=EnvType.CLIENT)
public class BoggedModel
extends SkeletonModel<BoggedRenderState> {
    private final ModelPart mushrooms;

    public BoggedModel(ModelPart root) {
        super(root);
        this.mushrooms = root.getChild("head").getChild("mushrooms");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        SkeletonModel.createDefaultSkeletonMesh(root);
        PartDefinition mushrooms = root.getChild("head").addOrReplaceChild("mushrooms", CubeListBuilder.create(), PartPose.ZERO);
        mushrooms.addOrReplaceChild("red_mushroom_1", CubeListBuilder.create().texOffs(50, 16).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 4.0f, 0.0f), PartPose.offsetAndRotation(3.0f, -8.0f, 3.0f, 0.0f, 0.7853982f, 0.0f));
        mushrooms.addOrReplaceChild("red_mushroom_2", CubeListBuilder.create().texOffs(50, 16).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 4.0f, 0.0f), PartPose.offsetAndRotation(3.0f, -8.0f, 3.0f, 0.0f, 2.3561945f, 0.0f));
        mushrooms.addOrReplaceChild("brown_mushroom_1", CubeListBuilder.create().texOffs(50, 22).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 4.0f, 0.0f), PartPose.offsetAndRotation(-3.0f, -8.0f, -3.0f, 0.0f, 0.7853982f, 0.0f));
        mushrooms.addOrReplaceChild("brown_mushroom_2", CubeListBuilder.create().texOffs(50, 22).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 4.0f, 0.0f), PartPose.offsetAndRotation(-3.0f, -8.0f, -3.0f, 0.0f, 2.3561945f, 0.0f));
        mushrooms.addOrReplaceChild("brown_mushroom_3", CubeListBuilder.create().texOffs(50, 28).addBox(-3.0f, -4.0f, 0.0f, 6.0f, 4.0f, 0.0f), PartPose.offsetAndRotation(-2.0f, -1.0f, 4.0f, -1.5707964f, 0.0f, 0.7853982f));
        mushrooms.addOrReplaceChild("brown_mushroom_4", CubeListBuilder.create().texOffs(50, 28).addBox(-3.0f, -4.0f, 0.0f, 6.0f, 4.0f, 0.0f), PartPose.offsetAndRotation(-2.0f, -1.0f, 4.0f, -1.5707964f, 0.0f, 2.3561945f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(BoggedRenderState state) {
        super.setupAnim(state);
        this.mushrooms.visible = !state.isSheared;
    }
}

