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
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.TippableArrowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.Arrow;

@Environment(value=EnvType.CLIENT)
public class TippableArrowRenderer
extends ArrowRenderer<Arrow, TippableArrowRenderState> {
    public static final Identifier NORMAL_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    public static final Identifier TIPPED_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow_tipped.png");

    public TippableArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Identifier getTextureLocation(TippableArrowRenderState state) {
        return state.isTipped ? TIPPED_ARROW_LOCATION : NORMAL_ARROW_LOCATION;
    }

    @Override
    public TippableArrowRenderState createRenderState() {
        return new TippableArrowRenderState();
    }

    @Override
    public void extractRenderState(Arrow entity, TippableArrowRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isTipped = entity.getColor() > 0;
    }
}

