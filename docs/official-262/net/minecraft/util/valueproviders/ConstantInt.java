/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.util.valueproviders;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

public record ConstantInt(int value) implements IntProvider
{
    public static final ConstantInt ZERO = new ConstantInt(0);
    public static final MapCodec<ConstantInt> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.INT.fieldOf("value").forGetter(ConstantInt::value)).apply((Applicative)i, ConstantInt::of));

    public static ConstantInt of(int value) {
        if (value == 0) {
            return ZERO;
        }
        return new ConstantInt(value);
    }

    @Override
    public int sample(RandomSource random) {
        return this.value;
    }

    @Override
    public int minInclusive() {
        return this.value;
    }

    @Override
    public int maxInclusive() {
        return this.value;
    }

    public MapCodec<ConstantInt> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return Integer.toString(this.value);
    }
}

