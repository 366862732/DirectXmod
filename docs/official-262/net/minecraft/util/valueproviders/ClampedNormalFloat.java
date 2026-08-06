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
import net.minecraft.util.valueproviders.FloatProvider;

public record ClampedNormalFloat(float mean, float deviation, float min, float max) implements FloatProvider
{
    public static final MapCodec<ClampedNormalFloat> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("mean").forGetter(ClampedNormalFloat::mean), (App)Codec.FLOAT.fieldOf("deviation").forGetter(ClampedNormalFloat::deviation), (App)Codec.FLOAT.fieldOf("min").forGetter(ClampedNormalFloat::min), (App)Codec.FLOAT.fieldOf("max").forGetter(ClampedNormalFloat::max)).apply((Applicative)i, ClampedNormalFloat::new)).validate(c -> {
        if (c.max < c.min) {
            return DataResult.error(() -> "Max must be larger than min: [" + c.min + ", " + c.max + "]");
        }
        return DataResult.success((Object)c);
    });

    public static ClampedNormalFloat of(float mean, float deviation, float min, float max) {
        return new ClampedNormalFloat(mean, deviation, min, max);
    }

    @Override
    public float sample(RandomSource random) {
        return ClampedNormalFloat.sample(random, this.mean, this.deviation, this.min, this.max);
    }

    public static float sample(RandomSource random, float mean, float deviation, float min, float max) {
        return Mth.clamp(Mth.normal(random, mean, deviation), min, max);
    }

    public MapCodec<ClampedNormalFloat> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.min + "-" + this.max + "]";
    }
}

