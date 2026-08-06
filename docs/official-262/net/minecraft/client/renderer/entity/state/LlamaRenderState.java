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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class LlamaRenderState
extends LivingEntityRenderState {
    public Llama.Variant variant = Llama.Variant.DEFAULT;
    public boolean hasChest;
    public ItemStack bodyItem = ItemStack.EMPTY;
    public boolean isTraderLlama;
}

