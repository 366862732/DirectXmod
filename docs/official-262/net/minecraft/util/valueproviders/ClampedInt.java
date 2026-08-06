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
import net.minecraft.util.valueproviders.IntProviders;

public record ClampedInt(IntProvider source, int minInclusive, int maxInclusive) implements IntProvider
{
    private final int minInclusive;
    private final int maxInclusive;
    public static final MapCodec<ClampedInt> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)IntProviders.CODEC.fieldOf("source").forGetter(ClampedInt::source), (App)Codec.INT.fieldOf("min_inclusive").forGetter(ClampedInt::minInclusive), (App)Codec.INT.fieldOf("max_inclusive").forGetter(ClampedInt::maxInclusive)).apply((Applicative)i, ClampedInt::new)).validate(u -> {
        if (u.maxInclusive < u.minInclusive) {
            return DataResult.error(() -> "Max must be at least min, min_inclusive: " + u.minInclusive + ", max_inclusive: " + u.maxInclusive);
        }
        return DataResult.success((Object)u);
    });

    public static ClampedInt of(IntProvider source, int minInclusive, int maxInclusive) {
        return new ClampedInt(source, minInclusive, maxInclusive);
    }

    @Override
    public int sample(RandomSource random) {
        return Mth.clamp(this.source.sample(random), this.minInclusive, this.maxInclusive);
    }

    @Override
    public int minInclusive() {
        return Math.max(this.minInclusive, this.source.minInclusive());
    }

    @Override
    public int maxInclusive() {
        return Math.min(this.maxInclusive, this.source.maxInclusive());
    }

    public MapCodec<ClampedInt> codec() {
        return MAP_CODEC;
    }
}

