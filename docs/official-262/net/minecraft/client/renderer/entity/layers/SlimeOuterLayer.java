/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Environment(value=EnvType.CLIENT)
public class SlimeOuterLayer
extends RenderLayer<SlimeRenderState, SlimeModel> {
    private final SlimeModel model;

    public SlimeOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new SlimeModel(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, SlimeRenderState state, float yRot, float xRot) {
        boolean appearsGlowingWithInvisibility;
        boolean bl = appearsGlowingWithInvisibility = state.appearsGlowing() && state.isInvisible;
        if (state.isInvisible && !appearsGlowingWithInvisibility) {
            return;
        }
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0f);
        if (appearsGlowingWithInvisibility) {
            submitNodeCollector.order(1).submitModel(this.model, state, poseStack, RenderTypes.outline(SlimeRenderer.SLIME_LOCATION), lightCoords, overlayCoords, state.outlineColor, null);
        } else {
            submitNodeCollector.order(1).submitModel(this.model, state, poseStack, RenderTypes.entityTranslucent(SlimeRenderer.SLIME_LOCATION), lightCoords, overlayCoords, state.outlineColor, null);
        }
    }
}

