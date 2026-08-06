/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.wolf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WolfRenderState;

@Environment(value=EnvType.CLIENT)
public class AdultWolfModel
extends WolfModel {
    private static final String REAL_HEAD = "real_head";
    private static final String UPPER_BODY = "upper_body";
    private static final String REAL_TAIL = "real_tail";
    private final ModelPart realHead;
    private final ModelPart realTail;
    private final ModelPart upperBody;
    private static final int LEG_SIZE = 8;

    public AdultWolfModel(ModelPart root) {
        super(root);
        this.realHead = this.head.getChild(REAL_HEAD);
        this.upperBody = root.getChild(UPPER_BODY);
        this.realTail = this.tail.getChild(REAL_TAIL);
    }

    public static MeshDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float headHeight = 13.5f;
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.0f, 13.5f, -7.0f));
        head.addOrReplaceChild(REAL_HEAD, CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -3.0f, -2.0f, 6.0f, 6.0f, 4.0f, g).texOffs(16, 14).addBox(-2.0f, -5.0f, 0.0f, 2.0f, 2.0f, 1.0f, g).texOffs(16, 14).addBox(2.0f, -5.0f, 0.0f, 2.0f, 2.0f, 1.0f, g).texOffs(0, 10).addBox(-0.5f, -0.001f, -5.0f, 3.0f, 3.0f, 4.0f, g), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 14).addBox(-3.0f, -2.0f, -3.0f, 6.0f, 9.0f, 6.0f, g), PartPose.offsetAndRotation(0.0f, 14.0f, 2.0f, 1.5707964f, 0.0f, 0.0f));
        root.addOrReplaceChild(UPPER_BODY, CubeListBuilder.create().texOffs(21, 0).addBox(-3.0f, -3.0f, -3.0f, 8.0f, 6.0f, 7.0f, g), PartPose.offsetAndRotation(-1.0f, 14.0f, -3.0f, 1.5707964f, 0.0f, 0.0f));
        CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(0, 18).addBox(0.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, g);
        CubeListBuilder rightLeg = CubeListBuilder.create().mirror().texOffs(0, 18).addBox(0.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, g);
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-2.5f, 16.0f, 7.0f));
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(0.5f, 16.0f, 7.0f));
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-2.5f, 16.0f, -4.0f));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(0.5f, 16.0f, -4.0f));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0f, 12.0f, 8.0f, 0.62831855f, 0.0f, 0.0f));
        tail.addOrReplaceChild(REAL_TAIL, CubeListBuilder.create().texOffs(9, 18).addBox(0.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, g), PartPose.ZERO);
        return mesh;
    }

    @Override
    protected void setSittingPose(WolfRenderState state) {
        super.setSittingPose(state);
        this.upperBody.y += 2.0f;
        this.upperBody.xRot = 1.2566371f;
        this.upperBody.yRot = 0.0f;
    }

    @Override
    protected void shakeOffWater(WolfRenderState state) {
        super.shakeOffWater(state);
        this.realHead.zRot = state.headRollAngle + state.getBodyRollAngle(0.0f);
        this.upperBody.zRot = state.getBodyRollAngle(-0.08f);
        this.realTail.zRot = state.getBodyRollAngle(-0.2f);
    }
}

