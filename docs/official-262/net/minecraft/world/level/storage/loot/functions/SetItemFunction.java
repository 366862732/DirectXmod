/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetItemFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetItemFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetItemFunction.commonFields(i).and((App)Item.CODEC.fieldOf("item").forGetter(f -> f.item)).apply((Applicative)i, SetItemFunction::new));
    private final Holder<Item> item;

    private SetItemFunction(List<LootItemCondition> predicates, Holder<Item> item) {
        super(predicates);
        this.item = item;
    }

    public MapCodec<SetItemFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        return itemStack.transmuteCopy(this.item.value());
    }
}

