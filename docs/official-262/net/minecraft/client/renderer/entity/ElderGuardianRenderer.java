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
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public class ElderGuardianRenderer
extends GuardianRenderer {
    public static final Identifier GUARDIAN_ELDER_LOCATION = Identifier.withDefaultNamespace("textures/entity/guardian/guardian_elder.png");

    public ElderGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, 1.2f, ModelLayers.ELDER_GUARDIAN);
    }

    @Override
    public Identifier getTextureLocation(GuardianRenderState state) {
        return GUARDIAN_ELDER_LOCATION;
    }
}

