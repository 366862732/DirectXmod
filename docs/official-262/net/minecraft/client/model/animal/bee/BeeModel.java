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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class BeeModel
extends EntityModel<BeeRenderState> {
    protected static final String BONE = "bone";
    protected static final String STINGER = "stinger";
    protected static final String FRONT_LEGS = "front_legs";
    protected static final String MIDDLE_LEGS = "middle_legs";
    protected static final String BACK_LEGS = "back_legs";
    protected final ModelPart bone;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart frontLeg;
    private final ModelPart midLeg;
    private final ModelPart backLeg;
    private final ModelPart stinger;

    public BeeModel(ModelPart root) {
        super(root);
        this.bone = root.getChild(BONE);
        ModelPart body = this.bone.getChild("body");
        this.stinger = body.getChild(STINGER);
        this.rightWing = this.bone.getChild("right_wing");
        this.leftWing = this.bone.getChild("left_wing");
        this.frontLeg = this.bone.getChild(FRONT_LEGS);
        this.midLeg = this.bone.getChild(MIDDLE_LEGS);
        this.backLeg = this.bone.getChild(BACK_LEGS);
    }

    @Override
    public void setupAnim(BeeRenderState state) {
        float rollAmount;
        float speed;
        super.setupAnim(state);
        this.stinger.visible = state.hasStinger;
        if (!state.isOnGround) {
            speed = state.ageInTicks * 120.32113f * ((float)Math.PI / 180);
            this.rightWing.yRot = 0.0f;
            this.rightWing.zRot = Mth.cos(speed) * (float)Math.PI * 0.15f;
            this.leftWing.xRot = this.rightWing.xRot;
            this.leftWing.yRot = this.rightWing.yRot;
            this.leftWing.zRot = -this.rightWing.zRot;
            this.frontLeg.xRot = 0.7853982f;
            this.midLeg.xRot = 0.7853982f;
            this.backLeg.xRot = 0.7853982f;
        }
        if (!state.isAngry && !state.isOnGround) {
            speed = Mth.cos(state.ageInTicks * 0.18f);
            this.bobUpAndDown(speed, state.ageInTicks);
        }
        if ((rollAmount = state.rollAmount) > 0.0f) {
            this.bone.xRot = Mth.rotLerpRad(rollAmount, this.bone.xRot, 3.0915928f);
        }
    }

    protected void bobUpAndDown(float speed, float ageInTicks) {
        this.bone.xRot = 0.1f + speed * (float)Math.PI * 0.025f;
        this.bone.y -= Mth.cos(ageInTicks * 0.18f) * 0.9f;
        this.frontLeg.xRot = -speed * (float)Math.PI * 0.1f + 0.3926991f;
        this.backLeg.xRot = -speed * (float)Math.PI * 0.05f + 0.7853982f;
    }
}

