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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LimitCount
extends LootItemConditionalFunction {
    public static final MapCodec<LimitCount> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> LimitCount.commonFields(i).and((App)IntRange.CODEC.fieldOf("limit").forGetter(f -> f.limit)).apply((Applicative)i, LimitCount::new));
    private final IntRange limit;

    private LimitCount(List<LootItemCondition> predicates, IntRange limit) {
        super(predicates);
        this.limit = limit;
    }

    public MapCodec<LimitCount> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "limit", this.limit);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        int newCount = this.limit.clamp(context, itemStack.getCount());
        itemStack.setCount(newCount);
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> limitCount(IntRange limit) {
        return LimitCount.simpleBuilder(conditions -> new LimitCount((List<LootItemCondition>)conditions, limit));
    }
}

