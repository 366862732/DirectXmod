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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class WolfModel
extends EntityModel<WolfRenderState> {
    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightHindLeg;
    protected final ModelPart leftHindLeg;
    protected final ModelPart rightFrontLeg;
    protected final ModelPart leftFrontLeg;
    protected final ModelPart tail;

    public WolfModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.tail = root.getChild("tail");
    }

    @Override
    public void setupAnim(WolfRenderState state) {
        super.setupAnim(state);
        float animationPos = state.walkAnimationPos;
        float animationSpeed = state.walkAnimationSpeed;
        this.tail.yRot = state.isAngry ? 0.0f : Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        if (state.isSitting) {
            this.setSittingPose(state);
        } else {
            this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
            this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
            this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
            this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        }
        this.shakeOffWater(state);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.tail.xRot = state.tailAngle;
    }

    protected void shakeOffWater(WolfRenderState state) {
        this.body.zRot = state.getBodyRollAngle(-0.16f);
    }

    protected void setSittingPose(WolfRenderState state) {
        float ageScale = state.ageScale;
        this.body.y += 4.0f * ageScale;
        this.body.z -= 2.0f * ageScale;
        this.body.xRot = 0.7853982f;
        this.tail.y += 9.0f * ageScale;
        this.tail.z -= 2.0f * ageScale;
        this.rightHindLeg.y += 6.7f * ageScale;
        this.rightHindLeg.z -= 5.0f * ageScale;
        this.rightHindLeg.xRot = 4.712389f;
        this.leftHindLeg.y += 6.7f * ageScale;
        this.leftHindLeg.z -= 5.0f * ageScale;
        this.leftHindLeg.xRot = 4.712389f;
        this.rightFrontLeg.xRot = 5.811947f;
        this.rightFrontLeg.x += 0.01f * ageScale;
        this.rightFrontLeg.y += 1.0f * ageScale;
        this.leftFrontLeg.xRot = 5.811947f;
        this.leftFrontLeg.x -= 0.01f * ageScale;
        this.leftFrontLeg.y += 1.0f * ageScale;
    }
}

