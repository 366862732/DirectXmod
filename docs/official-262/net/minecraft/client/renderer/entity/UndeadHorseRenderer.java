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
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

@Environment(value=EnvType.CLIENT)
public class UndeadHorseRenderer
extends AbstractHorseRenderer<AbstractHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
    private final Identifier adultTexture;
    private final Identifier babyTexture;

    public UndeadHorseRenderer(EntityRendererProvider.Context context, EquipmentClientInfo.LayerType saddleLayer, ModelLayerLocation saddleModel, Type adult, Type baby) {
        super(context, new HorseModel(context.bakeLayer(adult.model)), new BabyHorseModel(context.bakeLayer(baby.model)));
        this.adultTexture = adult.texture;
        this.babyTexture = baby.texture;
        this.addLayer(new SimpleEquipmentLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>, Object>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY, state -> state.bodyArmorItem, new HorseModel(context.bakeLayer(ModelLayers.UNDEAD_HORSE_ARMOR)), null));
        this.addLayer(new SimpleEquipmentLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>, Object>(this, context.getEquipmentRenderer(), saddleLayer, state -> state.saddle, new EquineSaddleModel(context.bakeLayer(saddleModel)), null));
    }

    @Override
    public Identifier getTextureLocation(EquineRenderState state) {
        return state.isBaby ? this.babyTexture : this.adultTexture;
    }

    @Override
    public EquineRenderState createRenderState() {
        return new EquineRenderState();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Type {
        SKELETON(Identifier.withDefaultNamespace("textures/entity/horse/horse_skeleton.png"), ModelLayers.SKELETON_HORSE),
        SKELETON_BABY(Identifier.withDefaultNamespace("textures/entity/horse/horse_skeleton_baby.png"), ModelLayers.SKELETON_HORSE_BABY),
        ZOMBIE(Identifier.withDefaultNamespace("textures/entity/horse/horse_zombie.png"), ModelLayers.ZOMBIE_HORSE),
        ZOMBIE_BABY(Identifier.withDefaultNamespace("textures/entity/horse/horse_zombie_baby.png"), ModelLayers.ZOMBIE_HORSE_BABY);

        private final Identifier texture;
        private final ModelLayerLocation model;

        private Type(Identifier texture, ModelLayerLocation model) {
            this.texture = texture;
            this.model = model;
        }
    }
}

