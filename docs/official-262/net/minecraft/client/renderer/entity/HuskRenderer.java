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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public class HuskRenderer
extends ZombieRenderer {
    private static final Identifier HUSK_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/husk.png");
    private static final Identifier BABY_HUSK_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/husk_baby.png");

    public HuskRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.HUSK, ModelLayers.HUSK_BABY, ModelLayers.HUSK_ARMOR, ModelLayers.HUSK_BABY_ARMOR);
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return state.isBaby ? BABY_HUSK_LOCATION : HUSK_LOCATION;
    }
}

