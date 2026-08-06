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

public record UniformFloat(float min, float max) implements FloatProvider
{
    public static final MapCodec<UniformFloat> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("min_inclusive").forGetter(UniformFloat::min), (App)Codec.FLOAT.fieldOf("max_exclusive").forGetter(UniformFloat::max)).apply((Applicative)i, UniformFloat::new)).validate(u -> {
        if (u.max <= u.min) {
            return DataResult.error(() -> "Max must be larger than min, min: " + u.min + ", max: " + u.max);
        }
        return DataResult.success((Object)u);
    });

    public static UniformFloat of(float min, float max) {
        if (max <= min) {
            throw new IllegalArgumentException("Max must exceed min");
        }
        return new UniformFloat(min, max);
    }

    @Override
    public float sample(RandomSource random) {
        return Mth.randomBetween(random, this.min, this.max);
    }

    public MapCodec<UniformFloat> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "[" + this.min + "-" + this.max + "]";
    }
}

