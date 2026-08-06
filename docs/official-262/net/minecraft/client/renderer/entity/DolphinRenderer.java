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
import net.minecraft.client.model.animal.dolphin.DolphinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.DolphinCarryingItemLayer;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.dolphin.Dolphin;

@Environment(value=EnvType.CLIENT)
public class DolphinRenderer
extends AgeableMobRenderer<Dolphin, DolphinRenderState, DolphinModel> {
    private static final Identifier DOLPHIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/dolphin/dolphin.png");
    private static final Identifier DOLPHIN_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/dolphin/dolphin_baby.png");

    public DolphinRenderer(EntityRendererProvider.Context context) {
        super(context, new DolphinModel(context.bakeLayer(ModelLayers.DOLPHIN)), new DolphinModel(context.bakeLayer(ModelLayers.DOLPHIN_BABY)), 0.7f);
        this.addLayer(new DolphinCarryingItemLayer(this));
    }

    @Override
    public Identifier getTextureLocation(DolphinRenderState state) {
        return state.isBaby ? DOLPHIN_BABY_LOCATION : DOLPHIN_LOCATION;
    }

    @Override
    public DolphinRenderState createRenderState() {
        return new DolphinRenderState();
    }

    @Override
    public void extractRenderState(Dolphin entity, DolphinRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.isMoving = entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7;
    }
}

