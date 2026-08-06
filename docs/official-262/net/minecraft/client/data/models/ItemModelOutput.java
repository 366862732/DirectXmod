/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;

@Environment(value=EnvType.CLIENT)
public interface ItemModelOutput {
    default public void accept(Item item, ItemModel.Unbaked generator) {
        this.accept(item, generator, ClientItem.Properties.DEFAULT);
    }

    public void accept(Item var1, ItemModel.Unbaked var2, ClientItem.Properties var3);

    public void copy(Item var1, Item var2);
}

