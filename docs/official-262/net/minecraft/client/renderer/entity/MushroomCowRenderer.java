/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.cow.BabyCowModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.cow.MushroomCow;

@Environment(value=EnvType.CLIENT)
public class MushroomCowRenderer
extends AgeableMobRenderer<MushroomCow, MushroomCowRenderState, CowModel> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Map<MushroomCow.Variant, MushroomCowTexture> TEXTURES = Util.make(Maps.newHashMap(), map -> {
        map.put(MushroomCow.Variant.BROWN, new MushroomCowTexture(Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_brown.png"), Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_brown_baby.png")));
        map.put(MushroomCow.Variant.RED, new MushroomCowTexture(Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_red.png"), Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_red_baby.png")));
    });
    private final BlockModelResolver blockModelResolver;

    public MushroomCowRenderer(EntityRendererProvider.Context context) {
        super(context, new CowModel(context.bakeLayer(ModelLayers.MOOSHROOM)), new BabyCowModel(context.bakeLayer(ModelLayers.MOOSHROOM_BABY)), 0.7f);
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new MushroomCowMushroomLayer(this));
    }

    @Override
    public Identifier getTextureLocation(MushroomCowRenderState state) {
        return state.isBaby ? MushroomCowRenderer.TEXTURES.get((Object)state.variant).baby : MushroomCowRenderer.TEXTURES.get((Object)state.variant).adult;
    }

    @Override
    public MushroomCowRenderState createRenderState() {
        return new MushroomCowRenderState();
    }

    @Override
    public void extractRenderState(MushroomCow entity, MushroomCowRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.getVariant();
        this.blockModelResolver.update(state.mushroomModel, state.variant.getBlockState(), BLOCK_DISPLAY_CONTEXT);
    }

    @Environment(value=EnvType.CLIENT)
    private record MushroomCowTexture(Identifier adult, Identifier baby) {
    }
}

