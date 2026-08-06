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
import net.minecraft.client.model.animal.golem.SnowGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SnowGolemHeadLayer;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.level.block.Blocks;

@Environment(value=EnvType.CLIENT)
public class SnowGolemRenderer
extends MobRenderer<SnowGolem, SnowGolemRenderState, SnowGolemModel> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Identifier SNOW_GOLEM_LOCATION = Identifier.withDefaultNamespace("textures/entity/snow_golem/snow_golem.png");
    private final BlockModelResolver blockModelResolver;

    public SnowGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new SnowGolemModel(context.bakeLayer(ModelLayers.SNOW_GOLEM)), 0.5f);
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new SnowGolemHeadLayer(this));
    }

    @Override
    public Identifier getTextureLocation(SnowGolemRenderState state) {
        return SNOW_GOLEM_LOCATION;
    }

    @Override
    public SnowGolemRenderState createRenderState() {
        return new SnowGolemRenderState();
    }

    @Override
    public void extractRenderState(SnowGolem entity, SnowGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.hasPumpkin()) {
            this.blockModelResolver.update(state.headBlock, Blocks.CARVED_PUMPKIN.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
        } else {
            state.headBlock.clear();
        }
    }
}

