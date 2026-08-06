/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class WitchModel
extends EntityModel<WitchRenderState>
implements HeadedModel,
VillagerLikeModel<WitchRenderState> {
    protected final ModelPart nose;
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart arms;

    public WitchModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.nose = this.head.getChild("nose");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.arms = root.getChild("arms");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = VillagerModel.createBodyModel();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -10.0f, -4.0f, 8.0f, 10.0f, 8.0f), PartPose.ZERO);
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 64).addBox(0.0f, 0.0f, 0.0f, 10.0f, 2.0f, 10.0f), PartPose.offset(-5.0f, -10.03125f, -5.0f));
        PartDefinition hat2 = hat.addOrReplaceChild("hat2", CubeListBuilder.create().texOffs(0, 76).addBox(0.0f, 0.0f, 0.0f, 7.0f, 4.0f, 7.0f), PartPose.offsetAndRotation(1.75f, -4.0f, 2.0f, -0.05235988f, 0.0f, 0.02617994f));
        PartDefinition hat3 = hat2.addOrReplaceChild("hat3", CubeListBuilder.create().texOffs(0, 87).addBox(0.0f, 0.0f, 0.0f, 4.0f, 4.0f, 4.0f), PartPose.offsetAndRotation(1.75f, -4.0f, 2.0f, -0.10471976f, 0.0f, 0.05235988f));
        hat3.addOrReplaceChild("hat4", CubeListBuilder.create().texOffs(0, 95).addBox(0.0f, 0.0f, 0.0f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.25f)), PartPose.offsetAndRotation(1.75f, -2.0f, 2.0f, -0.20943952f, 0.0f, 0.10471976f));
        PartDefinition nose = head.getChild("nose");
        nose.addOrReplaceChild("mole", CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, 3.0f, -6.75f, 1.0f, 1.0f, 1.0f, new CubeDeformation(-0.25f)), PartPose.offset(0.0f, -2.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(WitchRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662f) * 1.4f * state.walkAnimationSpeed * 0.5f;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662f + (float)Math.PI) * 1.4f * state.walkAnimationSpeed * 0.5f;
        float speed = 0.01f * (float)(state.entityId % 10);
        this.nose.xRot = Mth.sin(state.ageInTicks * speed) * 4.5f * ((float)Math.PI / 180);
        this.nose.zRot = Mth.cos(state.ageInTicks * speed) * 2.5f * ((float)Math.PI / 180);
        if (state.isHoldingItem) {
            this.nose.setPos(0.0f, 1.0f, -1.5f);
            this.nose.xRot = -0.9f;
        }
    }

    public ModelPart getNose() {
        return this.nose;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @Override
    public void translateToArms(WitchRenderState state, PoseStack outputPoseStack) {
        this.root.translateAndRotate(outputPoseStack);
        this.arms.translateAndRotate(outputPoseStack);
    }
}

