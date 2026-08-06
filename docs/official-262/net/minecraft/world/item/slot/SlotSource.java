/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface SlotSource
extends LootContextUser {
    public MapCodec<? extends SlotSource> codec();

    public SlotCollection provide(LootContext var1);
}

