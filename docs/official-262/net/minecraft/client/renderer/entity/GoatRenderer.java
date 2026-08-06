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
import net.minecraft.client.model.animal.goat.BabyGoatModel;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.goat.Goat;

@Environment(value=EnvType.CLIENT)
public class GoatRenderer
extends AgeableMobRenderer<Goat, GoatRenderState, GoatModel> {
    private static final Identifier GOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/goat/goat.png");
    private static final Identifier BABY_GOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/goat/goat_baby.png");

    public GoatRenderer(EntityRendererProvider.Context context) {
        super(context, new GoatModel(context.bakeLayer(ModelLayers.GOAT)), new BabyGoatModel(context.bakeLayer(ModelLayers.GOAT_BABY)), 0.7f);
    }

    @Override
    public Identifier getTextureLocation(GoatRenderState state) {
        return state.isBaby ? BABY_GOAT_LOCATION : GOAT_LOCATION;
    }

    @Override
    public GoatRenderState createRenderState() {
        return new GoatRenderState();
    }

    @Override
    public void extractRenderState(Goat entity, GoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.hasLeftHorn = entity.hasLeftHorn();
        state.hasRightHorn = entity.hasRightHorn();
        state.rammingXHeadRot = entity.getRammingXHeadRot();
    }
}

