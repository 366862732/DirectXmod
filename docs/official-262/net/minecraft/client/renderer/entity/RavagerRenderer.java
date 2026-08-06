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
import net.minecraft.client.model.monster.ravager.RavagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Ravager;

@Environment(value=EnvType.CLIENT)
public class RavagerRenderer
extends MobRenderer<Ravager, RavagerRenderState, RavagerModel> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/illager/ravager.png");

    public RavagerRenderer(EntityRendererProvider.Context context) {
        super(context, new RavagerModel(context.bakeLayer(ModelLayers.RAVAGER)), 1.1f);
    }

    @Override
    public Identifier getTextureLocation(RavagerRenderState state) {
        return TEXTURE_LOCATION;
    }

    @Override
    public RavagerRenderState createRenderState() {
        return new RavagerRenderState();
    }

    @Override
    public void extractRenderState(Ravager entity, RavagerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.stunnedTicksRemaining = (float)entity.getStunnedTick() > 0.0f ? (float)entity.getStunnedTick() - partialTicks : 0.0f;
        state.attackTicksRemaining = (float)entity.getAttackTick() > 0.0f ? (float)entity.getAttackTick() - partialTicks : 0.0f;
        state.roarAnimation = entity.getRoarTick() > 0 ? ((float)(20 - entity.getRoarTick()) + partialTicks) / 20.0f : 0.0f;
    }
}

