/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.animal.feline.BabyCatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.feline.Cat;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class CatRenderer
extends AgeableMobRenderer<Cat, CatRenderState, AbstractFelineModel<CatRenderState>> {
    public CatRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultCatModel(context.bakeLayer(ModelLayers.CAT)), new BabyCatModel(context.bakeLayer(ModelLayers.CAT_BABY)), 0.4f);
        this.addLayer(new CatCollarLayer(this, context.getModelSet()));
    }

    @Override
    public Identifier getTextureLocation(CatRenderState state) {
        return state.texture;
    }

    @Override
    public CatRenderState createRenderState() {
        return new CatRenderState();
    }

    @Override
    public void extractRenderState(Cat entity, CatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.texture = entity.getVariant().value().assetInfo(state.isBaby).texturePath();
        state.isCrouching = entity.isCrouching();
        state.isSprinting = entity.isSprinting();
        state.isSitting = entity.isInSittingPose();
        state.lieDownAmount = entity.getLieDownAmount(partialTicks);
        state.lieDownAmountTail = entity.getLieDownAmountTail(partialTicks);
        state.relaxStateOneAmount = entity.getRelaxStateOneAmount(partialTicks);
        state.isLyingOnTopOfSleepingPlayer = entity.isLyingOnTopOfSleepingPlayer();
        state.collarColor = entity.isTame() ? entity.getCollarColor() : null;
    }

    @Override
    protected void setupRotations(CatRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        float lieDownAmount = state.lieDownAmount;
        if (lieDownAmount > 0.0f) {
            poseStack.translate(0.4f * lieDownAmount, 0.15f * lieDownAmount, 0.1f * lieDownAmount);
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(Mth.rotLerp(lieDownAmount, 0.0f, 90.0f)));
            if (state.isLyingOnTopOfSleepingPlayer) {
                poseStack.translate(0.15f * lieDownAmount, 0.0f, 0.0f);
            }
        }
    }
}

