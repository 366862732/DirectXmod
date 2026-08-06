/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.chicken;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;

@Environment(value=EnvType.CLIENT)
public class AdultChickenModel
extends ChickenModel {
    private static final String RED_THING = "red_thing";
    private final ModelPart head;

    public AdultChickenModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
    }

    @Override
    public void setupAnim(ChickenRenderState state) {
        super.setupAnim(state);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = AdultChickenModel.createBaseChickenModel();
        return LayerDefinition.create(mesh, 64, 32);
    }

    protected static MeshDefinition createBaseChickenModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -6.0f, -2.0f, 4.0f, 6.0f, 3.0f), PartPose.offset(0.0f, 15.0f, -4.0f));
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0f, -4.0f, -4.0f, 4.0f, 2.0f, 2.0f), PartPose.ZERO);
        head.addOrReplaceChild(RED_THING, CubeListBuilder.create().texOffs(14, 4).addBox(-1.0f, -2.0f, -3.0f, 2.0f, 2.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-3.0f, -4.0f, -3.0f, 6.0f, 8.0f, 6.0f), PartPose.offsetAndRotation(0.0f, 16.0f, 0.0f, 1.5707964f, 0.0f, 0.0f));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(26, 0).addBox(-1.0f, 0.0f, -3.0f, 3.0f, 5.0f, 3.0f);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-2.0f, 19.0f, 1.0f));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.0f, 19.0f, 1.0f));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(24, 13).addBox(0.0f, 0.0f, -3.0f, 1.0f, 4.0f, 6.0f), PartPose.offset(-4.0f, 13.0f, 0.0f));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(24, 13).addBox(-1.0f, 0.0f, -3.0f, 1.0f, 4.0f, 6.0f), PartPose.offset(4.0f, 13.0f, 0.0f));
        return mesh;
    }
}

