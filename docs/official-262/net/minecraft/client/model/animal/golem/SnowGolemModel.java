/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.golem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class SnowGolemModel
extends EntityModel<LivingEntityRenderState> {
    private static final String UPPER_BODY = "upper_body";
    private final ModelPart upperBody;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public SnowGolemModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.upperBody = root.getChild(UPPER_BODY);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float yOffset = 4.0f;
        CubeDeformation deformation = new CubeDeformation(-0.5f);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, deformation), PartPose.offset(0.0f, 4.0f, 0.0f));
        CubeListBuilder arm = CubeListBuilder.create().texOffs(32, 0).addBox(-1.0f, 0.0f, -1.0f, 12.0f, 2.0f, 2.0f, deformation);
        root.addOrReplaceChild("left_arm", arm, PartPose.offsetAndRotation(5.0f, 6.0f, 1.0f, 0.0f, 0.0f, 1.0f));
        root.addOrReplaceChild("right_arm", arm, PartPose.offsetAndRotation(-5.0f, 6.0f, -1.0f, 0.0f, (float)Math.PI, -1.0f));
        root.addOrReplaceChild(UPPER_BODY, CubeListBuilder.create().texOffs(0, 16).addBox(-5.0f, -10.0f, -5.0f, 10.0f, 10.0f, 10.0f, deformation), PartPose.offset(0.0f, 13.0f, 0.0f));
        root.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(0, 36).addBox(-6.0f, -12.0f, -6.0f, 12.0f, 12.0f, 12.0f, deformation), PartPose.offset(0.0f, 24.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.upperBody.yRot = state.yRot * ((float)Math.PI / 180) * 0.25f;
        float sin = Mth.sin(this.upperBody.yRot);
        float cos = Mth.cos(this.upperBody.yRot);
        this.leftArm.yRot = this.upperBody.yRot;
        this.rightArm.yRot = this.upperBody.yRot + (float)Math.PI;
        this.leftArm.x = cos * 5.0f;
        this.leftArm.z = -sin * 5.0f;
        this.rightArm.x = -cos * 5.0f;
        this.rightArm.z = sin * 5.0f;
    }

    public ModelPart getHead() {
        return this.head;
    }
}

