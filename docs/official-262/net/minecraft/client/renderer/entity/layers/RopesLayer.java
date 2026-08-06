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
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;

@Environment(value=EnvType.CLIENT)
public class RopesLayer<M extends HappyGhastModel>
extends RenderLayer<HappyGhastRenderState, M> {
    private final Identifier ropesTexture;
    private final HappyGhastModel adultModel;
    private final HappyGhastModel babyModel;

    public RopesLayer(RenderLayerParent<HappyGhastRenderState, M> renderer, EntityModelSet modelSet, Identifier ropesTexture) {
        super(renderer);
        this.ropesTexture = ropesTexture;
        this.adultModel = new HappyGhastModel(modelSet.bakeLayer(ModelLayers.HAPPY_GHAST_ROPES));
        this.babyModel = new HappyGhastModel(modelSet.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_ROPES));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HappyGhastRenderState state, float yRot, float xRot) {
        if (!state.isLeashHolder || !state.bodyItem.is(ItemTags.HARNESSES)) {
            return;
        }
        HappyGhastModel model = state.isBaby ? this.babyModel : this.adultModel;
        submitNodeCollector.submitModel(model, state, poseStack, this.ropesTexture, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }
}

