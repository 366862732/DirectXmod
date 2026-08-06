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
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultOcelotModel;
import net.minecraft.client.model.animal.feline.BabyOcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.feline.Ocelot;

@Environment(value=EnvType.CLIENT)
public class OcelotRenderer
extends AgeableMobRenderer<Ocelot, FelineRenderState, AbstractFelineModel<FelineRenderState>> {
    private static final Identifier CAT_OCELOT_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/ocelot.png");
    private static final Identifier CAT_OCELOT_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/ocelot_baby.png");

    public OcelotRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultOcelotModel(context.bakeLayer(ModelLayers.OCELOT)), new BabyOcelotModel(context.bakeLayer(ModelLayers.OCELOT_BABY)), 0.4f);
    }

    @Override
    public Identifier getTextureLocation(FelineRenderState state) {
        return state.isBaby ? CAT_OCELOT_BABY_LOCATION : CAT_OCELOT_LOCATION;
    }

    @Override
    public FelineRenderState createRenderState() {
        return new FelineRenderState();
    }

    @Override
    public void extractRenderState(Ocelot entity, FelineRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isCrouching = entity.isCrouching();
        state.isSprinting = entity.isSprinting();
    }
}

