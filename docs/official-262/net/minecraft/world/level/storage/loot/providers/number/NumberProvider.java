/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface NumberProvider
extends LootContextUser {
    public float getFloat(LootContext var1);

    default public int getInt(LootContext context) {
        return Math.round(this.getFloat(context));
    }

    public MapCodec<? extends NumberProvider> codec();
}

