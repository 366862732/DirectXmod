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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.RaftModel;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

@Environment(value=EnvType.CLIENT)
public class RaftRenderer
extends AbstractBoatRenderer {
    private final EntityModel<BoatRenderState> model;

    public RaftRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelId) {
        super(context, modelId.model().withPath(p -> "textures/entity/" + p + ".png"));
        this.model = new RaftModel(context.bakeLayer(modelId));
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return this.model;
    }
}

