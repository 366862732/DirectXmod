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
import net.minecraft.client.model.animal.camel.AdultCamelModel;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.camel.Camel;

@Environment(value=EnvType.CLIENT)
public class CamelHuskRenderer
extends MobRenderer<Camel, CamelRenderState, CamelModel> {
    private static final Identifier CAMEL_HUSK_LOCATION = Identifier.withDefaultNamespace("textures/entity/camel/camel_husk.png");

    public CamelHuskRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultCamelModel(context.bakeLayer(ModelLayers.CAMEL)), 0.7f);
        this.addLayer(CamelRenderer.createCamelSaddleLayer(context, this, EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE, ModelLayers.CAMEL_HUSK_SADDLE));
    }

    @Override
    public Identifier getTextureLocation(CamelRenderState state) {
        return CAMEL_HUSK_LOCATION;
    }

    @Override
    public CamelRenderState createRenderState() {
        return new CamelRenderState();
    }

    @Override
    public void extractRenderState(Camel entity, CamelRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        CamelRenderer.extractAdditionalState(entity, state, partialTicks);
    }
}

