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
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SquidRenderer;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.squid.GlowSquid;

@Environment(value=EnvType.CLIENT)
public class GlowSquidRenderer
extends SquidRenderer<GlowSquid> {
    private static final Identifier GLOW_SQUID_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/glow_squid.png");
    private static final Identifier GLOW_SQUID_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/glow_squid_baby.png");

    public GlowSquidRenderer(EntityRendererProvider.Context context, SquidModel model, SquidModel babyModel) {
        super(context, model, babyModel);
    }

    @Override
    public Identifier getTextureLocation(SquidRenderState state) {
        return state.isBaby ? GLOW_SQUID_BABY_LOCATION : GLOW_SQUID_LOCATION;
    }

    @Override
    protected int getBlockLightLevel(GlowSquid entity, BlockPos blockPos) {
        int glowLightLevel = (int)Mth.clampedLerp(1.0f - (float)entity.getDarkTicksRemaining() / 10.0f, 0.0f, 15.0f);
        if (glowLightLevel == 15) {
            return 15;
        }
        return Math.max(glowLightLevel, super.getBlockLightLevel(entity, blockPos));
    }
}

