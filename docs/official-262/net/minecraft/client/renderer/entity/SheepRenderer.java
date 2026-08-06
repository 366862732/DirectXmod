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
import net.minecraft.client.model.animal.sheep.BabySheepModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.client.renderer.entity.layers.SheepWoolUndercoatLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sheep.Sheep;

@Environment(value=EnvType.CLIENT)
public class SheepRenderer
extends AgeableMobRenderer<Sheep, SheepRenderState, SheepModel> {
    private static final Identifier SHEEP_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep.png");
    private static final Identifier SHEEP_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_baby.png");

    public SheepRenderer(EntityRendererProvider.Context context) {
        super(context, new SheepModel(context.bakeLayer(ModelLayers.SHEEP)), new BabySheepModel(context.bakeLayer(ModelLayers.SHEEP_BABY)), 0.7f);
        this.addLayer(new SheepWoolUndercoatLayer(this, context.getModelSet()));
        this.addLayer(new SheepWoolLayer(this, context.getModelSet()));
    }

    @Override
    public Identifier getTextureLocation(SheepRenderState state) {
        return state.isBaby ? SHEEP_BABY_LOCATION : SHEEP_LOCATION;
    }

    @Override
    public SheepRenderState createRenderState() {
        return new SheepRenderState();
    }

    @Override
    public void extractRenderState(Sheep entity, SheepRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.headEatAngleScale = entity.getHeadEatAngleScale(partialTicks);
        state.headEatPositionScale = entity.getHeadEatPositionScale(partialTicks);
        state.isSheared = entity.isSheared();
        state.woolColor = entity.getColor();
        state.isJebSheep = SheepRenderer.checkMagicName(entity, "jeb_");
    }
}

