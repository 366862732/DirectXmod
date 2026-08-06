/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.turtle;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class TurtleModel
extends QuadrupedModel<TurtleRenderState> {
    public TurtleModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
    }

    @Override
    public void setupAnim(TurtleRenderState state) {
        super.setupAnim(state);
        float animationPos = state.walkAnimationPos;
        float animationSpeed = state.walkAnimationSpeed;
        if (state.isOnLand) {
            float layEgg = state.isLayingEgg ? 4.0f : 1.0f;
            float layEggAmplitude = state.isLayingEgg ? 2.0f : 1.0f;
            float swingPos = animationPos * 5.0f;
            float frontSwing = Mth.cos(layEgg * swingPos);
            float hindSwing = Mth.cos(swingPos);
            this.rightFrontLeg.yRot = -frontSwing * 8.0f * animationSpeed * layEggAmplitude;
            this.leftFrontLeg.yRot = frontSwing * 8.0f * animationSpeed * layEggAmplitude;
            this.rightHindLeg.yRot = -hindSwing * 3.0f * animationSpeed;
            this.leftHindLeg.yRot = hindSwing * 3.0f * animationSpeed;
        } else {
            float swing;
            float swingScale = 0.5f * animationSpeed;
            this.rightHindLeg.xRot = swing = Mth.cos(animationPos * 0.6662f * 0.6f) * swingScale;
            this.leftHindLeg.xRot = -swing;
            this.rightFrontLeg.zRot = -swing;
            this.leftFrontLeg.zRot = swing;
        }
    }
}

