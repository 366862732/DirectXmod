/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.core.TypedInstance;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

public interface ItemInstance
extends TypedInstance<Item>,
DataComponentGetter {
    public static final String FIELD_ID = "id";
    public static final String FIELD_COUNT = "count";
    public static final String FIELD_COMPONENTS = "components";

    public int count();

    default public int getMaxStackSize() {
        return this.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }
}

