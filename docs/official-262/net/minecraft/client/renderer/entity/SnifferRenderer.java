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
import net.minecraft.client.model.animal.sniffer.SnifferModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.phys.AABB;

@Environment(value=EnvType.CLIENT)
public class SnifferRenderer
extends AgeableMobRenderer<Sniffer, SnifferRenderState, SnifferModel> {
    private static final Identifier SNIFFER_LOCATION = Identifier.withDefaultNamespace("textures/entity/sniffer/sniffer.png");
    private static final Identifier SNIFFLET_LOCATION = Identifier.withDefaultNamespace("textures/entity/sniffer/snifflet.png");

    public SnifferRenderer(EntityRendererProvider.Context context) {
        super(context, new SnifferModel(context.bakeLayer(ModelLayers.SNIFFER)), new SnifferModel(context.bakeLayer(ModelLayers.SNIFFER_BABY)), 1.1f);
    }

    @Override
    public Identifier getTextureLocation(SnifferRenderState state) {
        return state.isBaby ? SNIFFLET_LOCATION : SNIFFER_LOCATION;
    }

    @Override
    public SnifferRenderState createRenderState() {
        return new SnifferRenderState();
    }

    @Override
    public void extractRenderState(Sniffer entity, SnifferRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isSearching = entity.isSearching();
        state.diggingAnimationState.copyFrom(entity.diggingAnimationState);
        state.sniffingAnimationState.copyFrom(entity.sniffingAnimationState);
        state.risingAnimationState.copyFrom(entity.risingAnimationState);
        state.feelingHappyAnimationState.copyFrom(entity.feelingHappyAnimationState);
        state.scentingAnimationState.copyFrom(entity.scentingAnimationState);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Sniffer entity) {
        return super.getBoundingBoxForCulling(entity).inflate(0.6f);
    }
}

