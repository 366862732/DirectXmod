/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.piglin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.piglin.AbstractPiglinModel;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;

@Environment(value=EnvType.CLIENT)
public abstract class PiglinModel
extends AbstractPiglinModel<PiglinRenderState> {
    public PiglinModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(PiglinRenderState state) {
        super.setupAnim(state);
        float defaultAngle = 0.5235988f;
        float attackTime = state.attackTime;
        PiglinArmPose pose = state.armPose;
        if (pose == PiglinArmPose.DANCING) {
            float dancePos = state.ageInTicks / 60.0f;
            this.rightEar.zRot = 0.5235988f + (float)Math.PI / 180 * Mth.sin(dancePos * 30.0f) * 10.0f;
            this.leftEar.zRot = -0.5235988f - (float)Math.PI / 180 * Mth.cos(dancePos * 30.0f) * 10.0f;
            this.head.x += Mth.sin(dancePos * 10.0f);
            this.head.y += Mth.sin(dancePos * 40.0f) + 0.4f;
            this.rightArm.zRot = (float)Math.PI / 180 * (70.0f + Mth.cos(dancePos * 40.0f) * 10.0f);
            this.leftArm.zRot = this.rightArm.zRot * -1.0f;
            this.rightArm.y += Mth.sin(dancePos * 40.0f) * 0.5f - 0.5f;
            this.leftArm.y += Mth.sin(dancePos * 40.0f) * 0.5f + 0.5f;
            this.body.y += Mth.sin(dancePos * 40.0f) * 0.35f;
        } else if (pose == PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON && attackTime == 0.0f) {
            this.holdWeaponHigh(state);
        } else if (pose == PiglinArmPose.CROSSBOW_HOLD) {
            AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, state.mainArm == HumanoidArm.RIGHT);
        } else if (pose == PiglinArmPose.CROSSBOW_CHARGE) {
            AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, state.maxCrossbowChageDuration, state.ticksUsingItem, state.mainArm == HumanoidArm.RIGHT);
        } else if (pose == PiglinArmPose.ADMIRING_ITEM) {
            this.head.xRot = 0.5f;
            this.head.yRot = 0.0f;
            if (state.mainArm == HumanoidArm.LEFT) {
                this.rightArm.yRot = -0.5f;
                this.rightArm.xRot = -0.9f;
            } else {
                this.leftArm.yRot = 0.5f;
                this.leftArm.xRot = -0.9f;
            }
        }
    }

    @Override
    protected void setupAttackAnimation(PiglinRenderState state) {
        float attackTime = state.attackTime;
        if (attackTime > 0.0f && state.armPose == PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON) {
            AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, state.mainArm, attackTime, state.ageInTicks);
            return;
        }
        super.setupAttackAnimation(state);
    }

    private void holdWeaponHigh(PiglinRenderState state) {
        if (state.mainArm == HumanoidArm.LEFT) {
            this.leftArm.xRot = -1.8f;
        } else {
            this.rightArm.xRot = -1.8f;
        }
    }
}

