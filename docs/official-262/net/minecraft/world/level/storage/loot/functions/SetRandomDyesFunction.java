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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetRandomDyesFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetRandomDyesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetRandomDyesFunction.commonFields(i).and((App)NumberProviders.CODEC.fieldOf("number_of_dyes").forGetter(f -> f.numberOfDyes)).apply((Applicative)i, SetRandomDyesFunction::new));
    private final NumberProvider numberOfDyes;

    private SetRandomDyesFunction(List<LootItemCondition> predicates, NumberProvider numberOfDyes) {
        super(predicates);
        this.numberOfDyes = numberOfDyes;
    }

    public MapCodec<SetRandomDyesFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        RandomSource random = context.getRandom();
        int rolls = this.numberOfDyes.getInt(context);
        if (rolls <= 0) {
            return itemStack;
        }
        ArrayList<DyeColor> dyes = new ArrayList<DyeColor>(rolls);
        for (int i = 0; i < rolls; ++i) {
            dyes.add(Util.getRandom(DyeColor.VALUES, random));
        }
        return DyedItemColor.applyDyes(itemStack, dyes);
    }

    public static LootItemConditionalFunction.Builder<?> withCount(NumberProvider numberOfDyes) {
        return SetRandomDyesFunction.simpleBuilder(conditions -> new SetRandomDyesFunction((List<LootItemCondition>)conditions, numberOfDyes));
    }
}

