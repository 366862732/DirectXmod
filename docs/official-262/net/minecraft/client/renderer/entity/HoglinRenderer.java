/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHoglinRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

@Environment(value=EnvType.CLIENT)
public class HoglinRenderer
extends AbstractHoglinRenderer<Hoglin> {
    private static final Identifier HOGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/hoglin/hoglin.png");
    private static final Identifier BABY_HOGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/hoglin/hoglin_baby.png");

    public HoglinRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.HOGLIN, ModelLayers.HOGLIN_BABY, 0.7f);
    }

    @Override
    public Identifier getTextureLocation(HoglinRenderState state) {
        return state.isBaby ? BABY_HOGLIN_LOCATION : HOGLIN_LOCATION;
    }

    @Override
    public void extractRenderState(Hoglin entity, HoglinRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isConverting = entity.isConverting();
    }

    @Override
    protected boolean isShaking(HoglinRenderState state) {
        return super.isShaking(state) || state.isConverting;
    }
}

