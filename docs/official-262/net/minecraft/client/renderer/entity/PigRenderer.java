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
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.pig.BabyPigModel;
import net.minecraft.client.model.animal.pig.ColdPigModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariant;

@Environment(value=EnvType.CLIENT)
public class PigRenderer
extends MobRenderer<Pig, PigRenderState, PigModel> {
    private final Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> models;

    public PigRenderer(EntityRendererProvider.Context context) {
        super(context, new PigModel(context.bakeLayer(ModelLayers.PIG)), 0.7f);
        this.models = PigRenderer.bakeModels(context);
        this.addLayer(new SimpleEquipmentLayer<PigRenderState, PigModel, Object>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.PIG_SADDLE, state -> state.saddle, new PigModel(context.bakeLayer(ModelLayers.PIG_SADDLE)), null));
    }

    private static Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(Map.of(PigVariant.ModelType.NORMAL, new AdultAndBabyModelPair<BabyPigModel>((BabyPigModel)new PigModel(context.bakeLayer(ModelLayers.PIG)), new BabyPigModel(context.bakeLayer(ModelLayers.PIG_BABY))), PigVariant.ModelType.COLD, new AdultAndBabyModelPair<BabyPigModel>((BabyPigModel)((Object)new ColdPigModel(context.bakeLayer(ModelLayers.COLD_PIG))), new BabyPigModel(context.bakeLayer(ModelLayers.PIG_BABY)))));
    }

    @Override
    public void submit(PigRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.variant == null) {
            return;
        }
        this.model = this.models.get(state.variant.modelAndTexture().model()).getModel(state.isBaby);
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public Identifier getTextureLocation(PigRenderState state) {
        return state.variant == null ? MissingTextureAtlasSprite.getLocation() : (state.isBaby ? state.variant.babyTexture().texturePath() : state.variant.modelAndTexture().asset().texturePath());
    }

    @Override
    public PigRenderState createRenderState() {
        return new PigRenderState();
    }

    @Override
    public void extractRenderState(Pig entity, PigRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
        state.variant = entity.getVariant().value();
    }
}

