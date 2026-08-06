/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.armorstand;

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
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;

@Environment(value=EnvType.CLIENT)
public class ArmorStandArmorModel
extends HumanoidModel<ArmorStandRenderState> {
    public ArmorStandArmorModel(ModelPart root) {
        super(root);
    }

    public static ArmorModelSet<LayerDefinition> createArmorLayerSet(CubeDeformation innerDeformation, CubeDeformation outerDeformation) {
        return ArmorStandArmorModel.createArmorMeshSet(ArmorStandArmorModel::createBaseMesh, ADULT_ARMOR_PARTS_PER_SLOT, innerDeformation, outerDeformation).map(mesh -> LayerDefinition.create(mesh, 64, 32));
    }

    private static MeshDefinition createBaseMesh(CubeDeformation g) {
        MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, g), PartPose.offset(0.0f, 1.0f, 0.0f));
        head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, g.extend(0.5f)), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, g.extend(-0.1f)), PartPose.offset(-1.9f, 11.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, g.extend(-0.1f)), PartPose.offset(1.9f, 11.0f, 0.0f));
        return mesh;
    }

    @Override
    public void setupAnim(ArmorStandRenderState state) {
        super.setupAnim(state);
        this.head.xRot = (float)Math.PI / 180 * state.headPose.x();
        this.head.yRot = (float)Math.PI / 180 * state.headPose.y();
        this.head.zRot = (float)Math.PI / 180 * state.headPose.z();
        this.body.xRot = (float)Math.PI / 180 * state.bodyPose.x();
        this.body.yRot = (float)Math.PI / 180 * state.bodyPose.y();
        this.body.zRot = (float)Math.PI / 180 * state.bodyPose.z();
        this.leftArm.xRot = (float)Math.PI / 180 * state.leftArmPose.x();
        this.leftArm.yRot = (float)Math.PI / 180 * state.leftArmPose.y();
        this.leftArm.zRot = (float)Math.PI / 180 * state.leftArmPose.z();
        this.rightArm.xRot = (float)Math.PI / 180 * state.rightArmPose.x();
        this.rightArm.yRot = (float)Math.PI / 180 * state.rightArmPose.y();
        this.rightArm.zRot = (float)Math.PI / 180 * state.rightArmPose.z();
        this.leftLeg.xRot = (float)Math.PI / 180 * state.leftLegPose.x();
        this.leftLeg.yRot = (float)Math.PI / 180 * state.leftLegPose.y();
        this.leftLeg.zRot = (float)Math.PI / 180 * state.leftLegPose.z();
        this.rightLeg.xRot = (float)Math.PI / 180 * state.rightLegPose.x();
        this.rightLeg.yRot = (float)Math.PI / 180 * state.rightLegPose.y();
        this.rightLeg.zRot = (float)Math.PI / 180 * state.rightLegPose.z();
    }
}

