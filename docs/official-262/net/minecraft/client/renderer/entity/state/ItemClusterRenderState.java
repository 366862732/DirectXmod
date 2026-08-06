/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class ItemClusterRenderState
extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public int count;
    public int seed;

    public void extractItemGroupRenderState(Entity entity, ItemStack stack, ItemModelResolver itemModelResolver) {
        itemModelResolver.updateForNonLiving(this.item, stack, ItemDisplayContext.GROUND, entity);
        this.count = ItemClusterRenderState.getRenderedAmount(stack.getCount());
        this.seed = ItemClusterRenderState.getSeedForItemStack(stack);
    }

    public static int getSeedForItemStack(ItemStack itemStack) {
        return itemStack.isEmpty() ? 187 : Item.getId(itemStack.getItem()) + itemStack.getDamageValue();
    }

    public static int getRenderedAmount(int stackCount) {
        if (stackCount <= 1) {
            return 1;
        }
        if (stackCount <= 16) {
            return 2;
        }
        if (stackCount <= 32) {
            return 3;
        }
        if (stackCount <= 48) {
            return 4;
        }
        return 5;
    }
}

