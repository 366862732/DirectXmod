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
import net.minecraft.client.model.ambient.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ambient.Bat;

@Environment(value=EnvType.CLIENT)
public class BatRenderer
extends MobRenderer<Bat, BatRenderState, BatModel> {
    private static final Identifier BAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/bat/bat.png");

    public BatRenderer(EntityRendererProvider.Context context) {
        super(context, new BatModel(context.bakeLayer(ModelLayers.BAT)), 0.25f);
    }

    @Override
    public Identifier getTextureLocation(BatRenderState state) {
        return BAT_LOCATION;
    }

    @Override
    public BatRenderState createRenderState() {
        return new BatRenderState();
    }

    @Override
    public void extractRenderState(Bat entity, BatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isResting = entity.isResting();
        state.flyAnimationState.copyFrom(entity.flyAnimationState);
        state.restAnimationState.copyFrom(entity.restAnimationState);
    }
}

