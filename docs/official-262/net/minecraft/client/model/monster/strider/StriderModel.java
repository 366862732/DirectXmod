/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.strider;

import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class StriderModel
extends EntityModel<StriderRenderState> {
    protected static final float SPEED = 1.5f;
    protected final ModelPart rightLeg;
    protected final ModelPart leftLeg;
    protected final ModelPart body;

    public StriderModel(ModelPart root) {
        super(root);
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.body = root.getChild("body");
    }

    @Override
    public void setupAnim(StriderRenderState state) {
        super.setupAnim(state);
        float animationPos = state.walkAnimationPos;
        float animationSpeed = Math.min(state.walkAnimationSpeed, 0.25f);
        if (!state.isRidden) {
            this.body.xRot = state.xRot * ((float)Math.PI / 180);
            this.body.yRot = state.yRot * ((float)Math.PI / 180);
        } else {
            this.body.xRot = 0.0f;
            this.body.yRot = 0.0f;
        }
        this.body.zRot = 0.1f * Mth.sin(animationPos * 1.5f) * 4.0f * animationSpeed;
        this.leftLeg.xRot = Mth.sin(animationPos * 1.5f * 0.5f) * 2.0f * animationSpeed;
        this.rightLeg.xRot = Mth.sin(animationPos * 1.5f * 0.5f + (float)Math.PI) * 2.0f * animationSpeed;
        this.leftLeg.zRot = 0.17453292f * Mth.cos(animationPos * 1.5f * 0.5f) * animationSpeed;
        this.rightLeg.zRot = 0.17453292f * Mth.cos(animationPos * 1.5f * 0.5f + (float)Math.PI) * animationSpeed;
        this.customAnimations(animationPos, animationSpeed, state.ageInTicks);
    }

    protected abstract void customAnimations(float var1, float var2, float var3);

    public void animateBristle(float ageInTicks, float bristleFlow, ModelPart firstBristle, ModelPart secondBristle, ModelPart thirdBristle, BiConsumer<ModelPart, Float> addRotationFunction) {
        addRotationFunction.accept(firstBristle, Float.valueOf(bristleFlow * 0.6f));
        addRotationFunction.accept(secondBristle, Float.valueOf(bristleFlow * 1.2f));
        addRotationFunction.accept(thirdBristle, Float.valueOf(bristleFlow * 1.3f));
        addRotationFunction.accept(firstBristle, Float.valueOf(0.1f * Mth.sin(ageInTicks * 0.4f)));
        addRotationFunction.accept(secondBristle, Float.valueOf(0.1f * Mth.sin(ageInTicks * 0.2f)));
        addRotationFunction.accept(thirdBristle, Float.valueOf(0.05f * Mth.sin(ageInTicks * -0.4f)));
    }
}

