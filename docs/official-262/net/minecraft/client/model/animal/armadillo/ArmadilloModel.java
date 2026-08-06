/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.armadillo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmadilloRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class ArmadilloModel
extends EntityModel<ArmadilloRenderState> {
    private static final float MAX_DOWN_HEAD_ROTATION_EXTENT = 25.0f;
    private static final float MAX_UP_HEAD_ROTATION_EXTENT = 22.5f;
    private static final float MAX_WALK_ANIMATION_SPEED = 16.5f;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5f;
    protected static final String HEAD_CUBE = "head_cube";
    protected static final String RIGHT_EAR_CUBE = "right_ear_cube";
    protected static final String LEFT_EAR_CUBE = "left_ear_cube";
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart cube;
    private final ModelPart head;
    private final ModelPart tail;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation rollOutAnimation;
    private final KeyframeAnimation rollUpAnimation;
    private final KeyframeAnimation peekAnimation;

    public ArmadilloModel(ModelPart root, AnimationDefinition walk, AnimationDefinition rollOut, AnimationDefinition rollUp, AnimationDefinition peek) {
        super(root);
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.cube = root.getChild("cube");
        this.walkAnimation = walk.bake(root);
        this.rollOutAnimation = rollOut.bake(root);
        this.rollUpAnimation = rollUp.bake(root);
        this.peekAnimation = peek.bake(root);
    }

    @Override
    public void setupAnim(ArmadilloRenderState state) {
        super.setupAnim(state);
        if (state.isHidingInShell) {
            this.body.skipDraw = true;
            this.leftHindLeg.visible = false;
            this.rightHindLeg.visible = false;
            this.tail.visible = false;
            this.cube.visible = true;
        } else {
            this.body.skipDraw = false;
            this.leftHindLeg.visible = true;
            this.rightHindLeg.visible = true;
            this.tail.visible = true;
            this.cube.visible = false;
            this.head.xRot = Mth.clamp(state.xRot, -22.5f, 25.0f) * ((float)Math.PI / 180);
            this.head.yRot = Mth.clamp(state.yRot, -32.5f, 32.5f) * ((float)Math.PI / 180);
        }
        if (!state.isHidingInShell) {
            this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 16.5f, 2.5f);
        }
        this.rollOutAnimation.apply(state.rollOutAnimationState, state.ageInTicks);
        this.rollUpAnimation.apply(state.rollUpAnimationState, state.ageInTicks);
        this.peekAnimation.apply(state.peekAnimationState, state.ageInTicks);
    }
}

