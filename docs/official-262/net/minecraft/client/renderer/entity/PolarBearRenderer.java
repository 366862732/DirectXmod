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
import net.minecraft.client.model.animal.polarbear.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.polarbear.PolarBear;

@Environment(value=EnvType.CLIENT)
public class PolarBearRenderer
extends AgeableMobRenderer<PolarBear, PolarBearRenderState, PolarBearModel> {
    private static final Identifier BEAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/bear/polarbear.png");
    private static final Identifier BABY_BEAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/bear/polarbear_baby.png");

    public PolarBearRenderer(EntityRendererProvider.Context context) {
        super(context, new PolarBearModel(context.bakeLayer(ModelLayers.POLAR_BEAR)), new PolarBearModel(context.bakeLayer(ModelLayers.POLAR_BEAR_BABY)), 0.9f);
    }

    @Override
    public Identifier getTextureLocation(PolarBearRenderState state) {
        return state.isBaby ? BABY_BEAR_LOCATION : BEAR_LOCATION;
    }

    @Override
    public PolarBearRenderState createRenderState() {
        return new PolarBearRenderState();
    }

    @Override
    public void extractRenderState(PolarBear entity, PolarBearRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.standScale = entity.getStandingAnimationScale(partialTicks);
    }
}

