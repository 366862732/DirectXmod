/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.camel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class CamelModel
extends EntityModel<CamelRenderState> {
    private static final float MAX_WALK_ANIMATION_SPEED = 2.0f;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5f;
    protected final ModelPart head;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation sitAnimation;
    private final KeyframeAnimation sitPoseAnimation;
    private final KeyframeAnimation standupAnimation;
    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation dashAnimation;

    public CamelModel(ModelPart root, AnimationDefinition walk, AnimationDefinition sit, AnimationDefinition sitPose, AnimationDefinition standup, AnimationDefinition idle, AnimationDefinition dash) {
        super(root);
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.walkAnimation = walk.bake(root);
        this.sitAnimation = sit.bake(root);
        this.sitPoseAnimation = sitPose.bake(root);
        this.standupAnimation = standup.bake(root);
        this.idleAnimation = idle.bake(root);
        this.dashAnimation = dash.bake(root);
    }

    @Override
    public void setupAnim(CamelRenderState state) {
        super.setupAnim(state);
        this.applyHeadRotation(state, state.yRot, state.xRot);
        this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0f, 2.5f);
        this.sitAnimation.apply(state.sitAnimationState, state.ageInTicks);
        this.sitPoseAnimation.apply(state.sitPoseAnimationState, state.ageInTicks);
        this.standupAnimation.apply(state.sitUpAnimationState, state.ageInTicks);
        this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
        this.dashAnimation.apply(state.dashAnimationState, state.ageInTicks);
    }

    private void applyHeadRotation(CamelRenderState state, float yRot, float xRot) {
        yRot = Mth.clamp(yRot, -30.0f, 30.0f);
        xRot = Mth.clamp(xRot, -25.0f, 45.0f);
        if (state.jumpCooldown > 0.0f) {
            float headRotation = 45.0f * state.jumpCooldown / 55.0f;
            xRot = Mth.clamp(xRot + headRotation, -25.0f, 70.0f);
        }
        this.head.yRot = yRot * ((float)Math.PI / 180);
        this.head.xRot = xRot * ((float)Math.PI / 180);
    }
}

