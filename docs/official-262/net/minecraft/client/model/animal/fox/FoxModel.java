/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.fox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class FoxModel
extends EntityModel<FoxRenderState> {
    public final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightHindLeg;
    protected final ModelPart leftHindLeg;
    protected final ModelPart rightFrontLeg;
    protected final ModelPart leftFrontLeg;
    protected final ModelPart tail;
    private float legMotionPos;

    public FoxModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.tail = this.body.getChild("tail");
    }

    @Override
    public void setupAnim(FoxRenderState state) {
        super.setupAnim(state);
        this.setWalkingPose(state);
        if (state.isCrouching) {
            this.setCrouchingPose(state);
        } else if (state.isSleeping) {
            this.setSleepingPose(state);
        } else if (state.isSitting) {
            this.setSittingPose(state);
        }
        if (state.isPouncing) {
            this.setPouncingPose(state);
        }
        if (!(state.isSleeping || state.isFaceplanted || state.isCrouching)) {
            this.head.xRot = state.xRot * ((float)Math.PI / 180);
            this.head.yRot = state.yRot * ((float)Math.PI / 180);
        }
        if (state.isSleeping) {
            this.head.xRot = 0.0f;
            this.head.yRot = -2.0943952f;
            this.head.zRot = Mth.cos(state.ageInTicks * 0.027f) / 22.0f;
        }
        if (state.isFaceplanted) {
            float legMoveFactor = 0.1f;
            this.legMotionPos += 0.67f;
            this.rightHindLeg.xRot = Mth.cos(this.legMotionPos * 0.4662f) * 0.1f;
            this.leftHindLeg.xRot = Mth.cos(this.legMotionPos * 0.4662f + (float)Math.PI) * 0.1f;
            this.rightFrontLeg.xRot = Mth.cos(this.legMotionPos * 0.4662f + (float)Math.PI) * 0.1f;
            this.leftFrontLeg.xRot = Mth.cos(this.legMotionPos * 0.4662f) * 0.1f;
        }
    }

    protected void setSittingPose(FoxRenderState state) {
        this.head.xRot = 0.0f;
        this.head.yRot = 0.0f;
    }

    protected void setSleepingPose(FoxRenderState state) {
        this.rightHindLeg.visible = false;
        this.leftHindLeg.visible = false;
        this.rightFrontLeg.visible = false;
        this.leftFrontLeg.visible = false;
    }

    protected void setWalkingPose(FoxRenderState state) {
        this.head.zRot = state.headRollAngle;
        this.rightHindLeg.visible = true;
        this.leftHindLeg.visible = true;
        this.rightFrontLeg.visible = true;
        this.leftFrontLeg.visible = true;
    }

    protected void setCrouchingPose(FoxRenderState state) {
        float wiggleAmount;
        this.body.xRot += 0.10471976f;
        this.head.y += state.crouchAmount * state.ageScale;
        this.body.yRot = wiggleAmount = Mth.cos(state.ageInTicks) * 0.05f;
        this.rightHindLeg.zRot = wiggleAmount;
        this.leftHindLeg.zRot = wiggleAmount;
        this.rightFrontLeg.zRot = wiggleAmount / 2.0f;
        this.leftFrontLeg.zRot = wiggleAmount / 2.0f;
    }

    protected void setPouncingPose(FoxRenderState state) {
    }
}

