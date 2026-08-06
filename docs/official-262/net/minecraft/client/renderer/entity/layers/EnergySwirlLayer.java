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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public abstract class EnergySwirlLayer<S extends EntityRenderState, M extends EntityModel<S>>
extends RenderLayer<S, M> {
    public EnergySwirlLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (!this.isPowered(state)) {
            return;
        }
        float t = ((EntityRenderState)state).ageInTicks;
        M model = this.model();
        submitNodeCollector.order(1).submitModel(model, state, poseStack, RenderTypes.energySwirl(this.getTextureLocation(), this.xOffset(t) % 1.0f, t * 0.01f % 1.0f), lightCoords, OverlayTexture.NO_OVERLAY, -8355712, (TextureAtlasSprite)null, ((EntityRenderState)state).outlineColor, (ModelFeatureRenderer.CrumblingOverlay)null);
    }

    protected abstract boolean isPowered(S var1);

    protected abstract float xOffset(float var1);

    protected abstract Identifier getTextureLocation();

    protected abstract M model();
}

