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

public record ScaleExponentially(LevelBasedValue base, LevelBasedValue exponent) implements EnchantmentValueEffect
{
    public static final MapCodec<ScaleExponentially> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LevelBasedValue.CODEC.fieldOf("base").forGetter(ScaleExponentially::base), (App)LevelBasedValue.CODEC.fieldOf("exponent").forGetter(ScaleExponentially::exponent)).apply((Applicative)i, ScaleExponentially::new));

    @Override
    public float process(int level, RandomSource random, float inputValue) {
        return (float)((double)inputValue * Math.pow(this.base.calculate(level), this.exponent.calculate(level)));
    }

    public MapCodec<ScaleExponentially> codec() {
        return CODEC;
    }
}

