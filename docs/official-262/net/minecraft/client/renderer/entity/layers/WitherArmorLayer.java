/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class WitherArmorLayer
extends EnergySwirlLayer<WitherRenderState, WitherBossModel> {
    private static final Identifier WITHER_ARMOR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_armor.png");
    private final WitherBossModel model;

    public WitherArmorLayer(RenderLayerParent<WitherRenderState, WitherBossModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new WitherBossModel(modelSet.bakeLayer(ModelLayers.WITHER_ARMOR));
    }

    @Override
    protected boolean isPowered(WitherRenderState state) {
        return state.isPowered;
    }

    @Override
    protected float xOffset(float t) {
        return Mth.cos(t * 0.02f) * 3.0f;
    }

    @Override
    protected Identifier getTextureLocation() {
        return WITHER_ARMOR_LOCATION;
    }

    @Override
    protected WitherBossModel model() {
        return this.model;
    }
}

