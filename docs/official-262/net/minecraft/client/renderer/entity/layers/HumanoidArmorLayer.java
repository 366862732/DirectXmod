/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

@Environment(value=EnvType.CLIENT)
public class HumanoidArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>>
extends RenderLayer<S, M> {
    private final ArmorModelSet<A> modelSet;
    private final ArmorModelSet<A> babyModelSet;
    private final EquipmentLayerRenderer equipmentRenderer;

    public HumanoidArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet, EquipmentLayerRenderer equipmentRenderer) {
        this(renderer, modelSet, modelSet, equipmentRenderer);
    }

    public HumanoidArmorLayer(RenderLayerParent<S, M> renderer, ArmorModelSet<A> modelSet, ArmorModelSet<A> babyModelSet, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.modelSet = modelSet;
        this.babyModelSet = babyModelSet;
        this.equipmentRenderer = equipmentRenderer;
    }

    public static boolean shouldRender(ItemStack itemStack, EquipmentSlot slot) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        return equippable != null && HumanoidArmorLayer.shouldRender(equippable, slot);
    }

    private static boolean shouldRender(Equippable equippable, EquipmentSlot slot) {
        return equippable.assetId().isPresent() && equippable.slot() == slot;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        this.renderArmorPiece(poseStack, submitNodeCollector, ((HumanoidRenderState)state).chestEquipment, EquipmentSlot.CHEST, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, ((HumanoidRenderState)state).legsEquipment, EquipmentSlot.LEGS, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, ((HumanoidRenderState)state).feetEquipment, EquipmentSlot.FEET, lightCoords, state);
        this.renderArmorPiece(poseStack, submitNodeCollector, ((HumanoidRenderState)state).headEquipment, EquipmentSlot.HEAD, lightCoords, state);
    }

    private void renderArmorPiece(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int lightCoords, S state) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || !HumanoidArmorLayer.shouldRender(equippable, slot)) {
            return;
        }
        A model = this.getArmorModel(state, slot);
        EquipmentClientInfo.LayerType layerType = ((HumanoidRenderState)state).isBaby && ((HumanoidRenderState)state).entityType != EntityTypes.ARMOR_STAND ? EquipmentClientInfo.LayerType.HUMANOID_BABY : (this.usesInnerModel(slot) ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID);
        this.equipmentRenderer.renderLayers(layerType, equippable.assetId().orElseThrow(), model, state, itemStack, poseStack, submitNodeCollector, lightCoords, ((HumanoidRenderState)state).outlineColor);
    }

    private A getArmorModel(S state, EquipmentSlot slot) {
        return (A)((HumanoidModel)(((HumanoidRenderState)state).isBaby ? this.babyModelSet : this.modelSet).get(slot));
    }

    private boolean usesInnerModel(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS;
    }
}

