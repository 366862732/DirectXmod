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
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

public record BiasedToBottomInt(int minInclusive, int maxInclusive) implements IntProvider
{
    public static final MapCodec<BiasedToBottomInt> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.INT.fieldOf("min_inclusive").forGetter(BiasedToBottomInt::minInclusive), (App)Codec.INT.fieldOf("max_inclusive").forGetter(BiasedToBottomInt::maxInclusive)).apply((Applicative)i, BiasedToBottomInt::new)).validate(u -> {
        if (u.maxInclusive < u.minInclusive) {
            return DataResult.error(() -> "Max must be at least min, min_inclusive: " + u.minInclusive + ", max_inclusive: " + u.maxInclusive);
        }
        return DataResult.success((Object)u);
    });

    public static BiasedToBottomInt of(int minInclusive, int maxInclusive) {
        return new BiasedToBottomInt(minInclusive, maxInclusive);
    }

    @Override
    public int sample(RandomSource random) {
        return this.minInclusive + random.nextInt(random.nextInt(this.maxInclusive - this.minInclusive + 1) + 1);
    }

    public MapCodec<BiasedToBottomInt> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}

