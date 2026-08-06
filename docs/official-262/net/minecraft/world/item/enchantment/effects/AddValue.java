/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.enchantment.effects;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

public record AddValue(LevelBasedValue value) implements EnchantmentValueEffect
{
    public static final MapCodec<AddValue> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LevelBasedValue.CODEC.fieldOf("value").forGetter(AddValue::value)).apply((Applicative)i, AddValue::new));

    @Override
    public float process(int enchantmentLevel, RandomSource random, float inputValue) {
        return inputValue + this.value.calculate(enchantmentLevel);
    }

    public MapCodec<AddValue> codec() {
        return CODEC;
    }
}

