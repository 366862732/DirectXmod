/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.chicken;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class ChickenModel
extends EntityModel<ChickenRenderState> {
    public static final float Y_OFFSET = 16.0f;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ChickenModel(ModelPart root) {
        super(root);
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    @Override
    public void setupAnim(ChickenRenderState state) {
        super.setupAnim(state);
        float flapAngle = (Mth.sin(state.flap) + 1.0f) * state.flapSpeed;
        float animationSpeed = state.walkAnimationSpeed;
        float animationPos = state.walkAnimationPos;
        this.rightLeg.xRot = Mth.cos(animationPos * 0.6662f) * 1.4f * animationSpeed;
        this.leftLeg.xRot = Mth.cos(animationPos * 0.6662f + (float)Math.PI) * 1.4f * animationSpeed;
        this.rightWing.zRot = flapAngle;
        this.leftWing.zRot = -flapAngle;
    }
}

