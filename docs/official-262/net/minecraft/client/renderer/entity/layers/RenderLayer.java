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
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
    private final RenderLayerParent<S, M> renderer;

    public RenderLayer(RenderLayerParent<S, M> renderer) {
        this.renderer = renderer;
    }

    protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(Model<? super S> model, Identifier texture, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, int color, int order) {
        if (!state.isInvisible) {
            RenderLayer.renderColoredCutoutModel(model, texture, poseStack, submitNodeCollector, lightCoords, state, color, order);
        }
    }

    protected static <S extends LivingEntityRenderState> void renderColoredCutoutModel(Model<? super S> model, Identifier texture, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, int color, int order) {
        submitNodeCollector.order(order).submitModel(model, state, poseStack, RenderTypes.entityCutout(texture), lightCoords, LivingEntityRenderer.getOverlayCoords(state, 0.0f), color, null, state.outlineColor, null);
    }

    public M getParentModel() {
        return this.renderer.getModel();
    }

    public abstract void submit(PoseStack var1, SubmitNodeCollector var2, int var3, S var4, float var5, float var6);
}

