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
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.HorseMarkingLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Variant;

@Environment(value=EnvType.CLIENT)
public final class HorseRenderer
extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {
    private static final Map<Variant, HorseTextures> LOCATION_BY_VARIANT = Maps.newEnumMap(Map.of(Variant.WHITE, new HorseTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_white.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_white_baby.png")), Variant.CREAMY, new HorseTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_creamy.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_creamy_baby.png")), Variant.CHESTNUT, new HorseTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_chestnut.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_chestnut_baby.png")), Variant.BROWN, new HorseTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_brown.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_brown_baby.png")), Variant.BLACK, new HorseTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_black.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_black_baby.png")), Variant.GRAY, new HorseTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_gray.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_gray_baby.png")), Variant.DARK_BROWN, new HorseTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_darkbrown.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_darkbrown_baby.png"))));

    public HorseRenderer(EntityRendererProvider.Context context) {
        super(context, new HorseModel(context.bakeLayer(ModelLayers.HORSE)), new BabyHorseModel(context.bakeLayer(ModelLayers.HORSE_BABY)));
        this.addLayer(new HorseMarkingLayer(this));
        this.addLayer(new SimpleEquipmentLayer<HorseRenderState, HorseModel, Object>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY, state -> state.bodyArmorItem, new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)), null, 2));
        this.addLayer(new SimpleEquipmentLayer<HorseRenderState, HorseModel, Object>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_SADDLE, state -> state.saddle, new EquineSaddleModel(context.bakeLayer(ModelLayers.HORSE_SADDLE)), null, 2));
    }

    @Override
    public Identifier getTextureLocation(HorseRenderState state) {
        HorseTextures variant = LOCATION_BY_VARIANT.get(state.variant);
        return state.isBaby ? variant.baby : variant.adult;
    }

    @Override
    public HorseRenderState createRenderState() {
        return new HorseRenderState();
    }

    @Override
    public void extractRenderState(Horse entity, HorseRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.getVariant();
        state.markings = entity.getMarkings();
        state.bodyArmorItem = entity.getBodyArmorItem().copy();
    }

    @Environment(value=EnvType.CLIENT)
    private record HorseTextures(Identifier adult, Identifier baby) {
    }
}

