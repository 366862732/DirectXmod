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
import net.minecraft.client.model.animal.turtle.AdultTurtleModel;
import net.minecraft.client.model.animal.turtle.BabyTurtleModel;
import net.minecraft.client.model.animal.turtle.TurtleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.turtle.Turtle;

@Environment(value=EnvType.CLIENT)
public class TurtleRenderer
extends AgeableMobRenderer<Turtle, TurtleRenderState, TurtleModel> {
    private static final Identifier TURTLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/turtle/turtle.png");
    private static final Identifier BABY_TURTLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/turtle/turtle_baby.png");

    public TurtleRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultTurtleModel(context.bakeLayer(ModelLayers.TURTLE)), new BabyTurtleModel(context.bakeLayer(ModelLayers.TURTLE_BABY)), 0.7f);
    }

    @Override
    protected float getShadowRadius(TurtleRenderState state) {
        float radius = super.getShadowRadius(state);
        if (state.isBaby) {
            return radius * 0.83f;
        }
        return radius;
    }

    @Override
    public TurtleRenderState createRenderState() {
        return new TurtleRenderState();
    }

    @Override
    public void extractRenderState(Turtle entity, TurtleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isOnLand = !entity.isInWater() && entity.onGround();
        state.isLayingEgg = entity.isLayingEgg();
        state.hasEgg = !entity.isBaby() && entity.hasEgg();
    }

    @Override
    public Identifier getTextureLocation(TurtleRenderState state) {
        return state.isBaby ? BABY_TURTLE_LOCATION : TURTLE_LOCATION;
    }
}

