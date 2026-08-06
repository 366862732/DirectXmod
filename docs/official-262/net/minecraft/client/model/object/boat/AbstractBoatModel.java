/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.boat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractBoatModel
extends EntityModel<BoatRenderState> {
    private final ModelPart leftPaddle;
    private final ModelPart rightPaddle;

    public AbstractBoatModel(ModelPart root) {
        super(root);
        this.leftPaddle = root.getChild("left_paddle");
        this.rightPaddle = root.getChild("right_paddle");
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        AbstractBoatModel.animatePaddle(state.rowingTimeLeft, 0, this.leftPaddle);
        AbstractBoatModel.animatePaddle(state.rowingTimeRight, 1, this.rightPaddle);
    }

    private static void animatePaddle(float time, int side, ModelPart paddle) {
        paddle.xRot = Mth.clampedLerp((Mth.sin(-time) + 1.0f) / 2.0f, -1.0471976f, -0.2617994f);
        paddle.yRot = Mth.clampedLerp((Mth.sin(-time + 1.0f) + 1.0f) / 2.0f, -0.7853982f, 0.7853982f);
        if (side == 1) {
            paddle.yRot = (float)Math.PI - paddle.yRot;
        }
    }
}

