/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.panda.BabyPandaModel;
import net.minecraft.client.model.animal.panda.PandaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.panda.Panda;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class PandaRenderer
extends AgeableMobRenderer<Panda, PandaRenderState, PandaModel> {
    private static final Map<Panda.Gene, Identifier> TEXTURES = Maps.newEnumMap(Map.of(Panda.Gene.NORMAL, Identifier.withDefaultNamespace("textures/entity/panda/panda.png"), Panda.Gene.LAZY, Identifier.withDefaultNamespace("textures/entity/panda/panda_lazy.png"), Panda.Gene.WORRIED, Identifier.withDefaultNamespace("textures/entity/panda/panda_worried.png"), Panda.Gene.PLAYFUL, Identifier.withDefaultNamespace("textures/entity/panda/panda_playful.png"), Panda.Gene.BROWN, Identifier.withDefaultNamespace("textures/entity/panda/panda_brown.png"), Panda.Gene.WEAK, Identifier.withDefaultNamespace("textures/entity/panda/panda_weak.png"), Panda.Gene.AGGRESSIVE, Identifier.withDefaultNamespace("textures/entity/panda/panda_aggressive.png")));
    private static final Map<Panda.Gene, Identifier> BABY_TEXTURES = Maps.newEnumMap(Map.of(Panda.Gene.NORMAL, Identifier.withDefaultNamespace("textures/entity/panda/panda_baby.png"), Panda.Gene.LAZY, Identifier.withDefaultNamespace("textures/entity/panda/lazy_panda_baby.png"), Panda.Gene.WORRIED, Identifier.withDefaultNamespace("textures/entity/panda/worried_panda_baby.png"), Panda.Gene.PLAYFUL, Identifier.withDefaultNamespace("textures/entity/panda/playful_panda_baby.png"), Panda.Gene.BROWN, Identifier.withDefaultNamespace("textures/entity/panda/brown_panda_baby.png"), Panda.Gene.WEAK, Identifier.withDefaultNamespace("textures/entity/panda/weak_panda_baby.png"), Panda.Gene.AGGRESSIVE, Identifier.withDefaultNamespace("textures/entity/panda/aggressive_panda_baby.png")));

    public PandaRenderer(EntityRendererProvider.Context context) {
        super(context, new PandaModel(context.bakeLayer(ModelLayers.PANDA)), new BabyPandaModel(context.bakeLayer(ModelLayers.PANDA_BABY)), 0.9f);
        this.addLayer(new PandaHoldsItemLayer(this));
    }

    @Override
    public Identifier getTextureLocation(PandaRenderState state) {
        Map<Panda.Gene, Identifier> textures = state.isBaby ? BABY_TEXTURES : TEXTURES;
        return textures.getOrDefault(state.variant, textures.get(Panda.Gene.NORMAL));
    }

    @Override
    public PandaRenderState createRenderState() {
        return new PandaRenderState();
    }

    @Override
    public void extractRenderState(Panda entity, PandaRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.variant = entity.getVariant();
        state.isUnhappy = entity.getUnhappyCounter() > 0;
        state.isSneezing = entity.isSneezing();
        state.sneezeTime = entity.getSneezeCounter();
        state.isEating = entity.isEating();
        state.isScared = entity.isScared();
        state.isSitting = entity.isSitting();
        state.sitAmount = entity.getSitAmount(partialTicks);
        state.lieOnBackAmount = entity.getLieOnBackAmount(partialTicks);
        state.rollAmount = entity.isBaby() ? 0.0f : entity.getRollAmount(partialTicks);
        state.rollTime = entity.rollCounter > 0 ? (float)entity.rollCounter + partialTicks : 0.0f;
    }

    @Override
    protected void setupRotations(PandaRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        float lieOnBackAmount;
        float sitAmount;
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (state.rollTime > 0.0f) {
            float y;
            float rollTransitionTime = Mth.frac(state.rollTime);
            int rollPos = Mth.floor(state.rollTime);
            int nextRollPos = rollPos + 1;
            float divider = 7.0f;
            float f = y = state.isBaby ? 0.3f : 0.8f;
            if ((float)rollPos < 8.0f) {
                float thisAngle = 90.0f * (float)rollPos / 7.0f;
                float nextAngle = 90.0f * (float)nextRollPos / 7.0f;
                float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 8.0f);
                poseStack.translate(0.0f, (y + 0.2f) * (angle / 90.0f), 0.0f);
                poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-angle));
            } else if ((float)rollPos < 16.0f) {
                float internalRollCounter = ((float)rollPos - 8.0f) / 7.0f;
                float thisAngle = 90.0f + 90.0f * internalRollCounter;
                float nextAngle = 90.0f + 90.0f * ((float)nextRollPos - 8.0f) / 7.0f;
                float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 16.0f);
                poseStack.translate(0.0f, y + 0.2f + (y - 0.2f) * (angle - 90.0f) / 90.0f, 0.0f);
                poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-angle));
            } else if ((float)rollPos < 24.0f) {
                float internalRollCounter = ((float)rollPos - 16.0f) / 7.0f;
                float thisAngle = 180.0f + 90.0f * internalRollCounter;
                float nextAngle = 180.0f + 90.0f * ((float)nextRollPos - 16.0f) / 7.0f;
                float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 24.0f);
                poseStack.translate(0.0f, y + y * (270.0f - angle) / 90.0f, 0.0f);
                poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-angle));
            } else if (rollPos < 32) {
                float internalRollCounter = ((float)rollPos - 24.0f) / 7.0f;
                float thisAngle = 270.0f + 90.0f * internalRollCounter;
                float nextAngle = 270.0f + 90.0f * ((float)nextRollPos - 24.0f) / 7.0f;
                float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 32.0f);
                poseStack.translate(0.0f, y * ((360.0f - angle) / 90.0f), 0.0f);
                poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-angle));
            }
        }
        if ((sitAmount = state.sitAmount) > 0.0f) {
            poseStack.translate(0.0f, 0.8f * sitAmount, 0.0f);
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(Mth.lerp(sitAmount, state.xRot, state.xRot + 90.0f)));
            poseStack.translate(0.0f, -1.0f * sitAmount, 0.0f);
            if (state.isScared) {
                float shakeRot = (float)(Math.cos(state.ageInTicks * 1.25f) * Math.PI * (double)0.05f);
                poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(shakeRot));
                if (state.isBaby) {
                    poseStack.translate(0.0f, 0.8f, 0.55f);
                }
            }
        }
        if ((lieOnBackAmount = state.lieOnBackAmount) > 0.0f) {
            float y = state.isBaby ? 0.5f : 1.3f;
            poseStack.translate(0.0f, y * lieOnBackAmount, 0.0f);
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(Mth.lerp(lieOnBackAmount, state.xRot, state.xRot + 180.0f)));
        }
    }

    private float getAngle(float thisAngle, float nextAngle, int nextRollPos, float rollTransitionTime, float threshold) {
        if ((float)nextRollPos < threshold) {
            return Mth.lerp(rollTransitionTime, thisAngle, nextAngle);
        }
        return thisAngle;
    }
}

