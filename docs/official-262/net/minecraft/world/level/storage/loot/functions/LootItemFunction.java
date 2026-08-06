/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface LootItemFunction
extends LootContextUser,
BiFunction<ItemStack, LootContext, ItemStack> {
    public MapCodec<? extends LootItemFunction> codec();

    public static Consumer<ItemStack> decorate(BiFunction<ItemStack, LootContext, ItemStack> function, Consumer<ItemStack> output, LootContext context) {
        return drop -> output.accept((ItemStack)function.apply((ItemStack)drop, context));
    }

    public static interface Builder {
        public LootItemFunction build();
    }
}

