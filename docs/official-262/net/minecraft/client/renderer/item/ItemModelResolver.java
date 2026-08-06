/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ItemModelResolver {
    private final ModelManager modelManager;

    public ItemModelResolver(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    private ClientItem.Properties getItemProperties(Identifier modelId) {
        return this.modelManager.getItemProperties(modelId);
    }

    private ItemModel getItemModel(Identifier modelId) {
        return this.modelManager.getItemModel(modelId);
    }

    public void updateForLiving(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, LivingEntity entity) {
        this.updateForTopItem(output, item, displayContext, entity.level(), entity, entity.getId() + displayContext.ordinal());
    }

    public void updateForNonLiving(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, Entity entity) {
        this.updateForTopItem(output, item, displayContext, entity.level(), entity, entity.getId());
    }

    public void updateForTopItem(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, @Nullable Level level, @Nullable ItemOwner owner, int seed) {
        output.clear();
        if (!item.isEmpty()) {
            output.displayContext = displayContext;
            this.appendItemLayers(output, item, displayContext, level, owner, seed);
        }
    }

    public void appendItemLayers(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, @Nullable Level level, @Nullable ItemOwner owner, int seed) {
        ClientLevel clientLevel;
        Identifier modelId = item.get(DataComponents.ITEM_MODEL);
        if (modelId == null) {
            return;
        }
        output.setOversizedInGui(this.getItemProperties(modelId).oversizedInGui());
        this.getItemModel(modelId).update(output, item, this, displayContext, level instanceof ClientLevel ? (clientLevel = (ClientLevel)level) : null, owner, seed);
    }

    public boolean shouldPlaySwapAnimation(ItemStack stack) {
        Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
        if (modelId == null) {
            return true;
        }
        return this.getItemProperties(modelId).handAnimationOnSwap();
    }

    public float swapAnimationScale(ItemStack stack) {
        Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
        if (modelId == null) {
            return 1.0f;
        }
        return this.getItemProperties(modelId).swapAnimationScale();
    }
}

