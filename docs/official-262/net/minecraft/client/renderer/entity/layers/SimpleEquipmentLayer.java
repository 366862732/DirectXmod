/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SimpleEquipmentLayer<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
extends RenderLayer<S, RM> {
    private final EquipmentLayerRenderer equipmentRenderer;
    private final EquipmentClientInfo.LayerType layer;
    private final Function<S, ItemStack> itemGetter;
    private final EM adultModel;
    private final @Nullable EM babyModel;
    private final int order;

    public SimpleEquipmentLayer(RenderLayerParent<S, RM> renderer, EquipmentLayerRenderer equipmentRenderer, EquipmentClientInfo.LayerType layer, Function<S, ItemStack> itemGetter, EM adultModel, @Nullable EM babyModel, int order) {
        super(renderer);
        this.equipmentRenderer = equipmentRenderer;
        this.layer = layer;
        this.itemGetter = itemGetter;
        this.adultModel = adultModel;
        this.babyModel = babyModel;
        this.order = order;
    }

    public SimpleEquipmentLayer(RenderLayerParent<S, RM> renderer, EquipmentLayerRenderer equipmentRenderer, EquipmentClientInfo.LayerType layer, Function<S, ItemStack> itemGetter, EM adultModel, @Nullable EM babyModel) {
        this(renderer, equipmentRenderer, layer, itemGetter, adultModel, babyModel, 0);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        ItemStack equipment = this.itemGetter.apply(state);
        Equippable equippable = equipment.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty() || ((LivingEntityRenderState)state).isBaby && this.babyModel == null) {
            return;
        }
        EM model = ((LivingEntityRenderState)state).isBaby ? this.babyModel : this.adultModel;
        this.equipmentRenderer.renderLayers(this.layer, equippable.assetId().get(), model, state, equipment, poseStack, submitNodeCollector, lightCoords, (Identifier)null, ((LivingEntityRenderState)state).outlineColor, this.order);
    }
}

