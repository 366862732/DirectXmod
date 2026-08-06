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
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class EndermanRenderer
extends MobRenderer<EnderMan, EndermanRenderState, EndermanModel<EndermanRenderState>> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Identifier ENDERMAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");
    private final RandomSource random = RandomSource.create();
    private final BlockModelResolver blockModelResolver;

    public EndermanRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5f);
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new EnderEyesLayer(this));
        this.addLayer(new CarriedBlockLayer(this));
    }

    @Override
    public Vec3 getRenderOffset(EndermanRenderState state) {
        Vec3 offset = super.getRenderOffset(state);
        if (state.isCreepy) {
            double d = 0.02 * (double)state.scale;
            return offset.add(this.random.nextGaussian() * d, 0.0, this.random.nextGaussian() * d);
        }
        return offset;
    }

    @Override
    public Identifier getTextureLocation(EndermanRenderState state) {
        return ENDERMAN_LOCATION;
    }

    @Override
    public EndermanRenderState createRenderState() {
        return new EndermanRenderState();
    }

    @Override
    public void extractRenderState(EnderMan entity, EndermanRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.isCreepy = entity.isCreepy();
        BlockState carriedBlock = entity.getCarriedBlock();
        if (carriedBlock != null) {
            this.blockModelResolver.update(state.carriedBlock, carriedBlock, BLOCK_DISPLAY_CONTEXT);
        } else {
            state.carriedBlock.clear();
        }
    }
}

