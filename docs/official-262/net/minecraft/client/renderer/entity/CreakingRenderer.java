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
import net.minecraft.client.model.monster.creaking.CreakingModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.creaking.Creaking;

@Environment(value=EnvType.CLIENT)
public class CreakingRenderer<T extends Creaking>
extends MobRenderer<T, CreakingRenderState, CreakingModel> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/creaking/creaking.png");
    private static final Identifier EYES_TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/creaking/creaking_eyes.png");

    public CreakingRenderer(EntityRendererProvider.Context context) {
        super(context, new CreakingModel(context.bakeLayer(ModelLayers.CREAKING)), 0.6f);
        this.addLayer(new LivingEntityEmissiveLayer<CreakingRenderState, CreakingModel>(this, renderState -> EYES_TEXTURE_LOCATION, (state, ageInTicks) -> state.eyesGlowing ? 1.0f : 0.0f, new CreakingModel(context.bakeLayer(ModelLayers.CREAKING_EYES)), RenderTypes::eyes, true));
    }

    @Override
    public Identifier getTextureLocation(CreakingRenderState state) {
        return TEXTURE_LOCATION;
    }

    @Override
    public CreakingRenderState createRenderState() {
        return new CreakingRenderState();
    }

    @Override
    public void extractRenderState(T entity, CreakingRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnimationState.copyFrom(((Creaking)entity).attackAnimationState);
        state.invulnerabilityAnimationState.copyFrom(((Creaking)entity).invulnerabilityAnimationState);
        state.deathAnimationState.copyFrom(((Creaking)entity).deathAnimationState);
        if (((Creaking)entity).isTearingDown()) {
            state.deathTime = 0.0f;
            state.hasRedOverlay = false;
            state.eyesGlowing = ((Creaking)entity).hasGlowingEyes();
        } else {
            state.eyesGlowing = ((Creaking)entity).isActive();
        }
        state.canMove = ((Creaking)entity).canMove();
    }
}

