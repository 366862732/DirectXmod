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
import net.minecraft.client.model.animal.llama.BabyLlamaModel;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

@Environment(value=EnvType.CLIENT)
public class LlamaDecorLayer
extends RenderLayer<LlamaRenderState, LlamaModel> {
    private final LlamaModel adultModel;
    private final LlamaModel babyModel;
    private final EquipmentLayerRenderer equipmentRenderer;

    public LlamaDecorLayer(RenderLayerParent<LlamaRenderState, LlamaModel> renderer, EntityModelSet modelSet, EquipmentLayerRenderer equipmentRenderer) {
        super(renderer);
        this.equipmentRenderer = equipmentRenderer;
        this.adultModel = new LlamaModel(modelSet.bakeLayer(ModelLayers.LLAMA_DECOR));
        this.babyModel = new BabyLlamaModel(modelSet.bakeLayer(ModelLayers.LLAMA_BABY_DECOR));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, LlamaRenderState state, float yRot, float xRot) {
        ItemStack itemStack = state.bodyItem;
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.assetId().isPresent() && !state.isBaby) {
            this.renderEquipment(poseStack, submitNodeCollector, state, itemStack, equippable.assetId().get(), lightCoords);
        } else if (state.isTraderLlama) {
            this.renderEquipment(poseStack, submitNodeCollector, state, ItemStack.EMPTY, state.isBaby ? EquipmentAssets.TRADER_LLAMA_BABY : EquipmentAssets.TRADER_LLAMA, lightCoords);
        }
    }

    private void renderEquipment(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LlamaRenderState state, ItemStack itemStack, ResourceKey<EquipmentAsset> equipmentAssetId, int lightCoords) {
        LlamaModel model = state.isBaby ? this.babyModel : this.adultModel;
        this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, equipmentAssetId, model, state, itemStack, poseStack, submitNodeCollector, lightCoords, state.outlineColor);
    }
}

