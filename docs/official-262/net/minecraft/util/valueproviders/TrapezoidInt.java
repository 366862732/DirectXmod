/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.util.valueproviders;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

public record TrapezoidInt(int minInclusive, int maxInclusive, int plateau) implements IntProvider
{
    public static final MapCodec<TrapezoidInt> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.INT.fieldOf("min").forGetter(TrapezoidInt::minInclusive), (App)Codec.INT.fieldOf("max").forGetter(TrapezoidInt::maxInclusive), (App)Codec.INT.fieldOf("plateau").forGetter(TrapezoidInt::plateau)).apply((Applicative)i, TrapezoidInt::new)).validate(c -> {
        if (c.maxInclusive < c.minInclusive) {
            return DataResult.error(() -> "Max must be larger than min: [" + c.minInclusive + ", " + c.maxInclusive + "]");
        }
        if (c.plateau > c.maxInclusive - c.minInclusive) {
            return DataResult.error(() -> "Plateau can at most be the full span: [" + c.minInclusive + ", " + c.maxInclusive + "]");
        }
        return DataResult.success((Object)c);
    });

    public static TrapezoidInt of(int min, int max, int plateau) {
        return new TrapezoidInt(min, max, plateau);
    }

    public static IntProvider triangle(int range) {
        return TrapezoidInt.of(-range, range, 0);
    }

    @Override
    public int sample(RandomSource random) {
        if (this.plateau == 0 && this.maxInclusive == -this.minInclusive) {
            return random.nextInt(this.maxInclusive + 1) - random.nextInt(this.maxInclusive + 1);
        }
        int range = this.maxInclusive - this.minInclusive;
        if (this.plateau == range) {
            return Mth.randomBetweenInclusive(random, this.minInclusive, this.maxInclusive);
        }
        int plateauStart = (range - this.plateau) / 2;
        int plateauEnd = range - plateauStart;
        return this.minInclusive + Mth.randomBetweenInclusive(random, 0, plateauEnd) + Mth.randomBetweenInclusive(random, 0, plateauStart);
    }

    public MapCodec<TrapezoidInt> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "trapezoid(" + this.plateau + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}

