/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.ambient;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.BatAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Environment(value=EnvType.CLIENT)
public class BatModel
extends EntityModel<BatRenderState> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightWingTip;
    private final ModelPart leftWingTip;
    private final ModelPart feet;
    private final KeyframeAnimation flyingAnimation;
    private final KeyframeAnimation restingAnimation;

    public BatModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightWing = this.body.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
        this.leftWing = this.body.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.feet = this.body.getChild("feet");
        this.flyingAnimation = BatAnimation.BAT_FLYING.bake(root);
        this.restingAnimation = BatAnimation.BAT_RESTING.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, 0.0f, -1.0f, 3.0f, 5.0f, 2.0f), PartPose.offset(0.0f, 17.0f, 0.0f));
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 7).addBox(-2.0f, -3.0f, -1.0f, 4.0f, 3.0f, 2.0f), PartPose.offset(0.0f, 17.0f, 0.0f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(1, 15).addBox(-2.5f, -4.0f, 0.0f, 3.0f, 5.0f, 0.0f), PartPose.offset(-1.5f, -2.0f, 0.0f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(8, 15).addBox(-0.1f, -3.0f, 0.0f, 3.0f, 5.0f, 0.0f), PartPose.offset(1.1f, -3.0f, 0.0f));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(12, 0).addBox(-2.0f, -2.0f, 0.0f, 2.0f, 7.0f, 0.0f), PartPose.offset(-1.5f, 0.0f, 0.0f));
        rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create().texOffs(16, 0).addBox(-6.0f, -2.0f, 0.0f, 6.0f, 8.0f, 0.0f), PartPose.offset(-2.0f, 0.0f, 0.0f));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(12, 7).addBox(0.0f, -2.0f, 0.0f, 2.0f, 7.0f, 0.0f), PartPose.offset(1.5f, 0.0f, 0.0f));
        leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().texOffs(16, 8).addBox(0.0f, -2.0f, 0.0f, 6.0f, 8.0f, 0.0f), PartPose.offset(2.0f, 0.0f, 0.0f));
        body.addOrReplaceChild("feet", CubeListBuilder.create().texOffs(16, 16).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 2.0f, 0.0f), PartPose.offset(0.0f, 5.0f, 0.0f));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(BatRenderState state) {
        super.setupAnim(state);
        if (state.isResting) {
            this.applyHeadRotation(state.yRot);
        }
        this.flyingAnimation.apply(state.flyAnimationState, state.ageInTicks);
        this.restingAnimation.apply(state.restAnimationState, state.ageInTicks);
    }

    private void applyHeadRotation(float yRot) {
        this.head.yRot = yRot * ((float)Math.PI / 180);
    }
}

