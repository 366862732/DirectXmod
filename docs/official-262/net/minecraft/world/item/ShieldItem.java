/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ShieldItem
extends Item {
    public ShieldItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        DyeColor baseColor = itemStack.get(DataComponents.BASE_COLOR);
        if (baseColor != null) {
            return Component.translatable(this.descriptionId + "." + baseColor.getName());
        }
        return super.getName(itemStack);
    }
}

