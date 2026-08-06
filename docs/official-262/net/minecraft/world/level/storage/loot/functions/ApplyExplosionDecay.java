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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyExplosionDecay
extends LootItemConditionalFunction {
    public static final MapCodec<ApplyExplosionDecay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> ApplyExplosionDecay.commonFields(i).apply((Applicative)i, ApplyExplosionDecay::new));

    private ApplyExplosionDecay(List<LootItemCondition> predicates) {
        super(predicates);
    }

    public MapCodec<ApplyExplosionDecay> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        Float explosionRadius = context.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (explosionRadius != null) {
            RandomSource random = context.getRandom();
            float probability = 1.0f / explosionRadius.floatValue();
            int currentCount = itemStack.getCount();
            int resultCount = 0;
            for (int i = 0; i < currentCount; ++i) {
                if (!(random.nextFloat() <= probability)) continue;
                ++resultCount;
            }
            itemStack.setCount(resultCount);
        }
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> explosionDecay() {
        return ApplyExplosionDecay.simpleBuilder(ApplyExplosionDecay::new);
    }
}

