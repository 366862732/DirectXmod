/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.UndeadRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;

@Environment(value=EnvType.CLIENT)
public class AnimationUtils {
    public static void animateCrossbowHold(ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean holdingInRightArm) {
        ModelPart holdingArm = holdingInRightArm ? rightArm : leftArm;
        ModelPart shootingArm = holdingInRightArm ? leftArm : rightArm;
        holdingArm.yRot = (holdingInRightArm ? -0.3f : 0.3f) + head.yRot;
        shootingArm.yRot = (holdingInRightArm ? 0.6f : -0.6f) + head.yRot;
        holdingArm.xRot = -1.5707964f + head.xRot + 0.1f;
        shootingArm.xRot = -1.5f + head.xRot;
    }

    public static void animateCrossbowCharge(ModelPart rightArm, ModelPart leftArm, float maxCrossbowChargeDuration, float ticksUsingItem, boolean holdingInRightArm) {
        ModelPart holdingArm = holdingInRightArm ? rightArm : leftArm;
        ModelPart pullingArm = holdingInRightArm ? leftArm : rightArm;
        holdingArm.yRot = holdingInRightArm ? -0.8f : 0.8f;
        pullingArm.xRot = holdingArm.xRot = -0.97079635f;
        float useTicks = Mth.clamp(ticksUsingItem, 0.0f, maxCrossbowChargeDuration);
        float lerpAlpha = useTicks / maxCrossbowChargeDuration;
        pullingArm.yRot = Mth.lerp(lerpAlpha, 0.4f, 0.85f) * (float)(holdingInRightArm ? 1 : -1);
        pullingArm.xRot = Mth.lerp(lerpAlpha, pullingArm.xRot, -1.5707964f);
    }

    public static void swingWeaponDown(ModelPart rightArm, ModelPart leftArm, HumanoidArm mainArm, float attackTime, float ageInTicks) {
        float attack2 = Mth.sin(attackTime * (float)Math.PI);
        float attack = Mth.sin((1.0f - (1.0f - attackTime) * (1.0f - attackTime)) * (float)Math.PI);
        rightArm.zRot = 0.0f;
        leftArm.zRot = 0.0f;
        rightArm.yRot = 0.15707964f;
        leftArm.yRot = -0.15707964f;
        if (mainArm == HumanoidArm.RIGHT) {
            rightArm.xRot = -1.8849558f + Mth.cos(ageInTicks * 0.09f) * 0.15f;
            leftArm.xRot = -0.0f + Mth.cos(ageInTicks * 0.19f) * 0.5f;
            rightArm.xRot += attack2 * 2.2f - attack * 0.4f;
            leftArm.xRot += attack2 * 1.2f - attack * 0.4f;
        } else {
            rightArm.xRot = -0.0f + Mth.cos(ageInTicks * 0.19f) * 0.5f;
            leftArm.xRot = -1.8849558f + Mth.cos(ageInTicks * 0.09f) * 0.15f;
            rightArm.xRot += attack2 * 1.2f - attack * 0.4f;
            leftArm.xRot += attack2 * 2.2f - attack * 0.4f;
        }
        AnimationUtils.bobArms(rightArm, leftArm, ageInTicks);
    }

    public static void bobModelPart(ModelPart modelPart, float ageInTicks, float scale) {
        modelPart.zRot += scale * (Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f);
        modelPart.xRot += scale * (Mth.sin(ageInTicks * 0.067f) * 0.05f);
    }

    public static void bobArms(ModelPart rightArm, ModelPart leftArm, float ageInTicks) {
        AnimationUtils.bobModelPart(rightArm, ageInTicks, 1.0f);
        AnimationUtils.bobModelPart(leftArm, ageInTicks, -1.0f);
    }

    public static <T extends UndeadRenderState> void animateZombieArms(ModelPart leftArm, ModelPart rightArm, boolean aggressive, T state) {
        boolean animateAttack;
        boolean bl = animateAttack = state.swingAnimationType != SwingAnimationType.STAB;
        if (animateAttack) {
            boolean raiseArms;
            boolean bl2 = raiseArms = !state.isBaby || state.getMainHandItemStack() == ItemStack.EMPTY;
            float armDrop = raiseArms ? (float)(-Math.PI) / (aggressive ? 1.5f : 2.25f) : 0.0f;
            AnimationUtils.animateAttackArms(leftArm, rightArm, state.attackTime, raiseArms, armDrop);
        }
        AnimationUtils.bobArms(rightArm, leftArm, state.ageInTicks);
    }

    private static void animateAttackArms(ModelPart leftArm, ModelPart rightArm, float attackTime, boolean negateArmRotation, float armDrop) {
        float attackYRotModifier = (negateArmRotation ? 1.0f : -1.0f) * Mth.sin(attackTime * (float)Math.PI);
        float attackXRotModifier = Mth.sin((1.0f - (1.0f - attackTime) * (1.0f - attackTime)) * (float)Math.PI);
        float xRot = armDrop + attackYRotModifier * 1.2f - attackXRotModifier * 0.4f;
        float yRot = 0.1f - attackYRotModifier * 0.6f;
        rightArm.xRot = xRot;
        rightArm.yRot = negateArmRotation ? -yRot : yRot;
        rightArm.zRot = 0.0f;
        leftArm.xRot = xRot;
        leftArm.yRot = negateArmRotation ? yRot : -yRot;
        leftArm.zRot = 0.0f;
    }
}

