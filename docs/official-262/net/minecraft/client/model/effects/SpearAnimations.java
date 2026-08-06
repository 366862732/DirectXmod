/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.model.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class SpearAnimations {
    private static float progress(float time, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0f, 1.0f);
    }

    public static <T extends HumanoidRenderState> void thirdPersonHandUse(ModelPart arm, ModelPart head, boolean holdingInRightArm, ItemStack item, T state) {
        int invert = holdingInRightArm ? 1 : -1;
        arm.yRot = -0.1f * (float)invert + head.yRot;
        arm.xRot = -1.5707964f + head.xRot + 0.8f;
        if (state.isFallFlying || state.swimAmount > 0.0f) {
            arm.xRot -= 0.9599311f;
        }
        arm.yRot = (float)Math.PI / 180 * Math.clamp(57.295776f * arm.yRot, -60.0f, 60.0f);
        arm.xRot = (float)Math.PI / 180 * Math.clamp(57.295776f * arm.xRot, -120.0f, 30.0f);
        if (state.ticksUsingItem <= 0.0f || state.isUsingItem && state.useItemHand != (holdingInRightArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)) {
            return;
        }
        KineticWeapon kineticWeapon = item.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return;
        }
        UseParams params = UseParams.fromKineticWeapon(kineticWeapon, state.ticksUsingItem);
        arm.yRot += (float)(-invert) * params.swayScaleFast() * ((float)Math.PI / 180) * params.swayIntensity() * 1.0f;
        arm.zRot += (float)(-invert) * params.swayScaleSlow() * ((float)Math.PI / 180) * params.swayIntensity() * 0.5f;
        arm.xRot += (float)Math.PI / 180 * (-40.0f * params.raiseProgressStart() + 30.0f * params.raiseProgressMiddle() + -20.0f * params.raiseProgressEnd() + 20.0f * params.lowerProgress() + 10.0f * params.raiseBackProgress() + 0.6f * params.swayScaleSlow() * params.swayIntensity());
    }

    public static <S extends ArmedEntityRenderState> void thirdPersonUseItem(S state, PoseStack poseStack, float timeHeld, HumanoidArm arm, ItemStack actualItem) {
        KineticWeapon kineticWeapon = actualItem.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null || timeHeld == 0.0f) {
            return;
        }
        float attack = Ease.inQuad(SpearAnimations.progress(state.attackTime, 0.05f, 0.2f));
        float retract = Ease.inOutExpo(SpearAnimations.progress(state.attackTime, 0.4f, 1.0f));
        UseParams params = UseParams.fromKineticWeapon(kineticWeapon, timeHeld);
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        float raiseProgressModified = 1.0f - Ease.outBack(1.0f - params.raiseProgress());
        float itemInHandDepth = 0.125f;
        float hitFeedback = SpearAnimations.hitFeedbackAmount(state.ticksSinceKineticHitFeedback);
        poseStack.translate(0.0, (double)(-hitFeedback) * 0.4, (double)(-kineticWeapon.forwardMovement() * (raiseProgressModified - params.raiseBackProgress()) + hitFeedback));
        poseStack.rotateAround((Quaternionfc)Axis.XN.rotationDegrees(70.0f * (params.raiseProgress() - params.raiseBackProgress()) - 40.0f * (attack - retract)), 0.0f, -0.03125f, 0.125f);
        poseStack.rotateAround((Quaternionfc)Axis.YP.rotationDegrees((float)(invert * 90) * (params.raiseProgress() - params.swayProgress() + 3.0f * retract + attack)), 0.0f, 0.0f, 0.125f);
    }

    public static <T extends HumanoidRenderState> void thirdPersonAttackHand(HumanoidModel<T> model, T state) {
        float attackTime = state.attackTime;
        HumanoidArm arm = state.attackArm;
        model.rightArm.yRot -= model.body.yRot;
        model.leftArm.yRot -= model.body.yRot;
        model.leftArm.xRot -= model.body.yRot;
        float prepare = Ease.inOutSine(SpearAnimations.progress(attackTime, 0.0f, 0.05f));
        float attack = Ease.inQuad(SpearAnimations.progress(attackTime, 0.05f, 0.2f));
        float retract = Ease.inOutExpo(SpearAnimations.progress(attackTime, 0.4f, 1.0f));
        model.getArm((HumanoidArm)arm).xRot += (90.0f * prepare - 120.0f * attack + 30.0f * retract) * ((float)Math.PI / 180);
    }

    public static <S extends ArmedEntityRenderState> void thirdPersonAttackItem(S state, PoseStack poseStack) {
        if (state.attackTime <= 0.0f) {
            return;
        }
        KineticWeapon kineticWeapon = state.getMainHandItemStack().get(DataComponents.KINETIC_WEAPON);
        float jetForward = kineticWeapon != null ? kineticWeapon.forwardMovement() : 0.0f;
        float itemInHandDepth = 0.125f;
        float attackTime = state.attackTime;
        float attack = Ease.inQuad(SpearAnimations.progress(attackTime, 0.05f, 0.2f));
        float retract = Ease.inOutExpo(SpearAnimations.progress(attackTime, 0.4f, 1.0f));
        poseStack.rotateAround((Quaternionfc)Axis.XN.rotationDegrees(70.0f * (attack - retract)), 0.0f, -0.125f, 0.125f);
        poseStack.translate(0.0f, jetForward * (attack - retract), 0.0f);
    }

    private static float hitFeedbackAmount(float ticksSinceFeedbackStart) {
        return 0.4f * (Ease.outQuart(SpearAnimations.progress(ticksSinceFeedbackStart, 1.0f, 3.0f)) - Ease.inOutSine(SpearAnimations.progress(ticksSinceFeedbackStart, 3.0f, 10.0f)));
    }

    public static void firstPersonUse(float ticksSinceKineticHitFeedback, PoseStack poseStack, float timeHeld, HumanoidArm arm, ItemStack itemStack) {
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon == null) {
            return;
        }
        UseParams params = UseParams.fromKineticWeapon(kineticWeapon, timeHeld);
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate((double)((float)invert * (params.raiseProgress() * 0.15f + params.raiseProgressEnd() * -0.05f + params.swayProgress() * -0.1f + params.swayScaleSlow() * 0.005f)), (double)(params.raiseProgress() * -0.075f + params.raiseProgressMiddle() * 0.075f + params.swayScaleFast() * 0.01f), (double)params.raiseProgressStart() * 0.05 + (double)params.raiseProgressEnd() * -0.05 + (double)(params.swayScaleSlow() * 0.005f));
        poseStack.rotateAround((Quaternionfc)Axis.XP.rotationDegrees(-65.0f * Ease.inOutBack(params.raiseProgress()) - 35.0f * params.lowerProgress() + 100.0f * params.raiseBackProgress() + -0.5f * params.swayScaleFast()), 0.0f, 0.1f, 0.0f);
        poseStack.rotateAround((Quaternionfc)Axis.YN.rotationDegrees((float)invert * (-90.0f * SpearAnimations.progress(params.raiseProgress(), 0.5f, 0.55f) + 90.0f * params.swayProgress() + 2.0f * params.swayScaleSlow())), (float)invert * 0.15f, 0.0f, 0.0f);
        poseStack.translate(0.0f, -SpearAnimations.hitFeedbackAmount(ticksSinceKineticHitFeedback), 0.0f);
    }

    public static void firstPersonAttack(float attack, PoseStack poseStack, int invert, HumanoidArm arm) {
        float startingAmount = Ease.inOutSine(SpearAnimations.progress(attack, 0.0f, 0.05f));
        float middleAmount = Ease.outBack(SpearAnimations.progress(attack, 0.05f, 0.2f));
        float endingAmount = Ease.inOutExpo(SpearAnimations.progress(attack, 0.4f, 1.0f));
        poseStack.translate((float)invert * 0.1f * (startingAmount - middleAmount), -0.075f * (startingAmount - endingAmount), 0.65f * (startingAmount - middleAmount));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-70.0f * (startingAmount - endingAmount)));
        poseStack.translate(0.0, 0.0, -0.25 * (double)(endingAmount - middleAmount));
    }

    @Environment(value=EnvType.CLIENT)
    private record UseParams(float raiseProgress, float raiseProgressStart, float raiseProgressMiddle, float raiseProgressEnd, float swayProgress, float lowerProgress, float raiseBackProgress, float swayIntensity, float swayScaleSlow, float swayScaleFast) {
        public static UseParams fromKineticWeapon(KineticWeapon kineticWeapon, float time) {
            int finishRaisingTick = kineticWeapon.delayTicks();
            int finishSwayingTick = kineticWeapon.dismountConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + finishRaisingTick;
            int startSwayingTick = finishSwayingTick - 20;
            int finishLoweringTick = kineticWeapon.knockbackConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + finishRaisingTick;
            int startLoweringTick = finishLoweringTick - 40;
            int finishRaisingBackTick = kineticWeapon.damageConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + finishRaisingTick;
            float raiseProgress = SpearAnimations.progress(time, 0.0f, finishRaisingTick);
            float raiseProgressStart = SpearAnimations.progress(raiseProgress, 0.0f, 0.5f);
            float raiseProgressMiddle = SpearAnimations.progress(raiseProgress, 0.5f, 0.8f);
            float raiseProgressEnd = SpearAnimations.progress(raiseProgress, 0.8f, 1.0f);
            float swayProgress = SpearAnimations.progress(time, startSwayingTick, finishSwayingTick);
            float lowerProgress = Ease.outCubic(Ease.inOutElastic(SpearAnimations.progress(time - 20.0f, startLoweringTick, finishLoweringTick)));
            float raiseBackProgress = SpearAnimations.progress(time, finishRaisingBackTick - 5, finishRaisingBackTick);
            float swayIntensity = 2.0f * Ease.outCirc(swayProgress) - 2.0f * Ease.inCirc(raiseBackProgress);
            float swayScaleSlow = Mth.sin(time * 19.0f * ((float)Math.PI / 180)) * swayIntensity;
            float swayScaleFast = Mth.sin(time * 30.0f * ((float)Math.PI / 180)) * swayIntensity;
            return new UseParams(raiseProgress, raiseProgressStart, raiseProgressMiddle, raiseProgressEnd, swayProgress, lowerProgress, raiseBackProgress, swayIntensity, swayScaleSlow, swayScaleFast);
        }
    }
}

