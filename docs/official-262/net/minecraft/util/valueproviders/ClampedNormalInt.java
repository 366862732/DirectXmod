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

public record ClampedNormalInt(float mean, float deviation, int minInclusive, int maxInclusive) implements IntProvider
{
    public static final MapCodec<ClampedNormalInt> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("mean").forGetter(ClampedNormalInt::mean), (App)Codec.FLOAT.fieldOf("deviation").forGetter(ClampedNormalInt::deviation), (App)Codec.INT.fieldOf("min_inclusive").forGetter(ClampedNormalInt::minInclusive), (App)Codec.INT.fieldOf("max_inclusive").forGetter(ClampedNormalInt::maxInclusive)).apply((Applicative)i, ClampedNormalInt::new)).validate(c -> {
        if (c.maxInclusive < c.minInclusive) {
            return DataResult.error(() -> "Max must be larger than min: [" + c.minInclusive + ", " + c.maxInclusive + "]");
        }
        return DataResult.success((Object)c);
    });

    public static ClampedNormalInt of(float mean, float deviation, int minInclusive, int maxInclusive) {
        return new ClampedNormalInt(mean, deviation, minInclusive, maxInclusive);
    }

    @Override
    public int sample(RandomSource random) {
        return ClampedNormalInt.sample(random, this.mean, this.deviation, this.minInclusive, this.maxInclusive);
    }

    public static int sample(RandomSource random, float mean, float deviation, float minInclusive, float maxInclusive) {
        return (int)Mth.clamp(Mth.normal(random, mean, deviation), minInclusive, maxInclusive);
    }

    public MapCodec<ClampedNormalInt> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}

