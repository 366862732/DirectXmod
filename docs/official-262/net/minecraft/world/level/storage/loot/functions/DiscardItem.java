/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DiscardItem
extends LootItemConditionalFunction {
    public static final MapCodec<DiscardItem> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> DiscardItem.commonFields(i).apply((Applicative)i, DiscardItem::new));

    protected DiscardItem(List<LootItemCondition> predicates) {
        super(predicates);
    }

    public MapCodec<DiscardItem> codec() {
        return MAP_CODEC;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        return ItemStack.EMPTY;
    }

    public static LootItemConditionalFunction.Builder<?> discardItem() {
        return DiscardItem.simpleBuilder(DiscardItem::new);
    }
}

