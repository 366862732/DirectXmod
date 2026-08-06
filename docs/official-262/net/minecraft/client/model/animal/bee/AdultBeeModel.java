/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.bee;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class AdultBeeModel
extends BeeModel {
    private static final String LEFT_ANTENNA = "left_antenna";
    private static final String RIGHT_ANTENNA = "right_antenna";
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;

    public AdultBeeModel(ModelPart root) {
        super(root);
        ModelPart body = this.bone.getChild("body");
        this.leftAntenna = body.getChild(LEFT_ANTENNA);
        this.rightAntenna = body.getChild(RIGHT_ANTENNA);
    }

    @Override
    protected void bobUpAndDown(float speed, float ageInTicks) {
        super.bobUpAndDown(speed, ageInTicks);
        this.leftAntenna.xRot = speed * (float)Math.PI * 0.03f;
        this.rightAntenna.xRot = speed * (float)Math.PI * 0.03f;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0f, 19.0f, 0.0f));
        PartDefinition body = bone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -4.0f, -5.0f, 7.0f, 7.0f, 10.0f), PartPose.ZERO);
        body.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(26, 7).addBox(0.0f, -1.0f, 5.0f, 0.0f, 1.0f, 2.0f), PartPose.ZERO);
        body.addOrReplaceChild(LEFT_ANTENNA, CubeListBuilder.create().texOffs(2, 0).addBox(1.5f, -2.0f, -3.0f, 1.0f, 2.0f, 3.0f), PartPose.offset(0.0f, -2.0f, -5.0f));
        body.addOrReplaceChild(RIGHT_ANTENNA, CubeListBuilder.create().texOffs(2, 3).addBox(-2.5f, -2.0f, -3.0f, 1.0f, 2.0f, 3.0f), PartPose.offset(0.0f, -2.0f, -5.0f));
        CubeDeformation wingDeformation = new CubeDeformation(0.001f);
        bone.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 18).addBox(-9.0f, 0.0f, 0.0f, 9.0f, 0.0f, 6.0f, wingDeformation), PartPose.offsetAndRotation(-1.5f, -4.0f, -3.0f, 0.0f, -0.2618f, 0.0f));
        bone.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(0.0f, 0.0f, 0.0f, 9.0f, 0.0f, 6.0f, wingDeformation), PartPose.offsetAndRotation(1.5f, -4.0f, -3.0f, 0.0f, 0.2618f, 0.0f));
        bone.addOrReplaceChild("front_legs", CubeListBuilder.create().addBox("front_legs", -5.0f, 0.0f, 0.0f, 7, 2, 0, 26, 1), PartPose.offset(1.5f, 3.0f, -2.0f));
        bone.addOrReplaceChild("middle_legs", CubeListBuilder.create().addBox("middle_legs", -5.0f, 0.0f, 0.0f, 7, 2, 0, 26, 3), PartPose.offset(1.5f, 3.0f, 0.0f));
        bone.addOrReplaceChild("back_legs", CubeListBuilder.create().addBox("back_legs", -5.0f, 0.0f, 0.0f, 7, 2, 0, 26, 5), PartPose.offset(1.5f, 3.0f, 2.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

