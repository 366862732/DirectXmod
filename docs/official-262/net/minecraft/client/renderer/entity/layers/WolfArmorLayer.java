/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

@Environment(value=EnvType.CLIENT)
public class WolfArmorLayer
extends RenderLayer<WolfRenderState, WolfModel> {
    private final WolfModel adultModel;
    private final EquipmentLayerRenderer equipmentRenderer;
    private static final Map<Crackiness.Level, Identifier> ARMOR_CRACK_LOCATIONS = Map.of(Crackiness.Level.LOW, Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"), Crackiness.Level.MEDIUM, Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"), Crackiness.Level.HIGH, Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png"));

    public WolfArmorLayer(RenderLayerParent<WolfRenderState, WolfModel> renderer, EntityModelSet modelSet, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.adultModel = new AdultWolfModel(modelSet.bakeLayer(ModelLayers.WOLF_ARMOR));
        this.equipmentRenderer = equipmentRenderer;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, WolfRenderState state, float yRot, float xRot) {
        ItemStack armorItem = state.bodyArmorItem;
        Equippable equippable = armorItem.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty() || state.isBaby) {
            return;
        }
        this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.WOLF_BODY, equippable.assetId().get(), this.adultModel, state, armorItem, poseStack, submitNodeCollector, lightCoords, state.outlineColor);
        this.maybeRenderCracks(poseStack, submitNodeCollector, lightCoords, armorItem, this.adultModel, state);
    }

    private void maybeRenderCracks(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemStack armorItem, Model<WolfRenderState> model, WolfRenderState state) {
        Crackiness.Level crackiness = Crackiness.WOLF_ARMOR.byDamage(armorItem);
        if (crackiness == Crackiness.Level.NONE) {
            return;
        }
        Identifier damageTexture = ARMOR_CRACK_LOCATIONS.get((Object)crackiness);
        submitNodeCollector.submitModel(model, state, poseStack, RenderTypes.armorTranslucent(damageTexture), lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }
}

