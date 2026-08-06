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
import net.minecraft.client.model.monster.strider.AdultStriderModel;
import net.minecraft.client.model.monster.strider.BabyStriderModel;
import net.minecraft.client.model.monster.strider.StriderModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Strider;

@Environment(value=EnvType.CLIENT)
public class StriderRenderer
extends AgeableMobRenderer<Strider, StriderRenderState, StriderModel> {
    private static final Identifier STRIDER_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider.png");
    private static final Identifier STRIDER_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider_baby.png");
    private static final Identifier COLD_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider_cold.png");
    private static final Identifier COLD_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider_cold_baby.png");
    private static final float SHADOW_RADIUS = 0.5f;

    public StriderRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultStriderModel(context.bakeLayer(ModelLayers.STRIDER)), new BabyStriderModel(context.bakeLayer(ModelLayers.STRIDER_BABY)), 0.5f);
        this.addLayer(new SimpleEquipmentLayer<StriderRenderState, StriderModel, Object>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.STRIDER_SADDLE, state -> state.saddle, new AdultStriderModel(context.bakeLayer(ModelLayers.STRIDER_SADDLE)), null));
    }

    @Override
    public Identifier getTextureLocation(StriderRenderState state) {
        if (state.isSuffocating) {
            return state.isBaby ? COLD_BABY_LOCATION : COLD_LOCATION;
        }
        return state.isBaby ? STRIDER_BABY_LOCATION : STRIDER_LOCATION;
    }

    @Override
    protected float getShadowRadius(StriderRenderState state) {
        float radius = super.getShadowRadius(state);
        if (state.isBaby) {
            return radius * 0.5f;
        }
        return radius;
    }

    @Override
    public StriderRenderState createRenderState() {
        return new StriderRenderState();
    }

    @Override
    public void extractRenderState(Strider entity, StriderRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
        state.isSuffocating = entity.isSuffocating();
        state.isRidden = entity.isVehicle();
    }

    @Override
    protected boolean isShaking(StriderRenderState state) {
        return super.isShaking(state) || state.isSuffocating;
    }
}

